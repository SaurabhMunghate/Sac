/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.Iterator;
import java.util.Map;

import com.shatam.shatamindex.index.BufferedDeletesStream.QueryAndLimit;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.RamUsageEstimator;

class FrozenBufferedDeletes {

	final static int BYTES_PER_DEL_QUERY = RamUsageEstimator.NUM_BYTES_OBJECT_REF
			+ RamUsageEstimator.NUM_BYTES_INT + 24;

	final PrefixCodedTerms terms;
	int termCount;

	final Query[] queries;
	final int[] queryLimits;
	final int bytesUsed;
	final int numTermDeletes;
	final long gen;

	public FrozenBufferedDeletes(BufferedDeletes deletes, long gen) {
		Term termsArray[] = deletes.terms.keySet().toArray(
				new Term[deletes.terms.size()]);
		termCount = termsArray.length;
		ArrayUtil.mergeSort(termsArray);
		PrefixCodedTerms.Builder builder = new PrefixCodedTerms.Builder();
		for (Term term : termsArray) {
			builder.add(term);
		}
		terms = builder.finish();

		queries = new Query[deletes.queries.size()];
		queryLimits = new int[deletes.queries.size()];
		int upto = 0;
		for (Map.Entry<Query, Integer> ent : deletes.queries.entrySet()) {
			queries[upto] = ent.getKey();
			queryLimits[upto] = ent.getValue();
			upto++;
		}

		bytesUsed = (int) terms.getSizeInBytes() + queries.length
				* BYTES_PER_DEL_QUERY;
		numTermDeletes = deletes.numTermDeletes.get();
		this.gen = gen;
	}

	public Iterable<Term> termsIterable() {
		return new Iterable<Term>() {

			public Iterator<Term> iterator() {
				return terms.iterator();
			}
		};
	}

	public Iterable<QueryAndLimit> queriesIterable() {
		return new Iterable<QueryAndLimit>() {

			public Iterator<QueryAndLimit> iterator() {
				return new Iterator<QueryAndLimit>() {
					private int upto;

					public boolean hasNext() {
						return upto < queries.length;
					}

					public QueryAndLimit next() {
						QueryAndLimit ret = new QueryAndLimit(queries[upto],
								queryLimits[upto]);
						upto++;
						return ret;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	@Override
	public String toString() {
		String s = "";
		if (numTermDeletes != 0) {
			s += " " + numTermDeletes + " deleted terms (unique count="
					+ termCount + ")";
		}
		if (queries.length != 0) {
			s += " " + queries.length + " deleted queries";
		}
		if (bytesUsed != 0) {
			s += " bytesUsed=" + bytesUsed;
		}

		return s;
	}

	boolean any() {
		return termCount > 0 || queries.length > 0;
	}
}
