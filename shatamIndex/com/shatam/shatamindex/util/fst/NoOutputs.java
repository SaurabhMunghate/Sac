/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import com.shatam.shatamindex.store.DataInput;
import com.shatam.shatamindex.store.DataOutput;

public final class NoOutputs extends Outputs<Object> {

	static final Object NO_OUTPUT = new Object() {

		@Override
		public int hashCode() {
			return 42;
		}

		@Override
		public boolean equals(Object other) {
			return other == this;
		}
	};

	private static final NoOutputs singleton = new NoOutputs();

	private NoOutputs() {
	}

	public static NoOutputs getSingleton() {
		return singleton;
	}

	@Override
	public Object common(Object output1, Object output2) {
		assert output1 == NO_OUTPUT;
		assert output2 == NO_OUTPUT;
		return NO_OUTPUT;
	}

	@Override
	public Object subtract(Object output, Object inc) {
		assert output == NO_OUTPUT;
		assert inc == NO_OUTPUT;
		return NO_OUTPUT;
	}

	@Override
	public Object add(Object prefix, Object output) {
		assert prefix == NO_OUTPUT : "got " + prefix;
		assert output == NO_OUTPUT;
		return NO_OUTPUT;
	}

	@Override
	public void write(Object prefix, DataOutput out) {

	}

	@Override
	public Object read(DataInput in) {

		return NO_OUTPUT;
	}

	@Override
	public Object getNoOutput() {
		return NO_OUTPUT;
	}

	@Override
	public String outputToString(Object output) {
		return "";
	}
}
