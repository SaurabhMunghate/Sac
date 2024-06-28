/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

public class TopFieldDocs extends TopDocs {

	public SortField[] fields;

	public TopFieldDocs(int totalHits, ScoreDoc[] scoreDocs,
			SortField[] fields, float maxScore) {
		super(totalHits, scoreDocs, maxScore);
		this.fields = fields;
	}
}