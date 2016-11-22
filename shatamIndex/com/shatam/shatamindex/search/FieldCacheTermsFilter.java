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
import com.shatam.shatamindex.index.TermDocs;
import com.shatam.shatamindex.util.FixedBitSet;

public class FieldCacheTermsFilter extends Filter {
	private String field;
	private String[] terms;

	public FieldCacheTermsFilter(String field, String... terms) {
		this.field = field;
		this.terms = terms;
	}

	public FieldCache getFieldCache() {
		return FieldCache.DEFAULT;
	}

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		return new FieldCacheTermsFilterDocIdSet(getFieldCache()
				.getStringIndex(reader, field));
	}

	protected class FieldCacheTermsFilterDocIdSet extends DocIdSet {
		private FieldCache.StringIndex fcsi;

		private FixedBitSet bits;

		public FieldCacheTermsFilterDocIdSet(FieldCache.StringIndex fcsi) {
			this.fcsi = fcsi;
			bits = new FixedBitSet(this.fcsi.lookup.length);
			for (int i = 0; i < terms.length; i++) {
				int termNumber = this.fcsi.binarySearchLookup(terms[i]);
				if (termNumber > 0) {
					bits.set(termNumber);
				}
			}
		}

		@Override
		public DocIdSetIterator iterator() {
			return new FieldCacheTermsFilterDocIdSetIterator();
		}

		@Override
		public boolean isCacheable() {
			return true;
		}

		protected class FieldCacheTermsFilterDocIdSetIterator extends
				DocIdSetIterator {
			private int doc = -1;

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				try {
					while (!bits.get(fcsi.order[++doc])) {
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					doc = NO_MORE_DOCS;
				}
				return doc;
			}

			@Override
			public int advance(int target) {
				try {
					doc = target;
					while (!bits.get(fcsi.order[doc])) {
						doc++;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					doc = NO_MORE_DOCS;
				}
				return doc;
			}
		}
	}
}
