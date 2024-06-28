/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

public abstract class FilteredDocIdSetIterator extends DocIdSetIterator {
	protected DocIdSetIterator _innerIter;
	private int doc;

	public FilteredDocIdSetIterator(DocIdSetIterator innerIter) {
		if (innerIter == null) {
			throw new IllegalArgumentException("null iterator");
		}
		_innerIter = innerIter;
		doc = -1;
	}

	abstract protected boolean match(int doc) throws IOException;

	@Override
	public int docID() {
		return doc;
	}

	@Override
	public int nextDoc() throws IOException {
		while ((doc = _innerIter.nextDoc()) != NO_MORE_DOCS) {
			if (match(doc)) {
				return doc;
			}
		}
		return doc;
	}

	@Override
	public int advance(int target) throws IOException {
		doc = _innerIter.advance(target);
		if (doc != NO_MORE_DOCS) {
			if (match(doc)) {
				return doc;
			} else {
				while ((doc = _innerIter.nextDoc()) != NO_MORE_DOCS) {
					if (match(doc)) {
						return doc;
					}
				}
				return doc;
			}
		}
		return doc;
	}

}
