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
import com.shatam.shatamindex.search.function.DocValues;

import java.io.IOException;

public class FloatFieldSource extends FieldCacheSource {
	private FieldCache.FloatParser parser;

	public FloatFieldSource(String field) {
		this(field, null);
	}

	public FloatFieldSource(String field, FieldCache.FloatParser parser) {
		super(field);
		this.parser = parser;
	}

	@Override
	public String description() {
		return "float(" + super.description() + ')';
	}

	@Override
	public DocValues getCachedFieldValues(FieldCache cache, String field,
			IndexReader reader) throws IOException {
		final float[] arr = cache.getFloats(reader, field, parser);
		return new DocValues() {

			@Override
			public float floatVal(int doc) {
				return arr[doc];
			}

			@Override
			public String toString(int doc) {
				return description() + '=' + arr[doc];
			}

			@Override
			Object getInnerArray() {
				return arr;
			}
		};
	}

	@Override
	public boolean cachedFieldSourceEquals(FieldCacheSource o) {
		if (o.getClass() != FloatFieldSource.class) {
			return false;
		}
		FloatFieldSource other = (FloatFieldSource) o;
		return this.parser == null ? other.parser == null : this.parser
				.getClass() == other.parser.getClass();
	}

	@Override
	public int cachedFieldSourceHashCode() {
		return parser == null ? Float.class.hashCode() : parser.getClass()
				.hashCode();
	}
}