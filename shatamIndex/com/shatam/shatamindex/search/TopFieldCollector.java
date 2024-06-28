/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.FieldValueHitQueue.Entry;
import com.shatam.shatamindex.util.PriorityQueue;

public abstract class TopFieldCollector extends TopDocsCollector<Entry> {

	private static class OneComparatorNonScoringCollector extends
			TopFieldCollector {

		final FieldComparator comparator;
		final int reverseMul;

		public OneComparatorNonScoringCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
			comparator = queue.getComparators()[0];
			reverseMul = queue.getReverseMul()[0];
		}

		final void updateBottom(int doc) {

			bottom.doc = docBase + doc;
			bottom = pq.updateTop();
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {
				if ((reverseMul * comparator.compareBottom(doc)) <= 0) {

					return;
				}

				comparator.copy(bottom.slot, doc);
				updateBottom(doc);
				comparator.setBottom(bottom.slot);
			} else {

				final int slot = totalHits - 1;

				comparator.copy(slot, doc);
				add(slot, doc, Float.NaN);
				if (queueFull) {
					comparator.setBottom(bottom.slot);
				}
			}
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {
			this.docBase = docBase;
			comparator.setNextReader(reader, docBase);
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			comparator.setScorer(scorer);
		}

	}

	private static class OutOfOrderOneComparatorNonScoringCollector extends
			OneComparatorNonScoringCollector {

		public OutOfOrderOneComparatorNonScoringCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {

				final int cmp = reverseMul * comparator.compareBottom(doc);
				if (cmp < 0 || (cmp == 0 && doc + docBase > bottom.doc)) {
					return;
				}

				comparator.copy(bottom.slot, doc);
				updateBottom(doc);
				comparator.setBottom(bottom.slot);
			} else {

				final int slot = totalHits - 1;

				comparator.copy(slot, doc);
				add(slot, doc, Float.NaN);
				if (queueFull) {
					comparator.setBottom(bottom.slot);
				}
			}
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static class OneComparatorScoringNoMaxScoreCollector extends
			OneComparatorNonScoringCollector {

		Scorer scorer;

		public OneComparatorScoringNoMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		final void updateBottom(int doc, float score) {
			bottom.doc = docBase + doc;
			bottom.score = score;
			bottom = pq.updateTop();
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {
				if ((reverseMul * comparator.compareBottom(doc)) <= 0) {

					return;
				}

				final float score = scorer.score();

				comparator.copy(bottom.slot, doc);
				updateBottom(doc, score);
				comparator.setBottom(bottom.slot);
			} else {

				final float score = scorer.score();

				final int slot = totalHits - 1;

				comparator.copy(slot, doc);
				add(slot, doc, score);
				if (queueFull) {
					comparator.setBottom(bottom.slot);
				}
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
			comparator.setScorer(scorer);
		}

	}

	private static class OutOfOrderOneComparatorScoringNoMaxScoreCollector
			extends OneComparatorScoringNoMaxScoreCollector {

		public OutOfOrderOneComparatorScoringNoMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {

				final int cmp = reverseMul * comparator.compareBottom(doc);
				if (cmp < 0 || (cmp == 0 && doc + docBase > bottom.doc)) {
					return;
				}

				final float score = scorer.score();

				comparator.copy(bottom.slot, doc);
				updateBottom(doc, score);
				comparator.setBottom(bottom.slot);
			} else {

				final float score = scorer.score();

				final int slot = totalHits - 1;

				comparator.copy(slot, doc);
				add(slot, doc, score);
				if (queueFull) {
					comparator.setBottom(bottom.slot);
				}
			}
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static class OneComparatorScoringMaxScoreCollector extends
			OneComparatorNonScoringCollector {

		Scorer scorer;

		public OneComparatorScoringMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);

			maxScore = Float.NEGATIVE_INFINITY;
		}

		final void updateBottom(int doc, float score) {
			bottom.doc = docBase + doc;
			bottom.score = score;
			bottom = pq.updateTop();
		}

		@Override
		public void collect(int doc) throws IOException {
			final float score = scorer.score();
			if (score > maxScore) {
				maxScore = score;
			}
			++totalHits;
			if (queueFull) {
				if ((reverseMul * comparator.compareBottom(doc)) <= 0) {

					return;
				}

				comparator.copy(bottom.slot, doc);
				updateBottom(doc, score);
				comparator.setBottom(bottom.slot);
			} else {

				final int slot = totalHits - 1;

				comparator.copy(slot, doc);
				add(slot, doc, score);
				if (queueFull) {
					comparator.setBottom(bottom.slot);
				}
			}

		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
			super.setScorer(scorer);
		}
	}

	private static class OutOfOrderOneComparatorScoringMaxScoreCollector extends
			OneComparatorScoringMaxScoreCollector {

		public OutOfOrderOneComparatorScoringMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		@Override
		public void collect(int doc) throws IOException {
			final float score = scorer.score();
			if (score > maxScore) {
				maxScore = score;
			}
			++totalHits;
			if (queueFull) {

				final int cmp = reverseMul * comparator.compareBottom(doc);
				if (cmp < 0 || (cmp == 0 && doc + docBase > bottom.doc)) {
					return;
				}

				comparator.copy(bottom.slot, doc);
				updateBottom(doc, score);
				comparator.setBottom(bottom.slot);
			} else {

				final int slot = totalHits - 1;

				comparator.copy(slot, doc);
				add(slot, doc, score);
				if (queueFull) {
					comparator.setBottom(bottom.slot);
				}
			}
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static class MultiComparatorNonScoringCollector extends
			TopFieldCollector {

		final FieldComparator[] comparators;
		final int[] reverseMul;

		public MultiComparatorNonScoringCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
			comparators = queue.getComparators();
			reverseMul = queue.getReverseMul();
		}

		final void updateBottom(int doc) {

			bottom.doc = docBase + doc;
			bottom = pq.updateTop();
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {

				for (int i = 0;; i++) {
					final int c = reverseMul[i]
							* comparators[i].compareBottom(doc);
					if (c < 0) {

						return;
					} else if (c > 0) {

						break;
					} else if (i == comparators.length - 1) {

						return;
					}
				}

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(bottom.slot, doc);
				}

				updateBottom(doc);

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].setBottom(bottom.slot);
				}
			} else {

				final int slot = totalHits - 1;

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(slot, doc);
				}
				add(slot, doc, Float.NaN);
				if (queueFull) {
					for (int i = 0; i < comparators.length; i++) {
						comparators[i].setBottom(bottom.slot);
					}
				}
			}
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {
			this.docBase = docBase;
			for (int i = 0; i < comparators.length; i++) {
				comparators[i].setNextReader(reader, docBase);
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {

			for (int i = 0; i < comparators.length; i++) {
				comparators[i].setScorer(scorer);
			}
		}
	}

	private static class OutOfOrderMultiComparatorNonScoringCollector extends
			MultiComparatorNonScoringCollector {

		public OutOfOrderMultiComparatorNonScoringCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {

				for (int i = 0;; i++) {
					final int c = reverseMul[i]
							* comparators[i].compareBottom(doc);
					if (c < 0) {

						return;
					} else if (c > 0) {

						break;
					} else if (i == comparators.length - 1) {

						if (doc + docBase > bottom.doc) {

							return;
						}
						break;
					}
				}

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(bottom.slot, doc);
				}

				updateBottom(doc);

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].setBottom(bottom.slot);
				}
			} else {

				final int slot = totalHits - 1;

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(slot, doc);
				}
				add(slot, doc, Float.NaN);
				if (queueFull) {
					for (int i = 0; i < comparators.length; i++) {
						comparators[i].setBottom(bottom.slot);
					}
				}
			}
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static class MultiComparatorScoringMaxScoreCollector extends
			MultiComparatorNonScoringCollector {

		Scorer scorer;

		public MultiComparatorScoringMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);

			maxScore = Float.NEGATIVE_INFINITY;
		}

		final void updateBottom(int doc, float score) {
			bottom.doc = docBase + doc;
			bottom.score = score;
			bottom = pq.updateTop();
		}

		@Override
		public void collect(int doc) throws IOException {
			final float score = scorer.score();
			if (score > maxScore) {
				maxScore = score;
			}
			++totalHits;
			if (queueFull) {

				for (int i = 0;; i++) {
					final int c = reverseMul[i]
							* comparators[i].compareBottom(doc);
					if (c < 0) {

						return;
					} else if (c > 0) {

						break;
					} else if (i == comparators.length - 1) {

						return;
					}
				}

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(bottom.slot, doc);
				}

				updateBottom(doc, score);

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].setBottom(bottom.slot);
				}
			} else {

				final int slot = totalHits - 1;

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(slot, doc);
				}
				add(slot, doc, score);
				if (queueFull) {
					for (int i = 0; i < comparators.length; i++) {
						comparators[i].setBottom(bottom.slot);
					}
				}
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
			super.setScorer(scorer);
		}
	}

	private final static class OutOfOrderMultiComparatorScoringMaxScoreCollector
			extends MultiComparatorScoringMaxScoreCollector {

		public OutOfOrderMultiComparatorScoringMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		@Override
		public void collect(int doc) throws IOException {
			final float score = scorer.score();
			if (score > maxScore) {
				maxScore = score;
			}
			++totalHits;
			if (queueFull) {

				for (int i = 0;; i++) {
					final int c = reverseMul[i]
							* comparators[i].compareBottom(doc);
					if (c < 0) {

						return;
					} else if (c > 0) {

						break;
					} else if (i == comparators.length - 1) {

						if (doc + docBase > bottom.doc) {

							return;
						}
						break;
					}
				}

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(bottom.slot, doc);
				}

				updateBottom(doc, score);

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].setBottom(bottom.slot);
				}
			} else {

				final int slot = totalHits - 1;

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(slot, doc);
				}
				add(slot, doc, score);
				if (queueFull) {
					for (int i = 0; i < comparators.length; i++) {
						comparators[i].setBottom(bottom.slot);
					}
				}
			}
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static class MultiComparatorScoringNoMaxScoreCollector extends
			MultiComparatorNonScoringCollector {

		Scorer scorer;

		public MultiComparatorScoringNoMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		final void updateBottom(int doc, float score) {
			bottom.doc = docBase + doc;
			bottom.score = score;
			bottom = pq.updateTop();
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {

				for (int i = 0;; i++) {
					final int c = reverseMul[i]
							* comparators[i].compareBottom(doc);
					if (c < 0) {

						return;
					} else if (c > 0) {

						break;
					} else if (i == comparators.length - 1) {

						return;
					}
				}

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(bottom.slot, doc);
				}

				final float score = scorer.score();
				updateBottom(doc, score);

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].setBottom(bottom.slot);
				}
			} else {

				final int slot = totalHits - 1;

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(slot, doc);
				}

				final float score = scorer.score();
				add(slot, doc, score);
				if (queueFull) {
					for (int i = 0; i < comparators.length; i++) {
						comparators[i].setBottom(bottom.slot);
					}
				}
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
			super.setScorer(scorer);
		}
	}

	private final static class OutOfOrderMultiComparatorScoringNoMaxScoreCollector
			extends MultiComparatorScoringNoMaxScoreCollector {

		public OutOfOrderMultiComparatorScoringNoMaxScoreCollector(
				FieldValueHitQueue<Entry> queue, int numHits, boolean fillFields)
				throws IOException {
			super(queue, numHits, fillFields);
		}

		@Override
		public void collect(int doc) throws IOException {
			++totalHits;
			if (queueFull) {

				for (int i = 0;; i++) {
					final int c = reverseMul[i]
							* comparators[i].compareBottom(doc);
					if (c < 0) {

						return;
					} else if (c > 0) {

						break;
					} else if (i == comparators.length - 1) {

						if (doc + docBase > bottom.doc) {

							return;
						}
						break;
					}
				}

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(bottom.slot, doc);
				}

				final float score = scorer.score();
				updateBottom(doc, score);

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].setBottom(bottom.slot);
				}
			} else {

				final int slot = totalHits - 1;

				for (int i = 0; i < comparators.length; i++) {
					comparators[i].copy(slot, doc);
				}

				final float score = scorer.score();
				add(slot, doc, score);
				if (queueFull) {
					for (int i = 0; i < comparators.length; i++) {
						comparators[i].setBottom(bottom.slot);
					}
				}
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
			super.setScorer(scorer);
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

	}

	private static final ScoreDoc[] EMPTY_SCOREDOCS = new ScoreDoc[0];

	private final boolean fillFields;

	float maxScore = Float.NaN;

	final int numHits;
	FieldValueHitQueue.Entry bottom = null;
	boolean queueFull;
	int docBase;

	private TopFieldCollector(PriorityQueue<Entry> pq, int numHits,
			boolean fillFields) {
		super(pq);
		this.numHits = numHits;
		this.fillFields = fillFields;
	}

	public static TopFieldCollector create(Sort sort, int numHits,
			boolean fillFields, boolean trackDocScores, boolean trackMaxScore,
			boolean docsScoredInOrder) throws IOException {
		if (sort.fields.length == 0) {
			throw new IllegalArgumentException(
					"Sort must contain at least one field");
		}

		if (numHits <= 0) {
			throw new IllegalArgumentException(
					"numHits must be > 0; please use TotalHitCountCollector if you just need the total hit count");
		}

		FieldValueHitQueue<Entry> queue = FieldValueHitQueue.create(
				sort.fields, numHits);
		if (queue.getComparators().length == 1) {
			if (docsScoredInOrder) {
				if (trackMaxScore) {
					return new OneComparatorScoringMaxScoreCollector(queue,
							numHits, fillFields);
				} else if (trackDocScores) {
					return new OneComparatorScoringNoMaxScoreCollector(queue,
							numHits, fillFields);
				} else {
					return new OneComparatorNonScoringCollector(queue, numHits,
							fillFields);
				}
			} else {
				if (trackMaxScore) {
					return new OutOfOrderOneComparatorScoringMaxScoreCollector(
							queue, numHits, fillFields);
				} else if (trackDocScores) {
					return new OutOfOrderOneComparatorScoringNoMaxScoreCollector(
							queue, numHits, fillFields);
				} else {
					return new OutOfOrderOneComparatorNonScoringCollector(
							queue, numHits, fillFields);
				}
			}
		}

		if (docsScoredInOrder) {
			if (trackMaxScore) {
				return new MultiComparatorScoringMaxScoreCollector(queue,
						numHits, fillFields);
			} else if (trackDocScores) {
				return new MultiComparatorScoringNoMaxScoreCollector(queue,
						numHits, fillFields);
			} else {
				return new MultiComparatorNonScoringCollector(queue, numHits,
						fillFields);
			}
		} else {
			if (trackMaxScore) {
				return new OutOfOrderMultiComparatorScoringMaxScoreCollector(
						queue, numHits, fillFields);
			} else if (trackDocScores) {
				return new OutOfOrderMultiComparatorScoringNoMaxScoreCollector(
						queue, numHits, fillFields);
			} else {
				return new OutOfOrderMultiComparatorNonScoringCollector(queue,
						numHits, fillFields);
			}
		}
	}

	final void add(int slot, int doc, float score) {
		bottom = pq.add(new Entry(slot, docBase + doc, score));
		queueFull = totalHits == numHits;
	}

	@Override
	protected void populateResults(ScoreDoc[] results, int howMany) {
		if (fillFields) {

			FieldValueHitQueue<Entry> queue = (FieldValueHitQueue<Entry>) pq;
			for (int i = howMany - 1; i >= 0; i--) {
				results[i] = queue.fillFields(queue.pop());
			}
		} else {
			for (int i = howMany - 1; i >= 0; i--) {
				Entry entry = pq.pop();
				results[i] = new FieldDoc(entry.doc, entry.score);
			}
		}
	}

	@Override
	protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
		if (results == null) {
			results = EMPTY_SCOREDOCS;

			maxScore = Float.NaN;
		}

		return new TopFieldDocs(totalHits, results,
				((FieldValueHitQueue<Entry>) pq).getFields(), maxScore);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}
}
