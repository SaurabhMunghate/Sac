/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.io.Reader;

public final class CharReader extends CharStream {

	private final Reader input;

	public static CharStream get(Reader input) {
		return input instanceof CharStream ? (CharStream) input
				: new CharReader(input);
	}

	private CharReader(Reader in) {
		input = in;
	}

	@Override
	public int correctOffset(int currentOff) {
		return currentOff;
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
