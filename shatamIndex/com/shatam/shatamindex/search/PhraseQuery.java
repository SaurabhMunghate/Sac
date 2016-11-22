/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.Set;
import java.util.ArrayList;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermPositions;
import com.shatam.shatamindex.search.Explanation.IDFExplanation;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.ToStringUtils;

public class PhraseQuery extends Query {
	private String field;
	private ArrayList<Term> terms = new ArrayList<Term>(4);
	private ArrayList<Integer> positions = new ArrayList<Integer>(4);
	private int maxPosition = 0;
	private int slop = 0;

	public PhraseQuery() {
	}

	public void setSlop(int s) {
		slop = s;
	}

	public int getSlop() {
		return slop;
	}

	public void add(Term term) {
		int position = 0;
		if (positions.size() > 0)
			position = positions.get(positions.size() - 1).intValue() + 1;

		add(term, position);
	}

	public void add(Term term, int position) {
		if (terms.size() == 0)
			field = term.field();
		else if (term.field() != field)
			throw new IllegalArgumentException(
					"All phrase terms must be in the same field: " + term);

		terms.add(term);
		positions.add(Integer.valueOf(position));
		if (position > maxPosition)
			maxPosition = position;
	}

	public Term[] getTerms() {
		return terms.toArray(new Term[0]);
	}

	public int[] getPositions() {
		int[] result = new int[positions.size()];
		for (int i = 0; i < positions.size(); i++)
			result[i] = positions.get(i).intValue();
		return result;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if (terms.size() == 1) {
			TermQuery tq = new TermQuery(terms.get(0));
			tq.setBoost(getBoost());
			return tq;
		} else
			return super.rewrite(reader);
	}

	static class PostingsAndFreq implements Comparable<PostingsAndFreq> {
		final TermPositions postings;
		final int docFreq;
		final int position;
		final Term term;

		public PostingsAndFreq(TermPositions postings, int docFreq,
				int position, Term term) {
			this.postings = postings;
			this.docFreq = docFreq;
			this.position = position;
			this.term = term;
		}

		public int compareTo(PostingsAndFreq other) {
			if (docFreq == other.docFreq) {
				if (position == other.position) {
					return term.compareTo(other.term);
				}
				return position - other.position;
			}
			return docFreq - other.docFreq;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + docFreq;
			result = prime * result + position;
			result = prime * result + ((term == null) ? 0 : term.hashCode());
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
			PostingsAndFreq other = (PostingsAndFreq) obj;
			if (docFreq != other.docFreq)
				return false;
			if (position != other.position)
				return false;
			if (term == null) {
				if (other.term != null)
					return false;
			} else if (!term.equals(other.term))
				return false;
			return true;
		}
	}

	private class PhraseWeight extends Weight {
		private final Similarity similarity;
		private float value;
		private float idf;
		private float queryNorm;
		private float queryWeight;
		private IDFExplanation idfExp;

		public PhraseWeight(Searcher searcher) throws IOException {
			this.similarity = getSimilarity(searcher);

			idfExp = similarity.idfExplain(terms, searcher);
			idf = idfExp.getIdf();
		}

		@Override
		public String toString() {
			return "weight(" + PhraseQuery.this + ")";
		}

		@Override
		public Query getQuery() {
			return PhraseQuery.this;
		}

		@Override
		public float getValue() {
			return value;
		}

		@Override
		public float sumOfSquaredWeights() {
			queryWeight = idf * getBoost();
			return queryWeight * queryWeight;
		}

		@Override
		public void normalize(float queryNorm) {
			this.queryNorm = queryNorm;
			queryWeight *= queryNorm;
			value = queryWeight * idf;
		}

		@Override
		public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
				boolean topScorer) throws IOException {
			if (terms.size() == 0)
				return null;

			PostingsAndFreq[] postingsFreqs = new PostingsAndFreq[terms.size()];
			for (int i = 0; i < terms.size(); i++) {
				final Term t = terms.get(i);
				TermPositions p = reader.termPositions(t);
				if (p == null)
					return null;
				postingsFreqs[i] = new PostingsAndFreq(p, reader.docFreq(t),
						positions.get(i).intValue(), t);
			}

			if (slop == 0) {
				ArrayUtil.mergeSort(postingsFreqs);
			}

			if (slop == 0) {
				ExactPhraseScorer s = new ExactPhraseScorer(this,
						postingsFreqs, similarity, reader.norms(field));
				if (s.noDocs) {
					return null;
				} else {
					return s;
				}
			} else {
				return new SloppyPhraseScorer(this, postingsFreqs, similarity,
						slop, reader.norms(field));
			}
		}

