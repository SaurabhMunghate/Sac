/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.function;

import java.io.IOException;
import java.util.Set;
import java.util.Arrays;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.ComplexExplanation;
import com.shatam.shatamindex.search.Explanation;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.Scorer;
import com.shatam.shatamindex.search.Searcher;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.search.Weight;
import com.shatam.shatamindex.util.ToStringUtils;

public class CustomScoreQuery extends Query {

	private Query subQuery;
	private ValueSourceQuery[] valSrcQueries;
	private boolean strict = false;

	public CustomScoreQuery(Query subQuery) {
		this(subQuery, new ValueSourceQuery[0]);
	}

	public CustomScoreQuery(Query subQuery, ValueSourceQuery valSrcQuery) {
		this(subQuery,
				valSrcQuery != null ? new ValueSourceQuery[] { valSrcQuery }
						: new ValueSourceQuery[0]);
	}

	public CustomScoreQuery(Query subQuery, ValueSourceQuery... valSrcQueries) {
		this.subQuery = subQuery;
		this.valSrcQueries = valSrcQueries != null ? valSrcQueries
				: new ValueSourceQuery[0];
		if (subQuery == null)
			throw new IllegalArgumentException("<subquery> must not be null!");
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		CustomScoreQuery clone = null;

		final Query sq = subQuery.rewrite(reader);
		if (sq != subQuery) {
			clone = (CustomScoreQuery) clone();
			clone.subQuery = sq;
		}

		for (int i = 0; i < valSrcQueries.length; i++) {
			final ValueSourceQuery v = (ValueSourceQuery) valSrcQueries[i]
					.rewrite(reader);
			if (v != valSrcQueries[i]) {
				if (clone == null)
					clone = (CustomScoreQuery) clone();
				clone.valSrcQueries[i] = v;
			}
		}

		return (clone == null) ? this : clone;
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		subQuery.extractTerms(terms);
		for (int i = 0; i < valSrcQueries.length; i++) {
			valSrcQueries[i].extractTerms(terms);
		}
	}

