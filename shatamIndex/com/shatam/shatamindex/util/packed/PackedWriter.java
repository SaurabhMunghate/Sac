/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.packed;

import com.shatam.shatamindex.store.DataOutput;

import java.io.IOException;

class PackedWriter extends PackedInts.Writer {
	private long pending;
	private int pendingBitPos;

	private final long[] masks;
	private int written = 0;

	public PackedWriter(DataOutput out, int valueCount, int bitsPerValue)
			throws IOException {
		super(out, valueCount, bitsPerValue);

		pendingBitPos = 64;
		masks = new long[bitsPerValue - 1];

		long v = 1;
		for (int i = 0; i < bitsPerValue - 1; i++) {
			v *= 2;
			masks[i] = v - 1;
		}
	}

	@Override
	public void add(long v) throws IOException {
		assert v <= PackedInts.maxValue(bitsPerValue) : "v=" + v + " maxValue="
				+ PackedInts.maxValue(bitsPerValue);
		assert v >= 0;

		if (pendingBitPos >= bitsPerValue) {

			pending |= v << (pendingBitPos - bitsPerValue);
			if (pendingBitPos == bitsPerValue) {

				out.writeLong(pending);
				pending = 0;
				pendingBitPos = 64;
			} else {
				pendingBitPos -= bitsPerValue;
			}

		} else {

			pending |= (v >> (bitsPerValue - pendingBitPos))
					& masks[pendingBitPos - 1];

			out.writeLong(pending);

			pendingBitPos = 64 - bitsPerValue + pendingBitPos;

			pending = (v << pendingBitPos);
		}
		written++;
	}

	@Override
	public void finish() throws IOException {
		while (written < valueCount) {
			add(0L);
		}

		if (pendingBitPos != 64) {
			out.writeLong(pending);
		}
	}

	@Override
	public String toString() {
		return "PackedWriter(written " + written + "/" + valueCount + " with "
				+ bitsPerValue + " bits/value)";
	}
}
