/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class UpgradeIndexMergePolicy extends MergePolicy {

	protected final MergePolicy base;

	public UpgradeIndexMergePolicy(MergePolicy base) {
		this.base = base;
	}

	protected boolean shouldUpgradeSegment(SegmentInfo si) {
		return !Constants.SHATAM_MAIN_VERSION.equals(si.getVersion());
	}

	@Override
	public void setIndexWriter(IndexWriter writer) {
		super.setIndexWriter(writer);
		base.setIndexWriter(writer);
	}

	@Override
	public MergeSpecification findMerges(SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		return base.findMerges(segmentInfos);
	}

	@Override
	public MergeSpecification findForcedMerges(SegmentInfos segmentInfos,
			int maxSegmentCount, Map<SegmentInfo, Boolean> segmentsToMerge)
			throws CorruptIndexException, IOException {

		final Map<SegmentInfo, Boolean> oldSegments = new HashMap<SegmentInfo, Boolean>();
		for (final SegmentInfo si : segmentInfos) {
			final Boolean v = segmentsToMerge.get(si);
			if (v != null && shouldUpgradeSegment(si)) {
				oldSegments.put(si, v);
			}
		}

		if (verbose())
			message("findForcedMerges: segmentsToUpgrade=" + oldSegments);

		if (oldSegments.isEmpty())
			return null;

		MergeSpecification spec = base.findForcedMerges(segmentInfos,
				maxSegmentCount, oldSegments);

		if (spec != null) {

			for (final OneMerge om : spec.merges) {
				oldSegments.keySet().removeAll(om.segments);
			}
		}

		if (!oldSegments.isEmpty()) {
			if (verbose())
				message("findForcedMerges: "
						+ base.getClass().getSimpleName()
						+ " does not want to merge all old segments, merge remaining ones into new segment: "
						+ oldSegments);
			final List<SegmentInfo> newInfos = new ArrayList<SegmentInfo>();
			for (final SegmentInfo si : segmentInfos) {
				if (oldSegments.containsKey(si)) {
					newInfos.add(si);
				}
			}

			if (spec == null) {
				spec = new MergeSpecification();
			}
			spec.add(new OneMerge(newInfos));
		}

		return spec;
	}

	@Override
	public MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		return base.findForcedDeletesMerges(segmentInfos);
	}

	@Override
	public boolean useCompoundFile(SegmentInfos segments, SegmentInfo newSegment)
			throws IOException {
		return base.useCompoundFile(segments, newSegment);
	}

	@Override
	public void close() {
		base.close();
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + "->" + base + "]";
	}

	private boolean verbose() {
		IndexWriter w = writer.get();
		return w != null && w.verbose();
	}

	private void message(String message) {
		if (verbose())
			writer.get().message("UPGMP: " + message);
	}

}
