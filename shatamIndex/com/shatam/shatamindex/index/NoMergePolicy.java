/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Map;

public final class NoMergePolicy extends MergePolicy {

	public static final MergePolicy NO_COMPOUND_FILES = new NoMergePolicy(false);

	public static final MergePolicy COMPOUND_FILES = new NoMergePolicy(true);

	private final boolean useCompoundFile;

	private NoMergePolicy(boolean useCompoundFile) {

		this.useCompoundFile = useCompoundFile;
	}

	@Override
	public void close() {
	}

	@Override
	public MergeSpecification findMerges(SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		return null;
	}

	@Override
	public MergeSpecification findForcedMerges(SegmentInfos segmentInfos,
			int maxSegmentCount, Map<SegmentInfo, Boolean> segmentsToMerge)
			throws CorruptIndexException, IOException {
		return null;
	}

	@Override
	public MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		return null;
	}

	@Override
	public boolean useCompoundFile(SegmentInfos segments, SegmentInfo newSegment) {
		return useCompoundFile;
	}

	@Override
	public void setIndexWriter(IndexWriter writer) {
	}

	@Override
	public String toString() {
		return "NoMergePolicy";
	}
}
