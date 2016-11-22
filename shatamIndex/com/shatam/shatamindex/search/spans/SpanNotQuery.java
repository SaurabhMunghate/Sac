/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.ToStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class SpanNotQuery extends SpanQuery implements Cloneable {
	private SpanQuery include;
	private SpanQuery exclude;

	public SpanNotQuery(SpanQuery include, SpanQuery exclude) {
		this.include = include;
		this.exclude = exclude;

		if (!include.getField().equals(exclude.getField()))
			throw new IllegalArgumentException("Clauses must have same field.");
	}

	public SpanQuery getInclude() {
		return include;
	}

	public SpanQuery getExclude() {
		return exclude;
	}

	@Override
	public String getField() {
		return include.getField();
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		include.extractTerms(terms);
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("spanNot(");
		buffer.append(include.toString(field));
		buffer.append(", ");
		buffer.append(exclude.toString(field));
		buffer.append(")");
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public Object clone() {
		SpanNotQuery spanNotQuery = new SpanNotQuery(
				(SpanQuery) include.clone(), (SpanQuery) exclude.clone());
		spanNotQuery.setBoost(getBoost());
		return spanNotQuery;
	}

	@Override
	public Spans getSpans(final IndexReader reader) throws IOException {
		return new Spans() {
			private Spans includeSpans = include.getSpans(reader);
			private boolean moreInclude = true;

			private Spans excludeSpans = exclude.getSpans(reader);
			private boolean moreExclude = excludeSpans.next();

			@Override
			public boolean next() throws IOException {
				if (moreInclude)
					moreInclude = includeSpans.next();

				while (moreInclude && moreExclude) {

					if (includeSpans.doc() > excludeSpans.doc())
						moreExclude = excludeSpans.skipTo(includeSpans.doc());

					while (moreExclude
							&& includeSpans.doc() == excludeSpans.doc()
							&& excludeSpans.end() <= includeSpans.start()) {
						moreExclude = excludeSpans.next();
					}

					if (!moreExclude
							|| includeSpans.doc() != excludeSpans.doc()
							|| includeSpans.end() <= excludeSpans.start())
						break;

					moreInclude = includeSpans.next();
				}
				return moreInclude;
			}

			@Override
			public boolean skipTo(int target) throws IOException {
				if (moreInclude)
					moreInclude = includeSpans.skipTo(target);

				if (!moreInclude)
					return false;

				if (moreExclude && includeSpans.doc() > excludeSpans.doc())
					moreExclude = excludeSpans.skipTo(includeSpans.doc());

				while (moreExclude && includeSpans.doc() == excludeSpans.doc()
						&& excludeSpans.end() <= includeSpans.start()) {
					moreExclude = excludeSpans.next();
				}

				if (!moreExclude || includeSpans.doc() != excludeSpans.doc()
						|| includeSpans.end() <= excludeSpans.start())
					return true;

				return next();
			}

			@Override
			public int doc() {
				return includeSpans.doc();
			}

			@Override
			public int start() {
				return includeSpans.start();
			}

			@Override
			public int end() {
				return includeSpans.end();
			}

			@Override
			public Collection<byte[]> getPayload() throws IOException {
				ArrayList<byte[]> result = null;
				if (includeSpans.isPayloadAvailable()) {
					result = new ArrayList<byte[]>(includeSpans.getPayload());
				}
				return result;
			}

			@Override
			public boolean isPayloadAvailable() {
				return includeSpans.isPayloadAvailable();
			}

			@Override
			public String toString() {
				return "spans(" + SpanNotQuery.this.toString() + ")";
			}

		};
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		SpanNotQuery clone = null;

		SpanQuery rewrittenInclude = (SpanQuery) include.rewrite(reader);
		if (rewrittenInclude != include) {
			clone = (SpanNotQuery) this.clone();
			clone.include = rewrittenInclude;
		}
		SpanQuery rewrittenExclude = (SpanQuery) exclude.rewrite(reader);
		if (rewrittenExclude != exclude) {
			if (clone == null)
				clone = (SpanNotQuery) this.clone();
			clone.exclude = rewrittenExclude;
		}

		if (clone != null) {
			return clone;
		} else {
			return this;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SpanNotQuery))
			return false;

		SpanNotQuery other = (SpanNotQuery) o;
		return this.include.equals(other.include)
				&& this.exclude.equals(other.exclude)
				&& this.getBoost() == other.getBoost();
	}

	@Override
	public int hashCode() {
		int h = include.hashCode();
		h = (h << 1) | (h >>> 31);
		h ^= exclude.hashCode();
		h = (h << 1) | (h >>> 31);
		h ^= Float.floatToRawIntBits(getBoost());
		return h;
	}

}
