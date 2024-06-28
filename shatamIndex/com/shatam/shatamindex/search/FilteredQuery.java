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
import com.shatam.shatamindex.util.ToStringUtils;

import java.io.IOException;
import java.util.Set;

public class FilteredQuery extends Query {

	Query query;
	Filter filter;

	public FilteredQuery(Query query, Filter filter) {
		this.query = query;
		this.filter = filter;
	}

	@Override
	public Weight createWeight(final Searcher searcher) throws IOException {
		final Weight weight = query.createWeight(searcher);
		final Similarity similarity = query.getSimilarity(searcher);
		return new Weight() {
			private float value;

			@Override
			public float getValue() {
				return value;
			}

			@Override
			public boolean scoresDocsOutOfOrder() {
				return false;
			}

			public float sumOfSquaredWeights() throws IOException {
				return weight.sumOfSquaredWeights() * getBoost() * getBoost();
			}

			@Override
			public void normalize(float v) {
				weight.normalize(v * getBoost());
				value = weight.getValue();
			}

			@Override
			public Explanation explain(IndexReader ir, int i)
					throws IOException {
				Explanation inner = weight.explain(ir, i);
				Filter f = FilteredQuery.this.filter;
				DocIdSet docIdSet = f.getDocIdSet(ir);
				DocIdSetIterator docIdSetIterator = docIdSet == null ? DocIdSet.EMPTY_DOCIDSET
						.iterator() : docIdSet.iterator();
				if (docIdSetIterator == null) {
					docIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
				}
				if (docIdSetIterator.advance(i) == i) {
					return inner;
				} else {
					Explanation result = new Explanation(0.0f,
							"failure to match filter: " + f.toString());
					result.addDetail(inner);
					return result;
				}
			}

			@Override
			public Query getQuery() {
				return FilteredQuery.this;
			}

			@Override
			public Scorer scorer(IndexReader indexReader,
					boolean scoreDocsInOrder, boolean topScorer)
					throws IOException {

				return FilteredQuery.getFilteredScorer(indexReader, similarity,
						weight, this, filter);
			}
		};
	}

	static Scorer getFilteredScorer(final IndexReader indexReader,
			final Similarity similarity, final Weight weight,
			final Weight wrapperWeight, final Filter filter) throws IOException {
		assert filter != null;

		final DocIdSet filterDocIdSet = filter.getDocIdSet(indexReader);
		if (filterDocIdSet == null) {

			return null;
		}

		final DocIdSetIterator filterIter = filterDocIdSet.iterator();
		if (filterIter == null) {

			return null;
		}

		final Scorer scorer = weight.scorer(indexReader, true, false);
		return (scorer == null) ? null : new Scorer(similarity, wrapperWeight) {
			private int scorerDoc = -1, filterDoc = -1;

			@Override
			public void score(Collector collector) throws IOException {
				int filterDoc = filterIter.nextDoc();
				int scorerDoc = scorer.advance(filterDoc);

				collector.setScorer(scorer);
				for (;;) {
					if (scorerDoc == filterDoc) {

						if (scorerDoc == DocIdSetIterator.NO_MORE_DOCS) {
							break;
						}
						collector.collect(scorerDoc);
						filterDoc = filterIter.nextDoc();
						scorerDoc = scorer.advance(filterDoc);
					} else if (scorerDoc > filterDoc) {
						filterDoc = filterIter.advance(scorerDoc);
					} else {
						scorerDoc = scorer.advance(filterDoc);
					}
				}
			}

			private int advanceToNextCommonDoc() throws IOException {
				for (;;) {
					if (scorerDoc < filterDoc) {
						scorerDoc = scorer.advance(filterDoc);
					} else if (scorerDoc == filterDoc) {
						return scorerDoc;
					} else {
						filterDoc = filterIter.advance(scorerDoc);
					}
				}
			}

			@Override
			public int nextDoc() throws IOException {
				filterDoc = filterIter.nextDoc();
				return advanceToNextCommonDoc();
			}

			@Override
			public int advance(int target) throws IOException {
				if (target > filterDoc) {
					filterDoc = filterIter.advance(target);
				}
				return advanceToNextCommonDoc();
			}

			@Override
			public int docID() {
				return scorerDoc;
			}

			@Override
			public float score() throws IOException {
				return scorer.score();
			}
		};
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		Query rewritten = query.rewrite(reader);
		if (rewritten != query) {
			FilteredQuery clone = (FilteredQuery) this.clone();
			clone.query = rewritten;
			return clone;
		} else {
			return this;
		}
	}

	public Query getQuery() {
		return query;
	}

	public Filter getFilter() {
		return filter;
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		getQuery().extractTerms(terms);
	}

	@Override
	public String toString(String s) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("filtered(");
		buffer.append(query.toString(s));
		buffer.append(")->");
		buffer.append(filter);
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FilteredQuery) {
			FilteredQuery fq = (FilteredQuery) o;
			return (query.equals(fq.query) && filter.equals(fq.filter) && getBoost() == fq
					.getBoost());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return query.hashCode() ^ filter.hashCode()
				+ Float.floatToRawIntBits(getBoost());
	}
}
