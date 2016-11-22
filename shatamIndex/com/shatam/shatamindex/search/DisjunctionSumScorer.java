/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.util.List;
import java.io.IOException;

import com.shatam.shatamindex.util.ScorerDocQueue;

class DisjunctionSumScorer extends Scorer {

	private final int nrScorers;

	protected final List<Scorer> subScorers;

	private final int minimumNrMatchers;

	private ScorerDocQueue scorerDocQueue;

	private int currentDoc = -1;

	protected int nrMatchers = -1;

	private double currentScore = Float.NaN;

	public DisjunctionSumScorer(Weight weight, List<Scorer> subScorers,
			int minimumNrMatchers) throws IOException {
		super(weight);

		nrScorers = subScorers.size();

		if (minimumNrMatchers <= 0) {
			throw new IllegalArgumentException(
					"Minimum nr of matchers must be positive");
		}
		if (nrScorers <= 1) {
			throw new IllegalArgumentException(
					"There must be at least 2 subScorers");
		}

		this.minimumNrMatchers = minimumNrMatchers;
		this.subScorers = subScorers;

		initScorerDocQueue();
	}

	public DisjunctionSumScorer(Weight weight, List<Scorer> subScorers)
			throws IOException {
		this(weight, subScorers, 1);
	}

	private void initScorerDocQueue() throws IOException {
		scorerDocQueue = new ScorerDocQueue(nrScorers);
		for (Scorer se : subScorers) {
			if (se.nextDoc() != NO_MORE_DOCS) {
				scorerDocQueue.insert(se);
			}
		}
	}

	@Override
	public void score(Collector collector) throws IOException {
		collector.setScorer(this);
		while (nextDoc() != NO_MORE_DOCS) {
			collector.collect(currentDoc);
		}
	}

	@Override
	protected boolean score(Collector collector, int max, int firstDocID)
			throws IOException {

		collector.setScorer(this);
		while (currentDoc < max) {
			collector.collect(currentDoc);
			if (nextDoc() == NO_MORE_DOCS) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int nextDoc() throws IOException {
		if (scorerDocQueue.size() < minimumNrMatchers || !advanceAfterCurrent()) {
			currentDoc = NO_MORE_DOCS;
		}
		return currentDoc;
	}

	protected boolean advanceAfterCurrent() throws IOException {
		do {
			currentDoc = scorerDocQueue.topDoc();
			currentScore = scorerDocQueue.topScore();
			nrMatchers = 1;
			do {
				if (!scorerDocQueue.topNextAndAdjustElsePop()) {
					if (scorerDocQueue.size() == 0) {
						break;
					}
				}
				if (scorerDocQueue.topDoc() != currentDoc) {
					break;
				}
				currentScore += scorerDocQueue.topScore();
				nrMatchers++;
			} while (true);

			if (nrMatchers >= minimumNrMatchers) {
				return true;
			} else if (scorerDocQueue.size() < minimumNrMatchers) {
				return false;
			}
		} while (true);
	}

	@Override
	public float score() throws IOException {
		return (float) currentScore;
	}

	@Override
	public int docID() {
		return currentDoc;
	}

	public int nrMatchers() {
		return nrMatchers;
	}

	@Override
	public int advance(int target) throws IOException {
		if (scorerDocQueue.size() < minimumNrMatchers) {
			return currentDoc = NO_MORE_DOCS;
		}
		if (target <= currentDoc) {
			return currentDoc;
		}
		do {
			if (scorerDocQueue.topDoc() >= target) {
				return advanceAfterCurrent() ? currentDoc
						: (currentDoc = NO_MORE_DOCS);
			} else if (!scorerDocQueue.topSkipToAndAdjustElsePop(target)) {
				if (scorerDocQueue.size() < minimumNrMatchers) {
					return currentDoc = NO_MORE_DOCS;
				}
			}
		} while (true);
	}
}
