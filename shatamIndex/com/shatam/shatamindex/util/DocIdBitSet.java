/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.BitSet;

import com.shatam.shatamindex.search.DocIdSet;
import com.shatam.shatamindex.search.DocIdSetIterator;

public class DocIdBitSet extends DocIdSet {
	private BitSet bitSet;

	public DocIdBitSet(BitSet bitSet) {
		this.bitSet = bitSet;
	}

	@Override
	public DocIdSetIterator iterator() {
		return new DocIdBitSetIterator(bitSet);
	}

	@Override
	public boolean isCacheable() {
		return true;
	}

	public BitSet getBitSet() {
		return this.bitSet;
	}

	private static class DocIdBitSetIterator extends DocIdSetIterator {
		private int docId;
		private BitSet bitSet;

		DocIdBitSetIterator(BitSet bitSet) {
			this.bitSet = bitSet;
			this.docId = -1;
		}

		@Override
		public int docID() {
			return docId;
		}

		@Override
		public int nextDoc() {

			int d = bitSet.nextSetBit(docId + 1);

			docId = d == -1 ? NO_MORE_DOCS : d;
			return docId;
		}

		@Override
		public int advance(int target) {
			int d = bitSet.nextSetBit(target);

			docId = d == -1 ? NO_MORE_DOCS : d;
			return docId;
		}
	}
}
