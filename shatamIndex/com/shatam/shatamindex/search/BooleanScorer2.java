/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.shatam.shatamindex.search.BooleanClause.Occur;

class BooleanScorer2 extends Scorer {

	private final List<Scorer> requiredScorers;
	private final List<Scorer> optionalScorers;
	private final List<Scorer> prohibitedScorers;

	private class Coordinator {
		float[] coordFactors = null;
		int maxCoord = 0;
		int nrMatchers;

		void init(Similarity sim, boolean disableCoord) {

			coordFactors = new float[optionalScorers.size()
					+ requiredScorers.size() + 1];
			for (int i = 0; i < coordFactors.length; i++) {
				coordFactors[i] = disableCoord ? 1.0f : sim.coord(i, maxCoord);
			}
		}
	}

	private final Coordinator coordinator;

	private final Scorer countingSumScorer;

	private final int minNrShouldMatch;

	private int doc = -1;

	public BooleanScorer2(Weight weight, boolean disableCoord,
			Similarity similarity, int minNrShouldMatch, List<Scorer> required,
			List<Scorer> prohibited, List<Scorer> optional, int maxCoord)
			throws IOException {
		super(weight);
		if (minNrShouldMatch < 0) {
			throw new IllegalArgumentException(
					"Minimum number of optional scorers should not be negative");
		}
		coordinator = new Coordinator();
		this.minNrShouldMatch = minNrShouldMatch;
		coordinator.maxCoord = maxCoord;

		optionalScorers = optional;
		requiredScorers = required;
		prohibitedScorers = prohibited;

		coordinator.init(similarity, disableCoord);
		countingSumScorer = makeCountingSumScorer(disableCoord, similarity);
	}

	private class SingleMatchScorer extends Scorer {
		private Scorer scorer;
		private int lastScoredDoc = -1;

		private float lastDocScore = Float.NaN;

		SingleMatchScorer(Scorer scorer) {
			super(scorer.weight);
			this.scorer = scorer;
		}

		@Override
		public float score() throws IOException {
			int doc = docID();
			if (doc >= lastScoredDoc) {
				if (doc > lastScoredDoc) {
					lastDocScore = scorer.score();
					lastScoredDoc = doc;
				}
				coordinator.nrMatchers++;
			}
			return lastDocScore;
		}

		@Override
		public int docID() {
			return scorer.docID();
		}

		@Override
		public int nextDoc() throws IOException {
			return scorer.nextDoc();
		}

		@Override
		public int advance(int target) throws IOException {
			return scorer.advance(target);
		}
	}

	private Scorer countingDisjunctionSumScorer(final List<Scorer> scorers,
			int minNrShouldMatch) throws IOException {

		return new DisjunctionSumScorer(weight, scorers, minNrShouldMatch) {
			private int lastScoredDoc = -1;

			private float lastDocScore = Float.NaN;

			@Override
			public float score() throws IOException {
				int doc = docID();
				if (doc >= lastScoredDoc) {
					if (doc > lastScoredDoc) {
						lastDocScore = super.score();
						lastScoredDoc = doc;
					}
					coordinator.nrMatchers += super.nrMatchers;
				}
				return lastDocScore;
			}
		};
	}

	private Scorer countingConjunctionSumScorer(boolean disableCoord,
			Similarity similarity, List<Scorer> requiredScorers)
			throws IOException {

		final int requiredNrMatchers = requiredScorers.size();
		return new ConjunctionScorer(weight, disableCoord ? 1.0f
				: similarity.coord(requiredScorers.size(),
						requiredScorers.size()), requiredScorers) {
			private int lastScoredDoc = -1;

			private float lastDocScore = Float.NaN;

			@Override
			public float score() throws IOException {
				int doc = docID();
				if (doc >= lastScoredDoc) {
					if (doc > lastScoredDoc) {
						lastDocScore = super.score();
						lastScoredDoc = doc;
					}
					coordinator.nrMatchers += requiredNrMatchers;
				}

				return lastDocScore;
			}
		};
	}

	private Scorer dualConjunctionSumScorer(boolean disableCoord,
			Similarity similarity, Scorer req1, Scorer req2) throws IOException {

		return new ConjunctionScorer(weight, disableCoord ? 1.0f
				: similarity.coord(2, 2), new Scorer[] { req1, req2 });

	}

