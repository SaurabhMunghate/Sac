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

class Direct64 extends PackedInts.ReaderImpl implements PackedInts.Mutable {
	private long[] values;
	private static final int BITS_PER_VALUE = 64;

	public Direct64(int valueCount) {
		super(valueCount, BITS_PER_VALUE);
		values = new long[valueCount];
	}

	public Direct64(DataInput in, int valueCount) throws IOException {
		super(valueCount, BITS_PER_VALUE);
		long[] values = new long[valueCount];
		for (int i = 0; i < valueCount; i++) {
			values[i] = in.readLong();
		}

		this.values = values;
	}

	public Direct64(long[] values) {
		super(values.length, BITS_PER_VALUE);
		this.values = values;
	}

	public long get(final int index) {
		return values[index];
	}

	public void set(final int index, final long value) {
		values[index] = value;
	}

	public long ramBytesUsed() {
		return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + values.length
				* RamUsageEstimator.NUM_BYTES_LONG;
	}

	public void clear() {
		Arrays.fill(values, 0L);
	}

	@Override
	public long[] getArray() {
		return values;
	}

	@Override
	public boolean hasArray() {
		return true;
	}
}
