/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.shatam.shatamindex.store.DataOutput;

import static com.shatam.shatamindex.util.RamUsageEstimator.NUM_BYTES_OBJECT_REF;

public final class ByteBlockPool {
	public final static int BYTE_BLOCK_SHIFT = 15;
	public final static int BYTE_BLOCK_SIZE = 1 << BYTE_BLOCK_SHIFT;
	public final static int BYTE_BLOCK_MASK = BYTE_BLOCK_SIZE - 1;

	public abstract static class Allocator {
		protected final int blockSize;

		public Allocator(int blockSize) {
			this.blockSize = blockSize;
		}

		public abstract void recycleByteBlocks(byte[][] blocks, int start,
				int end);

		public void recycleByteBlocks(List<byte[]> blocks) {
			final byte[][] b = blocks.toArray(new byte[blocks.size()][]);
			recycleByteBlocks(b, 0, b.length);
		}

		public byte[] getByteBlock() {
			return new byte[blockSize];
		}
	}

	public static final class DirectAllocator extends Allocator {

		public DirectAllocator() {
			this(BYTE_BLOCK_SIZE);
		}

		public DirectAllocator(int blockSize) {
			super(blockSize);
		}

		@Override
		public void recycleByteBlocks(byte[][] blocks, int start, int end) {
		}

	}

	public static class DirectTrackingAllocator extends Allocator {
		private final AtomicLong bytesUsed;

		public DirectTrackingAllocator(AtomicLong bytesUsed) {
			this(BYTE_BLOCK_SIZE, bytesUsed);
		}

		public DirectTrackingAllocator(int blockSize, AtomicLong bytesUsed) {
			super(blockSize);
			this.bytesUsed = bytesUsed;
		}

		public byte[] getByteBlock() {
			bytesUsed.addAndGet(blockSize);
			return new byte[blockSize];
		}

		@Override
		public void recycleByteBlocks(byte[][] blocks, int start, int end) {
			bytesUsed.addAndGet(-((end - start) * blockSize));
			for (int i = start; i < end; i++) {
				blocks[i] = null;
			}
		}

	};

	public byte[][] buffers = new byte[10][];

	int bufferUpto = -1;
	public int byteUpto = BYTE_BLOCK_SIZE;

	public byte[] buffer;
	public int byteOffset = -BYTE_BLOCK_SIZE;

	private final Allocator allocator;

	public ByteBlockPool(Allocator allocator) {
		this.allocator = allocator;
	}

	public void dropBuffersAndReset() {
		if (bufferUpto != -1) {

			allocator.recycleByteBlocks(buffers, 0, 1 + bufferUpto);

			bufferUpto = -1;
			byteUpto = BYTE_BLOCK_SIZE;
			byteOffset = -BYTE_BLOCK_SIZE;
			buffers = new byte[10][];
			buffer = null;
		}
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
		byteOffset += BYTE_BLOCK_SIZE;
	}

	public int newSlice(final int size) {
		if (byteUpto > BYTE_BLOCK_SIZE - size)
			nextBuffer();
		final int upto = byteUpto;
		byteUpto += size;
		buffer[byteUpto - 1] = 16;
		return upto;
	}

	public final static int[] nextLevelArray = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 9 };
	public final static int[] levelSizeArray = { 5, 14, 20, 30, 40, 40, 80, 80,
			120, 200 };
	public final static int FIRST_LEVEL_SIZE = levelSizeArray[0];

	public int allocSlice(final byte[] slice, final int upto) {

		final int level = slice[upto] & 15;
		final int newLevel = nextLevelArray[level];
		final int newSize = levelSizeArray[newLevel];

		if (byteUpto > BYTE_BLOCK_SIZE - newSize)
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

	public final BytesRef setBytesRef(BytesRef term, int textStart) {
		final byte[] bytes = term.bytes = buffers[textStart >> BYTE_BLOCK_SHIFT];
		int pos = textStart & BYTE_BLOCK_MASK;
		if ((bytes[pos] & 0x80) == 0) {

			term.length = bytes[pos];
			term.offset = pos + 1;
		} else {

			term.length = (bytes[pos] & 0x7f) + ((bytes[pos + 1] & 0xff) << 7);
			term.offset = pos + 2;
		}
		assert term.length >= 0;
		return term;
	}

	public final void copy(final BytesRef bytes) {
		int length = bytes.length;
		int offset = bytes.offset;
		int overflow = (length + byteUpto) - BYTE_BLOCK_SIZE;
		do {
			if (overflow <= 0) {
				System.arraycopy(bytes.bytes, offset, buffer, byteUpto, length);
				byteUpto += length;
				break;
			} else {
				final int bytesToCopy = length - overflow;
				System.arraycopy(bytes.bytes, offset, buffer, byteUpto,
						bytesToCopy);
				offset += bytesToCopy;
				length -= bytesToCopy;
				nextBuffer();
				overflow = overflow - BYTE_BLOCK_SIZE;
			}
		} while (true);
	}

	public final void writePool(final DataOutput out) throws IOException {
		int bytesOffset = byteOffset;
		int block = 0;
		while (bytesOffset > 0) {
			out.writeBytes(buffers[block++], BYTE_BLOCK_SIZE);
			bytesOffset -= BYTE_BLOCK_SIZE;
		}
		out.writeBytes(buffers[block], byteUpto);
	}
}
