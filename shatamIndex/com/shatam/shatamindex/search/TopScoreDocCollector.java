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

public abstract class TopScoreDocCollector extends TopDocsCollector<ScoreDoc> {

	private static class InOrderTopScoreDocCollector extends
			TopScoreDocCollector {
		private InOrderTopScoreDocCollector(int numHits) {
			super(numHits);
		}

		@Override
		public void collect(int doc) throws IOException {
			float score = scorer.score();

			assert score != Float.NEGATIVE_INFINITY;
			assert !Float.isNaN(score);

			totalHits++;
			if (score <= pqTop.score) {

				return;
			}
			pqTop.doc = doc + docBase;
			pqTop.score = score;
			pqTop = pq.updateTop();
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return false;
		}
	}

	private static class InOrderPagingScoreDocCollector extends
			TopScoreDocCollector {
		private final ScoreDoc after;

		private int afterDoc;
		private int collectedHits;

		private InOrderPagingScoreDocCollector(ScoreDoc after, int numHits) {
			super(numHits);
			this.after = after;
		}

		@Override
		public void collect(int doc) throws IOException {
			float score = scorer.score();

			assert score != Float.NEGATIVE_INFINITY;
			assert !Float.isNaN(score);

			totalHits++;

			if (score > after.score
					|| (score == after.score && doc <= afterDoc)) {

				return;
			}

			if (score <= pqTop.score) {

				return;
			}
			collectedHits++;
			pqTop.doc = doc + docBase;
			pqTop.score = score;
			pqTop = pq.updateTop();
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return false;
		}

		@Override
		public void setNextReader(IndexReader reader, int base) {
			super.setNextReader(reader, base);
			afterDoc = after.doc - docBase;
		}

		@Override
		protected int topDocsSize() {
			return collectedHits < pq.size() ? collectedHits : pq.size();
		}

		@Override
		protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
			return results == null ? new TopDocs(totalHits, new ScoreDoc[0],
					Float.NaN) : new TopDocs(totalHits, results);
		}
	}

	private static class OutOfOrderTopScoreDocCollector extends
			TopScoreDocCollector {
		private OutOfOrderTopScoreDocCollector(int numHits) {
			super(numHits);
		}

		@Override
		public void collect(int doc) throws IOException {
			float score = scorer.score();

			assert !Float.isNaN(score);

			totalHits++;
			if (score < pqTop.score) {

				return;
			}
			doc += docBase;
			if (score == pqTop.score && doc > pqTop.doc) {

				return;
			}
			pqTop.doc = doc;
			pqTop.score = score;
			pqTop = pq.updateTop();
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}
	}

	private static class OutOfOrderPagingScoreDocCollector extends
			TopScoreDocCollector {
		private final ScoreDoc after;

		private int afterDoc;
		private int collectedHits;

		private OutOfOrderPagingScoreDocCollector(ScoreDoc after, int numHits) {
			super(numHits);
			this.after = after;
		}

		@Override
		public void collect(int doc) throws IOException {
			float score = scorer.score();

			assert !Float.isNaN(score);

			totalHits++;
			if (score > after.score
					|| (score == after.score && doc <= afterDoc)) {

				return;
			}
			if (score < pqTop.score) {

				return;
			}
			doc += docBase;
			if (score == pqTop.score && doc > pqTop.doc) {

				return;
			}
			collectedHits++;
			pqTop.doc = doc;
			pqTop.score = score;
			pqTop = pq.updateTop();
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

		@Override
		public void setNextReader(IndexReader reader, int base) {
			super.setNextReader(reader, base);
			afterDoc = after.doc - docBase;
		}

		@Override
		protected int topDocsSize() {
			return collectedHits < pq.size() ? collectedHits : pq.size();
		}

		@Override
		protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
			return results == null ? new TopDocs(totalHits, new ScoreDoc[0],
					Float.NaN) : new TopDocs(totalHits, results);
		}
	}

	public static TopScoreDocCollector create(int numHits,
			boolean docsScoredInOrder) {
		return create(numHits, null, docsScoredInOrder);
	}

	public static TopScoreDocCollector create(int numHits, ScoreDoc after,
			boolean docsScoredInOrder) {

		if (numHits <= 0) {
			throw new IllegalArgumentException(
					"numHits must be > 0; please use TotalHitCountCollector if you just need the total hit count");
		}

		if (docsScoredInOrder) {
			return after == null ? new InOrderTopScoreDocCollector(numHits)
					: new InOrderPagingScoreDocCollector(after, numHits);
		} else {
			return after == null ? new OutOfOrderTopScoreDocCollector(numHits)
					: new OutOfOrderPagingScoreDocCollector(after, numHits);
		}

	}

	ScoreDoc pqTop;
	int docBase = 0;
	Scorer scorer;

	private TopScoreDocCollector(int numHits) {
		super(new HitQueue(numHits, true));

		pqTop = pq.top();
	}

	@Override
	protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
		if (results == null) {
			return EMPTY_TOPDOCS;
		}

		float maxScore = Float.NaN;
		if (start == 0) {
			maxScore = results[0].score;
		} else {
			for (int i = pq.size(); i > 1; i--) {
				pq.pop();
			}
			maxScore = pq.pop().score;
		}

		return new TopDocs(totalHits, results, maxScore);
	}

	@Override
	public void setNextReader(IndexReader reader, int base) {
		docBase = base;
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		this.scorer = scorer;
	}
}
