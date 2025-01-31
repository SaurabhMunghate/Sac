/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;

public class RAMOutputStream extends IndexOutput {
	static final int BUFFER_SIZE = 1024;

	private RAMFile file;

	private byte[] currentBuffer;
	private int currentBufferIndex;

	private int bufferPosition;
	private long bufferStart;
	private int bufferLength;

	public RAMOutputStream() {
		this(new RAMFile());
	}

	public RAMOutputStream(RAMFile f) {
		file = f;

		currentBufferIndex = -1;
		currentBuffer = null;
	}

	public void writeTo(IndexOutput out) throws IOException {
		flush();
		final long end = file.length;
		long pos = 0;
		int buffer = 0;
		while (pos < end) {
			int length = BUFFER_SIZE;
			long nextPos = pos + length;
			if (nextPos > end) {
				length = (int) (end - pos);
			}
			out.writeBytes(file.getBuffer(buffer++), length);
			pos = nextPos;
		}
	}

	public void reset() {
		currentBuffer = null;
		currentBufferIndex = -1;
		bufferPosition = 0;
		bufferStart = 0;
		bufferLength = 0;
		file.setLength(0);
	}

	@Override
	public void close() throws IOException {
		flush();
	}

	@Override
	public void seek(long pos) throws IOException {

		setFileLength();
		if (pos < bufferStart || pos >= bufferStart + bufferLength) {
			currentBufferIndex = (int) (pos / BUFFER_SIZE);
			switchCurrentBuffer();
		}

		bufferPosition = (int) (pos % BUFFER_SIZE);
	}

	@Override
	public long length() {
		return file.length;
	}

	@Override
	public void writeByte(byte b) throws IOException {
		if (bufferPosition == bufferLength) {
			currentBufferIndex++;
			switchCurrentBuffer();
		}
		currentBuffer[bufferPosition++] = b;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int len) throws IOException {
		assert b != null;
		while (len > 0) {
			if (bufferPosition == bufferLength) {
				currentBufferIndex++;
				switchCurrentBuffer();
			}

			int remainInBuffer = currentBuffer.length - bufferPosition;
			int bytesToCopy = len < remainInBuffer ? len : remainInBuffer;
			System.arraycopy(b, offset, currentBuffer, bufferPosition,
					bytesToCopy);
			offset += bytesToCopy;
			len -= bytesToCopy;
			bufferPosition += bytesToCopy;
		}
	}

	private final void switchCurrentBuffer() throws IOException {
		if (currentBufferIndex == file.numBuffers()) {
			currentBuffer = file.addBuffer(BUFFER_SIZE);
		} else {
			currentBuffer = file.getBuffer(currentBufferIndex);
		}
		bufferPosition = 0;
		bufferStart = (long) BUFFER_SIZE * (long) currentBufferIndex;
		bufferLength = currentBuffer.length;
	}

	private void setFileLength() {
		long pointer = bufferStart + bufferPosition;
		if (pointer > file.length) {
			file.setLength(pointer);
		}
	}

	@Override
	public void flush() throws IOException {
		file.setLastModified(System.currentTimeMillis());
		setFileLength();
	}

	@Override
	public long getFilePointer() {
		return currentBufferIndex < 0 ? 0 : bufferStart + bufferPosition;
	}

	public long sizeInBytes() {
		return file.numBuffers() * BUFFER_SIZE;
	}

	@Override
	public void copyBytes(DataInput input, long numBytes) throws IOException {
		assert numBytes >= 0 : "numBytes=" + numBytes;

		while (numBytes > 0) {
			if (bufferPosition == bufferLength) {
				currentBufferIndex++;
				switchCurrentBuffer();
			}

			int toCopy = currentBuffer.length - bufferPosition;
			if (numBytes < toCopy) {
				toCopy = (int) numBytes;
			}
			input.readBytes(currentBuffer, bufferPosition, toCopy, false);
			numBytes -= toCopy;
			bufferPosition += toCopy;
		}

	}

}
