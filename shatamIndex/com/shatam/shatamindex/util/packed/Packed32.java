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

class Packed32 extends PackedInts.ReaderImpl implements PackedInts.Mutable {
	static final int BLOCK_SIZE = 32;
	static final int BLOCK_BITS = 5;
	static final int MOD_MASK = BLOCK_SIZE - 1;

	private static final int ENTRY_SIZE = BLOCK_SIZE + 1;
	private static final int FAC_BITPOS = 3;

	private static final int[][] SHIFTS = new int[ENTRY_SIZE][ENTRY_SIZE
			* FAC_BITPOS];
	private static final int[][] MASKS = new int[ENTRY_SIZE][ENTRY_SIZE];

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
					MASKS[elementBits][bitPos] = ~(~0 << rBits);
				}
			}
		}
	}

	private static final int[][] WRITE_MASKS = new int[ENTRY_SIZE][ENTRY_SIZE
			* FAC_BITPOS];
	static {
		for (int elementBits = 1; elementBits <= BLOCK_SIZE; elementBits++) {
			int elementPosMask = ~(~0 << elementBits);
			int[] currentShifts = SHIFTS[elementBits];
			int[] currentMasks = WRITE_MASKS[elementBits];
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

	private int[] blocks;

	private int maxPos;
	private int[] shifts;
	private int[] readMasks;
	private int[] writeMasks;

	public Packed32(int valueCount, int bitsPerValue) {
		this(
				new int[(int) (((long) valueCount) * bitsPerValue / BLOCK_SIZE + 2)],
				valueCount, bitsPerValue);
	}

	public Packed32(DataInput in, int valueCount, int bitsPerValue)
			throws IOException {
		super(valueCount, bitsPerValue);
		int size = size(bitsPerValue, valueCount);
		blocks = new int[size + 1];
		for (int i = 0; i < size; i++) {
			blocks[i] = in.readInt();
		}
		if (size % 2 == 1) {
			in.readInt();
		}
		updateCached();
	}

	private static int size(int bitsPerValue, int valueCount) {
		final long totBitCount = (long) valueCount * bitsPerValue;
		return (int) (totBitCount / 32 + ((totBitCount % 32 == 0) ? 0 : 1));
	}

	public Packed32(int[] blocks, int valueCount, int bitsPerValue) {

		super(valueCount, bitsPerValue);
		if (bitsPerValue > 31) {
			throw new IllegalArgumentException(
					String.format(
							"This array only supports values of 31 bits or less. The "
									+ "required number of bits was %d. The Packed64 "
									+ "implementation allows values with more than 31 bits",
							bitsPerValue));
		}
		this.blocks = blocks;
		updateCached();
	}

	private void updateCached() {
		readMasks = MASKS[bitsPerValue];
		maxPos = (int) ((((long) blocks.length) * BLOCK_SIZE / bitsPerValue) - 2);
		shifts = SHIFTS[bitsPerValue];
		writeMasks = WRITE_MASKS[bitsPerValue];
	}

	public long get(final int index) {
		final long majorBitPos = (long) index * bitsPerValue;
		final int elementPos = (int) (majorBitPos >>> BLOCK_BITS);

		final int bitPos = (int) (majorBitPos & MOD_MASK);

		final int base = bitPos * FAC_BITPOS;

		return ((blocks[elementPos] << shifts[base]) >>> shifts[base + 1])
				| ((blocks[elementPos + 1] >>> shifts[base + 2]) & readMasks[bitPos]);
	}

	public void set(final int index, final long value) {
		final int intValue = (int) value;
		final long majorBitPos = (long) index * bitsPerValue;
		final int elementPos = (int) (majorBitPos >>> BLOCK_BITS);

		final int bitPos = (int) (majorBitPos & MOD_MASK);
		final int base = bitPos * FAC_BITPOS;

		blocks[elementPos] = (blocks[elementPos] & writeMasks[base])
				| (intValue << shifts[base + 1] >>> shifts[base]);
		blocks[elementPos + 1] = (blocks[elementPos + 1] & writeMasks[base + 1])
				| ((intValue << shifts[base + 2]) & writeMasks[base + 2]);
	}

	public void clear() {
		Arrays.fill(blocks, 0);
	}

	@Override
	public String toString() {
		return "Packed32(bitsPerValue=" + bitsPerValue + ", maxPos=" + maxPos
				+ ", elements.length=" + blocks.length + ")";
	}

	public long ramBytesUsed() {
		return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + blocks.length
				* RamUsageEstimator.NUM_BYTES_INT;
	}
}
