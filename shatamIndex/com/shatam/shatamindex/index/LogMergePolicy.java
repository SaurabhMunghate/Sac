/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class LogMergePolicy extends MergePolicy {

	public static final double LEVEL_LOG_SPAN = 0.75;

	public static final int DEFAULT_MERGE_FACTOR = 10;

	public static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;

	public static final double DEFAULT_NO_CFS_RATIO = 0.1;

	protected int mergeFactor = DEFAULT_MERGE_FACTOR;

	protected long minMergeSize;
	protected long maxMergeSize;

	protected long maxMergeSizeForForcedMerge = Long.MAX_VALUE;
	protected int maxMergeDocs = DEFAULT_MAX_MERGE_DOCS;

	protected double noCFSRatio = DEFAULT_NO_CFS_RATIO;

	protected boolean calibrateSizeByDeletes = true;

	protected boolean useCompoundFile = true;

	public LogMergePolicy() {
		super();
	}

	protected boolean verbose() {
		IndexWriter w = writer.get();
		return w != null && w.verbose();
	}

	public double getNoCFSRatio() {
		return noCFSRatio;
	}

	public void setNoCFSRatio(double noCFSRatio) {
		if (noCFSRatio < 0.0 || noCFSRatio > 1.0) {
			throw new IllegalArgumentException(
					"noCFSRatio must be 0.0 to 1.0 inclusive; got "
							+ noCFSRatio);
		}
		this.noCFSRatio = noCFSRatio;
	}

	protected void message(String message) {
		if (verbose())
			writer.get().message("LMP: " + message);
	}

	public int getMergeFactor() {
		return mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		if (mergeFactor < 2)
			throw new IllegalArgumentException(
					"mergeFactor cannot be less than 2");
		this.mergeFactor = mergeFactor;
	}

	@Override
	public boolean useCompoundFile(SegmentInfos infos, SegmentInfo mergedInfo)
			throws IOException {
		final boolean doCFS;

		if (!useCompoundFile) {
			doCFS = false;
		} else if (noCFSRatio == 1.0) {
			doCFS = true;
		} else {
			long totalSize = 0;
			for (SegmentInfo info : infos)
				totalSize += size(info);

			doCFS = size(mergedInfo) <= noCFSRatio * totalSize;
		}
		return doCFS;
	}

	public void setUseCompoundFile(boolean useCompoundFile) {
		this.useCompoundFile = useCompoundFile;
	}

	public boolean getUseCompoundFile() {
		return useCompoundFile;
	}

	public void setCalibrateSizeByDeletes(boolean calibrateSizeByDeletes) {
		this.calibrateSizeByDeletes = calibrateSizeByDeletes;
	}

	public boolean getCalibrateSizeByDeletes() {
		return calibrateSizeByDeletes;
	}

	@Override
	public void close() {
	}

	abstract protected long size(SegmentInfo info) throws IOException;

	protected long sizeDocs(SegmentInfo info) throws IOException {
		if (calibrateSizeByDeletes) {
			int delCount = writer.get().numDeletedDocs(info);
			assert delCount <= info.docCount;
			return (info.docCount - (long) delCount);
		} else {
			return info.docCount;
		}
	}

	protected long sizeBytes(SegmentInfo info) throws IOException {
		long byteSize = info.sizeInBytes(true);
		if (calibrateSizeByDeletes) {
			int delCount = writer.get().numDeletedDocs(info);
			double delRatio = (info.docCount <= 0 ? 0.0f
					: ((float) delCount / (float) info.docCount));
			assert delRatio <= 1.0;
			return (info.docCount <= 0 ? byteSize
					: (long) (byteSize * (1.0 - delRatio)));
		} else {
			return byteSize;
		}
	}

	protected boolean isMerged(SegmentInfos infos, int maxNumSegments,
			Map<SegmentInfo, Boolean> segmentsToMerge) throws IOException {
		final int numSegments = infos.size();
		int numToMerge = 0;
		SegmentInfo mergeInfo = null;
		boolean segmentIsOriginal = false;
		for (int i = 0; i < numSegments && numToMerge <= maxNumSegments; i++) {
			final SegmentInfo info = infos.info(i);
			final Boolean isOriginal = segmentsToMerge.get(info);
			if (isOriginal != null) {
				segmentIsOriginal = isOriginal;
				numToMerge++;
				mergeInfo = info;
			}
		}

		return numToMerge <= maxNumSegments
				&& (numToMerge != 1 || !segmentIsOriginal || isMerged(mergeInfo));
	}

	protected boolean isMerged(SegmentInfo info) throws IOException {
		IndexWriter w = writer.get();
		assert w != null;
		boolean hasDeletions = w.numDeletedDocs(info) > 0;
		return !hasDeletions
				&& !info.hasSeparateNorms()
				&& info.dir == w.getDirectory()
				&& (info.getUseCompoundFile() == useCompoundFile || noCFSRatio < 1.0);
	}

	private MergeSpecification findForcedMergesSizeLimit(SegmentInfos infos,
			int maxNumSegments, int last) throws IOException {
		MergeSpecification spec = new MergeSpecification();
		final List<SegmentInfo> segments = infos.asList();

		int start = last - 1;
		while (start >= 0) {
			SegmentInfo info = infos.info(start);
			if (size(info) > maxMergeSizeForForcedMerge
					|| sizeDocs(info) > maxMergeDocs) {
				if (verbose()) {
					message("findForcedMergesSizeLimit: skip segment=" + info
							+ ": size is > maxMergeSize ("
							+ maxMergeSizeForForcedMerge
							+ ") or sizeDocs is > maxMergeDocs ("
							+ maxMergeDocs + ")");
				}

				if (last - start - 1 > 1
						|| (start != last - 1 && !isMerged(infos
								.info(start + 1)))) {

					spec.add(new OneMerge(segments.subList(start + 1, last)));
				}
				last = start;
			} else if (last - start == mergeFactor) {

				spec.add(new OneMerge(segments.subList(start, last)));
				last = start;
			}
			--start;
		}

		if (last > 0 && (++start + 1 < last || !isMerged(infos.info(start)))) {
			spec.add(new OneMerge(segments.subList(start, last)));
		}

		return spec.merges.size() == 0 ? null : spec;
	}

	private MergeSpecification findForcedMergesMaxNumSegments(
			SegmentInfos infos, int maxNumSegments, int last)
			throws IOException {
		MergeSpecification spec = new MergeSpecification();
		final List<SegmentInfo> segments = infos.asList();

		while (last - maxNumSegments + 1 >= mergeFactor) {
			spec.add(new OneMerge(segments.subList(last - mergeFactor, last)));
			last -= mergeFactor;
		}

		if (0 == spec.merges.size()) {
			if (maxNumSegments == 1) {

				if (last > 1 || !isMerged(infos.info(0))) {
					spec.add(new OneMerge(segments.subList(0, last)));
				}
			} else if (last > maxNumSegments) {

				final int finalMergeSize = last - maxNumSegments + 1;

				long bestSize = 0;
				int bestStart = 0;

				for (int i = 0; i < last - finalMergeSize + 1; i++) {
					long sumSize = 0;
					for (int j = 0; j < finalMergeSize; j++)
						sumSize += size(infos.info(j + i));
					if (i == 0
							|| (sumSize < 2 * size(infos.info(i - 1)) && sumSize < bestSize)) {
						bestStart = i;
						bestSize = sumSize;
					}
				}

				spec.add(new OneMerge(segments.subList(bestStart, bestStart
						+ finalMergeSize)));
			}
		}
		return spec.merges.size() == 0 ? null : spec;
	}

	@Override
	public MergeSpecification findForcedMerges(SegmentInfos infos,
			int maxNumSegments, Map<SegmentInfo, Boolean> segmentsToMerge)
			throws IOException {

		assert maxNumSegments > 0;
		if (verbose()) {
			message("findForcedMerges: maxNumSegs=" + maxNumSegments
					+ " segsToMerge=" + segmentsToMerge);
		}

		if (isMerged(infos, maxNumSegments, segmentsToMerge)) {
			if (verbose()) {
				message("already merged; skip");
			}
			return null;
		}

		int last = infos.size();
		while (last > 0) {
			final SegmentInfo info = infos.info(--last);
			if (segmentsToMerge.get(info) != null) {
				last++;
				break;
			}
		}

		if (last == 0)
			return null;

		if (maxNumSegments == 1 && last == 1 && isMerged(infos.info(0))) {
			if (verbose()) {
				message("already 1 seg; skip");
			}
			return null;
		}

		boolean anyTooLarge = false;
		for (int i = 0; i < last; i++) {
			SegmentInfo info = infos.info(i);
			if (size(info) > maxMergeSizeForForcedMerge
					|| sizeDocs(info) > maxMergeDocs) {
				anyTooLarge = true;
				break;
			}
		}

		if (anyTooLarge) {
			return findForcedMergesSizeLimit(infos, maxNumSegments, last);
		} else {
			return findForcedMergesMaxNumSegments(infos, maxNumSegments, last);
		}
	}

	@Override
	public MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		final List<SegmentInfo> segments = segmentInfos.asList();
		final int numSegments = segments.size();

		if (verbose())
			message("findForcedDeleteMerges: " + numSegments + " segments");

		MergeSpecification spec = new MergeSpecification();
		int firstSegmentWithDeletions = -1;
		IndexWriter w = writer.get();
		assert w != null;
		for (int i = 0; i < numSegments; i++) {
			final SegmentInfo info = segmentInfos.info(i);
			int delCount = w.numDeletedDocs(info);
			if (delCount > 0) {
				if (verbose())
					message("  segment " + info.name + " has deletions");
				if (firstSegmentWithDeletions == -1)
					firstSegmentWithDeletions = i;
				else if (i - firstSegmentWithDeletions == mergeFactor) {

					if (verbose())
						message("  add merge " + firstSegmentWithDeletions
								+ " to " + (i - 1) + " inclusive");
					spec.add(new OneMerge(segments.subList(
							firstSegmentWithDeletions, i)));
					firstSegmentWithDeletions = i;
				}
			} else if (firstSegmentWithDeletions != -1) {

				if (verbose())
					message("  add merge " + firstSegmentWithDeletions + " to "
							+ (i - 1) + " inclusive");
				spec.add(new OneMerge(segments.subList(
						firstSegmentWithDeletions, i)));
				firstSegmentWithDeletions = -1;
			}
		}

		if (firstSegmentWithDeletions != -1) {
			if (verbose())
				message("  add merge " + firstSegmentWithDeletions + " to "
						+ (numSegments - 1) + " inclusive");
			spec.add(new OneMerge(segments.subList(firstSegmentWithDeletions,
					numSegments)));
		}

		return spec;
	}

	private static class SegmentInfoAndLevel implements
			Comparable<SegmentInfoAndLevel> {
		SegmentInfo info;
		float level;
		int index;

		public SegmentInfoAndLevel(SegmentInfo info, float level, int index) {
			this.info = info;
			this.level = level;
			this.index = index;
		}

		public int compareTo(SegmentInfoAndLevel other) {
			if (level < other.level)
				return 1;
			else if (level > other.level)
				return -1;
			else
				return 0;
		}
	}

	@Override
	public MergeSpecification findMerges(SegmentInfos infos) throws IOException {

		final int numSegments = infos.size();
		if (verbose())
			message("findMerges: " + numSegments + " segments");

		final List<SegmentInfoAndLevel> levels = new ArrayList<SegmentInfoAndLevel>();
		final float norm = (float) Math.log(mergeFactor);

		final Collection<SegmentInfo> mergingSegments = writer.get()
				.getMergingSegments();

		for (int i = 0; i < numSegments; i++) {
			final SegmentInfo info = infos.info(i);
			long size = size(info);

			if (size < 1) {
				size = 1;
			}

			final SegmentInfoAndLevel infoLevel = new SegmentInfoAndLevel(info,
					(float) Math.log(size) / norm, i);
			levels.add(infoLevel);

			if (verbose()) {
				final long segBytes = sizeBytes(info);
				String extra = mergingSegments.contains(info) ? " [merging]"
						: "";
				if (size >= maxMergeSize) {
					extra += " [skip: too large]";
				}
				message("seg=" + writer.get().segString(info) + " level="
						+ infoLevel.level + " size="
						+ String.format("%.3f MB", segBytes / 1024 / 1024.)
						+ extra);
			}
		}

		final float levelFloor;
		if (minMergeSize <= 0)
			levelFloor = (float) 0.0;
		else
			levelFloor = (float) (Math.log(minMergeSize) / norm);

		MergeSpecification spec = null;

		final int numMergeableSegments = levels.size();

		int start = 0;
		while (start < numMergeableSegments) {

			float maxLevel = levels.get(start).level;
			for (int i = 1 + start; i < numMergeableSegments; i++) {
				final float level = levels.get(i).level;
				if (level > maxLevel)
					maxLevel = level;
			}

			float levelBottom;
			if (maxLevel <= levelFloor)

				levelBottom = -1.0F;
			else {
				levelBottom = (float) (maxLevel - LEVEL_LOG_SPAN);

				if (levelBottom < levelFloor && maxLevel >= levelFloor)
					levelBottom = levelFloor;
			}

			int upto = numMergeableSegments - 1;
			while (upto >= start) {
				if (levels.get(upto).level >= levelBottom) {
					break;
				}
				upto--;
			}
			if (verbose())
				message("  level " + levelBottom + " to " + maxLevel + ": "
						+ (1 + upto - start) + " segments");

			int end = start + mergeFactor;
			while (end <= 1 + upto) {
				boolean anyTooLarge = false;
				boolean anyMerging = false;
				for (int i = start; i < end; i++) {
					final SegmentInfo info = levels.get(i).info;
					anyTooLarge |= (size(info) >= maxMergeSize || sizeDocs(info) >= maxMergeDocs);
					if (mergingSegments.contains(info)) {
						anyMerging = true;
						break;
					}
				}

				if (anyMerging) {

				} else if (!anyTooLarge) {
					if (spec == null)
						spec = new MergeSpecification();
					final List<SegmentInfo> mergeInfos = new ArrayList<SegmentInfo>();
					for (int i = start; i < end; i++) {
						mergeInfos.add(levels.get(i).info);
						assert infos.contains(levels.get(i).info);
					}
					if (verbose()) {
						message("  add merge="
								+ writer.get().segString(mergeInfos)
								+ " start=" + start + " end=" + end);
					}
					spec.add(new OneMerge(mergeInfos));
				} else if (verbose()) {
					message("    "
							+ start
							+ " to "
							+ end
							+ ": contains segment over maxMergeSize or maxMergeDocs; skipping");
				}

				start = end;
				end = start + mergeFactor;
			}

			start = 1 + upto;
		}

		return spec;
	}

	public void setMaxMergeDocs(int maxMergeDocs) {
		this.maxMergeDocs = maxMergeDocs;
	}

	public int getMaxMergeDocs() {
		return maxMergeDocs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[" + getClass().getSimpleName()
				+ ": ");
		sb.append("minMergeSize=").append(minMergeSize).append(", ");
		sb.append("mergeFactor=").append(mergeFactor).append(", ");
		sb.append("maxMergeSize=").append(maxMergeSize).append(", ");
		sb.append("maxMergeSizeForForcedMerge=")
				.append(maxMergeSizeForForcedMerge).append(", ");
		sb.append("calibrateSizeByDeletes=").append(calibrateSizeByDeletes)
				.append(", ");
		sb.append("maxMergeDocs=").append(maxMergeDocs).append(", ");
		sb.append("useCompoundFile=").append(useCompoundFile).append(", ");
		sb.append("noCFSRatio=").append(noCFSRatio);
		sb.append("]");
		return sb.toString();
	}

}
