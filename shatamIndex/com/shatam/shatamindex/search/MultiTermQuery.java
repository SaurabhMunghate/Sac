/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.io.Serializable;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.queryParser.QueryParser;

public abstract class MultiTermQuery extends Query {
	protected RewriteMethod rewriteMethod = CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;
	transient int numberOfTerms = 0;

	public static abstract class RewriteMethod implements Serializable {
		public abstract Query rewrite(IndexReader reader, MultiTermQuery query)
				throws IOException;
	}

	public static final RewriteMethod CONSTANT_SCORE_FILTER_REWRITE = new RewriteMethod() {
		@Override
		public Query rewrite(IndexReader reader, MultiTermQuery query) {
			Query result = new ConstantScoreQuery(
					new MultiTermQueryWrapperFilter<MultiTermQuery>(query));
			result.setBoost(query.getBoost());
			return result;
		}

		protected Object readResolve() {
			return CONSTANT_SCORE_FILTER_REWRITE;
		}
	};

	public final static RewriteMethod SCORING_BOOLEAN_QUERY_REWRITE = ScoringRewrite.SCORING_BOOLEAN_QUERY_REWRITE;

	public final static RewriteMethod CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE = ScoringRewrite.CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE;

	public static final class TopTermsScoringBooleanQueryRewrite extends
			TopTermsRewrite<BooleanQuery> {

		public TopTermsScoringBooleanQueryRewrite(int size) {
			super(size);
		}

		@Override
		protected int getMaxSize() {
			return BooleanQuery.getMaxClauseCount();
		}

		@Override
		protected BooleanQuery getTopLevelQuery() {
			return new BooleanQuery(true);
		}

		@Override
		protected void addClause(BooleanQuery topLevel, Term term, float boost) {
			final TermQuery tq = new TermQuery(term);
			tq.setBoost(boost);
			topLevel.add(tq, BooleanClause.Occur.SHOULD);
		}
	}

	public static final class TopTermsBoostOnlyBooleanQueryRewrite extends
			TopTermsRewrite<BooleanQuery> {

		public TopTermsBoostOnlyBooleanQueryRewrite(int size) {
			super(size);
		}

		@Override
		protected int getMaxSize() {
			return BooleanQuery.getMaxClauseCount();
		}

		@Override
		protected BooleanQuery getTopLevelQuery() {
			return new BooleanQuery(true);
		}

		@Override
		protected void addClause(BooleanQuery topLevel, Term term, float boost) {
			final Query q = new ConstantScoreQuery(new TermQuery(term));
			q.setBoost(boost);
			topLevel.add(q, BooleanClause.Occur.SHOULD);
		}
	}

	public static class ConstantScoreAutoRewrite extends
			com.shatam.shatamindex.search.ConstantScoreAutoRewrite {
	}

	public final static RewriteMethod CONSTANT_SCORE_AUTO_REWRITE_DEFAULT = new ConstantScoreAutoRewrite() {
		@Override
		public void setTermCountCutoff(int count) {
			throw new UnsupportedOperationException(
					"Please create a private instance");
		}

		@Override
		public void setDocCountPercent(double percent) {
			throw new UnsupportedOperationException(
					"Please create a private instance");
		}

		protected Object readResolve() {
			return CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;
		}
	};

	public MultiTermQuery() {
	}

	protected abstract FilteredTermEnum getEnum(IndexReader reader)
			throws IOException;

	@Deprecated
	public int getTotalNumberOfTerms() {
		return numberOfTerms;
	}

	@Deprecated
	public void clearTotalNumberOfTerms() {
		numberOfTerms = 0;
	}

	@Deprecated
	protected void incTotalNumberOfTerms(int inc) {
		numberOfTerms += inc;
	}

	@Override
	public final Query rewrite(IndexReader reader) throws IOException {
		return rewriteMethod.rewrite(reader, this);
	}

	public RewriteMethod getRewriteMethod() {
		return rewriteMethod;
	}

	public void setRewriteMethod(RewriteMethod method) {
		rewriteMethod = method;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(getBoost());
		result = prime * result;
		result += rewriteMethod.hashCode();
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
		MultiTermQuery other = (MultiTermQuery) obj;
		if (Float.floatToIntBits(getBoost()) != Float.floatToIntBits(other
				.getBoost()))
			return false;
		if (!rewriteMethod.equals(other.rewriteMethod)) {
			return false;
		}
		return true;
	}

}
