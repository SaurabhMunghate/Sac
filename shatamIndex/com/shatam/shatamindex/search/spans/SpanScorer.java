/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import java.io.IOException;

import com.shatam.shatamindex.search.Explanation;
import com.shatam.shatamindex.search.Scorer;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.search.Weight;

public class SpanScorer extends Scorer {
	protected Spans spans;
	protected byte[] norms;
	protected float value;

	protected boolean more = true;

	protected int doc;
	protected float freq;

	protected SpanScorer(Spans spans, Weight weight, Similarity similarity,
			byte[] norms) throws IOException {
		super(similarity, weight);
		this.spans = spans;
		this.norms = norms;
		this.value = weight.getValue();
		if (this.spans.next()) {
			doc = -1;
		} else {
			doc = NO_MORE_DOCS;
			more = false;
		}
	}

	@Override
	public int nextDoc() throws IOException {
		if (!setFreqCurrentDoc()) {
			doc = NO_MORE_DOCS;
		}
		return doc;
	}

	@Override
	public int advance(int target) throws IOException {
		if (!more) {
			return doc = NO_MORE_DOCS;
		}
		if (spans.doc() < target) {
			more = spans.skipTo(target);
		}
		if (!setFreqCurrentDoc()) {
			doc = NO_MORE_DOCS;
		}
		return doc;
	}

	protected boolean setFreqCurrentDoc() throws IOException {
		if (!more) {
			return false;
		}
		doc = spans.doc();
		freq = 0.0f;
		do {
			int matchLength = spans.end() - spans.start();
			freq += getSimilarity().sloppyFreq(matchLength);
			more = spans.next();
		} while (more && (doc == spans.doc()));
		return true;
	}

	@Override
	public int docID() {
		return doc;
	}

	@Override
	public float score() throws IOException {
		float raw = getSimilarity().tf(freq) * value;
		return norms == null ? raw : raw
				* getSimilarity().decodeNormValue(norms[doc]);
	}

	@Override
	public float freq() throws IOException {
		return freq;
	}

	protected Explanation explain(final int doc) throws IOException {
		Explanation tfExplanation = new Explanation();

		int expDoc = advance(doc);

		float phraseFreq = (expDoc == doc) ? freq : 0.0f;
		tfExplanation.setValue(getSimilarity().tf(phraseFreq));
		tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");

		return tfExplanation;
	}

}
