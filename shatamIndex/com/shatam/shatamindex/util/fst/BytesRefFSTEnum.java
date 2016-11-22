/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import java.io.IOException;

import com.shatam.shatamindex.util.BytesRef;

public final class BytesRefFSTEnum<T> extends FSTEnum<T> {
	private final BytesRef current = new BytesRef(10);
	private final InputOutput<T> result = new InputOutput<T>();
	private BytesRef target;

	public static class InputOutput<T> {
		public BytesRef input;
		public T output;
	}

	public BytesRefFSTEnum(FST<T> fst) {
		super(fst);
		result.input = current;
		current.offset = 1;
	}

	public InputOutput<T> current() {
		return result;
	}

	public InputOutput<T> next() throws IOException {

		doNext();
		return setResult();
	}

	public InputOutput<T> seekCeil(BytesRef target) throws IOException {
		this.target = target;
		targetLength = target.length;
		super.doSeekCeil();
		return setResult();
	}

	public InputOutput<T> seekFloor(BytesRef target) throws IOException {
		this.target = target;
		targetLength = target.length;
		super.doSeekFloor();

		return setResult();
	}

	@Override
	protected int getTargetLabel() {
		if (upto - 1 == target.length) {
			return FST.END_LABEL;
		} else {
			return target.bytes[target.offset + upto - 1] & 0xFF;
		}
	}

	@Override
	protected int getCurrentLabel() {

		return current.bytes[upto] & 0xFF;
	}

	@Override
	protected void setCurrentLabel(int label) {
		current.bytes[upto] = (byte) label;
	}

	@Override
	protected void grow() {
		current.grow(upto + 1);
	}

	private InputOutput<T> setResult() {
		if (upto == 0) {
			return null;
		} else {
			current.length = upto - 1;
			result.output = output[upto];
			return result;
		}
	}
}
