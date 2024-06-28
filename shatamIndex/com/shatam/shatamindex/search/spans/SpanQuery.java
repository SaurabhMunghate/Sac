/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.Searcher;
import com.shatam.shatamindex.search.Weight;

public abstract class SpanQuery extends Query {

	public abstract Spans getSpans(IndexReader reader) throws IOException;

	public abstract String getField();

	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return new SpanWeight(this, searcher);
	}

}
