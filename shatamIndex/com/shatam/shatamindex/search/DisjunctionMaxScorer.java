/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

class DisjunctionMaxScorer extends Scorer {

	private final Scorer[] subScorers;
	private int numScorers;

	private final float tieBreakerMultiplier;
	private int doc = -1;

	private float scoreSum;
	private float scoreMax;

	public DisjunctionMaxScorer(Weight weight, float tieBreakerMultiplier,
			Similarity similarity, Scorer[] subScorers, int numScorers)
			throws IOException {
		super(similarity, weight);
		this.tieBreakerMultiplier = tieBreakerMultiplier;

		this.subScorers = subScorers;
		this.numScorers = numScorers;

		heapify();
	}

	@Override
	public int nextDoc() throws IOException {
		if (numScorers == 0)
			return doc = NO_MORE_DOCS;
		while (subScorers[0].docID() == doc) {
			if (subScorers[0].nextDoc() != NO_MORE_DOCS) {
				heapAdjust(0);
			} else {
				heapRemoveRoot();
				if (numScorers == 0) {
					return doc = NO_MORE_DOCS;
				}
			}
		}

		return doc = subScorers[0].docID();
	}

	@Override
	public int docID() {
		return doc;
	}

	@Override
	public float score() throws IOException {
		int doc = subScorers[0].docID();
		scoreSum = scoreMax = subScorers[0].score();
		int size = numScorers;
		scoreAll(1, size, doc);
		scoreAll(2, size, doc);
		return scoreMax + (scoreSum - scoreMax) * tieBreakerMultiplier;
	}

	private void scoreAll(int root, int size, int doc) throws IOException {
		if (root < size && subScorers[root].docID() == doc) {
			float sub = subScorers[root].score();
			scoreSum += sub;
			scoreMax = Math.max(scoreMax, sub);
			scoreAll((root << 1) + 1, size, doc);
			scoreAll((root << 1) + 2, size, doc);
		}
	}

	@Override
	public int advance(int target) throws IOException {
		if (numScorers == 0)
			return doc = NO_MORE_DOCS;
		while (subScorers[0].docID() < target) {
			if (subScorers[0].advance(target) != NO_MORE_DOCS) {
				heapAdjust(0);
			} else {
				heapRemoveRoot();
				if (numScorers == 0) {
					return doc = NO_MORE_DOCS;
				}
			}
		}
		return doc = subScorers[0].docID();
	}

	private void heapify() {
		for (int i = (numScorers >> 1) - 1; i >= 0; i--) {
			heapAdjust(i);
		}
	}

	private void heapAdjust(int root) {
		Scorer scorer = subScorers[root];
		int doc = scorer.docID();
		int i = root;
		while (i <= (numScorers >> 1) - 1) {
			int lchild = (i << 1) + 1;
			Scorer lscorer = subScorers[lchild];
			int ldoc = lscorer.docID();
			int rdoc = Integer.MAX_VALUE, rchild = (i << 1) + 2;
			Scorer rscorer = null;
			if (rchild < numScorers) {
				rscorer = subScorers[rchild];
				rdoc = rscorer.docID();
			}
			if (ldoc < doc) {
				if (rdoc < ldoc) {
					subScorers[i] = rscorer;
					subScorers[rchild] = scorer;
					i = rchild;
				} else {
					subScorers[i] = lscorer;
					subScorers[lchild] = scorer;
					i = lchild;
				}
			} else if (rdoc < doc) {
				subScorers[i] = rscorer;
				subScorers[rchild] = scorer;
				i = rchild;
			} else {
				return;
			}
		}
	}

	private void heapRemoveRoot() {
		if (numScorers == 1) {
			subScorers[0] = null;
			numScorers = 0;
		} else {
			subScorers[0] = subScorers[numScorers - 1];
			subScorers[numScorers - 1] = null;
			--numScorers;
			heapAdjust(0);
		}
	}

}
