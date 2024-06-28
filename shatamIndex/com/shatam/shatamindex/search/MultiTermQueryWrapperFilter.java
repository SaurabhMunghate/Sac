/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermDocs;
import com.shatam.shatamindex.index.TermEnum;
import com.shatam.shatamindex.util.FixedBitSet;

public class MultiTermQueryWrapperFilter<Q extends MultiTermQuery> extends
		Filter {

	protected final Q query;

	protected MultiTermQueryWrapperFilter(Q query) {
		this.query = query;
	}

	@Override
	public String toString() {

		return query.toString();
	}

	@Override
	public final boolean equals(final Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (this.getClass().equals(o.getClass())) {
			return this.query.equals(((MultiTermQueryWrapperFilter) o).query);
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return query.hashCode();
	}

	@Deprecated
	public int getTotalNumberOfTerms() {
		return query.getTotalNumberOfTerms();
	}

	@Deprecated
	public void clearTotalNumberOfTerms() {
		query.clearTotalNumberOfTerms();
	}

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		final TermEnum enumerator = query.getEnum(reader);
		try {

			if (enumerator.term() == null)
				return DocIdSet.EMPTY_DOCIDSET;

			final FixedBitSet bitSet = new FixedBitSet(reader.maxDoc());
			final int[] docs = new int[32];
			final int[] freqs = new int[32];
			TermDocs termDocs = reader.termDocs();
			try {
				int termCount = 0;
				do {
					Term term = enumerator.term();
					if (term == null)
						break;
					termCount++;
					termDocs.seek(term);
					while (true) {
						final int count = termDocs.read(docs, freqs);
						if (count != 0) {
							for (int i = 0; i < count; i++) {
								bitSet.set(docs[i]);
							}
						} else {
							break;
						}
					}
				} while (enumerator.next());

				query.incTotalNumberOfTerms(termCount);

			} finally {
				termDocs.close();
			}
			return bitSet;
		} finally {
			enumerator.close();
		}
	}

}
