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

public class SpanFirstQuery extends SpanPositionRangeQuery {

	public SpanFirstQuery(SpanQuery match, int end) {
		super(match, 0, end);
	}

	@Override
	protected AcceptStatus acceptPosition(Spans spans) throws IOException {
		assert spans.start() != spans.end();
		if (spans.start() >= end)
			return AcceptStatus.NO_AND_ADVANCE;
		else if (spans.end() <= end)
			return AcceptStatus.YES;
		else
			return AcceptStatus.NO;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("spanFirst(");
		buffer.append(match.toString(field));
		buffer.append(", ");
		buffer.append(end);
		buffer.append(")");
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public Object clone() {
		SpanFirstQuery spanFirstQuery = new SpanFirstQuery(
				(SpanQuery) match.clone(), end);
		spanFirstQuery.setBoost(getBoost());
		return spanFirstQuery;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SpanFirstQuery))
			return false;

		SpanFirstQuery other = (SpanFirstQuery) o;
		return this.end == other.end && this.match.equals(other.match)
				&& this.getBoost() == other.getBoost();
	}

	@Override
	public int hashCode() {
		int h = match.hashCode();
		h ^= (h << 8) | (h >>> 25);
		h ^= Float.floatToRawIntBits(getBoost()) ^ end;
		return h;
	}

}
