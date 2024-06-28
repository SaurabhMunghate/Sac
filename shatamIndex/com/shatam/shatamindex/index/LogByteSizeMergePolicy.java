/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

public class LogByteSizeMergePolicy extends LogMergePolicy {

	public static final double DEFAULT_MIN_MERGE_MB = 1.6;

	public static final double DEFAULT_MAX_MERGE_MB = 2048;

	public static final double DEFAULT_MAX_MERGE_MB_FOR_FORCED_MERGE = Long.MAX_VALUE;

	public LogByteSizeMergePolicy() {
		minMergeSize = (long) (DEFAULT_MIN_MERGE_MB * 1024 * 1024);
		maxMergeSize = (long) (DEFAULT_MAX_MERGE_MB * 1024 * 1024);
		maxMergeSizeForForcedMerge = (long) (DEFAULT_MAX_MERGE_MB_FOR_FORCED_MERGE * 1024 * 1024);
	}

	@Override
	protected long size(SegmentInfo info) throws IOException {
		return sizeBytes(info);
	}

	public void setMaxMergeMB(double mb) {
		maxMergeSize = (long) (mb * 1024 * 1024);
	}

	public double getMaxMergeMB() {
		return ((double) maxMergeSize) / 1024 / 1024;
	}

	@Deprecated
	public void setMaxMergeMBForOptimize(double mb) {
		setMaxMergeMBForForcedMerge(mb);
	}

	public void setMaxMergeMBForForcedMerge(double mb) {
		maxMergeSizeForForcedMerge = (long) (mb * 1024 * 1024);
	}

	@Deprecated
	public double getMaxMergeMBForOptimize() {
		return getMaxMergeMBForForcedMerge();
	}

	public double getMaxMergeMBForForcedMerge() {
		return ((double) maxMergeSizeForForcedMerge) / 1024 / 1024;
	}

	public void setMinMergeMB(double mb) {
		minMergeSize = (long) (mb * 1024 * 1024);
	}

	public double getMinMergeMB() {
		return ((double) minMergeSize) / 1024 / 1024;
	}
}
