
/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.util.PriorityQueue;

public class TopDocs implements java.io.Serializable {

	public int totalHits;

	public ScoreDoc[] scoreDocs;

	private float maxScore;

	public float getMaxScore() {
		return maxScore;
	}

	public void setMaxScore(float maxScore) {
		this.maxScore = maxScore;
	}

	TopDocs(int totalHits, ScoreDoc[] scoreDocs) {
		this(totalHits, scoreDocs, Float.NaN);
	}

	public TopDocs(int totalHits, ScoreDoc[] scoreDocs, float maxScore) {
		this.totalHits = totalHits;
		this.scoreDocs = scoreDocs;
		this.maxScore = maxScore;
	}

	private static class ShardRef {

		final int shardIndex;

		int hitIndex;

		public ShardRef(int shardIndex) {
			this.shardIndex = shardIndex;
		}

		@Override
		public String toString() {
			return "ShardRef(shardIndex=" + shardIndex + " hitIndex="
					+ hitIndex + ")";
		}
	};

	private static class ScoreMergeSortQueue extends PriorityQueue<ShardRef> {
		final ScoreDoc[][] shardHits;

		public ScoreMergeSortQueue(TopDocs[] shardHits) {
			initialize(shardHits.length);
			this.shardHits = new ScoreDoc[shardHits.length][];
			for (int shardIDX = 0; shardIDX < shardHits.length; shardIDX++) {
				this.shardHits[shardIDX] = shardHits[shardIDX].scoreDocs;
			}
		}

		public boolean lessThan(ShardRef first, ShardRef second) {
			assert first != second;
			final float firstScore = shardHits[first.shardIndex][first.hitIndex].score;
			final float secondScore = shardHits[second.shardIndex][second.hitIndex].score;

			if (firstScore < secondScore) {
				return false;
			} else if (firstScore > secondScore) {
				return true;
			} else {

				if (first.shardIndex < second.shardIndex) {
					return true;
				} else if (first.shardIndex > second.shardIndex) {
					return false;
				} else {

					assert first.hitIndex != second.hitIndex;
					return first.hitIndex < second.hitIndex;
				}
			}
		}
	}

	private static class MergeSortQueue extends PriorityQueue<ShardRef> {

		final ScoreDoc[][] shardHits;
		final FieldComparator[] comparators;
		final int[] reverseMul;

		public MergeSortQueue(Sort sort, TopDocs[] shardHits)
				throws IOException {
			initialize(shardHits.length);
			this.shardHits = new ScoreDoc[shardHits.length][];
			for (int shardIDX = 0; shardIDX < shardHits.length; shardIDX++) {
				final ScoreDoc[] shard = shardHits[shardIDX].scoreDocs;

				if (shard != null) {
					this.shardHits[shardIDX] = shard;

					for (int hitIDX = 0; hitIDX < shard.length; hitIDX++) {
						final ScoreDoc sd = shard[hitIDX];
						if (!(sd instanceof FieldDoc)) {
							throw new IllegalArgumentException(
									"shard "
											+ shardIDX
											+ " was not sorted by the provided Sort (expected FieldDoc but got ScoreDoc)");
						}
						final FieldDoc fd = (FieldDoc) sd;
						if (fd.fields == null) {
							throw new IllegalArgumentException(
									"shard "
											+ shardIDX
											+ " did not set sort field values (FieldDoc.fields is null); you must pass fillFields=true to IndexSearcher.search on each shard");
						}
					}
				}
			}

			final SortField[] sortFields = sort.getSort();
			comparators = new FieldComparator[sortFields.length];
			reverseMul = new int[sortFields.length];
			for (int compIDX = 0; compIDX < sortFields.length; compIDX++) {
				final SortField sortField = sortFields[compIDX];
				comparators[compIDX] = sortField.getComparator(1, compIDX);
				reverseMul[compIDX] = sortField.getReverse() ? -1 : 1;
			}
		}

		@SuppressWarnings("unchecked")
		public boolean lessThan(ShardRef first, ShardRef second) {
			assert first != second;
			final FieldDoc firstFD = (FieldDoc) shardHits[first.shardIndex][first.hitIndex];
			final FieldDoc secondFD = (FieldDoc) shardHits[second.shardIndex][second.hitIndex];

			for (int compIDX = 0; compIDX < comparators.length; compIDX++) {
				final FieldComparator comp = comparators[compIDX];

				final int cmp = reverseMul[compIDX]
						* comp.compareValues(firstFD.fields[compIDX],
								secondFD.fields[compIDX]);

				if (cmp != 0) {

					return cmp < 0;
				}
			}

			if (first.shardIndex < second.shardIndex) {

				return true;
			} else if (first.shardIndex > second.shardIndex) {

				return false;
			} else {

				assert first.hitIndex != second.hitIndex;
				return first.hitIndex < second.hitIndex;
			}
		}
	}

	public static TopDocs merge(Sort sort, int topN, TopDocs[] shardHits)
			throws IOException {

		final PriorityQueue<ShardRef> queue;
		if (sort == null) {
			queue = new ScoreMergeSortQueue(shardHits);
		} else {
			queue = new MergeSortQueue(sort, shardHits);
		}

		int totalHitCount = 0;
		int availHitCount = 0;
		float maxScore = Float.MIN_VALUE;
		for (int shardIDX = 0; shardIDX < shardHits.length; shardIDX++) {
			final TopDocs shard = shardHits[shardIDX];
			if (shard.scoreDocs != null && shard.scoreDocs.length > 0) {
				totalHitCount += shard.totalHits;
				availHitCount += shard.scoreDocs.length;
				queue.add(new ShardRef(shardIDX));
				maxScore = Math.max(maxScore, shard.getMaxScore());

			}
		}

		final ScoreDoc[] hits = new ScoreDoc[Math.min(topN, availHitCount)];

		int hitUpto = 0;
		while (hitUpto < hits.length) {
			assert queue.size() > 0;
			ShardRef ref = queue.pop();
			final ScoreDoc hit = shardHits[ref.shardIndex].scoreDocs[ref.hitIndex++];
			hit.shardIndex = ref.shardIndex;
			hits[hitUpto] = hit;

			hitUpto++;

			if (ref.hitIndex < shardHits[ref.shardIndex].scoreDocs.length) {

				queue.add(ref);
			}
		}

		if (sort == null) {
			return new TopDocs(totalHitCount, hits, maxScore);
		} else {
			return new TopFieldDocs(totalHitCount, hits, sort.getSort(),
					maxScore);
		}
	}
}
