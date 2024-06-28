/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.util.ToStringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class SpanNearPayloadCheckQuery extends SpanPositionCheckQuery {
	protected final Collection<byte[]> payloadToMatch;

	public SpanNearPayloadCheckQuery(SpanNearQuery match,
			Collection<byte[]> payloadToMatch) {
		super(match);
		this.payloadToMatch = payloadToMatch;
	}

	@Override
	protected AcceptStatus acceptPosition(Spans spans) throws IOException {
		boolean result = spans.isPayloadAvailable();
		if (result == true) {
			Collection<byte[]> candidate = spans.getPayload();
			if (candidate.size() == payloadToMatch.size()) {

				int matches = 0;
				for (byte[] candBytes : candidate) {

					for (byte[] payBytes : payloadToMatch) {
						if (Arrays.equals(candBytes, payBytes) == true) {
							matches++;
							break;
						}
					}
				}
				if (matches == payloadToMatch.size()) {

					return AcceptStatus.YES;
				} else {
					return AcceptStatus.NO;
				}
			} else {
				return AcceptStatus.NO;
			}
		}
		return AcceptStatus.NO;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("spanPayCheck(");
		buffer.append(match.toString(field));
		buffer.append(", payloadRef: ");
		for (byte[] bytes : payloadToMatch) {
			ToStringUtils.byteArray(buffer, bytes);
			buffer.append(';');
		}
		buffer.append(")");
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public Object clone() {
		SpanNearPayloadCheckQuery result = new SpanNearPayloadCheckQuery(
				(SpanNearQuery) match.clone(), payloadToMatch);
		result.setBoost(getBoost());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SpanNearPayloadCheckQuery))
			return false;

		SpanNearPayloadCheckQuery other = (SpanNearPayloadCheckQuery) o;
		return this.payloadToMatch.equals(other.payloadToMatch)
				&& this.match.equals(other.match)
				&& this.getBoost() == other.getBoost();
	}

	@Override
	public int hashCode() {
		int h = match.hashCode();
		h ^= (h << 8) | (h >>> 25);

		h ^= payloadToMatch.hashCode();
		h ^= Float.floatToRawIntBits(getBoost());
		return h;
	}
}