	@Override
	public Object clone() {
		CustomScoreQuery clone = (CustomScoreQuery) super.clone();
		clone.subQuery = (Query) subQuery.clone();
		clone.valSrcQueries = new ValueSourceQuery[valSrcQueries.length];
		for (int i = 0; i < valSrcQueries.length; i++) {
			clone.valSrcQueries[i] = (ValueSourceQuery) valSrcQueries[i]
					.clone();
		}
		return clone;
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder(name()).append("(");
		sb.append(subQuery.toString(field));
		for (int i = 0; i < valSrcQueries.length; i++) {
			sb.append(", ").append(valSrcQueries[i].toString(field));
		}
		sb.append(")");
		sb.append(strict ? " STRICT" : "");
		return sb.toString() + ToStringUtils.boost(getBoost());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!super.equals(o))
			return false;
		if (getClass() != o.getClass()) {
			return false;
		}
		CustomScoreQuery other = (CustomScoreQuery) o;
		if (this.getBoost() != other.getBoost()
				|| !this.subQuery.equals(other.subQuery)
				|| this.strict != other.strict
				|| this.valSrcQueries.length != other.valSrcQueries.length) {
			return false;
		}
		return Arrays.equals(valSrcQueries, other.valSrcQueries);
	}

	@Override
	public int hashCode() {
		return (getClass().hashCode() + subQuery.hashCode() + Arrays
				.hashCode(valSrcQueries))
				^ Float.floatToIntBits(getBoost())
				^ (strict ? 1234 : 4321);
	}

	protected CustomScoreProvider getCustomScoreProvider(IndexReader reader)
			throws IOException {
		return new CustomScoreProvider(reader);
	}

	private class CustomWeight extends Weight {
		Similarity similarity;
		Weight subQueryWeight;
		Weight[] valSrcWeights;
		boolean qStrict;

		public CustomWeight(Searcher searcher) throws IOException {
			this.similarity = getSimilarity(searcher);
			this.subQueryWeight = subQuery.createWeight(searcher);
			this.valSrcWeights = new Weight[valSrcQueries.length];
			for (int i = 0; i < valSrcQueries.length; i++) {
				this.valSrcWeights[i] = valSrcQueries[i].createWeight(searcher);
			}
			this.qStrict = strict;
		}

		@Override
		public Query getQuery() {
			return CustomScoreQuery.this;
		}

		@Override
		public float getValue() {
			return getBoost();
		}

		@Override
		public float sumOfSquaredWeights() throws IOException {
			float sum = subQueryWeight.sumOfSquaredWeights();
			for (int i = 0; i < valSrcWeights.length; i++) {
				if (qStrict) {
					valSrcWeights[i].sumOfSquaredWeights();
				} else {
					sum += valSrcWeights[i].sumOfSquaredWeights();
				}
			}
			sum *= getBoost() * getBoost();
			return sum;
		}

		@Override
		public void normalize(float norm) {
			norm *= getBoost();
			subQueryWeight.normalize(norm);
			for (int i = 0; i < valSrcWeights.length; i++) {
				if (qStrict) {
					valSrcWeights[i].normalize(1);
				} else {
					valSrcWeights[i].normalize(norm);
				}
			}
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {

			Scorer subQueryScorer = subQueryWeight.scorer(reader, true, false);
			if (subQueryScorer == null) {
				return null;
			}
			Scorer[] valSrcScorers = new Scorer[valSrcWeights.length];
			for (int i = 0; i < valSrcScorers.length; i++) {
				valSrcScorers[i] = valSrcWeights[i].scorer(reader, true,
						topScorer);
			}
			return new CustomScorer(similarity, reader, this, subQueryScorer,
					valSrcScorers);
		}

		@Override
		public Explanation explain(IndexReader reader, int doc)
				throws IOException {
			Explanation explain = doExplain(reader, doc);
			return explain == null ? new Explanation(0.0f, "no matching docs")
					: explain;
		}

		private Explanation doExplain(IndexReader reader, int doc)
				throws IOException {
			Explanation subQueryExpl = subQueryWeight.explain(reader, doc);
			if (!subQueryExpl.isMatch()) {
				return subQueryExpl;
			}

			Explanation[] valSrcExpls = new Explanation[valSrcWeights.length];
			for (int i = 0; i < valSrcWeights.length; i++) {
				valSrcExpls[i] = valSrcWeights[i].explain(reader, doc);
			}
			Explanation customExp = CustomScoreQuery.this
					.getCustomScoreProvider(reader).customExplain(doc,
							subQueryExpl, valSrcExpls);
			float sc = getValue() * customExp.getValue();
			Explanation res = new ComplexExplanation(true, sc,
					CustomScoreQuery.this.toString() + ", product of:");
			res.addDetail(customExp);
			res.addDetail(new Explanation(getValue(), "queryBoost"));
			return res;
		}

		@Override
		public boolean scoresDocsOutOfOrder() {
			return false;
		}

	}

	private class CustomScorer extends Scorer {
		private final float qWeight;
		private Scorer subQueryScorer;
		private Scorer[] valSrcScorers;
		private final CustomScoreProvider provider;
		private float vScores[];

		private CustomScorer(Similarity similarity, IndexReader reader,
				CustomWeight w, Scorer subQueryScorer, Scorer[] valSrcScorers)
				throws IOException {
			super(similarity, w);
			this.qWeight = w.getValue();
			this.subQueryScorer = subQueryScorer;
			this.valSrcScorers = valSrcScorers;
			this.vScores = new float[valSrcScorers.length];
			this.provider = CustomScoreQuery.this
					.getCustomScoreProvider(reader);
		}

		@Override
		public int nextDoc() throws IOException {
			int doc = subQueryScorer.nextDoc();
			if (doc != NO_MORE_DOCS) {
				for (int i = 0; i < valSrcScorers.length; i++) {
					valSrcScorers[i].advance(doc);
				}
			}
			return doc;
		}

		@Override
		public int docID() {
			return subQueryScorer.docID();
		}

		@Override
		public float score() throws IOException {
			for (int i = 0; i < valSrcScorers.length; i++) {
				vScores[i] = valSrcScorers[i].score();
			}
			return qWeight
					* provider.customScore(subQueryScorer.docID(),
							subQueryScorer.score(), vScores);
		}

		@Override
		public int advance(int target) throws IOException {
			int doc = subQueryScorer.advance(target);
			if (doc != NO_MORE_DOCS) {
				for (int i = 0; i < valSrcScorers.length; i++) {
					valSrcScorers[i].advance(doc);
				}
			}
			return doc;
		}
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return new CustomWeight(searcher);
	}

	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public String name() {
		return "custom";
	}

}
