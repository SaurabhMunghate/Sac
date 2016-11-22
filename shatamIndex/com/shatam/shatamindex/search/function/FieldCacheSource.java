/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.function;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.FieldCache;

public abstract class FieldCacheSource extends ValueSource {
	private String field;

	public FieldCacheSource(String field) {
		this.field = field;
	}

	@Override
	public final DocValues getValues(IndexReader reader) throws IOException {
		return getCachedFieldValues(FieldCache.DEFAULT, field, reader);
	}

	@Override
	public String description() {
		return field;
	}

	public abstract DocValues getCachedFieldValues(FieldCache cache,
			String field, IndexReader reader) throws IOException;

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof FieldCacheSource)) {
			return false;
		}
		FieldCacheSource other = (FieldCacheSource) o;
		return this.field.equals(other.field) && cachedFieldSourceEquals(other);
	}

	@Override
	public final int hashCode() {
		return field.hashCode() + cachedFieldSourceHashCode();
	}

	public abstract boolean cachedFieldSourceEquals(FieldCacheSource other);

	public abstract int cachedFieldSourceHashCode();
}
