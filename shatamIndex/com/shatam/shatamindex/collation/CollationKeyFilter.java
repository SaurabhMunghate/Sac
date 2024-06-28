/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.collation;

import com.shatam.shatamindex.analysis.TokenFilter;
import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.util.IndexableBinaryStringTools;

import java.io.IOException;
import java.text.Collator;

public final class CollationKeyFilter extends TokenFilter {
	private final Collator collator;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public CollationKeyFilter(TokenStream input, Collator collator) {
		super(input);

		this.collator = (Collator) collator.clone();
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			byte[] collationKey = collator.getCollationKey(termAtt.toString())
					.toByteArray();
			int encodedLength = IndexableBinaryStringTools.getEncodedLength(
					collationKey, 0, collationKey.length);
			termAtt.resizeBuffer(encodedLength);
			termAtt.setLength(encodedLength);
			IndexableBinaryStringTools.encode(collationKey, 0,
					collationKey.length, termAtt.buffer(), 0, encodedLength);
			return true;
		} else {
			return false;
		}
	}
}
