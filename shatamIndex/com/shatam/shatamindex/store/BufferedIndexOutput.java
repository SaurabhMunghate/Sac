/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;

public abstract class BufferedIndexOutput extends IndexOutput {
	static final int BUFFER_SIZE = 16384;

	private final byte[] buffer = new byte[BUFFER_SIZE];
	private long bufferStart = 0;
	private int bufferPosition = 0;

	@Override
	public void writeByte(byte b) throws IOException {
		if (bufferPosition >= BUFFER_SIZE)
			flush();
		buffer[bufferPosition++] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		int bytesLeft = BUFFER_SIZE - bufferPosition;

		if (bytesLeft >= length) {

			System.arraycopy(b, offset, buffer, bufferPosition, length);
			bufferPosition += length;

			if (BUFFER_SIZE - bufferPosition == 0)
				flush();
		} else {

			if (length > BUFFER_SIZE) {

				if (bufferPosition > 0)
					flush();

				flushBuffer(b, offset, length);
				bufferStart += length;
			} else {

				int pos = 0;
				int pieceLength;
				while (pos < length) {
					pieceLength = (length - pos < bytesLeft) ? length - pos
							: bytesLeft;
					System.arraycopy(b, pos + offset, buffer, bufferPosition,
							pieceLength);
					pos += pieceLength;
					bufferPosition += pieceLength;

					bytesLeft = BUFFER_SIZE - bufferPosition;
					if (bytesLeft == 0) {
						flush();
						bytesLeft = BUFFER_SIZE;
					}
				}
			}
		}
	}

	@Override
	public void flush() throws IOException {
		flushBuffer(buffer, bufferPosition);
		bufferStart += bufferPosition;
		bufferPosition = 0;
	}

	private void flushBuffer(byte[] b, int len) throws IOException {
		flushBuffer(b, 0, len);
	}

	protected abstract void flushBuffer(byte[] b, int offset, int len)
			throws IOException;

	@Override
	public void close() throws IOException {
		flush();
	}

	@Override
	public long getFilePointer() {
		return bufferStart + bufferPosition;
	}

	@Override
	public void seek(long pos) throws IOException {
		flush();
		bufferStart = pos;
	}

	@Override
	public abstract long length() throws IOException;

}
