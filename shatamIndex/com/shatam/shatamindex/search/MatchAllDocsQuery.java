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
import com.shatam.shatamindex.index.TermDocs;
import com.shatam.shatamindex.util.ToStringUtils;

import java.util.Set;
import java.io.IOException;

public class MatchAllDocsQuery extends Query {

	public MatchAllDocsQuery() {
		this(null);
	}

	private final String normsField;

	public MatchAllDocsQuery(String normsField) {
		this.normsField = normsField;
	}

	private class MatchAllScorer extends Scorer {
		final TermDocs termDocs;
		final float score;
		final byte[] norms;
		private int doc = -1;

		MatchAllScorer(IndexReader reader, Similarity similarity, Weight w,
				byte[] norms) throws IOException {
			super(similarity, w);
			this.termDocs = reader.termDocs(null);
			score = w.getValue();
			this.norms = norms;
		}

		@Override
		public int docID() {
			return doc;
		}

		@Override
		public int nextDoc() throws IOException {
			return doc = termDocs.next() ? termDocs.doc() : NO_MORE_DOCS;
		}

		@Override
		public float score() {
			return norms == null ? score : score
					* getSimilarity().decodeNormValue(norms[docID()]);
		}

		@Override
		public int advance(int target) throws IOException {
			return doc = termDocs.skipTo(target) ? termDocs.doc()
					: NO_MORE_DOCS;
		}
	}

	private class MatchAllDocsWeight extends Weight {
		private Similarity similarity;
		private float queryWeight;
		private float queryNorm;

		public MatchAllDocsWeight(Searcher searcher) {
			this.similarity = searcher.getSimilarity();
		}

		@Override
		public String toString() {
			return "weight(" + MatchAllDocsQuery.this + ")";
		}

		@Override
		public Query getQuery() {
			return MatchAllDocsQuery.this;
		}

		@Override
		public float getValue() {
			return queryWeight;
		}

		@Override
		public float sumOfSquaredWeights() {
			queryWeight = getBoost();
			return queryWeight * queryWeight;
		}

		@Override
		public void normalize(float queryNorm) {
			this.queryNorm = queryNorm;
			queryWeight *= this.queryNorm;
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {
			return new MatchAllScorer(reader, similarity, this,
					normsField != null ? reader.norms(normsField) : null);
		}

		@Override
		public Explanation explain(IndexReader reader, int doc) {

			Explanation queryExpl = new ComplexExplanation(true, getValue(),
					"MatchAllDocsQuery, product of:");
			if (getBoost() != 1.0f) {
				queryExpl.addDetail(new Explanation(getBoost(), "boost"));
			}
			queryExpl.addDetail(new Explanation(queryNorm, "queryNorm"));

			return queryExpl;
		}
	}

	@Override
	public Weight createWeight(Searcher searcher) {
		return new MatchAllDocsWeight(searcher);
	}

	@Override
	public void extractTerms(Set<Term> terms) {
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("*:*");
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MatchAllDocsQuery))
			return false;
		MatchAllDocsQuery other = (MatchAllDocsQuery) o;
		return this.getBoost() == other.getBoost();
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ 0x1AA71190;
	}
}
