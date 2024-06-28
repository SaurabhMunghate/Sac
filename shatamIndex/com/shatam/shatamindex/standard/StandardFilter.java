/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.standard;

import java.io.IOException;

import com.shatam.shatamindex.analysis.TokenFilter;
import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.TypeAttribute;
import com.shatam.shatamindex.util.Version;

public class StandardFilter extends TokenFilter {
	private final Version matchVersion;

	@Deprecated
	public StandardFilter(TokenStream in) {
		this(Version.SHATAM_30, in);
	}

	public StandardFilter(Version matchVersion, TokenStream in) {
		super(in);
		this.matchVersion = matchVersion;
	}

	private static final String APOSTROPHE_TYPE = ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.APOSTROPHE];
	private static final String ACRONYM_TYPE = ClassicTokenizer.TOKEN_TYPES[ClassicTokenizer.ACRONYM];

	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	@Override
	public final boolean incrementToken() throws IOException {
		if (matchVersion.onOrAfter(Version.SHATAM_31))
			return input.incrementToken();
		else
			return incrementTokenClassic();
	}

	public final boolean incrementTokenClassic() throws IOException {
		if (!input.incrementToken()) {
			return false;
		}

		final char[] buffer = termAtt.buffer();
		final int bufferLength = termAtt.length();
		final String type = typeAtt.type();

		if (type == APOSTROPHE_TYPE
				&& bufferLength >= 2
				&& buffer[bufferLength - 2] == '\''
				&& (buffer[bufferLength - 1] == 's' || buffer[bufferLength - 1] == 'S')) {

			termAtt.setLength(bufferLength - 2);
		} else if (type == ACRONYM_TYPE) {
			int upto = 0;
			for (int i = 0; i < bufferLength; i++) {
				char c = buffer[i];
				if (c != '.')
					buffer[upto++] = c;
			}
			termAtt.setLength(upto);
		}

		return true;
	}
}
