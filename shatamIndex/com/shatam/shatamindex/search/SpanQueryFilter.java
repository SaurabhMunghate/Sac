/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.spans.SpanQuery;
import com.shatam.shatamindex.search.spans.Spans;
import com.shatam.shatamindex.util.FixedBitSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpanQueryFilter extends SpanFilter {
	protected SpanQuery query;

	protected SpanQueryFilter() {

	}

	public SpanQueryFilter(SpanQuery query) {
		this.query = query;
	}

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		SpanFilterResult result = bitSpans(reader);
		return result.getDocIdSet();
	}

	@Override
	public SpanFilterResult bitSpans(IndexReader reader) throws IOException {

		final FixedBitSet bits = new FixedBitSet(reader.maxDoc());
		Spans spans = query.getSpans(reader);
		List<SpanFilterResult.PositionInfo> tmp = new ArrayList<SpanFilterResult.PositionInfo>(
				20);
		int currentDoc = -1;
		SpanFilterResult.PositionInfo currentInfo = null;
		while (spans.next()) {
			int doc = spans.doc();
			bits.set(doc);
			if (currentDoc != doc) {
				currentInfo = new SpanFilterResult.PositionInfo(doc);
				tmp.add(currentInfo);
				currentDoc = doc;
			}
			currentInfo.addPosition(spans.start(), spans.end());
		}
		return new SpanFilterResult(bits, tmp);
	}

	public SpanQuery getQuery() {
		return query;
	}

	@Override
	public String toString() {
		return "SpanQueryFilter(" + query + ")";
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof SpanQueryFilter
				&& this.query.equals(((SpanQueryFilter) o).query);
	}

	@Override
	public int hashCode() {
		return query.hashCode() ^ 0x923F64B9;
	}
}
