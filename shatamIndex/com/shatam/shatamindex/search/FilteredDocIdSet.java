/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

public abstract class FilteredDocIdSet extends DocIdSet {
	private final DocIdSet _innerSet;

	public FilteredDocIdSet(DocIdSet innerSet) {
		_innerSet = innerSet;
	}

	@Override
	public boolean isCacheable() {
		return _innerSet.isCacheable();
	}

	protected abstract boolean match(int docid) throws IOException;

	@Override
	public DocIdSetIterator iterator() throws IOException {
		return new FilteredDocIdSetIterator(_innerSet.iterator()) {
			@Override
			protected boolean match(int docid) throws IOException {
				return FilteredDocIdSet.this.match(docid);
			}
		};
	}
}
