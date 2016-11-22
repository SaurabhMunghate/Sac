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

class Direct16 extends PackedInts.ReaderImpl implements PackedInts.Mutable {
	private short[] values;
	private static final int BITS_PER_VALUE = 16;

	public Direct16(int valueCount) {
		super(valueCount, BITS_PER_VALUE);
		values = new short[valueCount];
	}

	public Direct16(DataInput in, int valueCount) throws IOException {
		super(valueCount, BITS_PER_VALUE);
		short[] values = new short[valueCount];
		for (int i = 0; i < valueCount; i++) {
			values[i] = in.readShort();
		}
		final int mod = valueCount % 4;
		if (mod != 0) {
			final int pad = 4 - mod;

			for (int i = 0; i < pad; i++) {
				in.readShort();
			}
		}

		this.values = values;
	}

	public Direct16(short[] values) {
		super(values.length, BITS_PER_VALUE);
		this.values = values;
	}

	public long get(final int index) {
		return 0xFFFFL & values[index];
	}

	public void set(final int index, final long value) {
		values[index] = (short) (value & 0xFFFF);
	}

	public long ramBytesUsed() {
		return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + values.length
				* RamUsageEstimator.NUM_BYTES_SHORT;
	}

	public void clear() {
		Arrays.fill(values, (short) 0);
	}

	@Override
	public Object getArray() {
		return values;
	}

	@Override
	public boolean hasArray() {
		return true;
	}
}
