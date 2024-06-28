/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.shatam.shatamindex.util.AttributeSource;

public final class CachingTokenFilter extends TokenFilter {
	private List<AttributeSource.State> cache = null;
	private Iterator<AttributeSource.State> iterator = null;
	private AttributeSource.State finalState;

	public CachingTokenFilter(TokenStream input) {
		super(input);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (cache == null) {

			cache = new LinkedList<AttributeSource.State>();
			fillCache();
			iterator = cache.iterator();
		}

		if (!iterator.hasNext()) {

			return false;
		}

		restoreState(iterator.next());
		return true;
	}

	@Override
	public final void end() throws IOException {
		if (finalState != null) {
			restoreState(finalState);
		}
	}

	@Override
	public void reset() throws IOException {
		if (cache != null) {
			iterator = cache.iterator();
		}
	}

	private void fillCache() throws IOException {
		while (input.incrementToken()) {
			cache.add(captureState());
		}

		input.end();
		finalState = captureState();
	}

}
