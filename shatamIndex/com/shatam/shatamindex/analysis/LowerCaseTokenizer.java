/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.Reader;

import com.shatam.shatamindex.util.AttributeSource;
import com.shatam.shatamindex.util.Version;

public final class LowerCaseTokenizer extends LetterTokenizer {

	public LowerCaseTokenizer(Version matchVersion, Reader in) {
		super(matchVersion, in);
	}

	public LowerCaseTokenizer(Version matchVersion, AttributeSource source,
			Reader in) {
		super(matchVersion, source, in);
	}

	public LowerCaseTokenizer(Version matchVersion, AttributeFactory factory,
			Reader in) {
		super(matchVersion, factory, in);
	}

	@Deprecated
	public LowerCaseTokenizer(Reader in) {
		super(Version.SHATAM_30, in);
	}

	@Deprecated
	public LowerCaseTokenizer(AttributeSource source, Reader in) {
		super(Version.SHATAM_30, source, in);
	}

	@Deprecated
	public LowerCaseTokenizer(AttributeFactory factory, Reader in) {
		super(Version.SHATAM_30, factory, in);
	}

	@Override
	protected int normalize(int c) {
		return Character.toLowerCase(c);
	}
}
