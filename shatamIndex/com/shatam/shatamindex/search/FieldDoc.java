/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

public class FieldDoc extends ScoreDoc {

	public Object[] fields;

	public FieldDoc(int doc, float score) {
		super(doc, score);
	}

	public FieldDoc(int doc, float score, Object[] fields) {
		super(doc, score);
		this.fields = fields;
	}

	public FieldDoc(int doc, float score, Object[] fields, int shardIndex) {
		super(doc, score, shardIndex);
		this.fields = fields;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("[");
		for (int i = 0; i < fields.length; i++) {
			sb.append(fields[i]).append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append("]");
		return sb.toString();
	}
}
