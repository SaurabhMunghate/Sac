/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.io.Serializable;

import com.shatam.shatamindex.index.IndexReader;

public abstract class Weight implements Serializable {

	public abstract Explanation explain(IndexReader reader, int doc)
			throws IOException;

	public abstract Query getQuery();

	public abstract float getValue();

	public abstract void normalize(float norm);

	public abstract Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
			boolean topScorer) throws IOException;

	public abstract float sumOfSquaredWeights() throws IOException;

	public boolean scoresDocsOutOfOrder() {
		return false;
	}

}
