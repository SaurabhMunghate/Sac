/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.ArrayList;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;

class ConstantScoreAutoRewrite extends TermCollectingRewrite<BooleanQuery> {

	public static int DEFAULT_TERM_COUNT_CUTOFF = 350;

	public static double DEFAULT_DOC_COUNT_PERCENT = 0.1;

	private int termCountCutoff = DEFAULT_TERM_COUNT_CUTOFF;
	private double docCountPercent = DEFAULT_DOC_COUNT_PERCENT;

	public void setTermCountCutoff(int count) {
		termCountCutoff = count;
	}

	public int getTermCountCutoff() {
		return termCountCutoff;
	}

	public void setDocCountPercent(double percent) {
		docCountPercent = percent;
	}

	public double getDocCountPercent() {
		return docCountPercent;
	}

	@Override
	protected BooleanQuery getTopLevelQuery() {
		return new BooleanQuery(true);
	}

	@Override
	protected void addClause(BooleanQuery topLevel, Term term, float boost) {
		topLevel.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
	}

	@Override
	public Query rewrite(final IndexReader reader, final MultiTermQuery query)
			throws IOException {

		final int docCountCutoff = (int) ((docCountPercent / 100.) * reader
				.maxDoc());
		final int termCountLimit = Math.min(BooleanQuery.getMaxClauseCount(),
				termCountCutoff);

		final CutOffTermCollector col = new CutOffTermCollector(reader,
				docCountCutoff, termCountLimit);
		collectTerms(reader, query, col);

		if (col.hasCutOff) {
			return MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE.rewrite(reader,
					query);
		} else {
			final Query result;
			if (col.pendingTerms.isEmpty()) {
				result = getTopLevelQuery();
			} else {
				BooleanQuery bq = getTopLevelQuery();
				for (Term term : col.pendingTerms) {
					addClause(bq, term, 1.0f);
				}

				result = new ConstantScoreQuery(bq);
				result.setBoost(query.getBoost());
			}
			query.incTotalNumberOfTerms(col.pendingTerms.size());
			return result;
		}
	}

	private static final class CutOffTermCollector implements TermCollector {
		CutOffTermCollector(IndexReader reader, int docCountCutoff,
				int termCountLimit) {
			this.reader = reader;
			this.docCountCutoff = docCountCutoff;
			this.termCountLimit = termCountLimit;
		}

		public boolean collect(Term t, float boost) throws IOException {
			pendingTerms.add(t);

			docVisitCount += reader.docFreq(t);
			if (pendingTerms.size() >= termCountLimit
					|| docVisitCount >= docCountCutoff) {
				hasCutOff = true;
				return false;
			}
			return true;
		}

		int docVisitCount = 0;
		boolean hasCutOff = false;

		final IndexReader reader;
		final int docCountCutoff, termCountLimit;
		final ArrayList<Term> pendingTerms = new ArrayList<Term>();
	}

	@Override
	public int hashCode() {
		final int prime = 1279;
		return (int) (prime * termCountCutoff + Double
				.doubleToLongBits(docCountPercent));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		ConstantScoreAutoRewrite other = (ConstantScoreAutoRewrite) obj;
		if (other.termCountCutoff != termCountCutoff) {
			return false;
		}

		if (Double.doubleToLongBits(other.docCountPercent) != Double
				.doubleToLongBits(docCountPercent)) {
			return false;
		}

		return true;
	}
}
