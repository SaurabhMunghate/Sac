/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.shatam.shatamindex.store.DataInput;
import com.shatam.shatamindex.store.DataOutput;
import com.shatam.shatamindex.store.IndexInput;

public final class PagedBytes {
	private final List<byte[]> blocks = new ArrayList<byte[]>();
	private final List<Integer> blockEnd = new ArrayList<Integer>();
	private final int blockSize;
	private final int blockBits;
	private final int blockMask;
	private boolean didSkipBytes;
	private boolean frozen;
	private int upto;
	private byte[] currentBlock;

	private static final byte[] EMPTY_BYTES = new byte[0];

	public final static class Reader implements Closeable {
		private final byte[][] blocks;
		private final int[] blockEnds;
		private final int blockBits;
		private final int blockMask;
		private final int blockSize;
		private final CloseableThreadLocal<byte[]> threadBuffers = new CloseableThreadLocal<byte[]>();

		public Reader(PagedBytes pagedBytes) {
			blocks = new byte[pagedBytes.blocks.size()][];
			for (int i = 0; i < blocks.length; i++) {
				blocks[i] = pagedBytes.blocks.get(i);
			}
			blockEnds = new int[blocks.length];
			for (int i = 0; i < blockEnds.length; i++) {
				blockEnds[i] = pagedBytes.blockEnd.get(i);
			}
			blockBits = pagedBytes.blockBits;
			blockMask = pagedBytes.blockMask;
			blockSize = pagedBytes.blockSize;
		}

		public BytesRef fillSlice(BytesRef b, long start, int length) {
			assert length >= 0 : "length=" + length;
			final int index = (int) (start >> blockBits);
			final int offset = (int) (start & blockMask);
			b.length = length;
			if (blockSize - offset >= length) {

				b.bytes = blocks[index];
				b.offset = offset;
			} else {

				byte[] buffer = threadBuffers.get();
				if (buffer == null) {
					buffer = new byte[length];
					threadBuffers.set(buffer);
				} else if (buffer.length < length) {
					buffer = ArrayUtil.grow(buffer, length);
					threadBuffers.set(buffer);
				}
				b.bytes = buffer;
				b.offset = 0;
				System.arraycopy(blocks[index], offset, buffer, 0, blockSize
						- offset);
				System.arraycopy(blocks[1 + index], 0, buffer, blockSize
						- offset, length - (blockSize - offset));
			}
			return b;
		}

		public BytesRef fill(BytesRef b, long start) {
			final int index = (int) (start >> blockBits);
			final int offset = (int) (start & blockMask);
			final byte[] block = b.bytes = blocks[index];

			if ((block[offset] & 128) == 0) {
				b.length = block[offset];
				b.offset = offset + 1;
			} else {
				b.length = ((block[offset] & 0x7f) << 8)
						| (block[1 + offset] & 0xff);
				b.offset = offset + 2;
				assert b.length > 0;
			}
			return b;
		}

		public int fillAndGetIndex(BytesRef b, long start) {
			final int index = (int) (start >> blockBits);
			final int offset = (int) (start & blockMask);
			final byte[] block = b.bytes = blocks[index];

			if ((block[offset] & 128) == 0) {
				b.length = block[offset];
				b.offset = offset + 1;
			} else {
				b.length = ((block[offset] & 0x7f) << 8)
						| (block[1 + offset] & 0xff);
				b.offset = offset + 2;
				assert b.length > 0;
			}
			return index;
		}

		public long fillAndGetStart(BytesRef b, long start) {
			final int index = (int) (start >> blockBits);
			final int offset = (int) (start & blockMask);
			final byte[] block = b.bytes = blocks[index];

			if ((block[offset] & 128) == 0) {
				b.length = block[offset];
				b.offset = offset + 1;
				start += 1L + b.length;
			} else {
				b.length = ((block[offset] & 0x7f) << 8)
						| (block[1 + offset] & 0xff);
				b.offset = offset + 2;
				start += 2L + b.length;
				assert b.length > 0;
			}
			return start;
		}

