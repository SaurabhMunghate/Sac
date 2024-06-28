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
import java.util.Map;
import java.util.HashMap;

public final class PerFieldAnalyzerWrapper extends Analyzer {
	private final Analyzer defaultAnalyzer;
	private final Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();

	public PerFieldAnalyzerWrapper(Analyzer defaultAnalyzer) {
		this(defaultAnalyzer, null);
	}

	public PerFieldAnalyzerWrapper(Analyzer defaultAnalyzer,
			Map<String, Analyzer> fieldAnalyzers) {
		this.defaultAnalyzer = defaultAnalyzer;
		if (fieldAnalyzers != null) {
			analyzerMap.putAll(fieldAnalyzers);
		}
	}

	@Deprecated
	public void addAnalyzer(String fieldName, Analyzer analyzer) {
		analyzerMap.put(fieldName, analyzer);
	}

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		Analyzer analyzer = analyzerMap.get(fieldName);
		if (analyzer == null) {
			analyzer = defaultAnalyzer;
		}

		return analyzer.tokenStream(fieldName, reader);
	}

	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader)
			throws IOException {
		Analyzer analyzer = analyzerMap.get(fieldName);
		if (analyzer == null)
			analyzer = defaultAnalyzer;

		return analyzer.reusableTokenStream(fieldName, reader);
	}

	@Override
	public int getPositionIncrementGap(String fieldName) {
		Analyzer analyzer = analyzerMap.get(fieldName);
		if (analyzer == null)
			analyzer = defaultAnalyzer;
		return analyzer.getPositionIncrementGap(fieldName);
	}

	@Override
	public int getOffsetGap(Fieldable field) {
		Analyzer analyzer = analyzerMap.get(field.name());
		if (analyzer == null)
			analyzer = defaultAnalyzer;
		return analyzer.getOffsetGap(field);
	}

	@Override
	public String toString() {
		return "PerFieldAnalyzerWrapper(" + analyzerMap + ", default="
				+ defaultAnalyzer + ")";
	}
}
