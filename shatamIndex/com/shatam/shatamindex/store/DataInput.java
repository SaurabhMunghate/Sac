/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class DataInput implements Cloneable {

	private boolean preUTF8Strings;

	public void setModifiedUTF8StringsMode() {
		preUTF8Strings = true;
	}

	public abstract byte readByte() throws IOException;

	public abstract void readBytes(byte[] b, int offset, int len)
			throws IOException;

	public void readBytes(byte[] b, int offset, int len, boolean useBuffer)
			throws IOException {

		readBytes(b, offset, len);
	}

	public short readShort() throws IOException {
		return (short) (((readByte() & 0xFF) << 8) | (readByte() & 0xFF));
	}

	public int readInt() throws IOException {
		return ((readByte() & 0xFF) << 24) | ((readByte() & 0xFF) << 16)
				| ((readByte() & 0xFF) << 8) | (readByte() & 0xFF);
	}

	public int readVInt() throws IOException {

		byte b = readByte();
		int i = b & 0x7F;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7F) << 7;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7F) << 14;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7F) << 21;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		assert (b & 0x80) == 0;
		return i | ((b & 0x7F) << 28);
	}

	public long readLong() throws IOException {
		return (((long) readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
	}

	public long readVLong() throws IOException {

		byte b = readByte();
		long i = b & 0x7FL;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 7;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 14;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 21;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 28;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 35;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 42;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		i |= (b & 0x7FL) << 49;
		if ((b & 0x80) == 0)
			return i;
		b = readByte();
		assert (b & 0x80) == 0;
		return i | ((b & 0x7FL) << 56);
	}

	public String readString() throws IOException {
		if (preUTF8Strings)
			return readModifiedUTF8String();
		int length = readVInt();
		final byte[] bytes = new byte[length];
		readBytes(bytes, 0, length);
		return new String(bytes, 0, length, "UTF-8");
	}

	private String readModifiedUTF8String() throws IOException {
		int length = readVInt();
		final char[] chars = new char[length];
		readChars(chars, 0, length);
		return new String(chars, 0, length);
	}

	@Deprecated
	public void readChars(char[] buffer, int start, int length)
			throws IOException {
		final int end = start + length;
		for (int i = start; i < end; i++) {
			byte b = readByte();
			if ((b & 0x80) == 0)
				buffer[i] = (char) (b & 0x7F);
			else if ((b & 0xE0) != 0xE0) {
				buffer[i] = (char) (((b & 0x1F) << 6) | (readByte() & 0x3F));
			} else {
				buffer[i] = (char) (((b & 0x0F) << 12)
						| ((readByte() & 0x3F) << 6) | (readByte() & 0x3F));
			}
		}
	}

	@Override
	public Object clone() {
		DataInput clone = null;
		try {
			clone = (DataInput) super.clone();
		} catch (CloneNotSupportedException e) {
		}

		return clone;
	}

	public Map<String, String> readStringStringMap() throws IOException {
		final Map<String, String> map = new HashMap<String, String>();
		final int count = readInt();
		for (int i = 0; i < count; i++) {
			final String key = readString();
			final String val = readString();
			map.put(key, val);
		}

		return map;
	}
}
