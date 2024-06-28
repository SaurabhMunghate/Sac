/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.Term;

public class PrefixFilter extends MultiTermQueryWrapperFilter<PrefixQuery> {

	public PrefixFilter(Term prefix) {
		super(new PrefixQuery(prefix));
	}

	public Term getPrefix() {
		return query.getPrefix();
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("PrefixFilter(");
		buffer.append(getPrefix().toString());
		buffer.append(")");
		return buffer.toString();
	}

}
