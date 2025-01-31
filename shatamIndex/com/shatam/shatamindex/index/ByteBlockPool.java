/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.Arrays;
import java.util.List;

import static com.shatam.shatamindex.util.RamUsageEstimator.NUM_BYTES_OBJECT_REF;

import com.shatam.shatamindex.util.ArrayUtil;

final class ByteBlockPool {

	abstract static class Allocator {
		abstract void recycleByteBlocks(byte[][] blocks, int start, int end);

		abstract void recycleByteBlocks(List<byte[]> blocks);

		abstract byte[] getByteBlock();
	}

	public byte[][] buffers = new byte[10][];

	int bufferUpto = -1;
	public int byteUpto = DocumentsWriter.BYTE_BLOCK_SIZE;

	public byte[] buffer;
	public int byteOffset = -DocumentsWriter.BYTE_BLOCK_SIZE;

	private final Allocator allocator;

	public ByteBlockPool(Allocator allocator) {
		this.allocator = allocator;
	}

	public void reset() {
		if (bufferUpto != -1) {

			for (int i = 0; i < bufferUpto; i++)

				Arrays.fill(buffers[i], (byte) 0);

			Arrays.fill(buffers[bufferUpto], 0, byteUpto, (byte) 0);

			if (bufferUpto > 0)

				allocator.recycleByteBlocks(buffers, 1, 1 + bufferUpto);

			bufferUpto = 0;
			byteUpto = 0;
			byteOffset = 0;
			buffer = buffers[0];
		}
	}

	public void nextBuffer() {
		if (1 + bufferUpto == buffers.length) {
			byte[][] newBuffers = new byte[ArrayUtil.oversize(
					buffers.length + 1, NUM_BYTES_OBJECT_REF)][];
			System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
			buffers = newBuffers;
		}
		buffer = buffers[1 + bufferUpto] = allocator.getByteBlock();
		bufferUpto++;

		byteUpto = 0;
		byteOffset += DocumentsWriter.BYTE_BLOCK_SIZE;
	}

	public int newSlice(final int size) {
		if (byteUpto > DocumentsWriter.BYTE_BLOCK_SIZE - size)
			nextBuffer();
		final int upto = byteUpto;
		byteUpto += size;
		buffer[byteUpto - 1] = 16;
		return upto;
	}

	final static int[] nextLevelArray = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 9 };
	final static int[] levelSizeArray = { 5, 14, 20, 30, 40, 40, 80, 80, 120,
			200 };
	final static int FIRST_LEVEL_SIZE = levelSizeArray[0];

	public int allocSlice(final byte[] slice, final int upto) {

		final int level = slice[upto] & 15;
		final int newLevel = nextLevelArray[level];
		final int newSize = levelSizeArray[newLevel];

		if (byteUpto > DocumentsWriter.BYTE_BLOCK_SIZE - newSize)
			nextBuffer();

		final int newUpto = byteUpto;
		final int offset = newUpto + byteOffset;
		byteUpto += newSize;

		buffer[newUpto] = slice[upto - 3];
		buffer[newUpto + 1] = slice[upto - 2];
		buffer[newUpto + 2] = slice[upto - 1];

		slice[upto - 3] = (byte) (offset >>> 24);
		slice[upto - 2] = (byte) (offset >>> 16);
		slice[upto - 1] = (byte) (offset >>> 8);
		slice[upto] = (byte) offset;

		buffer[byteUpto - 1] = (byte) (16 | newLevel);

		return newUpto + 3;
	}
}
