/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.util.PriorityQueue;

public abstract class TopDocsCollector<T extends ScoreDoc> extends Collector {

	protected static final TopDocs EMPTY_TOPDOCS = new TopDocs(0,
			new ScoreDoc[0], Float.NaN);

	protected PriorityQueue<T> pq;

	protected int totalHits;

	protected TopDocsCollector(PriorityQueue<T> pq) {
		this.pq = pq;
	}

	protected void populateResults(ScoreDoc[] results, int howMany) {
		for (int i = howMany - 1; i >= 0; i--) {
			results[i] = pq.pop();
		}
	}

	protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
		return results == null ? EMPTY_TOPDOCS
				: new TopDocs(totalHits, results);
	}

	public int getTotalHits() {
		return totalHits;
	}

	protected int topDocsSize() {

		return totalHits < pq.size() ? totalHits : pq.size();
	}

	public TopDocs topDocs() {

		return topDocs(0, topDocsSize());
	}

	public TopDocs topDocs(int start) {

		return topDocs(start, topDocsSize());
	}

	public TopDocs topDocs(int start, int howMany) {

		int size = topDocsSize();

		if (start < 0 || start >= size || howMany <= 0) {
			return newTopDocs(null, start);
		}

		howMany = Math.min(size - start, howMany);
		ScoreDoc[] results = new ScoreDoc[howMany];

		for (int i = pq.size() - start - howMany; i > 0; i--) {
			pq.pop();
		}

		populateResults(results, howMany);

		return newTopDocs(results, start);
	}

}
