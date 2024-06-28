/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.packed;

public class GrowableWriter implements PackedInts.Mutable {

	private long currentMaxValue;
	private PackedInts.Mutable current;
	private final boolean roundFixedSize;

	public GrowableWriter(int startBitsPerValue, int valueCount,
			boolean roundFixedSize) {
		this.roundFixedSize = roundFixedSize;
		current = PackedInts.getMutable(valueCount, getSize(startBitsPerValue));
		currentMaxValue = PackedInts.maxValue(current.getBitsPerValue());
	}

	private final int getSize(int bpv) {
		if (roundFixedSize) {
			return PackedInts.getNextFixedSize(bpv);
		} else {
			return bpv;
		}
	}

	public long get(int index) {
		return current.get(index);
	}

	public int size() {
		return current.size();
	}

	public int getBitsPerValue() {
		return current.getBitsPerValue();
	}

	public PackedInts.Mutable getMutable() {
		return current;
	}

	public Object getArray() {
		return current.getArray();
	}

	public boolean hasArray() {
		return current.hasArray();
	}

	public void set(int index, long value) {
		if (value >= currentMaxValue) {
			int bpv = getBitsPerValue();
			while (currentMaxValue <= value
					&& currentMaxValue != Long.MAX_VALUE) {
				bpv++;
				currentMaxValue *= 2;
			}
			final int valueCount = size();
			PackedInts.Mutable next = PackedInts.getMutable(valueCount,
					getSize(bpv));
			for (int i = 0; i < valueCount; i++) {
				next.set(i, current.get(i));
			}
			current = next;
			currentMaxValue = PackedInts.maxValue(current.getBitsPerValue());
		}
		current.set(index, value);
	}

	public void clear() {
		current.clear();
	}

	public GrowableWriter resize(int newSize) {
		GrowableWriter next = new GrowableWriter(getBitsPerValue(), newSize,
				roundFixedSize);
		final int limit = Math.min(size(), newSize);
		for (int i = 0; i < limit; i++) {
			next.set(i, get(i));
		}
		return next;
	}
}
