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
import com.shatam.shatamindex.util.CharacterUtils;
import com.shatam.shatamindex.util.Version;

public final class LowerCaseFilter extends TokenFilter {
	private final CharacterUtils charUtils;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public LowerCaseFilter(Version matchVersion, TokenStream in) {
		super(in);
		charUtils = CharacterUtils.getInstance(matchVersion);
	}

	@Deprecated
	public LowerCaseFilter(TokenStream in) {
		this(Version.SHATAM_30, in);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			final char[] buffer = termAtt.buffer();
			final int length = termAtt.length();
			for (int i = 0; i < length;) {
				i += Character
						.toChars(Character.toLowerCase(charUtils.codePointAt(
								buffer, i)), buffer, i);
			}
			return true;
		} else
			return false;
	}
}
