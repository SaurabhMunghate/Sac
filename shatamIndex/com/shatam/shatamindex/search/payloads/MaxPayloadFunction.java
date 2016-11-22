/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.payloads;

import com.shatam.shatamindex.search.Explanation;

public class MaxPayloadFunction extends PayloadFunction {
	@Override
	public float currentScore(int docId, String field, int start, int end,
			int numPayloadsSeen, float currentScore, float currentPayloadScore) {
		if (numPayloadsSeen == 0) {
			return currentPayloadScore;
		} else {
			return Math.max(currentPayloadScore, currentScore);
		}
	}

	@Override
	public float docScore(int docId, String field, int numPayloadsSeen,
			float payloadScore) {
		return numPayloadsSeen > 0 ? payloadScore : 1;
	}

	@Override
	public Explanation explain(int doc, int numPayloadsSeen, float payloadScore) {
		Explanation expl = new Explanation();
		float maxPayloadScore = (numPayloadsSeen > 0 ? payloadScore : 1);
		expl.setValue(maxPayloadScore);
		expl.setDescription("MaxPayloadFunction(...)");
		return expl;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.getClass().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}
}
