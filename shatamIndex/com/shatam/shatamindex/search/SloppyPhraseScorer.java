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

final class SloppyPhraseScorer extends PhraseScorer {
	private int slop;
	private boolean checkedRepeats;
	private boolean hasRepeats;
	private PhraseQueue pq;
	private PhrasePositions[] nrPps;

	SloppyPhraseScorer(Weight weight, PhraseQuery.PostingsAndFreq[] postings,
			Similarity similarity, int slop, byte[] norms) {
		super(weight, postings, similarity, norms);
		this.slop = slop;
	}

	@Override
	protected float phraseFreq() throws IOException {
		int end = initPhrasePositions();

		if (end == Integer.MIN_VALUE) {
			return 0.0f;
		}

		float freq = 0.0f;
		PhrasePositions pp = pq.pop();
		int matchLength = end - pp.position;
		int next = pq.size() > 0 ? pq.top().position : pp.position;

		while (pp.nextPosition()
				&& (end = advanceRepeats(pp, end)) != Integer.MIN_VALUE) {
			if (pp.position > next) {

				if (matchLength <= slop) {
					freq += getSimilarity().sloppyFreq(matchLength);
				}
				pq.add(pp);
				pp = pq.pop();
				next = pq.size() > 0 ? pq.top().position : pp.position;
				matchLength = end - pp.position;

			} else {
				int matchLength2 = end - pp.position;

				if (matchLength2 < matchLength) {
					matchLength = matchLength2;
				}
			}
		}
		if (matchLength <= slop) {
			freq += getSimilarity().sloppyFreq(matchLength);
		}
		return freq;
	}

	private int advanceRepeats(PhrasePositions pp, int end) throws IOException {
		int repeatsEnd = end;
		if (pp.position > repeatsEnd) {
			repeatsEnd = pp.position;
		}
		if (!hasRepeats) {
			return repeatsEnd;
		}
		int tpPos = tpPos(pp);
		for (PhrasePositions pp2 = pp.nextRepeating; pp2 != null; pp2 = pp2.nextRepeating) {
			while (tpPos(pp2) <= tpPos) {
				if (!pp2.nextPosition()) {
					return Integer.MIN_VALUE;
				}
			}
			tpPos = tpPos(pp2);
			if (pp2.position > repeatsEnd) {
				repeatsEnd = pp2.position;
			}

			if (pp2.position < pp.position) {
				pp.position = pp2.position;
			}
		}
		return repeatsEnd;
	}

	private int initPhrasePositions() throws IOException {
		int end = Integer.MIN_VALUE;

		if (checkedRepeats && !hasRepeats) {

			pq.clear();
			for (PhrasePositions pp = min, prev = null; prev != max; pp = (prev = pp).next) {
				pp.firstPosition();
				if (pp.position > end) {
					end = pp.position;
				}
				pq.add(pp);
			}
			return end;
		}

		for (PhrasePositions pp = min, prev = null; prev != max; pp = (prev = pp).next) {
			pp.firstPosition();
		}

		if (!checkedRepeats) {
			checkedRepeats = true;
			ArrayList<PhrasePositions> ppsA = new ArrayList<PhrasePositions>();
			PhrasePositions dummyPP = new PhrasePositions(null, -1, -1);

			for (PhrasePositions pp = min, prev = null; prev != max; pp = (prev = pp).next) {
				if (pp.nextRepeating != null) {
					continue;
				}
				ppsA.add(pp);
				int tpPos = tpPos(pp);
				for (PhrasePositions prevB = pp, pp2 = pp.next; pp2 != min; pp2 = pp2.next) {
					if (pp2.nextRepeating != null || pp.offset == pp2.offset
							|| tpPos(pp2) != tpPos) {
						continue;
					}

					hasRepeats = true;
					prevB.nextRepeating = pp2;
					pp2.nextRepeating = dummyPP;
					prevB = pp2;
				}
			}
			if (hasRepeats) {

				for (PhrasePositions pp = min, prev = null; prev != max; pp = (prev = pp).next) {
					if (pp.nextRepeating == dummyPP) {
						pp.nextRepeating = null;
					}
				}
			}
			nrPps = ppsA.toArray(new PhrasePositions[0]);
			pq = new PhraseQueue(nrPps.length);
		}

		if (hasRepeats) {
			for (PhrasePositions pp : nrPps) {
				if ((end = advanceRepeats(pp, end)) == Integer.MIN_VALUE) {
					return Integer.MIN_VALUE;
				}
			}
		}

		pq.clear();
		for (PhrasePositions pp : nrPps) {
			if (pp.position > end) {
				end = pp.position;
			}
			pq.add(pp);
		}

		return end;
	}

	private final int tpPos(PhrasePositions pp) {
		return pp.position + pp.offset;
	}

}