	private Scorer makeCountingSumScorer(boolean disableCoord,
			Similarity similarity) throws IOException {

		return (requiredScorers.size() == 0) ? makeCountingSumScorerNoReq(
				disableCoord, similarity) : makeCountingSumScorerSomeReq(
				disableCoord, similarity);
	}

	private Scorer makeCountingSumScorerNoReq(boolean disableCoord,
			Similarity similarity) throws IOException {

		int nrOptRequired = (minNrShouldMatch < 1) ? 1 : minNrShouldMatch;
		Scorer requiredCountingSumScorer;
		if (optionalScorers.size() > nrOptRequired)
			requiredCountingSumScorer = countingDisjunctionSumScorer(
					optionalScorers, nrOptRequired);
		else if (optionalScorers.size() == 1)
			requiredCountingSumScorer = new SingleMatchScorer(
					optionalScorers.get(0));
		else {
			requiredCountingSumScorer = countingConjunctionSumScorer(
					disableCoord, similarity, optionalScorers);
		}
		return addProhibitedScorers(requiredCountingSumScorer);
	}

	private Scorer makeCountingSumScorerSomeReq(boolean disableCoord,
			Similarity similarity) throws IOException {

		if (optionalScorers.size() == minNrShouldMatch) {

			ArrayList<Scorer> allReq = new ArrayList<Scorer>(requiredScorers);
			allReq.addAll(optionalScorers);
			return addProhibitedScorers(countingConjunctionSumScorer(
					disableCoord, similarity, allReq));
		} else {

			Scorer requiredCountingSumScorer = requiredScorers.size() == 1 ? new SingleMatchScorer(
					requiredScorers.get(0)) : countingConjunctionSumScorer(
					disableCoord, similarity, requiredScorers);
			if (minNrShouldMatch > 0) {

				return addProhibitedScorers(dualConjunctionSumScorer(

						disableCoord,
						similarity,
						requiredCountingSumScorer,
						countingDisjunctionSumScorer(optionalScorers,
								minNrShouldMatch)));
			} else {
				return new ReqOptSumScorer(
						addProhibitedScorers(requiredCountingSumScorer),
						optionalScorers.size() == 1 ? new SingleMatchScorer(
								optionalScorers.get(0))

						: countingDisjunctionSumScorer(optionalScorers, 1));
			}
		}
	}

	private Scorer addProhibitedScorers(Scorer requiredCountingSumScorer)
			throws IOException {
		return (prohibitedScorers.size() == 0) ? requiredCountingSumScorer

		: new ReqExclScorer(requiredCountingSumScorer,
				((prohibitedScorers.size() == 1) ? prohibitedScorers.get(0)
						: new DisjunctionSumScorer(weight, prohibitedScorers)));
	}

	@Override
	public void score(Collector collector) throws IOException {
		collector.setScorer(this);
		while ((doc = countingSumScorer.nextDoc()) != NO_MORE_DOCS) {
			collector.collect(doc);
		}
	}

	@Override
	protected boolean score(Collector collector, int max, int firstDocID)
			throws IOException {
		doc = firstDocID;
		collector.setScorer(this);
		while (doc < max) {
			collector.collect(doc);
			doc = countingSumScorer.nextDoc();
		}
		return doc != NO_MORE_DOCS;
	}

	@Override
	public int docID() {
		return doc;
	}

	@Override
	public int nextDoc() throws IOException {
		return doc = countingSumScorer.nextDoc();
	}

	@Override
	public float score() throws IOException {
		coordinator.nrMatchers = 0;
		float sum = countingSumScorer.score();
		return sum * coordinator.coordFactors[coordinator.nrMatchers];
	}

	@Override
	public float freq() {
		return coordinator.nrMatchers;
	}

	@Override
	public int advance(int target) throws IOException {
		return doc = countingSumScorer.advance(target);
	}

	@Override
	protected void visitSubScorers(Query parent, Occur relationship,
			ScorerVisitor<Query, Query, Scorer> visitor) {
		super.visitSubScorers(parent, relationship, visitor);
		final Query q = weight.getQuery();
		for (Scorer s : optionalScorers) {
			s.visitSubScorers(q, Occur.SHOULD, visitor);
		}
		for (Scorer s : prohibitedScorers) {
			s.visitSubScorers(q, Occur.MUST_NOT, visitor);
		}
		for (Scorer s : requiredScorers) {
			s.visitSubScorers(q, Occur.MUST, visitor);
		}
	}
}