		@Override
		public Explanation explain(IndexReader reader, int doc)
				throws IOException {

			ComplexExplanation result = new ComplexExplanation();
			result.setDescription("weight(" + getQuery() + " in " + doc
					+ "), product of:");

			StringBuilder docFreqs = new StringBuilder();
			StringBuilder query = new StringBuilder();
			query.append('\"');
			docFreqs.append(idfExp.explain());
			for (int i = 0; i < terms.size(); i++) {
				if (i != 0) {
					query.append(" ");
				}

				Term term = terms.get(i);

				query.append(term.text());
			}
			query.append('\"');

			Explanation idfExpl = new Explanation(idf, "idf(" + field + ":"
					+ docFreqs + ")");

			Explanation queryExpl = new Explanation();
			queryExpl.setDescription("queryWeight(" + getQuery()
					+ "), product of:");

			Explanation boostExpl = new Explanation(getBoost(), "boost");
			if (getBoost() != 1.0f)
				queryExpl.addDetail(boostExpl);
			queryExpl.addDetail(idfExpl);

			Explanation queryNormExpl = new Explanation(queryNorm, "queryNorm");
			queryExpl.addDetail(queryNormExpl);

			queryExpl.setValue(boostExpl.getValue() * idfExpl.getValue()
					* queryNormExpl.getValue());

			result.addDetail(queryExpl);

			Explanation fieldExpl = new Explanation();
			fieldExpl.setDescription("fieldWeight(" + field + ":" + query
					+ " in " + doc + "), product of:");

			Scorer scorer = scorer(reader, true, false);
			if (scorer == null) {
				return new Explanation(0.0f, "no matching docs");
			}
			Explanation tfExplanation = new Explanation();
			int d = scorer.advance(doc);
			float phraseFreq;
			if (d == doc) {
				phraseFreq = scorer.freq();
			} else {
				phraseFreq = 0.0f;
			}

			tfExplanation.setValue(similarity.tf(phraseFreq));
			tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");

			fieldExpl.addDetail(tfExplanation);
			fieldExpl.addDetail(idfExpl);

			Explanation fieldNormExpl = new Explanation();
			byte[] fieldNorms = reader.norms(field);
			float fieldNorm = fieldNorms != null ? similarity
					.decodeNormValue(fieldNorms[doc]) : 1.0f;
			fieldNormExpl.setValue(fieldNorm);
			fieldNormExpl.setDescription("fieldNorm(field=" + field + ", doc="
					+ doc + ")");
			fieldExpl.addDetail(fieldNormExpl);

			fieldExpl.setValue(tfExplanation.getValue() * idfExpl.getValue()
					* fieldNormExpl.getValue());

			result.addDetail(fieldExpl);

			result.setValue(queryExpl.getValue() * fieldExpl.getValue());
			result.setMatch(tfExplanation.isMatch());
			return result;
		}
	}

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		if (terms.size() == 1) {
			Term term = terms.get(0);
			Query termQuery = new TermQuery(term);
			termQuery.setBoost(getBoost());
			return termQuery.createWeight(searcher);
		}
		return new PhraseWeight(searcher);
	}

	@Override
	public void extractTerms(Set<Term> queryTerms) {
		queryTerms.addAll(terms);
	}

	@Override
	public String toString(String f) {
		StringBuilder buffer = new StringBuilder();
		if (field != null && !field.equals(f)) {
			buffer.append(field);
			buffer.append(":");
		}

		buffer.append("\"");
		String[] pieces = new String[maxPosition + 1];
		for (int i = 0; i < terms.size(); i++) {
			int pos = positions.get(i).intValue();
			String s = pieces[pos];
			if (s == null) {
				s = (terms.get(i)).text();
			} else {
				s = s + "|" + (terms.get(i)).text();
			}
			pieces[pos] = s;
		}
		for (int i = 0; i < pieces.length; i++) {
			if (i > 0) {
				buffer.append(' ');
			}
			String s = pieces[i];
			if (s == null) {
				buffer.append('?');
			} else {
				buffer.append(s);
			}
		}
		buffer.append("\"");

		if (slop != 0) {
			buffer.append("~");
			buffer.append(slop);
		}

		buffer.append(ToStringUtils.boost(getBoost()));

		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PhraseQuery))
			return false;
		PhraseQuery other = (PhraseQuery) o;
		return (this.getBoost() == other.getBoost())
				&& (this.slop == other.slop) && this.terms.equals(other.terms)
				&& this.positions.equals(other.positions);
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ slop ^ terms.hashCode()
				^ positions.hashCode();
	}

}
