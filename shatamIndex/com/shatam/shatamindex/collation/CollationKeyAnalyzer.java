/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.collation;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.analysis.KeywordTokenizer;
import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.Tokenizer;

/**
 * Licensed to Shatam Technologies
 */

import java.text.Collator;
import java.io.Reader;
import java.io.IOException;

public final class CollationKeyAnalyzer extends Analyzer {
	private Collator collator;

	public CollationKeyAnalyzer(Collator collator) {
		this.collator = collator;
	}

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream result = new KeywordTokenizer(reader);
		result = new CollationKeyFilter(result, collator);
		return result;
	}

	private class SavedStreams {
		Tokenizer source;
		TokenStream result;
	}

	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader)
			throws IOException {

		SavedStreams streams = (SavedStreams) getPreviousTokenStream();
		if (streams == null) {
			streams = new SavedStreams();
			streams.source = new KeywordTokenizer(reader);
			streams.result = new CollationKeyFilter(streams.source, collator);
			setPreviousTokenStream(streams);
		} else {
			streams.source.reset(reader);
		}
		return streams.result;
	}
}
