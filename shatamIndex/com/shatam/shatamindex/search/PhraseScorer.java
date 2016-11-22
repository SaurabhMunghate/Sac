/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

abstract class PhraseScorer extends Scorer {
	protected byte[] norms;
	protected float value;

	PhrasePositions min, max;

	private float freq;

	PhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings,
			Similarity similarity, byte[] norms) {
		super(similarity, weight);
		this.norms = norms;
		this.value = weight.getValue();

		if (postings.length > 0) {
			min = new PhrasePositions(postings[0].postings,
					postings[0].position, 0);
			max = min;
			max.doc = -1;
			for (int i = 1; i < postings.length; i++) {
				PhrasePositions pp = new PhrasePositions(postings[i].postings,
						postings[i].position, i);
				max.next = pp;
				max = pp;
				max.doc = -1;
			}
			max.next = min;
		}
	}

	@Override
	public int docID() {
		return max.doc;
	}

	@Override
	public int nextDoc() throws IOException {
		return advance(max.doc);
	}

	private boolean advanceMin(int target) throws IOException {
		if (!min.skipTo(target)) {
			max.doc = NO_MORE_DOCS;
			return false;
		}
		min = min.next;
		max = max.next;
		return true;
	}

	@Override
	public float score() throws IOException {

		float raw = getSimilarity().tf(freq) * value;
		return norms == null ? raw : raw
				* getSimilarity().decodeNormValue(norms[max.doc]);
	}

	@Override
	public int advance(int target) throws IOException {
		freq = 0.0f;
		if (!advanceMin(target)) {
			return NO_MORE_DOCS;
		}
		boolean restart = false;
		while (freq == 0.0f) {
			while (min.doc < max.doc || restart) {
				restart = false;
				if (!advanceMin(max.doc)) {
					return NO_MORE_DOCS;
				}
			}

			freq = phraseFreq();
			restart = true;
		}

		return max.doc;
	}

	@Override
	public final float freq() {
		return freq;
	}

	abstract float phraseFreq() throws IOException;

	@Override
	public String toString() {
		return "scorer(" + weight + ")";
	}

}
