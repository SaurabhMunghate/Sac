/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.concurrent.atomic.AtomicLong;

import com.shatam.shatamindex.util.ByteBlockPool.Allocator;

public final class RecyclingByteBlockAllocator extends ByteBlockPool.Allocator {
	private byte[][] freeByteBlocks;
	private final int maxBufferedBlocks;
	private int freeBlocks = 0;
	private final AtomicLong bytesUsed;
	public static final int DEFAULT_BUFFERED_BLOCKS = 64;

	public RecyclingByteBlockAllocator(int blockSize, int maxBufferedBlocks,
			AtomicLong bytesUsed) {
		super(blockSize);
		freeByteBlocks = new byte[Math.min(10, maxBufferedBlocks)][];
		this.maxBufferedBlocks = maxBufferedBlocks;
		this.bytesUsed = bytesUsed;
	}

	public RecyclingByteBlockAllocator(int blockSize, int maxBufferedBlocks) {
		this(blockSize, maxBufferedBlocks, new AtomicLong());
	}

	public RecyclingByteBlockAllocator() {
		this(ByteBlockPool.BYTE_BLOCK_SIZE, 64, new AtomicLong());
	}

	@Override
	public synchronized byte[] getByteBlock() {
		if (freeBlocks == 0) {
			bytesUsed.addAndGet(blockSize);
			return new byte[blockSize];
		}
		final byte[] b = freeByteBlocks[--freeBlocks];
		freeByteBlocks[freeBlocks] = null;
		return b;
	}

	@Override
	public synchronized void recycleByteBlocks(byte[][] blocks, int start,
			int end) {
		final int numBlocks = Math.min(maxBufferedBlocks - freeBlocks, end
				- start);
		final int size = freeBlocks + numBlocks;
		if (size >= freeByteBlocks.length) {
			final byte[][] newBlocks = new byte[ArrayUtil.oversize(size,
					RamUsageEstimator.NUM_BYTES_OBJECT_REF)][];
			System.arraycopy(freeByteBlocks, 0, newBlocks, 0, freeBlocks);
			freeByteBlocks = newBlocks;
		}
		final int stop = start + numBlocks;
		for (int i = start; i < stop; i++) {
			freeByteBlocks[freeBlocks++] = blocks[i];
			blocks[i] = null;
		}
		for (int i = stop; i < end; i++) {
			blocks[i] = null;
		}
		bytesUsed.addAndGet(-(end - stop) * blockSize);
		assert bytesUsed.get() >= 0;
	}

	public synchronized int numBufferedBlocks() {
		return freeBlocks;
	}

	public long bytesUsed() {
		return bytesUsed.get();
	}

	public int maxBufferedBlocks() {
		return maxBufferedBlocks;
	}

	public synchronized int freeBlocks(int num) {
		assert num >= 0;
		final int stop;
		final int count;
		if (num > freeBlocks) {
			stop = 0;
			count = freeBlocks;
		} else {
			stop = freeBlocks - num;
			count = num;
		}
		while (freeBlocks > stop) {
			freeByteBlocks[--freeBlocks] = null;
		}
		bytesUsed.addAndGet(-count * blockSize);
		assert bytesUsed.get() >= 0;
		return count;
	}
}