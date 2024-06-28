/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;

public class SingleTermEnum extends FilteredTermEnum {
	private Term singleTerm;
	private boolean endEnum = false;

	public SingleTermEnum(IndexReader reader, Term singleTerm)
			throws IOException {
		super();
		this.singleTerm = singleTerm;
		setEnum(reader.terms(singleTerm));
	}

	@Override
	public float difference() {
		return 1.0F;
	}

	@Override
	protected boolean endEnum() {
		return endEnum;
	}

	@Override
	protected boolean termCompare(Term term) {
		if (term.equals(singleTerm)) {
			return true;
		} else {
			endEnum = true;
			return false;
		}
	}
}
