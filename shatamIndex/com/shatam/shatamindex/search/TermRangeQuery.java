/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.text.Collator;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.ToStringUtils;

public class TermRangeQuery extends MultiTermQuery {
	private String lowerTerm;
	private String upperTerm;
	private Collator collator;
	private String field;
	private boolean includeLower;
	private boolean includeUpper;

	public TermRangeQuery(String field, String lowerTerm, String upperTerm,
			boolean includeLower, boolean includeUpper) {
		this(field, lowerTerm, upperTerm, includeLower, includeUpper, null);
	}

	public TermRangeQuery(String field, String lowerTerm, String upperTerm,
			boolean includeLower, boolean includeUpper, Collator collator) {
		this.field = field;
		this.lowerTerm = lowerTerm;
		this.upperTerm = upperTerm;
		this.includeLower = includeLower;
		this.includeUpper = includeUpper;
		this.collator = collator;
	}

	public String getField() {
		return field;
	}

	public String getLowerTerm() {
		return lowerTerm;
	}

	public String getUpperTerm() {
		return upperTerm;
	}

	public boolean includesLower() {
		return includeLower;
	}

	public boolean includesUpper() {
		return includeUpper;
	}

	public Collator getCollator() {
		return collator;
	}

	@Override
	protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
		return new TermRangeTermEnum(reader, field, lowerTerm, upperTerm,
				includeLower, includeUpper, collator);
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!getField().equals(field)) {
			buffer.append(getField());
			buffer.append(":");
		}
		buffer.append(includeLower ? '[' : '{');
		buffer.append(lowerTerm != null ? lowerTerm : "*");
		buffer.append(" TO ");
		buffer.append(upperTerm != null ? upperTerm : "*");
		buffer.append(includeUpper ? ']' : '}');
		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((collator == null) ? 0 : collator.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + (includeLower ? 1231 : 1237);
		result = prime * result + (includeUpper ? 1231 : 1237);
		result = prime * result
				+ ((lowerTerm == null) ? 0 : lowerTerm.hashCode());
		result = prime * result
				+ ((upperTerm == null) ? 0 : upperTerm.hashCode());
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
		TermRangeQuery other = (TermRangeQuery) obj;
		if (collator == null) {
			if (other.collator != null)
				return false;
		} else if (!collator.equals(other.collator))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (includeLower != other.includeLower)
			return false;
		if (includeUpper != other.includeUpper)
			return false;
		if (lowerTerm == null) {
			if (other.lowerTerm != null)
				return false;
		} else if (!lowerTerm.equals(other.lowerTerm))
			return false;
		if (upperTerm == null) {
			if (other.upperTerm != null)
				return false;
		} else if (!upperTerm.equals(other.upperTerm))
			return false;
		return true;
	}

}
