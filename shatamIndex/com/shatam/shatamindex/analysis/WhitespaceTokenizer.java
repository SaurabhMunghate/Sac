/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.Reader;

import com.shatam.shatamindex.util.AttributeSource;
import com.shatam.shatamindex.util.Version;

public final class WhitespaceTokenizer extends CharTokenizer {

	public WhitespaceTokenizer(Version matchVersion, Reader in) {
		super(matchVersion, in);
	}

	public WhitespaceTokenizer(Version matchVersion, AttributeSource source,
			Reader in) {
		super(matchVersion, source, in);
	}

	public WhitespaceTokenizer(Version matchVersion, AttributeFactory factory,
			Reader in) {
		super(matchVersion, factory, in);
	}

	@Deprecated
	public WhitespaceTokenizer(Reader in) {
		super(in);
	}

	@Deprecated
	public WhitespaceTokenizer(AttributeSource source, Reader in) {
		super(source, in);
	}

	@Deprecated
	public WhitespaceTokenizer(AttributeFactory factory, Reader in) {
		super(factory, in);
	}

	@Override
	protected boolean isTokenChar(int c) {
		return !Character.isWhitespace(c);
	}
}
