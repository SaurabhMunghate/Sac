/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;
import java.util.Arrays;

import com.shatam.shatamindex.search.DocIdSet;
import com.shatam.shatamindex.search.DocIdSetIterator;

public final class FixedBitSet extends DocIdSet implements Bits {
	private final long[] bits;
	private int numBits;

	public static int bits2words(int numBits) {
		int numLong = numBits >>> 6;
		if ((numBits & 63) != 0) {
			numLong++;
		}
		return numLong;
	}

	public FixedBitSet(int numBits) {
		this.numBits = numBits;
		bits = new long[bits2words(numBits)];
	}

	public FixedBitSet(FixedBitSet other) {
		bits = new long[other.bits.length];
		System.arraycopy(other.bits, 0, bits, 0, bits.length);
		numBits = other.numBits;
	}

	@Override
	public DocIdSetIterator iterator() {
		return new OpenBitSetIterator(bits, bits.length);
	}

	public int length() {
		return numBits;
	}

	@Override
	public boolean isCacheable() {
		return true;
	}

	public long[] getBits() {
		return bits;
	}

	public int cardinality() {
		return (int) BitUtil.pop_array(bits, 0, bits.length);
	}

	public boolean get(int index) {
		assert index >= 0 && index < numBits;
		int i = index >> 6;

		int bit = index & 0x3f;
		long bitmask = 1L << bit;
		return (bits[i] & bitmask) != 0;
	}

	public void set(int index) {
		assert index >= 0 && index < numBits;
		int wordNum = index >> 6;
		int bit = index & 0x3f;
		long bitmask = 1L << bit;
		bits[wordNum] |= bitmask;
	}

	public boolean getAndSet(int index) {
		assert index >= 0 && index < numBits;
		int wordNum = index >> 6;
		int bit = index & 0x3f;
		long bitmask = 1L << bit;
		boolean val = (bits[wordNum] & bitmask) != 0;
		bits[wordNum] |= bitmask;
		return val;
	}

	public void clear(int index) {
		assert index >= 0 && index < numBits;
		int wordNum = index >> 6;
		int bit = index & 0x03f;
		long bitmask = 1L << bit;
		bits[wordNum] &= ~bitmask;
	}

	public boolean getAndClear(int index) {
		assert index >= 0 && index < numBits;
		int wordNum = index >> 6;
		int bit = index & 0x3f;
		long bitmask = 1L << bit;
		boolean val = (bits[wordNum] & bitmask) != 0;
		bits[wordNum] &= ~bitmask;
		return val;
	}

	public int nextSetBit(int index) {
		assert index >= 0 && index < numBits;
		int i = index >> 6;
		final int subIndex = index & 0x3f;
		long word = bits[i] >> subIndex;

		if (word != 0) {
			return (i << 6) + subIndex + BitUtil.ntz(word);
		}

		while (++i < bits.length) {
			word = bits[i];
			if (word != 0) {
				return (i << 6) + BitUtil.ntz(word);
			}
		}

		return -1;
	}

	public int prevSetBit(int index) {
		assert index >= 0 && index < numBits : "index=" + index + " numBits="
				+ numBits;
		int i = index >> 6;
		final int subIndex = index & 0x3f;
		long word = (bits[i] << (63 - subIndex));

		if (word != 0) {
			return (i << 6) + subIndex - Long.numberOfLeadingZeros(word);
		}

		while (--i >= 0) {
			word = bits[i];
			if (word != 0) {
				return (i << 6) + 63 - Long.numberOfLeadingZeros(word);
			}
		}

		return -1;
	}

	public void or(DocIdSetIterator iter) throws IOException {
		if (iter instanceof OpenBitSetIterator && iter.docID() == -1) {
			final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
			or(obs.arr, obs.words);

			obs.advance(numBits);
		} else {
			int doc;
			while ((doc = iter.nextDoc()) < numBits) {
				set(doc);
			}
		}
	}

	public void or(FixedBitSet other) {
		or(other.bits, other.bits.length);
	}

	private void or(final long[] otherArr, final int otherLen) {
		final long[] thisArr = this.bits;
		int pos = Math.min(thisArr.length, otherLen);
		while (--pos >= 0) {
			thisArr[pos] |= otherArr[pos];
		}
	}

