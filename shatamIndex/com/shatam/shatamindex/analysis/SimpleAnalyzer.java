/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.Reader;

import com.shatam.shatamindex.util.Version;

public final class SimpleAnalyzer extends ReusableAnalyzerBase {

	private final Version matchVersion;

	public SimpleAnalyzer(Version matchVersion) {
		this.matchVersion = matchVersion;
	}

	@Deprecated
	public SimpleAnalyzer() {
		this(Version.SHATAM_30);
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName,
			final Reader reader) {
		return new TokenStreamComponents(new LowerCaseTokenizer(matchVersion,
				reader));
	}
}
