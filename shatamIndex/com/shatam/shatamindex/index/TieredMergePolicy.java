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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class TieredMergePolicy extends MergePolicy {

	private int maxMergeAtOnce = 10;
	private long maxMergedSegmentBytes = 5 * 1024 * 1024 * 1024L;
	private int maxMergeAtOnceExplicit = 30;

	private long floorSegmentBytes = 2 * 1024 * 1024L;
	private double segsPerTier = 10.0;
	private double forceMergeDeletesPctAllowed = 10.0;
	private boolean useCompoundFile = true;
	private double noCFSRatio = 0.1;
	private double reclaimDeletesWeight = 2.0;

	public TieredMergePolicy setMaxMergeAtOnce(int v) {
		if (v < 2) {
			throw new IllegalArgumentException(
					"maxMergeAtOnce must be > 1 (got " + v + ")");
		}
		maxMergeAtOnce = v;
		return this;
	}

	public int getMaxMergeAtOnce() {
		return maxMergeAtOnce;
	}

	public TieredMergePolicy setMaxMergeAtOnceExplicit(int v) {
		if (v < 2) {
			throw new IllegalArgumentException(
					"maxMergeAtOnceExplicit must be > 1 (got " + v + ")");
		}
		maxMergeAtOnceExplicit = v;
		return this;
	}

	public int getMaxMergeAtOnceExplicit() {
		return maxMergeAtOnceExplicit;
	}

	public TieredMergePolicy setMaxMergedSegmentMB(double v) {
		maxMergedSegmentBytes = (long) (v * 1024 * 1024);
		return this;
	}

	public double getMaxMergedSegmentMB() {
		return maxMergedSegmentBytes / 1024 / 1024.;
	}

	public TieredMergePolicy setReclaimDeletesWeight(double v) {
		if (v < 0.0) {
			throw new IllegalArgumentException(
					"reclaimDeletesWeight must be >= 0.0 (got " + v + ")");
		}
		reclaimDeletesWeight = v;
		return this;
	}

	public double getReclaimDeletesWeight() {
		return reclaimDeletesWeight;
	}

	public TieredMergePolicy setFloorSegmentMB(double v) {
		if (v <= 0.0) {
			throw new IllegalArgumentException(
					"floorSegmentMB must be >= 0.0 (got " + v + ")");
		}
		floorSegmentBytes = (long) (v * 1024 * 1024);
		return this;
	}

	public double getFloorSegmentMB() {
		return floorSegmentBytes / 1024 * 1024.;
	}

	public TieredMergePolicy setForceMergeDeletesPctAllowed(double v) {
		if (v < 0.0 || v > 100.0) {
			throw new IllegalArgumentException(
					"forceMergeDeletesPctAllowed must be between 0.0 and 100.0 inclusive (got "
							+ v + ")");
		}
		forceMergeDeletesPctAllowed = v;
		return this;
	}

	public double getForceMergeDeletesPctAllowed() {
		return forceMergeDeletesPctAllowed;
	}

	public TieredMergePolicy setSegmentsPerTier(double v) {
		if (v < 2.0) {
			throw new IllegalArgumentException(
					"segmentsPerTier must be >= 2.0 (got " + v + ")");
		}
		segsPerTier = v;
		return this;
	}

	public double getSegmentsPerTier() {
		return segsPerTier;
	}

	public TieredMergePolicy setUseCompoundFile(boolean useCompoundFile) {
		this.useCompoundFile = useCompoundFile;
		return this;
	}

	public boolean getUseCompoundFile() {
		return useCompoundFile;
	}

	public TieredMergePolicy setNoCFSRatio(double noCFSRatio) {
		if (noCFSRatio < 0.0 || noCFSRatio > 1.0) {
			throw new IllegalArgumentException(
					"noCFSRatio must be 0.0 to 1.0 inclusive; got "
							+ noCFSRatio);
		}
		this.noCFSRatio = noCFSRatio;
		return this;
	}

	public double getNoCFSRatio() {
		return noCFSRatio;
	}

	private class SegmentByteSizeDescending implements Comparator<SegmentInfo> {
		public int compare(SegmentInfo o1, SegmentInfo o2) {
			try {
				final long sz1 = size(o1);
				final long sz2 = size(o2);
				if (sz1 > sz2) {
					return -1;
				} else if (sz2 > sz1) {
					return 1;
				} else {
					return o1.name.compareTo(o2.name);
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	private final Comparator<SegmentInfo> segmentByteSizeDescending = new SegmentByteSizeDescending();

	protected static abstract class MergeScore {
		abstract double getScore();

		abstract String getExplanation();
	}

	@Override
	public MergeSpecification findMerges(SegmentInfos infos) throws IOException {
		if (verbose()) {
			message("findMerges: " + infos.size() + " segments");
		}
		if (infos.size() == 0) {
			return null;
		}
		final Collection<SegmentInfo> merging = writer.get()
				.getMergingSegments();
		final Collection<SegmentInfo> toBeMerged = new HashSet<SegmentInfo>();

		final List<SegmentInfo> infosSorted = new ArrayList<SegmentInfo>(
				infos.asList());
		Collections.sort(infosSorted, segmentByteSizeDescending);

		long totIndexBytes = 0;
		long minSegmentBytes = Long.MAX_VALUE;
		for (SegmentInfo info : infosSorted) {
			final long segBytes = size(info);
			if (verbose()) {
				String extra = merging.contains(info) ? " [merging]" : "";
				if (segBytes >= maxMergedSegmentBytes / 2.0) {
					extra += " [skip: too large]";
				} else if (segBytes < floorSegmentBytes) {
					extra += " [floored]";
				}
				message("  seg=" + writer.get().segString(info) + " size="
						+ String.format("%.3f", segBytes / 1024 / 1024.)
						+ " MB" + extra);
			}

			minSegmentBytes = Math.min(segBytes, minSegmentBytes);

			totIndexBytes += segBytes;
		}

		int tooBigCount = 0;
		while (tooBigCount < infosSorted.size()
				&& size(infosSorted.get(tooBigCount)) >= maxMergedSegmentBytes / 2.0) {
			totIndexBytes -= size(infosSorted.get(tooBigCount));
			tooBigCount++;
		}

		minSegmentBytes = floorSize(minSegmentBytes);

		long levelSize = minSegmentBytes;
		long bytesLeft = totIndexBytes;
		double allowedSegCount = 0;
		while (true) {
			final double segCountLevel = bytesLeft / (double) levelSize;
			if (segCountLevel < segsPerTier) {
				allowedSegCount += Math.ceil(segCountLevel);
				break;
			}
			allowedSegCount += segsPerTier;
			bytesLeft -= segsPerTier * levelSize;
			levelSize *= maxMergeAtOnce;
		}
		int allowedSegCountInt = (int) allowedSegCount;

		MergeSpecification spec = null;

		while (true) {

			long mergingBytes = 0;

			final List<SegmentInfo> eligible = new ArrayList<SegmentInfo>();
			for (int idx = tooBigCount; idx < infosSorted.size(); idx++) {
				final SegmentInfo info = infosSorted.get(idx);
				if (merging.contains(info)) {
					mergingBytes += info.sizeInBytes(true);
				} else if (!toBeMerged.contains(info)) {
					eligible.add(info);
				}
			}

			final boolean maxMergeIsRunning = mergingBytes >= maxMergedSegmentBytes;

			message("  allowedSegmentCount=" + allowedSegCountInt
					+ " vs count=" + infosSorted.size() + " (eligible count="
					+ eligible.size() + ") tooBigCount=" + tooBigCount);

			if (eligible.size() == 0) {
				return spec;
			}

			if (eligible.size() >= allowedSegCountInt) {

				MergeScore bestScore = null;
				List<SegmentInfo> best = null;
				boolean bestTooLarge = false;
				long bestMergeBytes = 0;

				for (int startIdx = 0; startIdx <= eligible.size()
						- maxMergeAtOnce; startIdx++) {

					long totAfterMergeBytes = 0;

					final List<SegmentInfo> candidate = new ArrayList<SegmentInfo>();
					boolean hitTooLarge = false;
					for (int idx = startIdx; idx < eligible.size()
							&& candidate.size() < maxMergeAtOnce; idx++) {
						final SegmentInfo info = eligible.get(idx);
						final long segBytes = size(info);

						if (totAfterMergeBytes + segBytes > maxMergedSegmentBytes) {
							hitTooLarge = true;

							continue;
						}
						candidate.add(info);
						totAfterMergeBytes += segBytes;
					}

					final MergeScore score = score(candidate, hitTooLarge,
							mergingBytes);
					message("  maybe="
							+ writer.get().segString(candidate)
							+ " score="
							+ score.getScore()
							+ " "
							+ score.getExplanation()
							+ " tooLarge="
							+ hitTooLarge
							+ " size="
							+ String.format("%.3f MB",
									totAfterMergeBytes / 1024. / 1024.));

					if ((bestScore == null || score.getScore() < bestScore
							.getScore())
							&& (!hitTooLarge || !maxMergeIsRunning)) {
						best = candidate;
						bestScore = score;
						bestTooLarge = hitTooLarge;
						bestMergeBytes = totAfterMergeBytes;
					}
				}

				if (best != null) {
					if (spec == null) {
						spec = new MergeSpecification();
					}
					final OneMerge merge = new OneMerge(best);
					spec.add(merge);
					for (SegmentInfo info : merge.segments) {
						toBeMerged.add(info);
					}

					if (verbose()) {
						message("  add merge="
								+ writer.get().segString(merge.segments)
								+ " size="
								+ String.format("%.3f MB",
										bestMergeBytes / 1024. / 1024.)
								+ " score="
								+ String.format("%.3f", bestScore.getScore())
								+ " " + bestScore.getExplanation()
								+ (bestTooLarge ? " [max merge]" : ""));
					}
				} else {
					return spec;
				}
			} else {
				return spec;
			}
		}
	}

	protected MergeScore score(List<SegmentInfo> candidate,
			boolean hitTooLarge, long mergingBytes) throws IOException {
		long totBeforeMergeBytes = 0;
		long totAfterMergeBytes = 0;
		long totAfterMergeBytesFloored = 0;
		for (SegmentInfo info : candidate) {
			final long segBytes = size(info);
			totAfterMergeBytes += segBytes;
			totAfterMergeBytesFloored += floorSize(segBytes);
			totBeforeMergeBytes += info.sizeInBytes(true);
		}

		final double skew;
		if (hitTooLarge) {

			skew = 1.0 / maxMergeAtOnce;
		} else {
			skew = ((double) floorSize(size(candidate.get(0))))
					/ totAfterMergeBytesFloored;
		}

		double mergeScore = skew;

		mergeScore *= Math.pow(totAfterMergeBytes, 0.05);

		final double nonDelRatio = ((double) totAfterMergeBytes)
				/ totBeforeMergeBytes;
		mergeScore *= Math.pow(nonDelRatio, reclaimDeletesWeight);

		final double finalMergeScore = mergeScore;

		return new MergeScore() {

			@Override
			public double getScore() {
				return finalMergeScore;
			}

			@Override
			public String getExplanation() {
				return "skew=" + String.format("%.3f", skew) + " nonDelRatio="
						+ String.format("%.3f", nonDelRatio);
			}
		};
	}

	@Override
	public MergeSpecification findForcedMerges(SegmentInfos infos,
			int maxSegmentCount, Map<SegmentInfo, Boolean> segmentsToMerge)
			throws IOException {
		if (verbose()) {
			message("findForcedMerges maxSegmentCount=" + maxSegmentCount
					+ " infos=" + writer.get().segString(infos)
					+ " segmentsToMerge=" + segmentsToMerge);
		}

		List<SegmentInfo> eligible = new ArrayList<SegmentInfo>();
		boolean forceMergeRunning = false;
		final Collection<SegmentInfo> merging = writer.get()
				.getMergingSegments();
		boolean segmentIsOriginal = false;
		for (SegmentInfo info : infos) {
			final Boolean isOriginal = segmentsToMerge.get(info);
			if (isOriginal != null) {
				segmentIsOriginal = isOriginal;
				if (!merging.contains(info)) {
					eligible.add(info);
				} else {
					forceMergeRunning = true;
				}
			}
		}

		if (eligible.size() == 0) {
			return null;
		}

		if ((maxSegmentCount > 1 && eligible.size() <= maxSegmentCount)
				|| (maxSegmentCount == 1 && eligible.size() == 1 && (!segmentIsOriginal || isMerged(eligible
						.get(0))))) {
			if (verbose()) {
				message("already merged");
			}
			return null;
		}

		Collections.sort(eligible, segmentByteSizeDescending);

		if (verbose()) {
			message("eligible=" + eligible);
			message("forceMergeRunning=" + forceMergeRunning);
		}

		int end = eligible.size();

		MergeSpecification spec = null;

		while (end >= maxMergeAtOnceExplicit + maxSegmentCount - 1) {
			if (spec == null) {
				spec = new MergeSpecification();
			}
			final OneMerge merge = new OneMerge(eligible.subList(end
					- maxMergeAtOnceExplicit, end));
			if (verbose()) {
				message("add merge=" + writer.get().segString(merge.segments));
			}
			spec.add(merge);
			end -= maxMergeAtOnceExplicit;
		}

		if (spec == null && !forceMergeRunning) {

			final int numToMerge = end - maxSegmentCount + 1;
			final OneMerge merge = new OneMerge(eligible.subList(end
					- numToMerge, end));
			if (verbose()) {
				message("add final merge="
						+ merge.segString(writer.get().getDirectory()));
			}
			spec = new MergeSpecification();
			spec.add(merge);
		}

		return spec;
	}

	@Override
	public MergeSpecification findForcedDeletesMerges(SegmentInfos infos)
			throws CorruptIndexException, IOException {
		if (verbose()) {
			message("findForcedDeletesMerges infos="
					+ writer.get().segString(infos)
					+ " forceMergeDeletesPctAllowed="
					+ forceMergeDeletesPctAllowed);
		}
		final List<SegmentInfo> eligible = new ArrayList<SegmentInfo>();
		final Collection<SegmentInfo> merging = writer.get()
				.getMergingSegments();
		for (SegmentInfo info : infos) {
			double pctDeletes = 100.
					* ((double) writer.get().numDeletedDocs(info))
					/ info.docCount;
			if (pctDeletes > forceMergeDeletesPctAllowed
					&& !merging.contains(info)) {
				eligible.add(info);
			}
		}

		if (eligible.size() == 0) {
			return null;
		}

		Collections.sort(eligible, segmentByteSizeDescending);

		if (verbose()) {
			message("eligible=" + eligible);
		}

		int start = 0;
		MergeSpecification spec = null;

		while (start < eligible.size()) {

			final int end = Math.min(start + maxMergeAtOnceExplicit,
					eligible.size());
			if (spec == null) {
				spec = new MergeSpecification();
			}

			final OneMerge merge = new OneMerge(eligible.subList(start, end));
			if (verbose()) {
				message("add merge=" + writer.get().segString(merge.segments));
			}
			spec.add(merge);
			start = end;
		}

		return spec;
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

	@Override
	public void close() {
	}

	private boolean isMerged(SegmentInfo info) throws IOException {
		IndexWriter w = writer.get();
		assert w != null;
		boolean hasDeletions = w.numDeletedDocs(info) > 0;
		return !hasDeletions
				&& !info.hasSeparateNorms()
				&& info.dir == w.getDirectory()
				&& (info.getUseCompoundFile() == useCompoundFile || noCFSRatio < 1.0);
	}

	private long size(SegmentInfo info) throws IOException {
		final long byteSize = info.sizeInBytes(true);
		final int delCount = writer.get().numDeletedDocs(info);
		final double delRatio = (info.docCount <= 0 ? 0.0f
				: ((double) delCount / (double) info.docCount));
		assert delRatio <= 1.0;
		return (long) (byteSize * (1.0 - delRatio));
	}

	private long floorSize(long bytes) {
		return Math.max(floorSegmentBytes, bytes);
	}

	private boolean verbose() {
		IndexWriter w = writer.get();
		return w != null && w.verbose();
	}

	private void message(String message) {
		if (verbose()) {
			writer.get().message("TMP: " + message);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[" + getClass().getSimpleName()
				+ ": ");
		sb.append("maxMergeAtOnce=").append(maxMergeAtOnce).append(", ");
		sb.append("maxMergeAtOnceExplicit=").append(maxMergeAtOnceExplicit)
				.append(", ");
		sb.append("maxMergedSegmentMB=")
				.append(maxMergedSegmentBytes / 1024 / 1024.).append(", ");
		sb.append("floorSegmentMB=").append(floorSegmentBytes / 1024 / 1024.)
				.append(", ");
		sb.append("forceMergeDeletesPctAllowed=")
				.append(forceMergeDeletesPctAllowed).append(", ");
		sb.append("segmentsPerTier=").append(segsPerTier).append(", ");
		sb.append("useCompoundFile=").append(useCompoundFile).append(", ");
		sb.append("noCFSRatio=").append(noCFSRatio);
		return sb.toString();
	}
}
