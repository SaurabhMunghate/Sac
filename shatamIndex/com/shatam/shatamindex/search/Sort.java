/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.Serializable;
import java.util.Arrays;

public class Sort implements Serializable {

	public static final Sort RELEVANCE = new Sort();

	public static final Sort INDEXORDER = new Sort(SortField.FIELD_DOC);

	SortField[] fields;

	public Sort() {
		this(SortField.FIELD_SCORE);
	}

	public Sort(SortField field) {
		setSort(field);
	}

	public Sort(SortField... fields) {
		setSort(fields);
	}

	public void setSort(SortField field) {
		this.fields = new SortField[] { field };
	}

	public void setSort(SortField... fields) {
		this.fields = fields;
	}

	public SortField[] getSort() {
		return fields;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		for (int i = 0; i < fields.length; i++) {
			buffer.append(fields[i].toString());
			if ((i + 1) < fields.length)
				buffer.append(',');
		}

		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Sort))
			return false;
		final Sort other = (Sort) o;
		return Arrays.equals(this.fields, other.fields);
	}

	@Override
	public int hashCode() {
		return 0x45aaf665 + Arrays.hashCode(fields);
	}
}
