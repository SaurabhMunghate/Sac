/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.payloads;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermPositions;
import com.shatam.shatamindex.search.ComplexExplanation;
import com.shatam.shatamindex.search.Explanation;
import com.shatam.shatamindex.search.Scorer;
import com.shatam.shatamindex.search.Searcher;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.search.Weight;
import com.shatam.shatamindex.search.spans.SpanScorer;
import com.shatam.shatamindex.search.spans.SpanTermQuery;
import com.shatam.shatamindex.search.spans.SpanWeight;
import com.shatam.shatamindex.search.spans.TermSpans;

import java.io.IOException;

public class PayloadTermQuery extends SpanTermQuery {
	protected PayloadFunction function;
	private boolean includeSpanScore;

	public PayloadTermQuery(Term term, PayloadFunction function) {
		this(term, function, true);
	}

	public PayloadTermQuery(Term term, PayloadFunction function,
			boolean includeSpanScore) {
		super(term);
		this.function = function;
		this.includeSpanScore = includeSpanScore;
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return new PayloadTermWeight(this, searcher);
	}

	protected class PayloadTermWeight extends SpanWeight {

		public PayloadTermWeight(PayloadTermQuery query, Searcher searcher)
				throws IOException {
			super(query, searcher);
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {
			return new PayloadTermSpanScorer(
					(TermSpans) query.getSpans(reader), this, similarity,
					reader.norms(query.getField()));
		}

		@Override
		public Explanation explain(IndexReader reader, int doc)
				throws IOException {
			if (includeSpanScore) {
				return super.explain(reader, doc);
			} else {

				PayloadTermSpanScorer scorer = (PayloadTermSpanScorer) scorer(
						reader, true, false);
				return scorer.explain(doc);
			}
		}

		protected class PayloadTermSpanScorer extends SpanScorer {

			protected byte[] payload = new byte[256];
			protected TermPositions positions;
			protected float payloadScore;
			protected int payloadsSeen;

			public PayloadTermSpanScorer(TermSpans spans, Weight weight,
					Similarity similarity, byte[] norms) throws IOException {
				super(spans, weight, similarity, norms);
				positions = spans.getPositions();
			}

			@Override
			protected boolean setFreqCurrentDoc() throws IOException {
				if (!more) {
					return false;
				}
				doc = spans.doc();
				freq = 0.0f;
				payloadScore = 0;
				payloadsSeen = 0;
				Similarity similarity1 = getSimilarity();
				while (more && doc == spans.doc()) {
					int matchLength = spans.end() - spans.start();

					freq += similarity1.sloppyFreq(matchLength);
					processPayload(similarity1);

					more = spans.next();

				}
				return more || (freq != 0);
			}

			protected void processPayload(Similarity similarity)
					throws IOException {
				if (positions.isPayloadAvailable()) {
					payload = positions.getPayload(payload, 0);
					payloadScore = function.currentScore(doc, term.field(),
							spans.start(), spans.end(), payloadsSeen,
							payloadScore, similarity.scorePayload(doc,
									term.field(), spans.start(), spans.end(),
									payload, 0, positions.getPayloadLength()));
					payloadsSeen++;

				} else {

				}
			}

			@Override
			public float score() throws IOException {

				return includeSpanScore ? getSpanScore() * getPayloadScore()
						: getPayloadScore();
			}

			protected float getSpanScore() throws IOException {
				return super.score();
			}

			protected float getPayloadScore() {
				return function.docScore(doc, term.field(), payloadsSeen,
						payloadScore);
			}

			@Override
			protected Explanation explain(final int doc) throws IOException {
				Explanation nonPayloadExpl = super.explain(doc);

				Explanation payloadBoost = new Explanation();

				float payloadScore = getPayloadScore();
				payloadBoost.setValue(payloadScore);

				payloadBoost.setDescription("scorePayload(...)");

				ComplexExplanation result = new ComplexExplanation();
				if (includeSpanScore) {
					result.addDetail(nonPayloadExpl);
					result.addDetail(payloadBoost);
					result.setValue(nonPayloadExpl.getValue() * payloadScore);
					result.setDescription("btq, product of:");
				} else {
					result.addDetail(payloadBoost);
					result.setValue(payloadScore);
					result.setDescription("btq(includeSpanScore=false), result of:");
				}
				result.setMatch(nonPayloadExpl.getValue() == 0 ? Boolean.FALSE
						: Boolean.TRUE);
				return result;
			}

		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((function == null) ? 0 : function.hashCode());
		result = prime * result + (includeSpanScore ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PayloadTermQuery other = (PayloadTermQuery) obj;
		if (function == null) {
			if (other.function != null)
				return false;
		} else if (!function.equals(other.function))
			return false;
		if (includeSpanScore != other.includeSpanScore)
			return false;
		return true;
	}

}
