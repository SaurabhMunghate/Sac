/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.io.Closeable;

public abstract class IndexInput extends DataInput implements Cloneable,
		Closeable {

	@Deprecated
	public void skipChars(int length) throws IOException {
		for (int i = 0; i < length; i++) {
			byte b = readByte();
			if ((b & 0x80) == 0) {

			} else if ((b & 0xE0) != 0xE0) {
				readByte();
			} else {

				readByte();
				readByte();
			}
		}
	}

	private final String resourceDescription;

	@Deprecated
	protected IndexInput() {
		this("anonymous IndexInput");
	}

	protected IndexInput(String resourceDescription) {
		if (resourceDescription == null) {
			throw new IllegalArgumentException(
					"resourceDescription must not be null");
		}
		this.resourceDescription = resourceDescription;
	}

	public abstract void close() throws IOException;

	public abstract long getFilePointer();

	public abstract void seek(long pos) throws IOException;

	public abstract long length();

	public void copyBytes(IndexOutput out, long numBytes) throws IOException {
		assert numBytes >= 0 : "numBytes=" + numBytes;

		byte copyBuf[] = new byte[BufferedIndexInput.BUFFER_SIZE];

		while (numBytes > 0) {
			final int toCopy = (int) (numBytes > copyBuf.length ? copyBuf.length
					: numBytes);
			readBytes(copyBuf, 0, toCopy);
			out.writeBytes(copyBuf, 0, toCopy);
			numBytes -= toCopy;
		}
	}

	@Override
	public String toString() {
		return resourceDescription;
	}
}
