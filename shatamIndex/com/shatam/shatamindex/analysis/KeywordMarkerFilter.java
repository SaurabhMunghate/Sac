/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.util.Set;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.KeywordAttribute;
import com.shatam.shatamindex.util.Version;

public final class KeywordMarkerFilter extends TokenFilter {

	private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final CharArraySet keywordSet;

	public KeywordMarkerFilter(final TokenStream in,
			final CharArraySet keywordSet) {
		super(in);
		this.keywordSet = keywordSet;
	}

	public KeywordMarkerFilter(final TokenStream in, final Set<?> keywordSet) {
		this(in, keywordSet instanceof CharArraySet ? (CharArraySet) keywordSet
				: CharArraySet.copy(Version.SHATAM_31, keywordSet));
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			if (keywordSet.contains(termAtt.buffer(), 0, termAtt.length())) {
				keywordAttr.setKeyword(true);
			}
			return true;
		} else {
			return false;
		}
	}
}
