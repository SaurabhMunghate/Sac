/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermEnum;

public abstract class FilteredTermEnum extends TermEnum {

	protected Term currentTerm = null;

	protected TermEnum actualEnum = null;

	public FilteredTermEnum() {
	}

	protected abstract boolean termCompare(Term term);

	public abstract float difference();

	protected abstract boolean endEnum();

	protected void setEnum(TermEnum actualEnum) throws IOException {
		this.actualEnum = actualEnum;

		Term term = actualEnum.term();
		if (term != null && termCompare(term))
			currentTerm = term;
		else
			next();
	}

	@Override
	public int docFreq() {
		if (currentTerm == null)
			return -1;
		assert actualEnum != null;
		return actualEnum.docFreq();
	}

	@Override
	public boolean next() throws IOException {
		if (actualEnum == null)
			return false;
		currentTerm = null;
		while (currentTerm == null) {
			if (endEnum())
				return false;
			if (actualEnum.next()) {
				Term term = actualEnum.term();
				if (termCompare(term)) {
					currentTerm = term;
					return true;
				}
			} else
				return false;
		}
		currentTerm = null;
		return false;
	}

	@Override
	public Term term() {
		return currentTerm;
	}

	@Override
	public void close() throws IOException {
		if (actualEnum != null)
			actualEnum.close();
		currentTerm = null;
		actualEnum = null;
	}
}
