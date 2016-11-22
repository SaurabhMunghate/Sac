/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.packed;

import com.shatam.shatamindex.store.DataInput;
import com.shatam.shatamindex.util.RamUsageEstimator;

import java.io.IOException;
import java.util.Arrays;

class Packed64 extends PackedInts.ReaderImpl implements PackedInts.Mutable {
	static final int BLOCK_SIZE = 64;
	static final int BLOCK_BITS = 6;
	static final int MOD_MASK = BLOCK_SIZE - 1;

	private static final int ENTRY_SIZE = BLOCK_SIZE + 1;
	private static final int FAC_BITPOS = 3;

	private static final int[][] SHIFTS = new int[ENTRY_SIZE][ENTRY_SIZE
			* FAC_BITPOS];

	private static final long[][] MASKS = new long[ENTRY_SIZE][ENTRY_SIZE];

	static {
		for (int elementBits = 1; elementBits <= BLOCK_SIZE; elementBits++) {
			for (int bitPos = 0; bitPos < BLOCK_SIZE; bitPos++) {
				int[] currentShifts = SHIFTS[elementBits];
				int base = bitPos * FAC_BITPOS;
				currentShifts[base] = bitPos;
				currentShifts[base + 1] = BLOCK_SIZE - elementBits;
				if (bitPos <= BLOCK_SIZE - elementBits) {
					currentShifts[base + 2] = 0;
					MASKS[elementBits][bitPos] = 0;
				} else {
					int rBits = elementBits - (BLOCK_SIZE - bitPos);
					currentShifts[base + 2] = BLOCK_SIZE - rBits;
					MASKS[elementBits][bitPos] = ~(~0L << rBits);
				}
			}
		}
	}

	private static final long[][] WRITE_MASKS = new long[ENTRY_SIZE][ENTRY_SIZE
			* FAC_BITPOS];
	static {
		for (int elementBits = 1; elementBits <= BLOCK_SIZE; elementBits++) {
			long elementPosMask = ~(~0L << elementBits);
			int[] currentShifts = SHIFTS[elementBits];
			long[] currentMasks = WRITE_MASKS[elementBits];
			for (int bitPos = 0; bitPos < BLOCK_SIZE; bitPos++) {
				int base = bitPos * FAC_BITPOS;
				currentMasks[base] = ~((elementPosMask << currentShifts[base + 1]) >>> currentShifts[base]);
				if (bitPos <= BLOCK_SIZE - elementBits) {
					currentMasks[base + 1] = ~0;
					currentMasks[base + 2] = 0;
				} else {
					currentMasks[base + 1] = ~(elementPosMask << currentShifts[base + 2]);
					currentMasks[base + 2] = currentShifts[base + 2] == 0 ? 0
							: ~0;
				}
			}
		}
	}

	private long[] blocks;

	private int maxPos;
	private int[] shifts;
	private long[] readMasks;
	private long[] writeMasks;

	public Packed64(int valueCount, int bitsPerValue) {

		this(
				new long[(int) ((long) valueCount * bitsPerValue / BLOCK_SIZE + 2)],
				valueCount, bitsPerValue);
	}

	public Packed64(long[] blocks, int valueCount, int bitsPerValue) {
		super(valueCount, bitsPerValue);
		this.blocks = blocks;
		updateCached();
	}

	public Packed64(DataInput in, int valueCount, int bitsPerValue)
			throws IOException {
		super(valueCount, bitsPerValue);
		int size = size(valueCount, bitsPerValue);
		blocks = new long[size + 1];

		for (int i = 0; i < size; i++) {
			blocks[i] = in.readLong();
		}
		updateCached();
	}

	private static int size(int valueCount, int bitsPerValue) {
		final long totBitCount = (long) valueCount * bitsPerValue;
		return (int) (totBitCount / 64 + ((totBitCount % 64 == 0) ? 0 : 1));
	}

	private void updateCached() {
		readMasks = MASKS[bitsPerValue];
		shifts = SHIFTS[bitsPerValue];
		writeMasks = WRITE_MASKS[bitsPerValue];
		maxPos = (int) ((((long) blocks.length) * BLOCK_SIZE / bitsPerValue) - 2);
	}

	public long get(final int index) {
		final long majorBitPos = (long) index * bitsPerValue;
		final int elementPos = (int) (majorBitPos >>> BLOCK_BITS);
		final int bitPos = (int) (majorBitPos & MOD_MASK);

		final int base = bitPos * FAC_BITPOS;
		assert elementPos < blocks.length : "elementPos: " + elementPos
				+ "; blocks.len: " + blocks.length;
		return ((blocks[elementPos] << shifts[base]) >>> shifts[base + 1])
				| ((blocks[elementPos + 1] >>> shifts[base + 2]) & readMasks[bitPos]);
	}

	public void set(final int index, final long value) {
		final long majorBitPos = (long) index * bitsPerValue;
		final int elementPos = (int) (majorBitPos >>> BLOCK_BITS);
		final int bitPos = (int) (majorBitPos & MOD_MASK);
		final int base = bitPos * FAC_BITPOS;

		blocks[elementPos] = (blocks[elementPos] & writeMasks[base])
				| (value << shifts[base + 1] >>> shifts[base]);
		blocks[elementPos + 1] = (blocks[elementPos + 1] & writeMasks[base + 1])
				| ((value << shifts[base + 2]) & writeMasks[base + 2]);
	}

	@Override
	public String toString() {
		return "Packed64(bitsPerValue=" + bitsPerValue + ", size=" + size()
				+ ", maxPos=" + maxPos + ", elements.length=" + blocks.length
				+ ")";
	}

	public long ramBytesUsed() {
		return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + blocks.length
				* RamUsageEstimator.NUM_BYTES_LONG;
	}

	public void clear() {
		Arrays.fill(blocks, 0L);
	}
}
