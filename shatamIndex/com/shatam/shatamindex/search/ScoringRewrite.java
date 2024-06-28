/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.MultiTermQuery.RewriteMethod;

public abstract class ScoringRewrite<Q extends Query> extends
		TermCollectingRewrite<Q> {

	public final static ScoringRewrite<BooleanQuery> SCORING_BOOLEAN_QUERY_REWRITE = new ScoringRewrite<BooleanQuery>() {
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

		protected Object readResolve() {
			return SCORING_BOOLEAN_QUERY_REWRITE;
		}
	};

	public final static RewriteMethod CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE = new RewriteMethod() {
		@Override
		public Query rewrite(IndexReader reader, MultiTermQuery query)
				throws IOException {
			final BooleanQuery bq = SCORING_BOOLEAN_QUERY_REWRITE.rewrite(
					reader, query);

			if (bq.clauses().isEmpty())
				return bq;

			final Query result = new ConstantScoreQuery(bq);
			result.setBoost(query.getBoost());
			return result;
		}

		protected Object readResolve() {
			return CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE;
		}
	};

	@Override
	public Q rewrite(final IndexReader reader, final MultiTermQuery query)
			throws IOException {
		final Q result = getTopLevelQuery();
		final int[] size = new int[1];
		collectTerms(reader, query, new TermCollector() {
			public boolean collect(Term t, float boost) throws IOException {
				addClause(result, t, query.getBoost() * boost);
				size[0]++;
				return true;
			}
		});
		query.incTotalNumberOfTerms(size[0]);
		return result;
	}
}
