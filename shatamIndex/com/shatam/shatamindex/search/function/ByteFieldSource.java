package com.shatam.shatamindex.search.function;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.FieldCache;
import com.shatam.shatamindex.search.function.DocValues;

import java.io.IOException;
/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
public class ByteFieldSource extends FieldCacheSource {
	private FieldCache.ByteParser parser;

	public ByteFieldSource(String field) {
		this(field, null);
	}

	public ByteFieldSource(String field, FieldCache.ByteParser parser) {
		super(field);
		this.parser = parser;
	}

	@Override
	public String description() {
		return "byte(" + super.description() + ')';
	}

	@Override
	public DocValues getCachedFieldValues(FieldCache cache, String field,
			IndexReader reader) throws IOException {
		final byte[] arr = cache.getBytes(reader, field, parser);
		return new DocValues() {

			@Override
			public float floatVal(int doc) {
				return arr[doc];
			}

			@Override
			public int intVal(int doc) {
				return arr[doc];
			}

			@Override
			public String toString(int doc) {
				return description() + '=' + intVal(doc);
			}

			@Override
			Object getInnerArray() {
				return arr;
			}
		};
	}

	@Override
	public boolean cachedFieldSourceEquals(FieldCacheSource o) {
		if (o.getClass() != ByteFieldSource.class) {
			return false;
		}
		ByteFieldSource other = (ByteFieldSource) o;
		return this.parser == null ? other.parser == null : this.parser
				.getClass() == other.parser.getClass();
	}

	@Override
	public int cachedFieldSourceHashCode() {
		return parser == null ? Byte.class.hashCode() : parser.getClass()
				.hashCode();
	}

}
