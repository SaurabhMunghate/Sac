/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.TermDocs;

final class TermScorer extends Scorer {
	private final TermDocs termDocs;
	private final byte[] norms;
	private float weightValue;
	private int doc = -1;
	private int freq;

	private final int[] docs = new int[32];
	private final int[] freqs = new int[32];
	private int pointer;
	private int pointerMax;

	private static final int SCORE_CACHE_SIZE = 32;
	private final float[] scoreCache = new float[SCORE_CACHE_SIZE];

	TermScorer(Weight weight, TermDocs td, Similarity similarity, byte[] norms) {
		super(similarity, weight);

		this.termDocs = td;
		this.norms = norms;
		this.weightValue = weight.getValue();

		for (int i = 0; i < SCORE_CACHE_SIZE; i++)
			scoreCache[i] = getSimilarity().tf(i) * weightValue;
	}

	@Override
	public void score(Collector c) throws IOException {
		score(c, Integer.MAX_VALUE, nextDoc());
	}

	@Override
	protected boolean score(Collector c, int end, int firstDocID)
			throws IOException {
		c.setScorer(this);
		while (doc < end) {
			c.collect(doc);

			if (++pointer >= pointerMax) {
				pointerMax = termDocs.read(docs, freqs);
				if (pointerMax != 0) {
					pointer = 0;
				} else {
					termDocs.close();
					doc = Integer.MAX_VALUE;
					return false;
				}
			}
			doc = docs[pointer];
			freq = freqs[pointer];
		}
		return true;
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
		pointer++;
		if (pointer >= pointerMax) {
			pointerMax = termDocs.read(docs, freqs);
			if (pointerMax != 0) {
				pointer = 0;
			} else {
				termDocs.close();
				return doc = NO_MORE_DOCS;
			}
		}
		doc = docs[pointer];
		freq = freqs[pointer];
		return doc;
	}

	@Override
	public float score() {
		assert doc != -1;
		float raw = freq < SCORE_CACHE_SIZE ? scoreCache[freq]
				: getSimilarity().tf(freq) * weightValue;

		return norms == null ? raw : raw
				* getSimilarity().decodeNormValue(norms[doc]);
	}

	@Override
	public int advance(int target) throws IOException {

		for (pointer++; pointer < pointerMax; pointer++) {
			if (docs[pointer] >= target) {
				freq = freqs[pointer];
				return doc = docs[pointer];
			}
		}

		boolean result = termDocs.skipTo(target);
		if (result) {
			pointerMax = 1;
			pointer = 0;
			docs[pointer] = doc = termDocs.doc();
			freqs[pointer] = freq = termDocs.freq();
		} else {
			doc = NO_MORE_DOCS;
		}
		return doc;
	}

	@Override
	public String toString() {
		return "scorer(" + weight + ")";
	}

}
