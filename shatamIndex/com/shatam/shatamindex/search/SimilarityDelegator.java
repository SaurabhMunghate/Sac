/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.FieldInvertState;

@Deprecated
public class SimilarityDelegator extends Similarity {

	private Similarity delegee;

	public SimilarityDelegator(Similarity delegee) {
		this.delegee = delegee;
	}

	@Override
	public float computeNorm(String fieldName, FieldInvertState state) {
		return delegee.computeNorm(fieldName, state);
	}

	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		return delegee.queryNorm(sumOfSquaredWeights);
	}

	@Override
	public float tf(float freq) {
		return delegee.tf(freq);
	}

	@Override
	public float sloppyFreq(int distance) {
		return delegee.sloppyFreq(distance);
	}

	@Override
	public float idf(int docFreq, int numDocs) {
		return delegee.idf(docFreq, numDocs);
	}

	@Override
	public float coord(int overlap, int maxOverlap) {
		return delegee.coord(overlap, maxOverlap);
	}

	@Override
	public float scorePayload(int docId, String fieldName, int start, int end,
			byte[] payload, int offset, int length) {
		return delegee.scorePayload(docId, fieldName, start, end, payload,
				offset, length);
	}
}
