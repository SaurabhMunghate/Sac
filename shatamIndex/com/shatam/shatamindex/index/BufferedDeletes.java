/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.RamUsageEstimator;

class BufferedDeletes {

	final static int BYTES_PER_DEL_TERM = 8
			* RamUsageEstimator.NUM_BYTES_OBJECT_REF + 5
			* RamUsageEstimator.NUM_BYTES_OBJECT_HEADER + 6
			* RamUsageEstimator.NUM_BYTES_INT;

	final static int BYTES_PER_DEL_DOCID = 2
			* RamUsageEstimator.NUM_BYTES_OBJECT_REF
			+ RamUsageEstimator.NUM_BYTES_OBJECT_HEADER
			+ RamUsageEstimator.NUM_BYTES_INT;

	final static int BYTES_PER_DEL_QUERY = 5
			* RamUsageEstimator.NUM_BYTES_OBJECT_REF + 2
			* RamUsageEstimator.NUM_BYTES_OBJECT_HEADER + 2
			* RamUsageEstimator.NUM_BYTES_INT + 24;

	final AtomicInteger numTermDeletes = new AtomicInteger();
	final Map<Term, Integer> terms = new HashMap<Term, Integer>();
	final Map<Query, Integer> queries = new HashMap<Query, Integer>();
	final List<Integer> docIDs = new ArrayList<Integer>();

	public static final Integer MAX_INT = Integer.valueOf(Integer.MAX_VALUE);

	final AtomicLong bytesUsed = new AtomicLong();

	private final static boolean VERBOSE_DELETES = false;

	long gen;

	@Override
	public String toString() {
		if (VERBOSE_DELETES) {
			return "gen=" + gen + " numTerms=" + numTermDeletes + ", terms="
					+ terms + ", queries=" + queries + ", docIDs=" + docIDs
					+ ", bytesUsed=" + bytesUsed;
		} else {
			String s = "gen=" + gen;
			if (numTermDeletes.get() != 0) {
				s += " " + numTermDeletes.get()
						+ " deleted terms (unique count=" + terms.size() + ")";
			}
			if (queries.size() != 0) {
				s += " " + queries.size() + " deleted queries";
			}
			if (docIDs.size() != 0) {
				s += " " + docIDs.size() + " deleted docIDs";
			}
			if (bytesUsed.get() != 0) {
				s += " bytesUsed=" + bytesUsed.get();
			}

			return s;
		}
	}

	public void addQuery(Query query, int docIDUpto) {
		Integer current = queries.put(query, docIDUpto);

		if (current == null) {
			bytesUsed.addAndGet(BYTES_PER_DEL_QUERY);
		}
	}

	public void addDocID(int docID) {
		docIDs.add(Integer.valueOf(docID));
		bytesUsed.addAndGet(BYTES_PER_DEL_DOCID);
	}

	public void addTerm(Term term, int docIDUpto) {
		Integer current = terms.get(term);
		if (current != null && docIDUpto < current) {

			return;
		}

		terms.put(term, Integer.valueOf(docIDUpto));
		numTermDeletes.incrementAndGet();
		if (current == null) {
			bytesUsed.addAndGet(BYTES_PER_DEL_TERM + term.text.length()
					* RamUsageEstimator.NUM_BYTES_CHAR);
		}
	}

	void clear() {
		terms.clear();
		queries.clear();
		docIDs.clear();
		numTermDeletes.set(0);
		bytesUsed.set(0);
	}

	void clearDocIDs() {
		bytesUsed.addAndGet(-docIDs.size() * BYTES_PER_DEL_DOCID);
		docIDs.clear();
	}

	boolean any() {
		return terms.size() > 0 || docIDs.size() > 0 || queries.size() > 0;
	}
}
