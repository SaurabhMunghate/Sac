/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.Comparator;

public final class CharsRef implements Comparable<CharsRef>, CharSequence {
	private static final char[] EMPTY_ARRAY = new char[0];
	public char[] chars;
	public int offset;
	public int length;

	public CharsRef() {
		this(EMPTY_ARRAY, 0, 0);
	}

	public CharsRef(int capacity) {
		chars = new char[capacity];
	}

	public CharsRef(char[] chars, int offset, int length) {
		assert chars != null;
		assert chars.length >= offset + length;
		this.chars = chars;
		this.offset = offset;
		this.length = length;
	}

	public CharsRef(String string) {
		this.chars = string.toCharArray();
		this.offset = 0;
		this.length = chars.length;
	}

	public CharsRef(CharsRef other) {
		copy(other);
	}

	@Override
	public Object clone() {
		return new CharsRef(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		final int end = offset + length;
		for (int i = offset; i < end; i++) {
			result = prime * result + chars[i];
		}
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof CharsRef) {
			return charsEquals((CharsRef) other);
		}

		if (other instanceof CharSequence) {
			final CharSequence seq = (CharSequence) other;
			if (length == seq.length()) {
				int n = length;
				int i = offset;
				int j = 0;
				while (n-- != 0) {
					if (chars[i++] != seq.charAt(j++))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	public boolean charsEquals(CharsRef other) {
		if (length == other.length) {
			int otherUpto = other.offset;
			final char[] otherChars = other.chars;
			final int end = offset + length;
			for (int upto = offset; upto < end; upto++, otherUpto++) {
				if (chars[upto] != otherChars[otherUpto]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public int compareTo(CharsRef other) {
		if (this == other)
			return 0;

		final char[] aChars = this.chars;
		int aUpto = this.offset;
		final char[] bChars = other.chars;
		int bUpto = other.offset;

		final int aStop = aUpto + Math.min(this.length, other.length);

		while (aUpto < aStop) {
			int aInt = aChars[aUpto++];
			int bInt = bChars[bUpto++];
			if (aInt > bInt) {
				return 1;
			} else if (aInt < bInt) {
				return -1;
			}
		}

		return this.length - other.length;
	}

	public void copy(CharsRef other) {
		if (chars == null) {
			chars = new char[other.length];
		} else {
			chars = ArrayUtil.grow(chars, other.length);
		}
		System.arraycopy(other.chars, other.offset, chars, 0, other.length);
		length = other.length;
		offset = 0;
	}

	public void grow(int newLength) {
		if (chars.length < newLength) {
			chars = ArrayUtil.grow(chars, newLength);
		}
	}

	public void copy(char[] otherChars, int otherOffset, int otherLength) {
		grow(otherLength);
		System.arraycopy(otherChars, otherOffset, this.chars, 0, otherLength);
		this.offset = 0;
		this.length = otherLength;
	}

	public void append(char[] otherChars, int otherOffset, int otherLength) {
		final int newLength = length + otherLength;
		grow(this.offset + newLength);
		System.arraycopy(otherChars, otherOffset, this.chars, this.offset
				+ length, otherLength);
		this.length += otherLength;
	}

	@Override
	public String toString() {
		return new String(chars, offset, length);
	}

	public int length() {
		return length;
	}

	public char charAt(int index) {
		return chars[offset + index];
	}

	public CharSequence subSequence(int start, int end) {
		return new CharsRef(chars, offset + start, offset + end - 1);
	}

	private final static Comparator<CharsRef> utf16SortedAsUTF8SortOrder = new UTF16SortedAsUTF8Comparator();

	public static Comparator<CharsRef> getUTF16SortedAsUTF8Comparator() {
		return utf16SortedAsUTF8SortOrder;
	}

	private static class UTF16SortedAsUTF8Comparator implements
			Comparator<CharsRef> {

		private UTF16SortedAsUTF8Comparator() {
		};

		public int compare(CharsRef a, CharsRef b) {
			if (a == b)
				return 0;

			final char[] aChars = a.chars;
			int aUpto = a.offset;
			final char[] bChars = b.chars;
			int bUpto = b.offset;

			final int aStop = aUpto + Math.min(a.length, b.length);

			while (aUpto < aStop) {
				char aChar = aChars[aUpto++];
				char bChar = bChars[bUpto++];
				if (aChar != bChar) {

					if (aChar >= 0xd800 && bChar >= 0xd800) {
						if (aChar >= 0xe000) {
							aChar -= 0x800;
						} else {
							aChar += 0x2000;
						}

						if (bChar >= 0xe000) {
							bChar -= 0x800;
						} else {
							bChar += 0x2000;
						}
					}

					return (int) aChar - (int) bChar;
				}
			}

			return a.length - b.length;
		}
	}
}