/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

final class ByteSliceWriter {

	private byte[] slice;
	private int upto;
	private final ByteBlockPool pool;

	int offset0;

	public ByteSliceWriter(ByteBlockPool pool) {
		this.pool = pool;
	}

	public void init(int address) {
		slice = pool.buffers[address >> DocumentsWriter.BYTE_BLOCK_SHIFT];
		assert slice != null;
		upto = address & DocumentsWriter.BYTE_BLOCK_MASK;
		offset0 = address;
		assert upto < slice.length;
	}

	public void writeByte(byte b) {
		assert slice != null;
		if (slice[upto] != 0) {
			upto = pool.allocSlice(slice, upto);
			slice = pool.buffer;
			offset0 = pool.byteOffset;
			assert slice != null;
		}
		slice[upto++] = b;
		assert upto != slice.length;
	}

	public void writeBytes(final byte[] b, int offset, final int len) {
		final int offsetEnd = offset + len;
		while (offset < offsetEnd) {
			if (slice[upto] != 0) {

				upto = pool.allocSlice(slice, upto);
				slice = pool.buffer;
				offset0 = pool.byteOffset;
			}

			slice[upto++] = b[offset++];
			assert upto != slice.length;
		}
	}

	public int getAddress() {
		return upto + (offset0 & DocumentsWriter.BYTE_BLOCK_NOT_MASK);
	}

	public void writeVInt(int i) {
		while ((i & ~0x7F) != 0) {
			writeByte((byte) ((i & 0x7f) | 0x80));
			i >>>= 7;
		}
		writeByte((byte) i);
	}
}
