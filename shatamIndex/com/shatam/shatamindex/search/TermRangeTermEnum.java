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
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.util.StringHelper;

public class TermRangeTermEnum extends FilteredTermEnum {

	private Collator collator = null;
	private boolean endEnum = false;
	private String field;
	private String upperTermText;
	private String lowerTermText;
	private boolean includeLower;
	private boolean includeUpper;

	public TermRangeTermEnum(IndexReader reader, String field,
			String lowerTermText, String upperTermText, boolean includeLower,
			boolean includeUpper, Collator collator) throws IOException {
		this.collator = collator;
		this.upperTermText = upperTermText;
		this.lowerTermText = lowerTermText;
		this.includeLower = includeLower;
		this.includeUpper = includeUpper;
		this.field = StringHelper.intern(field);

		if (this.lowerTermText == null) {
			this.lowerTermText = "";
			this.includeLower = true;
		}

		if (this.upperTermText == null) {
			this.includeUpper = true;
		}

		String startTermText = collator == null ? this.lowerTermText : "";
		setEnum(reader.terms(new Term(this.field, startTermText)));
	}

	@Override
	public float difference() {
		return 1.0f;
	}

	@Override
	protected boolean endEnum() {
		return endEnum;
	}

	@Override
	protected boolean termCompare(Term term) {
		if (collator == null) {

			boolean checkLower = false;
			if (!includeLower)
				checkLower = true;
			if (term != null && term.field() == field) {
				if (!checkLower || null == lowerTermText
						|| term.text().compareTo(lowerTermText) > 0) {
					checkLower = false;
					if (upperTermText != null) {
						int compare = upperTermText.compareTo(term.text());

						if ((compare < 0) || (!includeUpper && compare == 0)) {
							endEnum = true;
							return false;
						}
					}
					return true;
				}
			} else {

				endEnum = true;
				return false;
			}
			return false;
		} else {
			if (term != null && term.field() == field) {
				if ((lowerTermText == null || (includeLower ? collator.compare(
						term.text(), lowerTermText) >= 0 : collator.compare(
						term.text(), lowerTermText) > 0))
						&& (upperTermText == null || (includeUpper ? collator
								.compare(term.text(), upperTermText) <= 0
								: collator.compare(term.text(), upperTermText) < 0))) {
					return true;
				}
				return false;
			}
			endEnum = true;
			return false;
		}
	}
}
