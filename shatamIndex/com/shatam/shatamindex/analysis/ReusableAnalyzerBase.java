/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.io.Reader;

public abstract class ReusableAnalyzerBase extends Analyzer {

	protected abstract TokenStreamComponents createComponents(String fieldName,
			Reader aReader);

	@Override
	public final TokenStream reusableTokenStream(final String fieldName,
			final Reader reader) throws IOException {
		TokenStreamComponents streamChain = (TokenStreamComponents) getPreviousTokenStream();
		final Reader r = initReader(reader);
		if (streamChain == null || !streamChain.reset(r)) {
			streamChain = createComponents(fieldName, r);
			setPreviousTokenStream(streamChain);
		}
		return streamChain.getTokenStream();
	}

	@Override
	public final TokenStream tokenStream(final String fieldName,
			final Reader reader) {
		return createComponents(fieldName, initReader(reader)).getTokenStream();
	}

	protected Reader initReader(Reader reader) {
		return reader;
	}

	public static class TokenStreamComponents {
		protected final Tokenizer source;
		protected final TokenStream sink;

		public TokenStreamComponents(final Tokenizer source,
				final TokenStream result) {
			this.source = source;
			this.sink = result;
		}

		public TokenStreamComponents(final Tokenizer source) {
			this.source = source;
			this.sink = source;
		}

		protected boolean reset(final Reader reader) throws IOException {
			source.reset(reader);
			return true;
		}

		protected TokenStream getTokenStream() {
			return sink;
		}

	}

}
