/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.List;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.queryParser.QueryParser;
import com.shatam.shatamindex.util.Version;

public final class StopFilter extends FilteringTokenFilter {

	private final CharArraySet stopWords;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	@Deprecated
	public StopFilter(boolean enablePositionIncrements, TokenStream input,
			Set<?> stopWords, boolean ignoreCase) {
		this(Version.SHATAM_30, enablePositionIncrements, input, stopWords,
				ignoreCase);
	}

	public StopFilter(Version matchVersion, TokenStream input,
			Set<?> stopWords, boolean ignoreCase) {
		this(matchVersion, matchVersion.onOrAfter(Version.SHATAM_29), input,
				stopWords, ignoreCase);
	}

	private StopFilter(Version matchVersion, boolean enablePositionIncrements,
			TokenStream input, Set<?> stopWords, boolean ignoreCase) {
		super(enablePositionIncrements, input);
		this.stopWords = stopWords instanceof CharArraySet ? (CharArraySet) stopWords
				: new CharArraySet(matchVersion, stopWords, ignoreCase);
	}

	@Deprecated
	public StopFilter(boolean enablePositionIncrements, TokenStream in,
			Set<?> stopWords) {
		this(Version.SHATAM_30, enablePositionIncrements, in, stopWords, false);
	}

	public StopFilter(Version matchVersion, TokenStream in, Set<?> stopWords) {
		this(matchVersion, in, stopWords, false);
	}

	@Deprecated
	public static final Set<Object> makeStopSet(String... stopWords) {
		return makeStopSet(Version.SHATAM_30, stopWords, false);
	}

	public static final Set<Object> makeStopSet(Version matchVersion,
			String... stopWords) {
		return makeStopSet(matchVersion, stopWords, false);
	}

	@Deprecated
	public static final Set<Object> makeStopSet(List<?> stopWords) {
		return makeStopSet(Version.SHATAM_30, stopWords, false);
	}

	public static final Set<Object> makeStopSet(Version matchVersion,
			List<?> stopWords) {
		return makeStopSet(matchVersion, stopWords, false);
	}

	@Deprecated
	public static final Set<Object> makeStopSet(String[] stopWords,
			boolean ignoreCase) {
		return makeStopSet(Version.SHATAM_30, stopWords, ignoreCase);
	}

	public static final Set<Object> makeStopSet(Version matchVersion,
			String[] stopWords, boolean ignoreCase) {
		CharArraySet stopSet = new CharArraySet(matchVersion, stopWords.length,
				ignoreCase);
		stopSet.addAll(Arrays.asList(stopWords));
		return stopSet;
	}

	@Deprecated
	public static final Set<Object> makeStopSet(List<?> stopWords,
			boolean ignoreCase) {
		return makeStopSet(Version.SHATAM_30, stopWords, ignoreCase);
	}

	public static final Set<Object> makeStopSet(Version matchVersion,
			List<?> stopWords, boolean ignoreCase) {
		CharArraySet stopSet = new CharArraySet(matchVersion, stopWords.size(),
				ignoreCase);
		stopSet.addAll(stopWords);
		return stopSet;
	}

	@Override
	protected boolean accept() throws IOException {
		return !stopWords.contains(termAtt.buffer(), 0, termAtt.length());
	}

	@Deprecated
	public static boolean getEnablePositionIncrementsVersionDefault(
			Version matchVersion) {
		return matchVersion.onOrAfter(Version.SHATAM_29);
	}
}