	public void and(DocIdSetIterator iter) throws IOException {
		if (iter instanceof OpenBitSetIterator && iter.docID() == -1) {
			final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
			and(obs.arr, obs.words);

			obs.advance(numBits);
		} else {
			if (numBits == 0)
				return;
			int disiDoc, bitSetDoc = nextSetBit(0);
			while (bitSetDoc != -1
					&& (disiDoc = iter.advance(bitSetDoc)) < numBits) {
				clear(bitSetDoc, disiDoc);
				disiDoc++;
				bitSetDoc = (disiDoc < numBits) ? nextSetBit(disiDoc) : -1;
			}
			if (bitSetDoc != -1) {
				clear(bitSetDoc, numBits);
			}
		}
	}

	public void and(FixedBitSet other) {
		and(other.bits, other.bits.length);
	}

	private void and(final long[] otherArr, final int otherLen) {
		final long[] thisArr = this.bits;
		int pos = Math.min(thisArr.length, otherLen);
		while (--pos >= 0) {
			thisArr[pos] &= otherArr[pos];
		}
		if (thisArr.length > otherLen) {
			Arrays.fill(thisArr, otherLen, thisArr.length, 0L);
		}
	}

	public void andNot(DocIdSetIterator iter) throws IOException {
		if (iter instanceof OpenBitSetIterator && iter.docID() == -1) {
			final OpenBitSetIterator obs = (OpenBitSetIterator) iter;
			andNot(obs.arr, obs.words);

			obs.advance(numBits);
		} else {
			int doc;
			while ((doc = iter.nextDoc()) < numBits) {
				clear(doc);
			}
		}
	}

	public void andNot(FixedBitSet other) {
		andNot(other.bits, other.bits.length);
	}

	private void andNot(final long[] otherArr, final int otherLen) {
		final long[] thisArr = this.bits;
		int pos = Math.min(thisArr.length, otherLen);
		while (--pos >= 0) {
			thisArr[pos] &= ~otherArr[pos];
		}
	}

	public void flip(int startIndex, int endIndex) {
		assert startIndex >= 0 && startIndex < numBits;
		assert endIndex >= 0 && endIndex <= numBits;
		if (endIndex <= startIndex) {
			return;
		}

		int startWord = startIndex >> 6;
		int endWord = (endIndex - 1) >> 6;

		long startmask = -1L << startIndex;
		long endmask = -1L >>> -endIndex;

		if (startWord == endWord) {
			bits[startWord] ^= (startmask & endmask);
			return;
		}

		bits[startWord] ^= startmask;

		for (int i = startWord + 1; i < endWord; i++) {
			bits[i] = ~bits[i];
		}

		bits[endWord] ^= endmask;
	}

	public void set(int startIndex, int endIndex) {
		assert startIndex >= 0 && startIndex < numBits;
		assert endIndex >= 0 && endIndex <= numBits;
		if (endIndex <= startIndex) {
			return;
		}

		int startWord = startIndex >> 6;
		int endWord = (endIndex - 1) >> 6;

		long startmask = -1L << startIndex;
		long endmask = -1L >>> -endIndex;

		if (startWord == endWord) {
			bits[startWord] |= (startmask & endmask);
			return;
		}

		bits[startWord] |= startmask;
		Arrays.fill(bits, startWord + 1, endWord, -1L);
		bits[endWord] |= endmask;
	}

	public void clear(int startIndex, int endIndex) {
		assert startIndex >= 0 && startIndex < numBits;
		assert endIndex >= 0 && endIndex <= numBits;
		if (endIndex <= startIndex) {
			return;
		}

		int startWord = startIndex >> 6;
		int endWord = (endIndex - 1) >> 6;

		long startmask = -1L << startIndex;
		long endmask = -1L >>> -endIndex;

		startmask = ~startmask;
		endmask = ~endmask;

		if (startWord == endWord) {
			bits[startWord] &= (startmask | endmask);
			return;
		}

		bits[startWord] &= startmask;
		Arrays.fill(bits, startWord + 1, endWord, 0L);
		bits[endWord] &= endmask;
	}

	@Override
	public Object clone() {
		return new FixedBitSet(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FixedBitSet)) {
			return false;
		}
		FixedBitSet other = (FixedBitSet) o;
		if (numBits != other.length()) {
			return false;
		}
		return Arrays.equals(bits, other.bits);
	}

	@Override
	public int hashCode() {
		long h = 0;
		for (int i = bits.length; --i >= 0;) {
			h ^= bits[i];
			h = (h << 1) | (h >>> 63);
		}

		return (int) ((h >> 32) ^ h) + 0x98761234;
	}
}
