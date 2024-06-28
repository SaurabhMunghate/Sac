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

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.OffsetAttribute;
import com.shatam.shatamindex.util.AttributeSource;

public final class KeywordTokenizer extends Tokenizer {

	private static final int DEFAULT_BUFFER_SIZE = 256;

	private boolean done = false;
	private int finalOffset;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	public KeywordTokenizer(Reader input) {
		this(input, DEFAULT_BUFFER_SIZE);
	}

	public KeywordTokenizer(Reader input, int bufferSize) {
		super(input);
		termAtt.resizeBuffer(bufferSize);
	}

	public KeywordTokenizer(AttributeSource source, Reader input, int bufferSize) {
		super(source, input);
		termAtt.resizeBuffer(bufferSize);
	}

	public KeywordTokenizer(AttributeFactory factory, Reader input,
			int bufferSize) {
		super(factory, input);
		termAtt.resizeBuffer(bufferSize);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (!done) {
			clearAttributes();
			done = true;
			int upto = 0;
			char[] buffer = termAtt.buffer();
			while (true) {
				final int length = input.read(buffer, upto, buffer.length
						- upto);
				if (length == -1)
					break;
				upto += length;
				if (upto == buffer.length)
					buffer = termAtt.resizeBuffer(1 + buffer.length);
			}
			termAtt.setLength(upto);
			finalOffset = correctOffset(upto);
			offsetAtt.setOffset(correctOffset(0), finalOffset);
			return true;
		}
		return false;
	}

	@Override
	public final void end() {

		offsetAtt.setOffset(finalOffset, finalOffset);
	}

	@Override
	public void reset(Reader input) throws IOException {
		super.reset(input);
		this.done = false;
	}
}
