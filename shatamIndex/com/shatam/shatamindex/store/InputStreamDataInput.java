/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.*;

import com.shatam.shatamindex.store.DataInput;

public class InputStreamDataInput extends DataInput implements Closeable {
	private final InputStream is;

	public InputStreamDataInput(InputStream is) {
		this.is = is;
	}

	@Override
	public byte readByte() throws IOException {
		int v = is.read();
		if (v == -1)
			throw new EOFException();
		return (byte) v;
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		while (len > 0) {
			final int cnt = is.read(b, offset, len);
			if (cnt < 0) {

				throw new EOFException();
			}
			len -= cnt;
			offset += cnt;
		}
	}

	public void close() throws IOException {
		is.close();
	}
}
