/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.function;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.FieldCache;

import java.io.IOException;

public class ReverseOrdFieldSource extends ValueSource {
	public String field;

	public ReverseOrdFieldSource(String field) {
		this.field = field;
	}

	@Override
	public String description() {
		return "rord(" + field + ')';
	}

	@Override
	public DocValues getValues(IndexReader reader) throws IOException {
		final FieldCache.StringIndex sindex = FieldCache.DEFAULT
				.getStringIndex(reader, field);

		final int arr[] = sindex.order;
		final int end = sindex.lookup.length;

		return new DocValues() {

			@Override
			public float floatVal(int doc) {
				return (end - arr[doc]);
			}

			@Override
			public int intVal(int doc) {
				return end - arr[doc];
			}

			@Override
			public String strVal(int doc) {

				return Integer.toString(intVal(doc));
			}

			@Override
			public String toString(int doc) {
				return description() + '=' + strVal(doc);
			}

			@Override
			Object getInnerArray() {
				return arr;
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (o.getClass() != ReverseOrdFieldSource.class)
			return false;
		ReverseOrdFieldSource other = (ReverseOrdFieldSource) o;
		return this.field.equals(other.field);
	}

	private static final int hcode = ReverseOrdFieldSource.class.hashCode();

	@Override
	public int hashCode() {
		return hcode + field.hashCode();
	}
}
