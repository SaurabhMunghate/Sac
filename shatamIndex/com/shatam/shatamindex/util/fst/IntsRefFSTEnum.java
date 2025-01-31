/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import com.shatam.shatamindex.util.IntsRef;

import java.io.IOException;

public final class IntsRefFSTEnum<T> extends FSTEnum<T> {
	private final IntsRef current = new IntsRef(10);
	private final InputOutput<T> result = new InputOutput<T>();
	private IntsRef target;

	public static class InputOutput<T> {
		public IntsRef input;
		public T output;
	}

	public IntsRefFSTEnum(FST<T> fst) {
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

	public InputOutput<T> seekCeil(IntsRef target) throws IOException {
		this.target = target;
		targetLength = target.length;
		super.doSeekCeil();
		return setResult();
	}

	public InputOutput<T> seekFloor(IntsRef target) throws IOException {
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
			return target.ints[target.offset + upto - 1];
		}
	}

	@Override
	protected int getCurrentLabel() {

		return current.ints[upto];
	}

	@Override
	protected void setCurrentLabel(int label) {
		current.ints[upto] = label;
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
