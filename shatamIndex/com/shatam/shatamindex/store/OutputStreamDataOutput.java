/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.*;

public class OutputStreamDataOutput extends DataOutput implements Closeable {
	private final OutputStream os;

	public OutputStreamDataOutput(OutputStream os) {
		this.os = os;
	}

	@Override
	public void writeByte(byte b) throws IOException {
		os.write(b);
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		os.write(b, offset, length);
	}

	public void close() throws IOException {
		os.close();
	}
}
