/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.IndexReader;

public class TotalHitCountCollector extends Collector {
	private int totalHits;

	public int getTotalHits() {
		return totalHits;
	}

	@Override
	public void setScorer(Scorer scorer) {
	}

	@Override
	public void collect(int doc) {
		totalHits++;
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) {
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
}
