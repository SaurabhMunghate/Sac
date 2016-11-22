/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.*;

class SegmentTermVector implements TermFreqVector {
	private String field;
	private String terms[];
	private int termFreqs[];

	SegmentTermVector(String field, String terms[], int termFreqs[]) {
		this.field = field;
		this.terms = terms;
		this.termFreqs = termFreqs;
	}

	public String getField() {
		return field;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		sb.append(field).append(": ");
		if (terms != null) {
			for (int i = 0; i < terms.length; i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(terms[i]).append('/').append(termFreqs[i]);
			}
		}
		sb.append('}');

		return sb.toString();
	}

	public int size() {
		return terms == null ? 0 : terms.length;
	}

	public String[] getTerms() {
		return terms;
	}

	public int[] getTermFrequencies() {
		return termFreqs;
	}

	public int indexOf(String termText) {
		if (terms == null)
			return -1;
		int res = Arrays.binarySearch(terms, termText);
		return res >= 0 ? res : -1;
	}

	public int[] indexesOf(String[] termNumbers, int start, int len) {

		int res[] = new int[len];

		for (int i = 0; i < len; i++) {
			res[i] = indexOf(termNumbers[start + i]);
		}
		return res;
	}
}
