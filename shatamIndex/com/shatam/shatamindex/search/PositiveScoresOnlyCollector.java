/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;

public class PositiveScoresOnlyCollector extends Collector {

	final private Collector c;
	private Scorer scorer;

	public PositiveScoresOnlyCollector(Collector c) {
		this.c = c;
	}

	@Override
	public void collect(int doc) throws IOException {
		if (scorer.score() > 0) {
			c.collect(doc);
		}
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase)
			throws IOException {
		c.setNextReader(reader, docBase);
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {

		this.scorer = new ScoreCachingWrappingScorer(scorer);
		c.setScorer(this.scorer);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return c.acceptsDocsOutOfOrder();
	}

}
