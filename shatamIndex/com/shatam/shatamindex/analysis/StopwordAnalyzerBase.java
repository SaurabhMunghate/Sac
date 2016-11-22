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
import java.util.Set;

import com.shatam.shatamindex.analysis.CharArraySet;
import com.shatam.shatamindex.analysis.ReusableAnalyzerBase;
import com.shatam.shatamindex.analysis.WordlistLoader;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.Version;

public abstract class StopwordAnalyzerBase extends ReusableAnalyzerBase {

	protected final CharArraySet stopwords;

	protected final Version matchVersion;

	public Set<?> getStopwordSet() {
		return stopwords;
	}

	protected StopwordAnalyzerBase(final Version version, final Set<?> stopwords) {
		matchVersion = version;

		this.stopwords = stopwords == null ? CharArraySet.EMPTY_SET
				: CharArraySet.unmodifiableSet(CharArraySet.copy(version,
						stopwords));
	}

	protected StopwordAnalyzerBase(final Version version) {
		this(version, null);
	}

	protected static CharArraySet loadStopwordSet(final boolean ignoreCase,
			final Class<? extends ReusableAnalyzerBase> aClass,
			final String resource, final String comment) throws IOException {
		Reader reader = null;
		try {
			reader = IOUtils
					.getDecodingReader(aClass.getResourceAsStream(resource),
							IOUtils.CHARSET_UTF_8);
			return WordlistLoader.getWordSet(reader, comment, new CharArraySet(
					Version.SHATAM_31, 16, ignoreCase));
		} finally {
			IOUtils.close(reader);
		}

	}

	protected static CharArraySet loadStopwordSet(File stopwords,
			Version matchVersion) throws IOException {
		Reader reader = null;
		try {
			reader = IOUtils
					.getDecodingReader(stopwords, IOUtils.CHARSET_UTF_8);
			return WordlistLoader.getWordSet(reader, matchVersion);
		} finally {
			IOUtils.close(reader);
		}
	}

	protected static CharArraySet loadStopwordSet(Reader stopwords,
			Version matchVersion) throws IOException {
		try {
			return WordlistLoader.getWordSet(stopwords, matchVersion);
		} finally {
			IOUtils.close(stopwords);
		}
	}
}
