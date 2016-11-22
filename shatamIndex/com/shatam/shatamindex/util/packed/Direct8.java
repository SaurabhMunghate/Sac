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

class Direct8 extends PackedInts.ReaderImpl implements PackedInts.Mutable {
	private byte[] values;
	private static final int BITS_PER_VALUE = 8;

	public Direct8(int valueCount) {
		super(valueCount, BITS_PER_VALUE);
		values = new byte[valueCount];
	}

	public Direct8(DataInput in, int valueCount) throws IOException {
		super(valueCount, BITS_PER_VALUE);
		byte[] values = new byte[valueCount];
		for (int i = 0; i < valueCount; i++) {
			values[i] = in.readByte();
		}
		final int mod = valueCount % 8;
		if (mod != 0) {
			final int pad = 8 - mod;

			for (int i = 0; i < pad; i++) {
				in.readByte();
			}
		}

		this.values = values;
	}

	public Direct8(byte[] values) {
		super(values.length, BITS_PER_VALUE);
		this.values = values;
	}

	public long get(final int index) {
		return 0xFFL & values[index];
	}

	public void set(final int index, final long value) {
		values[index] = (byte) (value & 0xFF);
	}

	public long ramBytesUsed() {
		return RamUsageEstimator.NUM_BYTES_ARRAY_HEADER + values.length;
	}

	public void clear() {
		Arrays.fill(values, (byte) 0);
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
