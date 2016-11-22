/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import com.shatam.shatamindex.util.ArrayUtil;

public abstract class BaseCharFilter extends CharFilter {

	private int offsets[];
	private int diffs[];
	private int size = 0;

	public BaseCharFilter(CharStream in) {
		super(in);
	}

	@Override
	protected int correct(int currentOff) {
		if (offsets == null || currentOff < offsets[0]) {
			return currentOff;
		}

		int hi = size - 1;
		if (currentOff >= offsets[hi])
			return currentOff + diffs[hi];

		int lo = 0;
		int mid = -1;

		while (hi >= lo) {
			mid = (lo + hi) >>> 1;
			if (currentOff < offsets[mid])
				hi = mid - 1;
			else if (currentOff > offsets[mid])
				lo = mid + 1;
			else
				return currentOff + diffs[mid];
		}

		if (currentOff < offsets[mid])
			return mid == 0 ? currentOff : currentOff + diffs[mid - 1];
		else
			return currentOff + diffs[mid];
	}

	protected int getLastCumulativeDiff() {
		return offsets == null ? 0 : diffs[size - 1];
	}

	protected void addOffCorrectMap(int off, int cumulativeDiff) {
		if (offsets == null) {
			offsets = new int[64];
			diffs = new int[64];
		} else if (size == offsets.length) {
			offsets = ArrayUtil.grow(offsets);
			diffs = ArrayUtil.grow(diffs);
		}

		offsets[size] = off;
		diffs[size++] = cumulativeDiff;
	}
}
