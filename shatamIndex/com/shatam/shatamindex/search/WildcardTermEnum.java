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

public class WildcardTermEnum extends FilteredTermEnum {
	final Term searchTerm;
	final String field;
	final String text;
	final String pre;
	final int preLen;
	boolean endEnum = false;

	public WildcardTermEnum(IndexReader reader, Term term) throws IOException {
		super();
		searchTerm = term;
		field = searchTerm.field();
		final String searchTermText = searchTerm.text();

		final int sidx = searchTermText.indexOf(WILDCARD_STRING);
		final int cidx = searchTermText.indexOf(WILDCARD_CHAR);
		int idx = sidx;
		if (idx == -1) {
			idx = cidx;
		} else if (cidx >= 0) {
			idx = Math.min(idx, cidx);
		}
		pre = idx != -1 ? searchTerm.text().substring(0, idx) : "";

		preLen = pre.length();
		text = searchTermText.substring(preLen);
		setEnum(reader.terms(new Term(searchTerm.field(), pre)));
	}

	@Override
	protected final boolean termCompare(Term term) {
		if (field == term.field()) {
			String searchText = term.text();
			if (searchText.startsWith(pre)) {
				return wildcardEquals(text, 0, searchText, preLen);
			}
		}
		endEnum = true;
		return false;
	}

	@Override
	public float difference() {
		return 1.0f;
	}

	@Override
	public final boolean endEnum() {
		return endEnum;
	}

	public static final char WILDCARD_STRING = '*';
	public static final char WILDCARD_CHAR = '?';

	public static final boolean wildcardEquals(String pattern, int patternIdx,
			String string, int stringIdx) {
		int p = patternIdx;

		for (int s = stringIdx;; ++p, ++s) {

			boolean sEnd = (s >= string.length());

			boolean pEnd = (p >= pattern.length());

			if (sEnd) {

				boolean justWildcardsLeft = true;

				int wildcardSearchPos = p;

				while (wildcardSearchPos < pattern.length()
						&& justWildcardsLeft) {

					char wildchar = pattern.charAt(wildcardSearchPos);

					if (wildchar != WILDCARD_CHAR
							&& wildchar != WILDCARD_STRING) {
						justWildcardsLeft = false;
					} else {

						if (wildchar == WILDCARD_CHAR) {
							return false;
						}

						wildcardSearchPos++;
					}
				}

				if (justWildcardsLeft) {
					return true;
				}
			}

			if (sEnd || pEnd) {
				break;
			}

			if (pattern.charAt(p) == WILDCARD_CHAR) {
				continue;
			}

			//
			if (pattern.charAt(p) == WILDCARD_STRING) {

				while (p < pattern.length()
						&& pattern.charAt(p) == WILDCARD_STRING)
					++p;

				for (int i = string.length(); i >= s; --i) {
					if (wildcardEquals(pattern, p, string, i)) {
						return true;
					}
				}
				break;
			}
			if (pattern.charAt(p) != string.charAt(s)) {
				break;
			}
		}
		return false;
	}
}