		public BytesRef fillSliceWithPrefix(BytesRef b, long start) {
			final int index = (int) (start >> blockBits);
			int offset = (int) (start & blockMask);
			final byte[] block = blocks[index];
			final int length;
			if ((block[offset] & 128) == 0) {
				length = block[offset];
				offset = offset + 1;
			} else {
				length = ((block[offset] & 0x7f) << 8)
						| (block[1 + offset] & 0xff);
				offset = offset + 2;
				assert length > 0;
			}
			assert length >= 0 : "length=" + length;
			b.length = length;
			if (blockSize - offset >= length) {

				b.offset = offset;
				b.bytes = blocks[index];
			} else {

				byte[] buffer = threadBuffers.get();
				if (buffer == null) {
					buffer = new byte[length];
					threadBuffers.set(buffer);
				} else if (buffer.length < length) {
					buffer = ArrayUtil.grow(buffer, length);
					threadBuffers.set(buffer);
				}
				b.bytes = buffer;
				b.offset = 0;
				System.arraycopy(blocks[index], offset, buffer, 0, blockSize
						- offset);
				System.arraycopy(blocks[1 + index], 0, buffer, blockSize
						- offset, length - (blockSize - offset));
			}
			return b;
		}

		public byte[][] getBlocks() {
			return blocks;
		}

		public int[] getBlockEnds() {
			return blockEnds;
		}

		public void close() {
			threadBuffers.close();
		}
	}

	public PagedBytes(int blockBits) {
		this.blockSize = 1 << blockBits;
		this.blockBits = blockBits;
		blockMask = blockSize - 1;
		upto = blockSize;
	}

	public void copy(IndexInput in, long byteCount) throws IOException {
		while (byteCount > 0) {
			int left = blockSize - upto;
			if (left == 0) {
				if (currentBlock != null) {
					blocks.add(currentBlock);
					blockEnd.add(upto);
				}
				currentBlock = new byte[blockSize];
				upto = 0;
				left = blockSize;
			}
			if (left < byteCount) {
				in.readBytes(currentBlock, upto, left, false);
				upto = blockSize;
				byteCount -= left;
			} else {
				in.readBytes(currentBlock, upto, (int) byteCount, false);
				upto += byteCount;
				break;
			}
		}
	}

	public void copy(BytesRef bytes) throws IOException {
		int byteCount = bytes.length;
		int bytesUpto = bytes.offset;
		while (byteCount > 0) {
			int left = blockSize - upto;
			if (left == 0) {
				if (currentBlock != null) {
					blocks.add(currentBlock);
					blockEnd.add(upto);
				}
				currentBlock = new byte[blockSize];
				upto = 0;
				left = blockSize;
			}
			if (left < byteCount) {
				System.arraycopy(bytes.bytes, bytesUpto, currentBlock, upto,
						left);
				upto = blockSize;
				byteCount -= left;
				bytesUpto += left;
			} else {
				System.arraycopy(bytes.bytes, bytesUpto, currentBlock, upto,
						byteCount);
				upto += byteCount;
				break;
			}
		}
	}

	public void copy(BytesRef bytes, BytesRef out) throws IOException {
		int left = blockSize - upto;
		if (bytes.length > left || currentBlock == null) {
			if (currentBlock != null) {
				blocks.add(currentBlock);
				blockEnd.add(upto);
				didSkipBytes = true;
			}
			currentBlock = new byte[blockSize];
			upto = 0;
			left = blockSize;
			assert bytes.length <= blockSize;

		}

		out.bytes = currentBlock;
		out.offset = upto;
		out.length = bytes.length;

		System.arraycopy(bytes.bytes, bytes.offset, currentBlock, upto,
				bytes.length);
		upto += bytes.length;
	}

	public Reader freeze(boolean trim) {
		if (frozen) {
			throw new IllegalStateException("already frozen");
		}
		if (didSkipBytes) {
			throw new IllegalStateException(
					"cannot freeze when copy(BytesRef, BytesRef) was used");
		}
		if (trim && upto < blockSize) {
			final byte[] newBlock = new byte[upto];
			System.arraycopy(currentBlock, 0, newBlock, 0, upto);
			currentBlock = newBlock;
		}
		if (currentBlock == null) {
			currentBlock = EMPTY_BYTES;
		}
		blocks.add(currentBlock);
		blockEnd.add(upto);
		frozen = true;
		currentBlock = null;
		return new Reader(this);
	}

	public long getPointer() {
		if (currentBlock == null) {
			return 0;
		} else {
			return (blocks.size() * ((long) blockSize)) + upto;
		}
	}

