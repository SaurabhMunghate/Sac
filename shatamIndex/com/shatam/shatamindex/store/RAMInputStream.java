/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.io.EOFException;

public class RAMInputStream extends IndexInput implements Cloneable {
	static final int BUFFER_SIZE = RAMOutputStream.BUFFER_SIZE;

	private RAMFile file;
	private long length;

	private byte[] currentBuffer;
	private int currentBufferIndex;

	private int bufferPosition;
	private long bufferStart;
	private int bufferLength;

	@Deprecated
	public RAMInputStream(RAMFile f) throws IOException {
		this("anonymous", f);
	}

	public RAMInputStream(String name, RAMFile f) throws IOException {
		super("RAMInputStream(name=" + name + ")");
		file = f;
		length = file.length;
		if (length / BUFFER_SIZE >= Integer.MAX_VALUE) {
			throw new IOException("RAMInputStream too large length=" + length
					+ ": " + name);
		}

		currentBufferIndex = -1;
		currentBuffer = null;
	}

	@Override
	public void close() {

	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public byte readByte() throws IOException {
		if (bufferPosition >= bufferLength) {
			currentBufferIndex++;
			switchCurrentBuffer(true);
		}
		return currentBuffer[bufferPosition++];
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		while (len > 0) {
			if (bufferPosition >= bufferLength) {
				currentBufferIndex++;
				switchCurrentBuffer(true);
			}

			int remainInBuffer = bufferLength - bufferPosition;
			int bytesToCopy = len < remainInBuffer ? len : remainInBuffer;
			System.arraycopy(currentBuffer, bufferPosition, b, offset,
					bytesToCopy);
			offset += bytesToCopy;
			len -= bytesToCopy;
			bufferPosition += bytesToCopy;
		}
	}

	private final void switchCurrentBuffer(boolean enforceEOF)
			throws IOException {
		bufferStart = (long) BUFFER_SIZE * (long) currentBufferIndex;
		if (currentBufferIndex >= file.numBuffers()) {

			if (enforceEOF) {
				throw new EOFException("Read past EOF (resource: " + this + ")");
			} else {

				currentBufferIndex--;
				bufferPosition = BUFFER_SIZE;
			}
		} else {
			currentBuffer = file.getBuffer(currentBufferIndex);
			bufferPosition = 0;
			long buflen = length - bufferStart;
			bufferLength = buflen > BUFFER_SIZE ? BUFFER_SIZE : (int) buflen;
		}
	}

	@Override
	public void copyBytes(IndexOutput out, long numBytes) throws IOException {
		assert numBytes >= 0 : "numBytes=" + numBytes;

		long left = numBytes;
		while (left > 0) {
			if (bufferPosition == bufferLength) {
				++currentBufferIndex;
				switchCurrentBuffer(true);
			}

			final int bytesInBuffer = bufferLength - bufferPosition;
			final int toCopy = (int) (bytesInBuffer < left ? bytesInBuffer
					: left);
			out.writeBytes(currentBuffer, bufferPosition, toCopy);
			bufferPosition += toCopy;
			left -= toCopy;
		}

		assert left == 0 : "Insufficient bytes to copy: numBytes=" + numBytes
				+ " copied=" + (numBytes - left);
	}

	@Override
	public long getFilePointer() {
		return currentBufferIndex < 0 ? 0 : bufferStart + bufferPosition;
	}

	@Override
	public void seek(long pos) throws IOException {
		if (currentBuffer == null || pos < bufferStart
				|| pos >= bufferStart + BUFFER_SIZE) {
			currentBufferIndex = (int) (pos / BUFFER_SIZE);
			switchCurrentBuffer(false);
		}
		bufferPosition = (int) (pos % BUFFER_SIZE);
	}
}
