/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;

public final class LimitTokenCountFilter extends TokenFilter {

	private final int maxTokenCount;
	private int tokenCount = 0;

	public LimitTokenCountFilter(TokenStream in, int maxTokenCount) {
		super(in);
		this.maxTokenCount = maxTokenCount;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (tokenCount < maxTokenCount && input.incrementToken()) {
			tokenCount++;
			return true;
		}
		return false;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		tokenCount = 0;
	}
}
