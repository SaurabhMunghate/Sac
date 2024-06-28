/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import java.io.IOException;

import com.shatam.shatamindex.store.DataInput;
import com.shatam.shatamindex.store.DataOutput;

public abstract class Outputs<T> {

	public abstract T common(T output1, T output2);

	public abstract T subtract(T output, T inc);

	public abstract T add(T prefix, T output);

	public abstract void write(T output, DataOutput out) throws IOException;

	public abstract T read(DataInput in) throws IOException;

	public abstract T getNoOutput();

	public abstract String outputToString(T output);

	public T merge(T first, T second) {
		throw new UnsupportedOperationException();
	}
}
