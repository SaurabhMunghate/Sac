/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.function;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.function.DocValues;

import java.io.IOException;
import java.io.Serializable;

public abstract class ValueSource implements Serializable {

	public abstract DocValues getValues(IndexReader reader) throws IOException;

	public abstract String description();

	@Override
	public String toString() {
		return description();
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

}
