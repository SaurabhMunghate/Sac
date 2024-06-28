/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.util.ToStringUtils;

import java.io.IOException;

public class WildcardQuery extends MultiTermQuery {
	private boolean termContainsWildcard;
	private boolean termIsPrefix;
	protected Term term;

	public WildcardQuery(Term term) {
		this.term = term;
		String text = term.text();
		this.termContainsWildcard = (text.indexOf('*') != -1)
				|| (text.indexOf('?') != -1);
		this.termIsPrefix = termContainsWildcard && (text.indexOf('?') == -1)
				&& (text.indexOf('*') == text.length() - 1);
	}

	@Override
	protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
		if (termIsPrefix) {
			return new PrefixTermEnum(reader, term.createTerm(term.text()
					.substring(0, term.text().indexOf('*'))));
		} else if (termContainsWildcard) {
			return new WildcardTermEnum(reader, getTerm());
		} else {
			return new SingleTermEnum(reader, getTerm());
		}
	}

	public Term getTerm() {
		return term;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!term.field().equals(field)) {
			buffer.append(term.field());
			buffer.append(":");
		}
		buffer.append(term.text());
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WildcardQuery other = (WildcardQuery) obj;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}

}
