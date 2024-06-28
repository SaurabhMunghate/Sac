/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.List;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.BooleanClause.Occur;

final class BooleanScorer extends Scorer {

	private static final class BooleanScorerCollector extends Collector {
		private BucketTable bucketTable;
		private int mask;
		private Scorer scorer;

		public BooleanScorerCollector(int mask, BucketTable bucketTable) {
			this.mask = mask;
			this.bucketTable = bucketTable;
		}

		@Override
		public void collect(final int doc) throws IOException {
			final BucketTable table = bucketTable;
			final int i = doc & BucketTable.MASK;
			final Bucket bucket = table.buckets[i];

			if (bucket.doc != doc) {
				bucket.doc = doc;
				bucket.score = scorer.score();
				bucket.bits = mask;
				bucket.coord = 1;

				bucket.next = table.first;
				table.first = bucket;
			} else {
				bucket.score += scorer.score();
				bucket.bits |= mask;
				bucket.coord++;
			}
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase) {

		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static final class BucketScorer extends Scorer {

		float score;
		int doc = NO_MORE_DOCS;
		int freq;

		public BucketScorer(Weight weight) {
			super(weight);
		}

		@Override
		public int advance(int target) throws IOException {
			return NO_MORE_DOCS;
		}

		@Override
		public int docID() {
			return doc;
		}

		@Override
		public float freq() {
			return freq;
		}

		@Override
		public int nextDoc() throws IOException {
			return NO_MORE_DOCS;
		}

		@Override
		public float score() throws IOException {
			return score;
		}

	}

	static final class Bucket {
		int doc = -1;
		float score;

		int bits;
		int coord;
		Bucket next;
	}

	static final class BucketTable {
		public static final int SIZE = 1 << 11;
		public static final int MASK = SIZE - 1;

		final Bucket[] buckets = new Bucket[SIZE];
		Bucket first = null;

		public BucketTable() {

			for (int idx = 0; idx < SIZE; idx++) {
				buckets[idx] = new Bucket();
			}
		}

		public Collector newCollector(int mask) {
			return new BooleanScorerCollector(mask, this);
		}

		public int size() {
			return SIZE;
		}
	}

	static final class SubScorer {
		public Scorer scorer;

		public boolean prohibited;
		public Collector collector;
		public SubScorer next;

		public SubScorer(Scorer scorer, boolean required, boolean prohibited,
				Collector collector, SubScorer next) throws IOException {
			if (required) {
				throw new IllegalArgumentException(
						"this scorer cannot handle required=true");
			}
			this.scorer = scorer;

			this.prohibited = prohibited;
			this.collector = collector;
			this.next = next;
		}
	}

	private SubScorer scorers = null;
	private BucketTable bucketTable = new BucketTable();
	private final float[] coordFactors;

	private final int minNrShouldMatch;
	private int end;
	private Bucket current;
	private int doc = -1;

	private static final int PROHIBITED_MASK = 1;

	BooleanScorer(Weight weight, boolean disableCoord, Similarity similarity,
			int minNrShouldMatch, List<Scorer> optionalScorers,
			List<Scorer> prohibitedScorers, int maxCoord) throws IOException {
		super(weight);
		this.minNrShouldMatch = minNrShouldMatch;

		if (optionalScorers != null && optionalScorers.size() > 0) {
			for (Scorer scorer : optionalScorers) {
				if (scorer.nextDoc() != NO_MORE_DOCS) {
					scorers = new SubScorer(scorer, false, false,
							bucketTable.newCollector(0), scorers);
				}
			}
		}

		if (prohibitedScorers != null && prohibitedScorers.size() > 0) {
			for (Scorer scorer : prohibitedScorers) {
				if (scorer.nextDoc() != NO_MORE_DOCS) {
					scorers = new SubScorer(scorer, false, true,
							bucketTable.newCollector(PROHIBITED_MASK), scorers);
				}
			}
		}

		coordFactors = new float[optionalScorers.size() + 1];
		for (int i = 0; i < coordFactors.length; i++) {
			coordFactors[i] = disableCoord ? 1.0f : similarity.coord(i,
					maxCoord);
		}
	}

	@Override
	protected boolean score(Collector collector, int max, int firstDocID)
			throws IOException {

		assert firstDocID == -1;
		boolean more;
		Bucket tmp;
		BucketScorer bs = new BucketScorer(weight);

		collector.setScorer(bs);
		do {
			bucketTable.first = null;

			while (current != null) {

				if ((current.bits & PROHIBITED_MASK) == 0) {

					if (current.doc >= max) {
						tmp = current;
						current = current.next;
						tmp.next = bucketTable.first;
						bucketTable.first = tmp;
						continue;
					}

					if (current.coord >= minNrShouldMatch) {
						bs.score = current.score * coordFactors[current.coord];
						bs.doc = current.doc;
						bs.freq = current.coord;
						collector.collect(current.doc);
					}
				}

				current = current.next;
			}

			if (bucketTable.first != null) {
				current = bucketTable.first;
				bucketTable.first = current.next;
				return true;
			}

			more = false;
			end += BucketTable.SIZE;
			for (SubScorer sub = scorers; sub != null; sub = sub.next) {
				int subScorerDocID = sub.scorer.docID();
				if (subScorerDocID != NO_MORE_DOCS) {
					more |= sub.scorer
							.score(sub.collector, end, subScorerDocID);
				}
			}
			current = bucketTable.first;

		} while (current != null || more);

		return false;
	}

	@Override
	public int advance(int target) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int docID() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int nextDoc() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public float score() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void score(Collector collector) throws IOException {
		score(collector, Integer.MAX_VALUE, -1);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("boolean(");
		for (SubScorer sub = scorers; sub != null; sub = sub.next) {
			buffer.append(sub.scorer.toString());
			buffer.append(" ");
		}
		buffer.append(")");
		return buffer.toString();
	}

	@Override
	protected void visitSubScorers(Query parent, Occur relationship,
			ScorerVisitor<Query, Query, Scorer> visitor) {
		super.visitSubScorers(parent, relationship, visitor);
		final Query q = weight.getQuery();
		SubScorer sub = scorers;
		while (sub != null) {

			if (!sub.prohibited) {
				relationship = Occur.SHOULD;
			} else {

				relationship = Occur.MUST_NOT;
			}
			sub.scorer.visitSubScorers(q, relationship, visitor);
			sub = sub.next;
		}
	}

}
