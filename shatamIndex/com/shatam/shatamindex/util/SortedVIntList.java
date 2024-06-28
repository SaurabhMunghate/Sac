/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;
import java.util.BitSet;

import com.shatam.shatamindex.search.DocIdSet;
import com.shatam.shatamindex.search.DocIdSetIterator;

public class SortedVIntList extends DocIdSet {

	final static int BITS2VINTLIST_SIZE = 8;

	private int size;
	private byte[] bytes;
	private int lastBytePos;

	public SortedVIntList(int... sortedInts) {
		this(sortedInts, sortedInts.length);
	}

	public SortedVIntList(int[] sortedInts, int inputSize) {
		SortedVIntListBuilder builder = new SortedVIntListBuilder();
		for (int i = 0; i < inputSize; i++) {
			builder.addInt(sortedInts[i]);
		}
		builder.done();
	}

	public SortedVIntList(BitSet bits) {
		SortedVIntListBuilder builder = new SortedVIntListBuilder();
		int nextInt = bits.nextSetBit(0);
		while (nextInt != -1) {
			builder.addInt(nextInt);
			nextInt = bits.nextSetBit(nextInt + 1);
		}
		builder.done();
	}

	public SortedVIntList(DocIdSetIterator docIdSetIterator) throws IOException {
		SortedVIntListBuilder builder = new SortedVIntListBuilder();
		int doc;
		while ((doc = docIdSetIterator.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
			builder.addInt(doc);
		}
		builder.done();
	}

	private class SortedVIntListBuilder {
		private int lastInt = 0;

		SortedVIntListBuilder() {
			initBytes();
			lastInt = 0;
		}

		void addInt(int nextInt) {
			int diff = nextInt - lastInt;
			if (diff < 0) {
				throw new IllegalArgumentException(
						"Input not sorted or first element negative.");
			}

			if ((lastBytePos + MAX_BYTES_PER_INT) > bytes.length) {

				resizeBytes(ArrayUtil.oversize(lastBytePos + MAX_BYTES_PER_INT,
						1));
			}

			while ((diff & ~VB1) != 0) {

				bytes[lastBytePos++] = (byte) ((diff & VB1) | ~VB1);
				diff >>>= BIT_SHIFT;
			}
			bytes[lastBytePos++] = (byte) diff;
			size++;
			lastInt = nextInt;
		}

		void done() {
			resizeBytes(lastBytePos);
		}
	}

	private void initBytes() {
		size = 0;
		bytes = new byte[128];
		lastBytePos = 0;
	}

	private void resizeBytes(int newSize) {
		if (newSize != bytes.length) {
			byte[] newBytes = new byte[newSize];
			System.arraycopy(bytes, 0, newBytes, 0, lastBytePos);
			bytes = newBytes;
		}
	}

	private static final int VB1 = 0x7F;
	private static final int BIT_SHIFT = 7;
	private final int MAX_BYTES_PER_INT = (31 / BIT_SHIFT) + 1;

	public int size() {
		return size;
	}

	public int getByteSize() {
		return bytes.length;
	}

	@Override
	public boolean isCacheable() {
		return true;
	}

	@Override
	public DocIdSetIterator iterator() {
		return new DocIdSetIterator() {
			int bytePos = 0;
			int lastInt = 0;
			int doc = -1;

			private void advance() {

				byte b = bytes[bytePos++];
				lastInt += b & VB1;
				for (int s = BIT_SHIFT; (b & ~VB1) != 0; s += BIT_SHIFT) {
					b = bytes[bytePos++];
					lastInt += (b & VB1) << s;
				}
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public int nextDoc() {
				if (bytePos >= lastBytePos) {
					doc = NO_MORE_DOCS;
				} else {
					advance();
					doc = lastInt;
				}
				return doc;
			}

			@Override
			public int advance(int target) {
				while (bytePos < lastBytePos) {
					advance();
					if (lastInt >= target) {
						return doc = lastInt;
					}
				}
				return doc = NO_MORE_DOCS;
			}

		};
	}
}
