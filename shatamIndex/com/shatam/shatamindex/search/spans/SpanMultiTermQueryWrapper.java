/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import java.io.IOException;
import java.lang.reflect.Method;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.MultiTermQuery;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.ScoringRewrite;
import com.shatam.shatamindex.search.TopTermsRewrite;
import com.shatam.shatamindex.search.BooleanClause.Occur;

public class SpanMultiTermQueryWrapper<Q extends MultiTermQuery> extends
		SpanQuery {
	protected final Q query;
	private Method getFieldMethod = null, getTermMethod = null;

	public SpanMultiTermQueryWrapper(Q query) {
		this.query = query;

		MultiTermQuery.RewriteMethod method = query.getRewriteMethod();
		if (method instanceof TopTermsRewrite) {
			final int pqsize = ((TopTermsRewrite) method).getSize();
			setRewriteMethod(new TopTermsSpanBooleanQueryRewrite(pqsize));
		} else {
			setRewriteMethod(SCORING_SPAN_QUERY_REWRITE);
		}

		try {
			getFieldMethod = query.getClass().getMethod("getField");
		} catch (Exception e1) {
			try {
				getTermMethod = query.getClass().getMethod("getTerm");
			} catch (Exception e2) {
				try {
					getTermMethod = query.getClass().getMethod("getPrefix");
				} catch (Exception e3) {
					throw new IllegalArgumentException(
							"SpanMultiTermQueryWrapper can only wrap MultiTermQueries"
									+ " that can return a field name using getField() or getTerm()");
				}
			}
		}
	}

	public final SpanRewriteMethod getRewriteMethod() {
		final MultiTermQuery.RewriteMethod m = query.getRewriteMethod();
		if (!(m instanceof SpanRewriteMethod))
			throw new UnsupportedOperationException(
					"You can only use SpanMultiTermQueryWrapper with a suitable SpanRewriteMethod.");
		return (SpanRewriteMethod) m;
	}

	public final void setRewriteMethod(SpanRewriteMethod rewriteMethod) {
		query.setRewriteMethod(rewriteMethod);
	}

	@Override
	public Spans getSpans(IndexReader reader) throws IOException {
		throw new UnsupportedOperationException(
				"Query should have been rewritten");
	}

	@Override
	public String getField() {
		try {
			if (getFieldMethod != null) {
				return (String) getFieldMethod.invoke(query);
			} else {
				assert getTermMethod != null;
				return ((Term) getTermMethod.invoke(query)).field();
			}
		} catch (Exception e) {
			throw new RuntimeException(
					"Cannot invoke getField() or getTerm() on wrapped query.",
					e);
		}
	}

	@Override
	public String toString(String field) {
		StringBuilder builder = new StringBuilder();
		builder.append("SpanMultiTermQueryWrapper(");
		builder.append(query.toString(field));
		builder.append(")");
		return builder.toString();
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		final Query q = query.rewrite(reader);
		if (!(q instanceof SpanQuery))
			throw new UnsupportedOperationException(
					"You can only use SpanMultiTermQueryWrapper with a suitable SpanRewriteMethod.");
		return q;
	}

	@Override
	public int hashCode() {
		return 31 * query.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SpanMultiTermQueryWrapper other = (SpanMultiTermQueryWrapper) obj;
		return query.equals(other.query);
	}

	public static abstract class SpanRewriteMethod extends
			MultiTermQuery.RewriteMethod {
		@Override
		public abstract SpanQuery rewrite(IndexReader reader,
				MultiTermQuery query) throws IOException;
	}

	public final static SpanRewriteMethod SCORING_SPAN_QUERY_REWRITE = new SpanRewriteMethod() {
		private final ScoringRewrite<SpanOrQuery> delegate = new ScoringRewrite<SpanOrQuery>() {
			@Override
			protected SpanOrQuery getTopLevelQuery() {
				return new SpanOrQuery();
			}

			@Override
			protected void addClause(SpanOrQuery topLevel, Term term,
					float boost) {
				final SpanTermQuery q = new SpanTermQuery(term);
				q.setBoost(boost);
				topLevel.addClause(q);
			}
		};

		@Override
		public SpanQuery rewrite(IndexReader reader, MultiTermQuery query)
				throws IOException {
			return delegate.rewrite(reader, query);
		}

		protected Object readResolve() {
			return SCORING_SPAN_QUERY_REWRITE;
		}
	};

	public static final class TopTermsSpanBooleanQueryRewrite extends
			SpanRewriteMethod {
		private final TopTermsRewrite<SpanOrQuery> delegate;

		public TopTermsSpanBooleanQueryRewrite(int size) {
			delegate = new TopTermsRewrite<SpanOrQuery>(size) {
				@Override
				protected int getMaxSize() {
					return Integer.MAX_VALUE;
				}

				@Override
				protected SpanOrQuery getTopLevelQuery() {
					return new SpanOrQuery();
				}

				@Override
				protected void addClause(SpanOrQuery topLevel, Term term,
						float boost) {
					final SpanTermQuery q = new SpanTermQuery(term);
					q.setBoost(boost);
					topLevel.addClause(q);
				}
			};
		}

		public int getSize() {
			return delegate.getSize();
		}

		@Override
		public SpanQuery rewrite(IndexReader reader, MultiTermQuery query)
				throws IOException {
			return delegate.rewrite(reader, query);
		}

		@Override
		public int hashCode() {
			return 31 * delegate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final TopTermsSpanBooleanQueryRewrite other = (TopTermsSpanBooleanQueryRewrite) obj;
			return delegate.equals(other.delegate);
		}

	}

}
