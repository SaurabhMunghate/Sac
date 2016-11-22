/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

public class LogDocMergePolicy extends LogMergePolicy {

	public static final int DEFAULT_MIN_MERGE_DOCS = 1000;

	public LogDocMergePolicy() {
		minMergeSize = DEFAULT_MIN_MERGE_DOCS;

		maxMergeSize = Long.MAX_VALUE;
		maxMergeSizeForForcedMerge = Long.MAX_VALUE;
	}

	@Override
	protected long size(SegmentInfo info) throws IOException {
		return sizeDocs(info);
	}

	public void setMinMergeDocs(int minMergeDocs) {
		minMergeSize = minMergeDocs;
	}

	public int getMinMergeDocs() {
		return (int) minMergeSize;
	}
}
