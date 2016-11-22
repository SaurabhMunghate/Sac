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

class Direct32 extends PackedInts.ReaderImpl implements PackedInts.Mutable {
	private int[] values;
	private static final int BITS_PER_VALUE = 32;

	public Direct32(int valueCount) {
		super(valueCount, BITS_PER_VALUE);
		values = new int[valueCount];
	}

	public Direct32(DataInput in, int valueCount) throws IOException {
		super(valueCount, BITS_PER_VALUE);
		int[] values = new int[valueCount];
		for (int i = 0; i < valueCount; i++) {
			values[i] = in.readInt();
		}
		final int mod = valueCount % 2;
		if (mod != 0) {
			in.readInt();
		}

		this.values = values;
	}

	public Direct32(int[] values) {
		super(values.length, BITS_PER_VALUE);
		this.values = values;
	}

	public long get(final int index) {
		return 0xFFFFFFFFL & values[index];
	}

	public void set(final int index, final long value) {
		values[index] = (int) (value & 0xFFFFFFFF);
	}

	public long ramBytesUsed() {
		return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + values.length
				* RamUsageEstimator.NUM_BYTES_INT;
	}

	public void clear() {
		Arrays.fill(values, 0);
	}

	@Override
	public int[] getArray() {
		return values;
	}

	@Override
	public boolean hasArray() {
		return true;
	}
}
