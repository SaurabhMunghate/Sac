/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;

public final class LengthFilter extends FilteringTokenFilter {

	private final int min;
	private final int max;

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public LengthFilter(boolean enablePositionIncrements, TokenStream in,
			int min, int max) {
		super(enablePositionIncrements, in);
		this.min = min;
		this.max = max;
	}

	@Deprecated
	public LengthFilter(TokenStream in, int min, int max) {
		this(false, in, min, max);
	}

	@Override
	public boolean accept() throws IOException {
		final int len = termAtt.length();
		return (len >= min && len <= max);
	}
}
