
/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.RamUsageEstimator;

class ParallelPostingsArray {
	final static int BYTES_PER_POSTING = 3 * RamUsageEstimator.NUM_BYTES_INT;

	final int size;
	final int[] textStarts;
	final int[] intStarts;
	final int[] byteStarts;

	ParallelPostingsArray(final int size) {
		this.size = size;
		textStarts = new int[size];
		intStarts = new int[size];
		byteStarts = new int[size];
	}

	int bytesPerPosting() {
		return BYTES_PER_POSTING;
	}

	ParallelPostingsArray newInstance(int size) {
		return new ParallelPostingsArray(size);
	}

	final ParallelPostingsArray grow() {
		int newSize = ArrayUtil.oversize(size + 1, bytesPerPosting());
		ParallelPostingsArray newArray = newInstance(newSize);
		copyTo(newArray, size);
		return newArray;
	}

	void copyTo(ParallelPostingsArray toArray, int numToCopy) {
		System.arraycopy(textStarts, 0, toArray.textStarts, 0, numToCopy);
		System.arraycopy(intStarts, 0, toArray.intStarts, 0, numToCopy);
		System.arraycopy(byteStarts, 0, toArray.byteStarts, 0, numToCopy);
	}
}
