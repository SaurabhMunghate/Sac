/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import com.shatam.shatamindex.document.Fieldable;

import java.io.Reader;
import java.io.IOException;

public final class LimitTokenCountAnalyzer extends Analyzer {
	private final Analyzer delegate;
	private final int maxTokenCount;

	public LimitTokenCountAnalyzer(Analyzer delegate, int maxTokenCount) {
		this.delegate = delegate;
		this.maxTokenCount = maxTokenCount;
	}

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new LimitTokenCountFilter(
				delegate.tokenStream(fieldName, reader), maxTokenCount);
	}

	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader)
			throws IOException {
		return new LimitTokenCountFilter(delegate.reusableTokenStream(
				fieldName, reader), maxTokenCount);
	}

	@Override
	public int getPositionIncrementGap(String fieldName) {
		return delegate.getPositionIncrementGap(fieldName);
	}

	@Override
	public int getOffsetGap(Fieldable field) {
		return delegate.getOffsetGap(field);
	}

	@Override
	public String toString() {
		return "LimitTokenCountAnalyzer(" + delegate.toString()
				+ ", maxTokenCount=" + maxTokenCount + ")";
	}
}
