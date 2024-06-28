/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;
import java.util.List;

import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.Version;

public final class StopAnalyzer extends StopwordAnalyzerBase {

	public static final Set<?> ENGLISH_STOP_WORDS_SET;

	static {
		final List<String> stopWords = Arrays.asList("a", "an", "and", "are",
				"as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
				"it", "no", "not", "of", "on", "or", "such", "that", "the",
				"their", "then", "there", "these", "they", "this", "to", "was",
				"will", "with");
		final CharArraySet stopSet = new CharArraySet(Version.SHATAM_CURRENT,
				stopWords.size(), false);
		stopSet.addAll(stopWords);
		ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);
	}

	public StopAnalyzer(Version matchVersion) {
		this(matchVersion, ENGLISH_STOP_WORDS_SET);
	}

	public StopAnalyzer(Version matchVersion, Set<?> stopWords) {
		super(matchVersion, stopWords);
	}

	public StopAnalyzer(Version matchVersion, File stopwordsFile)
			throws IOException {
		this(matchVersion, WordlistLoader
				.getWordSet(IOUtils.getDecodingReader(stopwordsFile,
						IOUtils.CHARSET_UTF_8), matchVersion));
	}

	public StopAnalyzer(Version matchVersion, Reader stopwords)
			throws IOException {
		this(matchVersion, WordlistLoader.getWordSet(stopwords, matchVersion));
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		final Tokenizer source = new LowerCaseTokenizer(matchVersion, reader);
		return new TokenStreamComponents(source, new StopFilter(matchVersion,
				source, stopwords));
	}
}
