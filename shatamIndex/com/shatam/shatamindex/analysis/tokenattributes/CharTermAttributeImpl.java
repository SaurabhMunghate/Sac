/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import java.io.Serializable;
import java.nio.CharBuffer;

import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.AttributeImpl;
import com.shatam.shatamindex.util.AttributeReflector;
import com.shatam.shatamindex.util.RamUsageEstimator;

public class CharTermAttributeImpl extends AttributeImpl implements
		CharTermAttribute, TermAttribute, Cloneable, Serializable {
	private static int MIN_BUFFER_SIZE = 10;

	private char[] termBuffer = new char[ArrayUtil.oversize(MIN_BUFFER_SIZE,
			RamUsageEstimator.NUM_BYTES_CHAR)];
	private int termLength = 0;

	@Deprecated
	public String term() {

		return new String(termBuffer, 0, termLength);
	}

	public final void copyBuffer(char[] buffer, int offset, int length) {
		growTermBuffer(length);
		System.arraycopy(buffer, offset, termBuffer, 0, length);
		termLength = length;
	}

	@Deprecated
	public void setTermBuffer(char[] buffer, int offset, int length) {
		copyBuffer(buffer, offset, length);
	}

	@Deprecated
	public void setTermBuffer(String buffer) {
		int length = buffer.length();
		growTermBuffer(length);
		buffer.getChars(0, length, termBuffer, 0);
		termLength = length;
	}

	@Deprecated
	public void setTermBuffer(String buffer, int offset, int length) {
		assert offset <= buffer.length();
		assert offset + length <= buffer.length();
		growTermBuffer(length);
		buffer.getChars(offset, offset + length, termBuffer, 0);
		termLength = length;
	}

	public final char[] buffer() {
		return termBuffer;
	}

	@Deprecated
	public char[] termBuffer() {
		return termBuffer;
	}

	public final char[] resizeBuffer(int newSize) {
		if (termBuffer.length < newSize) {

			final char[] newCharBuffer = new char[ArrayUtil.oversize(newSize,
					RamUsageEstimator.NUM_BYTES_CHAR)];
			System.arraycopy(termBuffer, 0, newCharBuffer, 0, termBuffer.length);
			termBuffer = newCharBuffer;
		}
		return termBuffer;
	}

	@Deprecated
	public char[] resizeTermBuffer(int newSize) {
		return resizeBuffer(newSize);
	}

	private void growTermBuffer(int newSize) {
		if (termBuffer.length < newSize) {

			termBuffer = new char[ArrayUtil.oversize(newSize,
					RamUsageEstimator.NUM_BYTES_CHAR)];
		}
	}

	@Deprecated
	public int termLength() {
		return termLength;
	}

	public final CharTermAttribute setLength(int length) {
		if (length > termBuffer.length)
			throw new IllegalArgumentException("length " + length
					+ " exceeds the size of the termBuffer ("
					+ termBuffer.length + ")");
		termLength = length;
		return this;
	}

	public final CharTermAttribute setEmpty() {
		termLength = 0;
		return this;
	}

	@Deprecated
	public void setTermLength(int length) {
		setLength(length);
	}

	public final int length() {
		return termLength;
	}

	public final char charAt(int index) {
		if (index >= termLength)
			throw new IndexOutOfBoundsException();
		return termBuffer[index];
	}

	public final CharSequence subSequence(final int start, final int end) {
		if (start > termLength || end > termLength)
			throw new IndexOutOfBoundsException();
		return new String(termBuffer, start, end - start);
	}

	public final CharTermAttribute append(CharSequence csq) {
		if (csq == null)
			return appendNull();
		return append(csq, 0, csq.length());
	}

	public final CharTermAttribute append(CharSequence csq, int start, int end) {
		if (csq == null)
			csq = "null";
		final int len = end - start, csqlen = csq.length();
		if (len < 0 || start > csqlen || end > csqlen)
			throw new IndexOutOfBoundsException();
		if (len == 0)
			return this;
		resizeBuffer(termLength + len);
		if (len > 4) {
			if (csq instanceof String) {
				((String) csq).getChars(start, end, termBuffer, termLength);
			} else if (csq instanceof StringBuilder) {
				((StringBuilder) csq).getChars(start, end, termBuffer,
						termLength);
			} else if (csq instanceof CharTermAttribute) {
				System.arraycopy(((CharTermAttribute) csq).buffer(), start,
						termBuffer, termLength, len);
			} else if (csq instanceof CharBuffer
					&& ((CharBuffer) csq).hasArray()) {
				final CharBuffer cb = (CharBuffer) csq;
				System.arraycopy(cb.array(), cb.arrayOffset() + cb.position()
						+ start, termBuffer, termLength, len);
			} else if (csq instanceof StringBuffer) {
				((StringBuffer) csq).getChars(start, end, termBuffer,
						termLength);
			} else {
				while (start < end)
					termBuffer[termLength++] = csq.charAt(start++);

				return this;
			}
			termLength += len;
			return this;
		} else {
			while (start < end)
				termBuffer[termLength++] = csq.charAt(start++);
			return this;
		}
	}

	public final CharTermAttribute append(char c) {
		resizeBuffer(termLength + 1)[termLength++] = c;
		return this;
	}

	public final CharTermAttribute append(String s) {
		if (s == null)
			return appendNull();
		final int len = s.length();
		s.getChars(0, len, resizeBuffer(termLength + len), termLength);
		termLength += len;
		return this;
	}

	public final CharTermAttribute append(StringBuilder s) {
		if (s == null)
			return appendNull();
		final int len = s.length();
		s.getChars(0, len, resizeBuffer(termLength + len), termLength);
		termLength += len;
		return this;
	}

	public final CharTermAttribute append(CharTermAttribute ta) {
		if (ta == null)
			return appendNull();
		final int len = ta.length();
		System.arraycopy(ta.buffer(), 0, resizeBuffer(termLength + len),
				termLength, len);
		termLength += len;
		return this;
	}

	private CharTermAttribute appendNull() {
		resizeBuffer(termLength + 4);
		termBuffer[termLength++] = 'n';
		termBuffer[termLength++] = 'u';
		termBuffer[termLength++] = 'l';
		termBuffer[termLength++] = 'l';
		return this;
	}

	@Override
	public int hashCode() {
		int code = termLength;
		code = code * 31 + ArrayUtil.hashCode(termBuffer, 0, termLength);
		return code;
	}

	@Override
	public void clear() {
		termLength = 0;
	}

	@Override
	public Object clone() {
		CharTermAttributeImpl t = (CharTermAttributeImpl) super.clone();

		t.termBuffer = new char[this.termLength];
		System.arraycopy(this.termBuffer, 0, t.termBuffer, 0, this.termLength);
		return t;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (other instanceof CharTermAttributeImpl) {
			final CharTermAttributeImpl o = ((CharTermAttributeImpl) other);
			if (termLength != o.termLength)
				return false;
			for (int i = 0; i < termLength; i++) {
				if (termBuffer[i] != o.termBuffer[i]) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public String toString() {

		return new String(termBuffer, 0, termLength);
	}

	@Override
	public void reflectWith(AttributeReflector reflector) {
		reflector.reflect(CharTermAttribute.class, "term", toString());
	}

	@Override
	public void copyTo(AttributeImpl target) {
		if (target instanceof CharTermAttribute) {
			CharTermAttribute t = (CharTermAttribute) target;
			t.copyBuffer(termBuffer, 0, termLength);
		} else {
			TermAttribute t = (TermAttribute) target;
			t.setTermBuffer(termBuffer, 0, termLength);
		}
	}

}
