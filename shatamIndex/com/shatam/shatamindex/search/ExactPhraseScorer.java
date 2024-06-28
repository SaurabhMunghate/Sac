/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.Arrays;

import com.shatam.shatamindex.index.*;

final class ExactPhraseScorer extends Scorer {
	private final byte[] norms;
	private final float value;

	private static final int SCORE_CACHE_SIZE = 32;
	private final float[] scoreCache = new float[SCORE_CACHE_SIZE];

	private final int endMinus1;

	private final static int CHUNK = 4096;

	private int gen;
	private final int[] counts = new int[CHUNK];
	private final int[] gens = new int[CHUNK];

	boolean noDocs;

	private final static class ChunkState {
		final TermPositions posEnum;
		final int offset;
		final boolean useAdvance;
		int posUpto;
		int posLimit;
		int pos;
		int lastPos;

		public ChunkState(TermPositions posEnum, int offset, boolean useAdvance) {
			this.posEnum = posEnum;
			this.offset = offset;
			this.useAdvance = useAdvance;
		}
	}

	private final ChunkState[] chunkStates;

	private int docID = -1;
	private int freq;

	ExactPhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings,
			Similarity similarity, byte[] norms) throws IOException {
		super(similarity, weight);
		this.norms = norms;
		this.value = weight.getValue();

		chunkStates = new ChunkState[postings.length];

		endMinus1 = postings.length - 1;

		for (int i = 0; i < postings.length; i++) {

			final boolean useAdvance = postings[i].docFreq > 5 * postings[0].docFreq;
			chunkStates[i] = new ChunkState(postings[i].postings,
					-postings[i].position, useAdvance);
			if (i > 0 && !postings[i].postings.next()) {
				noDocs = true;
				return;
			}
		}

		for (int i = 0; i < SCORE_CACHE_SIZE; i++) {
			scoreCache[i] = getSimilarity().tf((float) i) * value;
		}
	}

	@Override
	public int nextDoc() throws IOException {
		while (true) {

			if (!chunkStates[0].posEnum.next()) {
				docID = DocIdSetIterator.NO_MORE_DOCS;
				return docID;
			}

			final int doc = chunkStates[0].posEnum.doc();

			int i = 1;
			while (i < chunkStates.length) {
				final ChunkState cs = chunkStates[i];
				int doc2 = cs.posEnum.doc();
				if (cs.useAdvance) {
					if (doc2 < doc) {
						if (!cs.posEnum.skipTo(doc)) {
							docID = DocIdSetIterator.NO_MORE_DOCS;
							return docID;
						} else {
							doc2 = cs.posEnum.doc();
						}
					}
				} else {
					int iter = 0;
					while (doc2 < doc) {

						if (++iter == 50) {
							if (!cs.posEnum.skipTo(doc)) {
								docID = DocIdSetIterator.NO_MORE_DOCS;
								return docID;
							} else {
								doc2 = cs.posEnum.doc();
							}
							break;
						} else {
							if (cs.posEnum.next()) {
								doc2 = cs.posEnum.doc();
							} else {
								docID = DocIdSetIterator.NO_MORE_DOCS;
								return docID;
							}
						}
					}
				}
				if (doc2 > doc) {
					break;
				}
				i++;
			}

			if (i == chunkStates.length) {

				docID = doc;

				freq = phraseFreq();
				if (freq != 0) {
					return docID;
				}
			}
		}
	}

	@Override
	public int advance(int target) throws IOException {

		if (!chunkStates[0].posEnum.skipTo(target)) {
			docID = DocIdSetIterator.NO_MORE_DOCS;
			return docID;
		}
		int doc = chunkStates[0].posEnum.doc();

		while (true) {

			int i = 1;
			while (i < chunkStates.length) {
				int doc2 = chunkStates[i].posEnum.doc();
				if (doc2 < doc) {
					if (!chunkStates[i].posEnum.skipTo(doc)) {
						docID = DocIdSetIterator.NO_MORE_DOCS;
						return docID;
					} else {
						doc2 = chunkStates[i].posEnum.doc();
					}
				}
				if (doc2 > doc) {
					break;
				}
				i++;
			}

			if (i == chunkStates.length) {

				docID = doc;
				freq = phraseFreq();
				if (freq != 0) {
					return docID;
				}
			}

			if (!chunkStates[0].posEnum.next()) {
				docID = DocIdSetIterator.NO_MORE_DOCS;
				return docID;
			} else {
				doc = chunkStates[0].posEnum.doc();
			}
		}
	}

	@Override
	public String toString() {
		return "ExactPhraseScorer(" + weight + ")";
	}

	@Override
	public float freq() {
		return freq;
	}

	@Override
	public int docID() {
		return docID;
	}

	@Override
	public float score() throws IOException {
		final float raw;
		if (freq < SCORE_CACHE_SIZE) {
			raw = scoreCache[freq];
		} else {
			raw = getSimilarity().tf((float) freq) * value;
		}
		return norms == null ? raw : raw
				* getSimilarity().decodeNormValue(norms[docID]);
	}

	private int phraseFreq() throws IOException {

		freq = 0;

		for (int i = 0; i < chunkStates.length; i++) {
			final ChunkState cs = chunkStates[i];
			cs.posLimit = cs.posEnum.freq();
			cs.pos = cs.offset + cs.posEnum.nextPosition();
			cs.posUpto = 1;
			cs.lastPos = -1;
		}

		int chunkStart = 0;
		int chunkEnd = CHUNK;

		boolean end = false;

		while (!end) {

			gen++;

			if (gen == 0) {

				Arrays.fill(gens, 0);
				gen++;
			}

			{
				final ChunkState cs = chunkStates[0];
				while (cs.pos < chunkEnd) {
					if (cs.pos > cs.lastPos) {
						cs.lastPos = cs.pos;
						final int posIndex = cs.pos - chunkStart;
						counts[posIndex] = 1;
						assert gens[posIndex] != gen;
						gens[posIndex] = gen;
					}

					if (cs.posUpto == cs.posLimit) {
						end = true;
						break;
					}
					cs.posUpto++;
					cs.pos = cs.offset + cs.posEnum.nextPosition();
				}
			}

			boolean any = true;
			for (int t = 1; t < endMinus1; t++) {
				final ChunkState cs = chunkStates[t];
				any = false;
				while (cs.pos < chunkEnd) {
					if (cs.pos > cs.lastPos) {
						cs.lastPos = cs.pos;
						final int posIndex = cs.pos - chunkStart;
						if (posIndex >= 0 && gens[posIndex] == gen
								&& counts[posIndex] == t) {

							counts[posIndex]++;
							any = true;
						}
					}

					if (cs.posUpto == cs.posLimit) {
						end = true;
						break;
					}
					cs.posUpto++;
					cs.pos = cs.offset + cs.posEnum.nextPosition();
				}

				if (!any) {
					break;
				}
			}

			if (!any) {

				chunkStart += CHUNK;
				chunkEnd += CHUNK;
				continue;
			}

			{
				final ChunkState cs = chunkStates[endMinus1];
				while (cs.pos < chunkEnd) {
					if (cs.pos > cs.lastPos) {
						cs.lastPos = cs.pos;
						final int posIndex = cs.pos - chunkStart;
						if (posIndex >= 0 && gens[posIndex] == gen
								&& counts[posIndex] == endMinus1) {
							freq++;
						}
					}

					if (cs.posUpto == cs.posLimit) {
						end = true;
						break;
					}
					cs.posUpto++;
					cs.pos = cs.offset + cs.posEnum.nextPosition();
				}
			}

			chunkStart += CHUNK;
			chunkEnd += CHUNK;
		}

		return freq;
	}
}
