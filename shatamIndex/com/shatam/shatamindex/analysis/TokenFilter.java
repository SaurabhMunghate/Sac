
/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;

public abstract class TokenFilter extends TokenStream {

	protected final TokenStream input;

	protected TokenFilter(TokenStream input) {
		super(input);
		this.input = input;
	}

	@Override
	public void end() throws IOException {
		input.end();
	}

	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public void reset() throws IOException {
		input.reset();
	}
}
