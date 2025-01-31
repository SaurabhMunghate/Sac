/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.UnicodeUtil;

final class TermBuffer implements Cloneable {

	private String field;
	private Term term;
	private boolean preUTF8Strings;
	private boolean dirty;

	private UnicodeUtil.UTF16Result text = new UnicodeUtil.UTF16Result();
	private UnicodeUtil.UTF8Result bytes = new UnicodeUtil.UTF8Result();

	public final int compareTo(TermBuffer other) {
		if (field == other.field)
			return compareChars(text.result, text.length, other.text.result,
					other.text.length);
		else
			return field.compareTo(other.field);
	}

	private static final int compareChars(char[] chars1, int len1,
			char[] chars2, int len2) {
		final int end = len1 < len2 ? len1 : len2;
		for (int k = 0; k < end; k++) {
			char c1 = chars1[k];
			char c2 = chars2[k];
			if (c1 != c2) {
				return c1 - c2;
			}
		}
		return len1 - len2;
	}

	void setPreUTF8Strings() {
		preUTF8Strings = true;
	}

	public final void read(IndexInput input, FieldInfos fieldInfos)
			throws IOException {
		this.term = null;
		int start = input.readVInt();
		int length = input.readVInt();
		int totalLength = start + length;
		if (preUTF8Strings) {
			text.setLength(totalLength);
			input.readChars(text.result, start, length);
		} else {

			if (dirty) {

				UnicodeUtil.UTF16toUTF8(text.result, 0, text.length, bytes);
				bytes.setLength(totalLength);
				input.readBytes(bytes.result, start, length);
				UnicodeUtil.UTF8toUTF16(bytes.result, 0, totalLength, text);
				dirty = false;
			} else {

				bytes.setLength(totalLength);
				input.readBytes(bytes.result, start, length);
				UnicodeUtil.UTF8toUTF16(bytes.result, start, length, text);
			}
		}
		this.field = fieldInfos.fieldName(input.readVInt());
	}

	public final void set(Term term) {
		if (term == null) {
			reset();
			return;
		}
		final String termText = term.text();
		final int termLen = termText.length();
		text.setLength(termLen);
		termText.getChars(0, termLen, text.result, 0);
		dirty = true;
		field = term.field();
		this.term = term;
	}

	public final void set(TermBuffer other) {
		text.copyText(other.text);
		dirty = true;
		field = other.field;
		term = other.term;
	}

	public void reset() {
		field = null;
		text.setLength(0);
		term = null;
		dirty = true;
	}

	public Term toTerm() {
		if (field == null)
			return null;

		if (term == null)
			term = new Term(field, new String(text.result, 0, text.length),
					false);

		return term;
	}

	@Override
	protected Object clone() {
		TermBuffer clone = null;
		try {
			clone = (TermBuffer) super.clone();
		} catch (CloneNotSupportedException e) {
		}

		clone.dirty = true;
		clone.bytes = new UnicodeUtil.UTF8Result();
		clone.text = new UnicodeUtil.UTF16Result();
		clone.text.copyText(text);
		return clone;
	}
}
