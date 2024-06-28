/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.util.PriorityQueue;

final class SegmentMergeQueue extends PriorityQueue<SegmentMergeInfo> {
	SegmentMergeQueue(int size) {
		initialize(size);
	}

	@Override
	protected final boolean lessThan(SegmentMergeInfo stiA,
			SegmentMergeInfo stiB) {
		int comparison = stiA.term.compareTo(stiB.term);
		if (comparison == 0)
			return stiA.base < stiB.base;
		else
			return comparison < 0;
	}

	final void close() throws IOException {
		while (top() != null)
			pop().close();
	}

}
