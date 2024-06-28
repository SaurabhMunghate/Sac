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

public abstract class Collector {

	public abstract void setScorer(Scorer scorer) throws IOException;

	public abstract void collect(int doc) throws IOException;

	public abstract void setNextReader(IndexReader reader, int docBase)
			throws IOException;

	public abstract boolean acceptsDocsOutOfOrder();

}
