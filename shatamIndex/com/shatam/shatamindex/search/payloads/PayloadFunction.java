/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.payloads;

import java.io.Serializable;

import com.shatam.shatamindex.search.Explanation;

public abstract class PayloadFunction implements Serializable {

	public abstract float currentScore(int docId, String field, int start,
			int end, int numPayloadsSeen, float currentScore,
			float currentPayloadScore);

	public abstract float docScore(int docId, String field,
			int numPayloadsSeen, float payloadScore);

	public Explanation explain(int docId, int numPayloadsSeen,
			float payloadScore) {
		Explanation result = new Explanation();
		result.setDescription("Unimpl Payload Function Explain");
		result.setValue(1);
		return result;
	};

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object o);

}
