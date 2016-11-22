/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;

import java.io.IOException;

final class ByteSliceReader extends IndexInput {
	ByteBlockPool pool;
	int bufferUpto;
	byte[] buffer;
	public int upto;
	int limit;
	int level;
	public int bufferOffset;

	public int endIndex;

	public void init(ByteBlockPool pool, int startIndex, int endIndex) {

		assert endIndex - startIndex >= 0;
		assert startIndex >= 0;
		assert endIndex >= 0;

		this.pool = pool;
		this.endIndex = endIndex;

		level = 0;
		bufferUpto = startIndex / DocumentsWriter.BYTE_BLOCK_SIZE;
		bufferOffset = bufferUpto * DocumentsWriter.BYTE_BLOCK_SIZE;
		buffer = pool.buffers[bufferUpto];
		upto = startIndex & DocumentsWriter.BYTE_BLOCK_MASK;

		final int firstSize = ByteBlockPool.levelSizeArray[0];

		if (startIndex + firstSize >= endIndex) {

			limit = endIndex & DocumentsWriter.BYTE_BLOCK_MASK;
		} else
			limit = upto + firstSize - 4;
	}

	public boolean eof() {
		assert upto + bufferOffset <= endIndex;
		return upto + bufferOffset == endIndex;
	}

	@Override
	public byte readByte() {
		assert !eof();
		assert upto <= limit;
		if (upto == limit)
			nextSlice();
		return buffer[upto++];
	}

	public long writeTo(IndexOutput out) throws IOException {
		long size = 0;
		while (true) {
			if (limit + bufferOffset == endIndex) {
				assert endIndex - bufferOffset >= upto;
				out.writeBytes(buffer, upto, limit - upto);
				size += limit - upto;
				break;
			} else {
				out.writeBytes(buffer, upto, limit - upto);
				size += limit - upto;
				nextSlice();
			}
		}

		return size;
	}

	public void nextSlice() {

		final int nextIndex = ((buffer[limit] & 0xff) << 24)
				+ ((buffer[1 + limit] & 0xff) << 16)
				+ ((buffer[2 + limit] & 0xff) << 8)
				+ (buffer[3 + limit] & 0xff);

		level = ByteBlockPool.nextLevelArray[level];
		final int newSize = ByteBlockPool.levelSizeArray[level];

		bufferUpto = nextIndex / DocumentsWriter.BYTE_BLOCK_SIZE;
		bufferOffset = bufferUpto * DocumentsWriter.BYTE_BLOCK_SIZE;

		buffer = pool.buffers[bufferUpto];
		upto = nextIndex & DocumentsWriter.BYTE_BLOCK_MASK;

		if (nextIndex + newSize >= endIndex) {

			assert endIndex - nextIndex > 0;
			limit = endIndex - bufferOffset;
		} else {

			limit = upto + newSize - 4;
		}
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) {
		while (len > 0) {
			final int numLeft = limit - upto;
			if (numLeft < len) {

				System.arraycopy(buffer, upto, b, offset, numLeft);
				offset += numLeft;
				len -= numLeft;
				nextSlice();
			} else {

				System.arraycopy(buffer, upto, b, offset, len);
				upto += len;
				break;
			}
		}
	}

	@Override
	public long getFilePointer() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public long length() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void seek(long pos) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void close() {
		throw new RuntimeException("not implemented");
	}
}
