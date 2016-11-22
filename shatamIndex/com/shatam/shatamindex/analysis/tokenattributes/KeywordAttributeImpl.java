/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.util.AttributeImpl;

public final class KeywordAttributeImpl extends AttributeImpl implements
		KeywordAttribute {
	private boolean keyword;

	@Override
	public void clear() {
		keyword = false;
	}

	@Override
	public void copyTo(AttributeImpl target) {
		KeywordAttribute attr = (KeywordAttribute) target;
		attr.setKeyword(keyword);
	}

	@Override
	public int hashCode() {
		return keyword ? 31 : 37;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final KeywordAttributeImpl other = (KeywordAttributeImpl) obj;
		return keyword == other.keyword;
	}

	public boolean isKeyword() {
		return keyword;
	}

	public void setKeyword(boolean isKeyword) {
		keyword = isKeyword;
	}

}
