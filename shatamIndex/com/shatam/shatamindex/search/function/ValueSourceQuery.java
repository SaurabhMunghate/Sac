/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.function;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermDocs;
import com.shatam.shatamindex.search.*;
import com.shatam.shatamindex.util.ToStringUtils;

import java.io.IOException;
import java.util.Set;

public class ValueSourceQuery extends Query {
	ValueSource valSrc;

	public ValueSourceQuery(ValueSource valSrc) {
		this.valSrc = valSrc;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		return this;
	}

	@Override
	public void extractTerms(Set<Term> terms) {

	}

	class ValueSourceWeight extends Weight {
		Similarity similarity;
		float queryNorm;
		float queryWeight;

		public ValueSourceWeight(Searcher searcher) {
			this.similarity = getSimilarity(searcher);
		}

		@Override
		public Query getQuery() {
			return ValueSourceQuery.this;
		}

		@Override
		public float getValue() {
			return queryWeight;
		}

		@Override
		public float sumOfSquaredWeights() throws IOException {
			queryWeight = getBoost();
			return queryWeight * queryWeight;
		}

		@Override
		public void normalize(float norm) {
			this.queryNorm = norm;
			queryWeight *= this.queryNorm;
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {
			return new ValueSourceScorer(similarity, reader, this);
		}

		@Override
		public Explanation explain(IndexReader reader, int doc)
				throws IOException {
			DocValues vals = valSrc.getValues(reader);
			float sc = queryWeight * vals.floatVal(doc);

			Explanation result = new ComplexExplanation(true, sc,
					ValueSourceQuery.this.toString() + ", product of:");

			result.addDetail(vals.explain(doc));
			result.addDetail(new Explanation(getBoost(), "boost"));
			result.addDetail(new Explanation(queryNorm, "queryNorm"));
			return result;
		}
	}

	private class ValueSourceScorer extends Scorer {
		private final float qWeight;
		private final DocValues vals;
		private final TermDocs termDocs;
		private int doc = -1;

		private ValueSourceScorer(Similarity similarity, IndexReader reader,
				ValueSourceWeight w) throws IOException {
			super(similarity, w);
			qWeight = w.getValue();

			vals = valSrc.getValues(reader);
			termDocs = reader.termDocs(null);
		}

		@Override
		public int nextDoc() throws IOException {
			return doc = termDocs.next() ? termDocs.doc() : NO_MORE_DOCS;
		}

		@Override
		public int docID() {
			return doc;
		}

		@Override
		public int advance(int target) throws IOException {
			return doc = termDocs.skipTo(target) ? termDocs.doc()
					: NO_MORE_DOCS;
		}

		@Override
		public float score() throws IOException {
			return qWeight * vals.floatVal(termDocs.doc());
		}
	}

	@Override
	public Weight createWeight(Searcher searcher) {
		return new ValueSourceQuery.ValueSourceWeight(searcher);
	}

	@Override
	public String toString(String field) {
		return valSrc.toString() + ToStringUtils.boost(getBoost());
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
		ValueSourceQuery other = (ValueSourceQuery) o;
		return this.getBoost() == other.getBoost()
				&& this.valSrc.equals(other.valSrc);
	}

	@Override
	public int hashCode() {
		return (getClass().hashCode() + valSrc.hashCode())
				^ Float.floatToIntBits(getBoost());
	}

}
