/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.BooleanClause.Occur;
import com.shatam.shatamindex.util.ToStringUtils;

import java.io.IOException;
import java.util.*;

public class BooleanQuery extends Query implements Iterable<BooleanClause> {

	private static int maxClauseCount = 1024;

	public static class TooManyClauses extends RuntimeException {
		public TooManyClauses() {
			super("maxClauseCount is set to " + maxClauseCount);
		}
	}

	public static int getMaxClauseCount() {
		return maxClauseCount;
	}

	public static void setMaxClauseCount(int maxClauseCount) {
		if (maxClauseCount < 1)
			throw new IllegalArgumentException("maxClauseCount must be >= 1");
		BooleanQuery.maxClauseCount = maxClauseCount;
	}

	private ArrayList<BooleanClause> clauses = new ArrayList<BooleanClause>();
	private final boolean disableCoord;

	public BooleanQuery() {
		disableCoord = false;
	}

	public BooleanQuery(boolean disableCoord) {
		this.disableCoord = disableCoord;
	}

	public boolean isCoordDisabled() {
		return disableCoord;
	}

	public void setMinimumNumberShouldMatch(int min) {
		this.minNrShouldMatch = min;
	}

	protected int minNrShouldMatch = 0;

	public int getMinimumNumberShouldMatch() {
		return minNrShouldMatch;
	}

	public void add(Query query, BooleanClause.Occur occur) {
		add(new BooleanClause(query, occur));
	}

	public void add(BooleanClause clause) {
		if (clauses.size() >= maxClauseCount)
			throw new TooManyClauses();

		clauses.add(clause);
	}

	public BooleanClause[] getClauses() {
		return clauses.toArray(new BooleanClause[clauses.size()]);
	}

	public List<BooleanClause> clauses() {
		return clauses;
	}

	public final Iterator<BooleanClause> iterator() {
		return clauses().iterator();
	}

	protected class BooleanWeight extends Weight {

		protected Similarity similarity;
		protected ArrayList<Weight> weights;
		protected int maxCoord;
		private final boolean disableCoord;

		public BooleanWeight(Searcher searcher, boolean disableCoord)
				throws IOException {
			this.similarity = getSimilarity(searcher);
			this.disableCoord = disableCoord;
			weights = new ArrayList<Weight>(clauses.size());
			for (int i = 0; i < clauses.size(); i++) {
				BooleanClause c = clauses.get(i);
				weights.add(c.getQuery().createWeight(searcher));
				if (!c.isProhibited())
					maxCoord++;
			}
		}

		@Override
		public Query getQuery() {
			return BooleanQuery.this;
		}

		@Override
		public float getValue() {
			return getBoost();
		}

		@Override
		public float sumOfSquaredWeights() throws IOException {
			float sum = 0.0f;
			for (int i = 0; i < weights.size(); i++) {

				float s = weights.get(i).sumOfSquaredWeights();

				if (!clauses.get(i).isProhibited())

					sum += s;
			}

			sum *= getBoost() * getBoost();

			return sum;
		}

		@Override
		public void normalize(float norm) {
			norm *= getBoost();
			for (Weight w : weights) {

				w.normalize(norm);
			}
		}

