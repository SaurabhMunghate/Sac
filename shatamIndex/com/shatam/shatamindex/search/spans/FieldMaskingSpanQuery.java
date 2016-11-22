/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import java.io.IOException;
import java.util.Set;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.Searcher;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.search.Weight;
import com.shatam.shatamindex.util.ToStringUtils;

public class FieldMaskingSpanQuery extends SpanQuery {
	private SpanQuery maskedQuery;
	private String field;

	public FieldMaskingSpanQuery(SpanQuery maskedQuery, String maskedField) {
		this.maskedQuery = maskedQuery;
		this.field = maskedField;
	}

	@Override
	public String getField() {
		return field;
	}

	public SpanQuery getMaskedQuery() {
		return maskedQuery;
	}

	@Override
	public Spans getSpans(IndexReader reader) throws IOException {
		return maskedQuery.getSpans(reader);
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		maskedQuery.extractTerms(terms);
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return maskedQuery.createWeight(searcher);
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		FieldMaskingSpanQuery clone = null;

		SpanQuery rewritten = (SpanQuery) maskedQuery.rewrite(reader);
		if (rewritten != maskedQuery) {
			clone = (FieldMaskingSpanQuery) this.clone();
			clone.maskedQuery = rewritten;
		}

		if (clone != null) {
			return clone;
		} else {
			return this;
		}
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("mask(");
		buffer.append(maskedQuery.toString(field));
		buffer.append(")");
		buffer.append(ToStringUtils.boost(getBoost()));
		buffer.append(" as ");
		buffer.append(this.field);
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FieldMaskingSpanQuery))
			return false;
		FieldMaskingSpanQuery other = (FieldMaskingSpanQuery) o;
		return (this.getField().equals(other.getField())
				&& (this.getBoost() == other.getBoost()) && this
				.getMaskedQuery().equals(other.getMaskedQuery()));

	}

	@Override
	public int hashCode() {
		return getMaskedQuery().hashCode() ^ getField().hashCode()
				^ Float.floatToRawIntBits(getBoost());
	}
}
