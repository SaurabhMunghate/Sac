/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.standard;

import com.shatam.shatamindex.analysis.*;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

public final class StandardAnalyzer extends StopwordAnalyzerBase {

	public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

	private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

	private final boolean replaceInvalidAcronym;

	public static final Set<?> STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

	public StandardAnalyzer(Version matchVersion, Set<?> stopWords) {
		super(matchVersion, stopWords);
		replaceInvalidAcronym = matchVersion.onOrAfter(Version.SHATAM_24);
	}

	public StandardAnalyzer(Version matchVersion) {
		this(matchVersion, STOP_WORDS_SET);
	}

	public StandardAnalyzer(Version matchVersion, File stopwords)
			throws IOException {
		this(matchVersion, WordlistLoader.getWordSet(
				IOUtils.getDecodingReader(stopwords, IOUtils.CHARSET_UTF_8),
				matchVersion));
	}

	public StandardAnalyzer(Version matchVersion, Reader stopwords)
			throws IOException {
		this(matchVersion, WordlistLoader.getWordSet(stopwords, matchVersion));
	}

	public void setMaxTokenLength(int length) {
		maxTokenLength = length;
	}

	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName,
			final Reader reader) {
		final StandardTokenizer src = new StandardTokenizer(matchVersion,
				reader);
		src.setMaxTokenLength(maxTokenLength);
		src.setReplaceInvalidAcronym(replaceInvalidAcronym);
		TokenStream tok = new StandardFilter(matchVersion, src);
		tok = new LowerCaseFilter(matchVersion, tok);
		tok = new StopFilter(matchVersion, tok, stopwords);
		return new TokenStreamComponents(src, tok) {
			@Override
			protected boolean reset(final Reader reader) throws IOException {
				src.setMaxTokenLength(StandardAnalyzer.this.maxTokenLength);
				return super.reset(reader);
			}
		};
	}
}
