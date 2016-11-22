/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;

public abstract class CharFilter extends CharStream {

	protected CharStream input;

	protected CharFilter(CharStream in) {
		input = in;
	}

	protected int correct(int currentOff) {
		return currentOff;
	}

	@Override
	public final int correctOffset(int currentOff) {
		return input.correctOffset(correct(currentOff));
	}

	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		return input.read(cbuf, off, len);
	}

	@Override
	public boolean markSupported() {
		return input.markSupported();
	}

	@Override
	public void mark(int readAheadLimit) throws IOException {
		input.mark(readAheadLimit);
	}

	@Override
	public void reset() throws IOException {
		input.reset();
	}
}
