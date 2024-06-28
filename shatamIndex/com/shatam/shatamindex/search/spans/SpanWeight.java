/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.*;
import com.shatam.shatamindex.search.Explanation.IDFExplanation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SpanWeight extends Weight {
	protected Similarity similarity;
	protected float value;
	protected float idf;
	protected float queryNorm;
	protected float queryWeight;

	protected Set<Term> terms;
	protected SpanQuery query;
	private IDFExplanation idfExp;

	public SpanWeight(SpanQuery query, Searcher searcher) throws IOException {
		this.similarity = query.getSimilarity(searcher);
		this.query = query;

		terms = new HashSet<Term>();
		query.extractTerms(terms);

		idfExp = similarity.idfExplain(terms, searcher);
		idf = idfExp.getIdf();
	}

	@Override
	public Query getQuery() {
		return query;
	}

	@Override
	public float getValue() {
		return value;
	}

	@Override
	public float sumOfSquaredWeights() throws IOException {
		queryWeight = idf * query.getBoost();
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
		return new SpanScorer(query.getSpans(reader), this, similarity,
				reader.norms(query.getField()));
	}

	@Override
	public Explanation explain(IndexReader reader, int doc) throws IOException {

		ComplexExplanation result = new ComplexExplanation();
		result.setDescription("weight(" + getQuery() + " in " + doc
				+ "), product of:");
		String field = ((SpanQuery) getQuery()).getField();

		Explanation idfExpl = new Explanation(idf, "idf(" + field + ": "
				+ idfExp.explain() + ")");

		Explanation queryExpl = new Explanation();
		queryExpl
				.setDescription("queryWeight(" + getQuery() + "), product of:");

		Explanation boostExpl = new Explanation(getQuery().getBoost(), "boost");
		if (getQuery().getBoost() != 1.0f)
			queryExpl.addDetail(boostExpl);
		queryExpl.addDetail(idfExpl);

		Explanation queryNormExpl = new Explanation(queryNorm, "queryNorm");
		queryExpl.addDetail(queryNormExpl);

		queryExpl.setValue(boostExpl.getValue() * idfExpl.getValue()
				* queryNormExpl.getValue());

		result.addDetail(queryExpl);

		ComplexExplanation fieldExpl = new ComplexExplanation();
		fieldExpl.setDescription("fieldWeight(" + field + ":"
				+ query.toString(field) + " in " + doc + "), product of:");

		Explanation tfExpl = ((SpanScorer) scorer(reader, true, false))
				.explain(doc);
		fieldExpl.addDetail(tfExpl);
		fieldExpl.addDetail(idfExpl);

		Explanation fieldNormExpl = new Explanation();
		byte[] fieldNorms = reader.norms(field);
		float fieldNorm = fieldNorms != null ? similarity
				.decodeNormValue(fieldNorms[doc]) : 1.0f;
		fieldNormExpl.setValue(fieldNorm);
		fieldNormExpl.setDescription("fieldNorm(field=" + field + ", doc="
				+ doc + ")");
		fieldExpl.addDetail(fieldNormExpl);

		fieldExpl.setMatch(Boolean.valueOf(tfExpl.isMatch()));
		fieldExpl.setValue(tfExpl.getValue() * idfExpl.getValue()
				* fieldNormExpl.getValue());

		result.addDetail(fieldExpl);
		result.setMatch(fieldExpl.getMatch());

		result.setValue(queryExpl.getValue() * fieldExpl.getValue());

		if (queryExpl.getValue() == 1.0f)
			return fieldExpl;

		return result;
	}
}