	public long copyUsingLengthPrefix(BytesRef bytes) throws IOException {

		if (upto + bytes.length + 2 > blockSize) {
			if (bytes.length + 2 > blockSize) {
				throw new IllegalArgumentException("block size " + blockSize
						+ " is too small to store length " + bytes.length
						+ " bytes");
			}
			if (currentBlock != null) {
				blocks.add(currentBlock);
				blockEnd.add(upto);
			}
			currentBlock = new byte[blockSize];
			upto = 0;
		}

		final long pointer = getPointer();

		if (bytes.length < 128) {
			currentBlock[upto++] = (byte) bytes.length;
		} else {
			currentBlock[upto++] = (byte) (0x80 | (bytes.length >> 8));
			currentBlock[upto++] = (byte) (bytes.length & 0xff);
		}
		System.arraycopy(bytes.bytes, bytes.offset, currentBlock, upto,
				bytes.length);
		upto += bytes.length;

		return pointer;
	}

	public final class PagedBytesDataInput extends DataInput {
		private int currentBlockIndex;
		private int currentBlockUpto;
		private byte[] currentBlock;

		PagedBytesDataInput() {
			currentBlock = blocks.get(0);
		}

		@Override
		public Object clone() {
			PagedBytesDataInput clone = getDataInput();
			clone.setPosition(getPosition());
			return clone;
		}

		public long getPosition() {
			return currentBlockIndex * blockSize + currentBlockUpto;
		}

		public void setPosition(long pos) {
			currentBlockIndex = (int) (pos >> blockBits);
			currentBlock = blocks.get(currentBlockIndex);
			currentBlockUpto = (int) (pos & blockMask);
		}

		@Override
		public byte readByte() {
			if (currentBlockUpto == blockSize) {
				nextBlock();
			}
			return currentBlock[currentBlockUpto++];
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) {
			assert b.length >= offset + len;
			final int offsetEnd = offset + len;
			while (true) {
				final int blockLeft = blockSize - currentBlockUpto;
				final int left = offsetEnd - offset;
				if (blockLeft < left) {
					System.arraycopy(currentBlock, currentBlockUpto, b, offset,
							blockLeft);
					nextBlock();
					offset += blockLeft;
				} else {

					System.arraycopy(currentBlock, currentBlockUpto, b, offset,
							left);
					currentBlockUpto += left;
					break;
				}
			}
		}

		private void nextBlock() {
			currentBlockIndex++;
			currentBlockUpto = 0;
			currentBlock = blocks.get(currentBlockIndex);
		}
	}

	public final class PagedBytesDataOutput extends DataOutput {
		@Override
		public void writeByte(byte b) {
			if (upto == blockSize) {
				if (currentBlock != null) {
					blocks.add(currentBlock);
					blockEnd.add(upto);
				}
				currentBlock = new byte[blockSize];
				upto = 0;
			}
			currentBlock[upto++] = b;
		}

		@Override
		public void writeBytes(byte[] b, int offset, int length)
				throws IOException {
			assert b.length >= offset + length;
			if (length == 0) {
				return;
			}

			if (upto == blockSize) {
				if (currentBlock != null) {
					blocks.add(currentBlock);
					blockEnd.add(upto);
				}
				currentBlock = new byte[blockSize];
				upto = 0;
			}

			final int offsetEnd = offset + length;
			while (true) {
				final int left = offsetEnd - offset;
				final int blockLeft = blockSize - upto;
				if (blockLeft < left) {
					System.arraycopy(b, offset, currentBlock, upto, blockLeft);
					blocks.add(currentBlock);
					blockEnd.add(blockSize);
					currentBlock = new byte[blockSize];
					upto = 0;
					offset += blockLeft;
				} else {

					System.arraycopy(b, offset, currentBlock, upto, left);
					upto += left;
					break;
				}
			}
		}

		public long getPosition() {
			if (currentBlock == null) {
				return 0;
			} else {
				return blocks.size() * blockSize + upto;
			}
		}
	}

	public PagedBytesDataInput getDataInput() {
		if (!frozen) {
			throw new IllegalStateException(
					"must call freeze() before getDataInput");
		}
		return new PagedBytesDataInput();
	}

	public PagedBytesDataOutput getDataOutput() {
		if (frozen) {
			throw new IllegalStateException(
					"cannot get DataOutput after freeze()");
		}
		return new PagedBytesDataOutput();
	}
}
