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
import com.shatam.shatamindex.analysis.tokenattributes.KeywordAttribute;

public final class PorterStemFilter extends TokenFilter {
	private final PorterStemmer stemmer = new PorterStemmer();
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

	public PorterStemFilter(TokenStream in) {
		super(in);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (!input.incrementToken())
			return false;

		if ((!keywordAttr.isKeyword())
				&& stemmer.stem(termAtt.buffer(), 0, termAtt.length()))
			termAtt.copyBuffer(stemmer.getResultBuffer(), 0,
					stemmer.getResultLength());
		return true;
	}
}
