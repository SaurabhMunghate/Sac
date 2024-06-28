/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.Comparator;
import java.io.UnsupportedEncodingException;

public final class BytesRef implements Comparable<BytesRef> {

	static final int HASH_PRIME = 31;
	public static final byte[] EMPTY_BYTES = new byte[0];

	public byte[] bytes;

	public int offset;

	public int length;

	public BytesRef() {
		bytes = EMPTY_BYTES;
	}

	public BytesRef(byte[] bytes, int offset, int length) {
		assert bytes != null;
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
	}

	public BytesRef(byte[] bytes) {
		assert bytes != null;
		this.bytes = bytes;
		this.offset = 0;
		this.length = bytes.length;
	}

	public BytesRef(int capacity) {
		this.bytes = new byte[capacity];
	}

	public BytesRef(CharSequence text) {
		this();
		copy(text);
	}

	public BytesRef(char text[], int offset, int length) {
		this(length * 4);
		copy(text, offset, length);
	}

	public BytesRef(BytesRef other) {
		this();
		copy(other);
	}

	public void copy(CharSequence text) {
		UnicodeUtil.UTF16toUTF8(text, 0, text.length(), this);
	}

	public void copy(char text[], int offset, int length) {
		UnicodeUtil.UTF16toUTF8(text, offset, length, this);
	}

	public boolean bytesEquals(BytesRef other) {
		if (length == other.length) {
			int otherUpto = other.offset;
			final byte[] otherBytes = other.bytes;
			final int end = offset + length;
			for (int upto = offset; upto < end; upto++, otherUpto++) {
				if (bytes[upto] != otherBytes[otherUpto]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Object clone() {
		return new BytesRef(this);
	}

	private boolean sliceEquals(BytesRef other, int pos) {
		if (pos < 0 || length - pos < other.length) {
			return false;
		}
		int i = offset + pos;
		int j = other.offset;
		final int k = other.offset + other.length;

		while (j < k) {
			if (bytes[i++] != other.bytes[j++]) {
				return false;
			}
		}

		return true;
	}

	public boolean startsWith(BytesRef other) {
		return sliceEquals(other, 0);
	}

	public boolean endsWith(BytesRef other) {
		return sliceEquals(other, length - other.length);
	}

	@Override
	public int hashCode() {
		int result = 0;
		final int end = offset + length;
		for (int i = offset; i < end; i++) {
			result = HASH_PRIME * result + bytes[i];
		}
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		return this.bytesEquals((BytesRef) other);
	}

	public String utf8ToString() {
		try {
			return new String(bytes, offset, length, "UTF-8");
		} catch (UnsupportedEncodingException uee) {

			throw new RuntimeException(uee);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		final int end = offset + length;
		for (int i = offset; i < end; i++) {
			if (i > offset) {
				sb.append(' ');
			}
			sb.append(Integer.toHexString(bytes[i] & 0xff));
		}
		sb.append(']');
		return sb.toString();
	}

	public void copy(BytesRef other) {
		if (bytes.length < other.length) {
			bytes = new byte[other.length];
		}
		System.arraycopy(other.bytes, other.offset, bytes, 0, other.length);
		length = other.length;
		offset = 0;
	}

	public void append(BytesRef other) {
		int newLen = length + other.length;
		if (bytes.length < newLen) {
			byte[] newBytes = new byte[newLen];
			System.arraycopy(bytes, offset, newBytes, 0, length);
			offset = 0;
			bytes = newBytes;
		}
		System.arraycopy(other.bytes, other.offset, bytes, length + offset,
				other.length);
		length = newLen;
	}

	public void grow(int newLength) {
		bytes = ArrayUtil.grow(bytes, newLength);
	}

	public int compareTo(BytesRef other) {
		if (this == other)
			return 0;

		final byte[] aBytes = this.bytes;
		int aUpto = this.offset;
		final byte[] bBytes = other.bytes;
		int bUpto = other.offset;

		final int aStop = aUpto + Math.min(this.length, other.length);

		while (aUpto < aStop) {
			int aByte = aBytes[aUpto++] & 0xff;
			int bByte = bBytes[bUpto++] & 0xff;
			int diff = aByte - bByte;
			if (diff != 0)
				return diff;
		}

		return this.length - other.length;
	}

	private final static Comparator<BytesRef> utf8SortedAsUnicodeSortOrder = new UTF8SortedAsUnicodeComparator();

	public static Comparator<BytesRef> getUTF8SortedAsUnicodeComparator() {
		return utf8SortedAsUnicodeSortOrder;
	}

	private static class UTF8SortedAsUnicodeComparator implements
			Comparator<BytesRef> {

		private UTF8SortedAsUnicodeComparator() {
		};

		public int compare(BytesRef a, BytesRef b) {
			final byte[] aBytes = a.bytes;
			int aUpto = a.offset;
			final byte[] bBytes = b.bytes;
			int bUpto = b.offset;

			final int aStop;
			if (a.length < b.length) {
				aStop = aUpto + a.length;
			} else {
				aStop = aUpto + b.length;
			}

			while (aUpto < aStop) {
				int aByte = aBytes[aUpto++] & 0xff;
				int bByte = bBytes[bUpto++] & 0xff;

				int diff = aByte - bByte;
				if (diff != 0) {
					return diff;
				}
			}

			return a.length - b.length;
		}
	}

	private final static Comparator<BytesRef> utf8SortedAsUTF16SortOrder = new UTF8SortedAsUTF16Comparator();

	public static Comparator<BytesRef> getUTF8SortedAsUTF16Comparator() {
		return utf8SortedAsUTF16SortOrder;
	}

	private static class UTF8SortedAsUTF16Comparator implements
			Comparator<BytesRef> {

		private UTF8SortedAsUTF16Comparator() {
		};

		public int compare(BytesRef a, BytesRef b) {

			final byte[] aBytes = a.bytes;
			int aUpto = a.offset;
			final byte[] bBytes = b.bytes;
			int bUpto = b.offset;

			final int aStop;
			if (a.length < b.length) {
				aStop = aUpto + a.length;
			} else {
				aStop = aUpto + b.length;
			}

			while (aUpto < aStop) {
				int aByte = aBytes[aUpto++] & 0xff;
				int bByte = bBytes[bUpto++] & 0xff;

				if (aByte != bByte) {

					if (aByte >= 0xee && bByte >= 0xee) {
						if ((aByte & 0xfe) == 0xee) {
							aByte += 0xe;
						}
						if ((bByte & 0xfe) == 0xee) {
							bByte += 0xe;
						}
					}
					return aByte - bByte;
				}
			}

			return a.length - b.length;
		}
	}
}