		@Override
		public Explanation explain(IndexReader reader, int doc)
				throws IOException {
			final int minShouldMatch = BooleanQuery.this
					.getMinimumNumberShouldMatch();
			ComplexExplanation sumExpl = new ComplexExplanation();
			sumExpl.setDescription("sum of:");
			int coord = 0;
			float sum = 0.0f;
			boolean fail = false;
			int shouldMatchCount = 0;
			Iterator<BooleanClause> cIter = clauses.iterator();
			for (Iterator<Weight> wIter = weights.iterator(); wIter.hasNext();) {
				Weight w = wIter.next();
				BooleanClause c = cIter.next();
				if (w.scorer(reader, true, true) == null) {
					if (c.isRequired()) {
						fail = true;
						Explanation r = new Explanation(0.0f,
								"no match on required clause ("
										+ c.getQuery().toString() + ")");
						sumExpl.addDetail(r);
					}
					continue;
				}
				Explanation e = w.explain(reader, doc);
				if (e.isMatch()) {
					if (!c.isProhibited()) {
						sumExpl.addDetail(e);
						sum += e.getValue();
						coord++;
					} else {
						Explanation r = new Explanation(0.0f,
								"match on prohibited clause ("
										+ c.getQuery().toString() + ")");
						r.addDetail(e);
						sumExpl.addDetail(r);
						fail = true;
					}
					if (c.getOccur() == Occur.SHOULD)
						shouldMatchCount++;
				} else if (c.isRequired()) {
					Explanation r = new Explanation(0.0f,
							"no match on required clause ("
									+ c.getQuery().toString() + ")");
					r.addDetail(e);
					sumExpl.addDetail(r);
					fail = true;
				}
			}
			if (fail) {
				sumExpl.setMatch(Boolean.FALSE);
				sumExpl.setValue(0.0f);
				sumExpl.setDescription("Failure to meet condition(s) of required/prohibited clause(s)");
				return sumExpl;
			} else if (shouldMatchCount < minShouldMatch) {
				sumExpl.setMatch(Boolean.FALSE);
				sumExpl.setValue(0.0f);
				sumExpl.setDescription("Failure to match minimum number "
						+ "of optional clauses: " + minShouldMatch);
				return sumExpl;
			}

			sumExpl.setMatch(0 < coord ? Boolean.TRUE : Boolean.FALSE);
			sumExpl.setValue(sum);

			final float coordFactor = disableCoord ? 1.0f : similarity.coord(
					coord, maxCoord);
			if (coordFactor == 1.0f) {
				return sumExpl;
			} else {
				ComplexExplanation result = new ComplexExplanation(
						sumExpl.isMatch(), sum * coordFactor, "product of:");
				result.addDetail(sumExpl);
				result.addDetail(new Explanation(coordFactor, "coord(" + coord
						+ "/" + maxCoord + ")"));
				return result;
			}
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {
			List<Scorer> required = new ArrayList<Scorer>();
			List<Scorer> prohibited = new ArrayList<Scorer>();
			List<Scorer> optional = new ArrayList<Scorer>();
			Iterator<BooleanClause> cIter = clauses.iterator();
			for (Weight w : weights) {
				BooleanClause c = cIter.next();
				Scorer subScorer = w.scorer(reader, true, false);
				if (subScorer == null) {
					if (c.isRequired()) {
						return null;
					}
				} else if (c.isRequired()) {
					required.add(subScorer);
				} else if (c.isProhibited()) {
					prohibited.add(subScorer);
				} else {
					optional.add(subScorer);
				}
			}

			if (!scoreDocsInOrder && topScorer && required.size() == 0) {
				return new BooleanScorer(this, disableCoord, similarity,
						minNrShouldMatch, optional, prohibited, maxCoord);
			}

			if (required.size() == 0 && optional.size() == 0) {

				return null;
			} else if (optional.size() < minNrShouldMatch) {

				return null;
			}

			return new BooleanScorer2(this, disableCoord, similarity,
					minNrShouldMatch, required, prohibited, optional, maxCoord);
		}

		@Override
		public boolean scoresDocsOutOfOrder() {
			for (BooleanClause c : clauses) {
				if (c.isRequired()) {
					return false;
				}
			}

			return true;
		}

	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return new BooleanWeight(searcher, disableCoord);
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if (minNrShouldMatch == 0 && clauses.size() == 1) {

			BooleanClause c = clauses.get(0);
			if (!c.isProhibited()) {

				Query query = c.getQuery().rewrite(reader);

				if (getBoost() != 1.0f) {
					if (query == c.getQuery())
						query = (Query) query.clone();

					query.setBoost(getBoost() * query.getBoost());
				}

				return query;
			}
		}

		BooleanQuery clone = null;
		for (int i = 0; i < clauses.size(); i++) {
			BooleanClause c = clauses.get(i);
			Query query = c.getQuery().rewrite(reader);
			if (query != c.getQuery()) {
				if (clone == null)
					clone = (BooleanQuery) this.clone();
				clone.clauses.set(i, new BooleanClause(query, c.getOccur()));
			}
		}
		if (clone != null) {
			return clone;
		} else
			return this;
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		for (BooleanClause clause : clauses) {
			clause.getQuery().extractTerms(terms);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() {
		BooleanQuery clone = (BooleanQuery) super.clone();
		clone.clauses = (ArrayList<BooleanClause>) this.clauses.clone();
		return clone;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		boolean needParens = (getBoost() != 1.0)
				|| (getMinimumNumberShouldMatch() > 0);
		if (needParens) {
			buffer.append("(");
		}

		for (int i = 0; i < clauses.size(); i++) {
			BooleanClause c = clauses.get(i);
			if (c.isProhibited())
				buffer.append("-");
			else if (c.isRequired())
				buffer.append("+");

			Query subQuery = c.getQuery();
			if (subQuery != null) {
				if (subQuery instanceof BooleanQuery) {

					buffer.append("(");
					buffer.append(subQuery.toString(field));
					buffer.append(")");
				} else {
					buffer.append(subQuery.toString(field));
				}
			} else {
				buffer.append("null");
			}

			if (i != clauses.size() - 1)
				buffer.append(" ");
		}

		if (needParens) {
			buffer.append(")");
		}

		if (getMinimumNumberShouldMatch() > 0) {
			buffer.append('~');
			buffer.append(getMinimumNumberShouldMatch());
		}

		if (getBoost() != 1.0f) {
			buffer.append(ToStringUtils.boost(getBoost()));
		}

		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BooleanQuery))
			return false;
		BooleanQuery other = (BooleanQuery) o;
		return (this.getBoost() == other.getBoost())
				&& this.clauses.equals(other.clauses)
				&& this.getMinimumNumberShouldMatch() == other
						.getMinimumNumberShouldMatch()
				&& this.disableCoord == other.disableCoord;
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ clauses.hashCode()
				+ getMinimumNumberShouldMatch() + (disableCoord ? 17 : 0);
	}

}
