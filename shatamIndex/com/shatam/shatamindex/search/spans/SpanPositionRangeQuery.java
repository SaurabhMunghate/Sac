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

public class SpanPositionRangeQuery extends SpanPositionCheckQuery {
	protected int start = 0;
	protected int end;

	public SpanPositionRangeQuery(SpanQuery match, int start, int end) {
		super(match);
		this.start = start;
		this.end = end;
	}

	@Override
	protected AcceptStatus acceptPosition(Spans spans) throws IOException {
		assert spans.start() != spans.end();
		if (spans.start() >= end)
			return AcceptStatus.NO_AND_ADVANCE;
		else if (spans.start() >= start && spans.end() <= end)
			return AcceptStatus.YES;
		else
			return AcceptStatus.NO;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("spanPosRange(");
		buffer.append(match.toString(field));
		buffer.append(", ").append(start).append(", ");
		buffer.append(end);
		buffer.append(")");
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public Object clone() {
		SpanPositionRangeQuery result = new SpanPositionRangeQuery(
				(SpanQuery) match.clone(), start, end);
		result.setBoost(getBoost());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SpanPositionRangeQuery))
			return false;

		SpanPositionRangeQuery other = (SpanPositionRangeQuery) o;
		return this.end == other.end && this.start == other.start
				&& this.match.equals(other.match)
				&& this.getBoost() == other.getBoost();
	}

	@Override
	public int hashCode() {
		int h = match.hashCode();
		h ^= (h << 8) | (h >>> 25);
		h ^= Float.floatToRawIntBits(getBoost()) ^ end ^ start;
		return h;
	}

}