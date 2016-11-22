/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import com.shatam.shatamindex.util.BytesRef;

public class ByteArrayDataOutput extends DataOutput {
	private byte[] bytes;

	private int pos;
	private int limit;

	public ByteArrayDataOutput(byte[] bytes) {
		reset(bytes);
	}

	public ByteArrayDataOutput(byte[] bytes, int offset, int len) {
		reset(bytes, offset, len);
	}

	public ByteArrayDataOutput() {
		reset(BytesRef.EMPTY_BYTES);
	}

	public void reset(byte[] bytes) {
		reset(bytes, 0, bytes.length);
	}

	public void reset(byte[] bytes, int offset, int len) {
		this.bytes = bytes;
		pos = offset;
		limit = offset + len;
	}

	public int getPosition() {
		return pos;
	}

	@Override
	public void writeByte(byte b) {
		assert pos < limit;
		bytes[pos++] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) {
		assert pos + length <= limit;
		System.arraycopy(b, offset, bytes, pos, length);
		pos += length;
	}
}
