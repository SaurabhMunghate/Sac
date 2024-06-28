/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.Term;

@Deprecated
public abstract class Searcher implements Searchable {

	public TopFieldDocs search(Query query, Filter filter, int n, Sort sort)
			throws IOException {
		return search(createNormalizedWeight(query), filter, n, sort);
	}

	public TopFieldDocs search(Query query, int n, Sort sort)
			throws IOException {
		return search(createNormalizedWeight(query), null, n, sort);
	}

	public void search(Query query, Collector results) throws IOException {
		search(createNormalizedWeight(query), null, results);
	}

	public void search(Query query, Filter filter, Collector results)
			throws IOException {
		search(createNormalizedWeight(query), filter, results);
	}

	public TopDocs search(Query query, Filter filter, int n) throws IOException {
		return search(createNormalizedWeight(query), filter, n);
	}

	public TopDocs search(Query query, int n) throws IOException {
		return search(query, null, n);
	}

	public Explanation explain(Query query, int doc) throws IOException {
		return explain(createNormalizedWeight(query), doc);
	}

	private Similarity similarity = Similarity.getDefault();

	public void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
	}

	public Similarity getSimilarity() {
		return this.similarity;
	}

	public Weight createNormalizedWeight(Query query) throws IOException {
		query = rewrite(query);
		Weight weight = query.createWeight(this);
		float sum = weight.sumOfSquaredWeights();

		float norm = query.getSimilarity(this).queryNorm(sum);
		if (Float.isInfinite(norm) || Float.isNaN(norm))
			norm = 1.0f;
		weight.normalize(norm);
		return weight;
	}

	@Deprecated
	protected final Weight createWeight(Query query) throws IOException {
		return createNormalizedWeight(query);
	}

	public int[] docFreqs(Term[] terms) throws IOException {
		int[] result = new int[terms.length];
		for (int i = 0; i < terms.length; i++) {
			result[i] = docFreq(terms[i]);
		}
		return result;
	}

	abstract public void search(Weight weight, Filter filter, Collector results)
			throws IOException;

	abstract public void close() throws IOException;

	abstract public int docFreq(Term term) throws IOException;

	abstract public int maxDoc() throws IOException;

	abstract public TopDocs search(Weight weight, Filter filter, int n)
			throws IOException;

	abstract public Document doc(int i) throws CorruptIndexException,
			IOException;

	abstract public Document doc(int docid, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException;

	abstract public Query rewrite(Query query) throws IOException;

	abstract public Explanation explain(Weight weight, int doc)
			throws IOException;

	abstract public TopFieldDocs search(Weight weight, Filter filter, int n,
			Sort sort) throws IOException;
	/* End patch for GCJ bug #15411. */
}
