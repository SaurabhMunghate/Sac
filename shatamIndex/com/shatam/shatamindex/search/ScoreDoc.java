/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

public class ScoreDoc implements java.io.Serializable {

	public float score;

	public int doc;

	public int shardIndex;

	public ScoreDoc(int doc, float score) {
		this(doc, score, -1);
	}

	public ScoreDoc(int doc, float score, int shardIndex) {
		this.doc = doc;
		this.score = score;
		this.shardIndex = shardIndex;
	}

	@Override
	public String toString() {
		return "doc=" + doc + " score=" + score;
	}
}
