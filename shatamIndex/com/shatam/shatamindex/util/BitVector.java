/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;

public final class BitVector implements Cloneable, Bits {

	private byte[] bits;
	private int size;
	private int count;

	public BitVector(int n) {
		size = n;
		bits = new byte[getNumBytes(size)];
		count = 0;
	}

	BitVector(byte[] bits, int size) {
		this.bits = bits;
		this.size = size;
		count = -1;
	}

	private int getNumBytes(int size) {
		int bytesLength = size >>> 3;
		if ((size & 7) != 0) {
			bytesLength++;
		}
		return bytesLength;
	}

	@Override
	public Object clone() {
		byte[] copyBits = new byte[bits.length];
		System.arraycopy(bits, 0, copyBits, 0, bits.length);
		BitVector clone = new BitVector(copyBits, size);
		clone.count = count;
		return clone;
	}

	public final void set(int bit) {
		if (bit >= size) {
			throw new ArrayIndexOutOfBoundsException("bit=" + bit + " size="
					+ size);
		}
		bits[bit >> 3] |= 1 << (bit & 7);
		count = -1;
	}

	public final boolean getAndSet(int bit) {
		if (bit >= size) {
			throw new ArrayIndexOutOfBoundsException("bit=" + bit + " size="
					+ size);
		}
		final int pos = bit >> 3;
		final int v = bits[pos];
		final int flag = 1 << (bit & 7);
		if ((flag & v) != 0)
			return true;
		else {
			bits[pos] = (byte) (v | flag);
			if (count != -1)
				count++;
			return false;
		}
	}

	public final void clear(int bit) {
		if (bit >= size) {
			throw new ArrayIndexOutOfBoundsException(bit);
		}
		bits[bit >> 3] &= ~(1 << (bit & 7));
		count = -1;
	}

	public final boolean get(int bit) {
		assert bit >= 0 && bit < size : "bit " + bit + " is out of bounds 0.."
				+ (size - 1);
		return (bits[bit >> 3] & (1 << (bit & 7))) != 0;
	}

	public final int size() {
		return size;
	}

	public final int length() {
		return size;
	}

	public final int count() {

		if (count == -1) {
			int c = 0;
			int end = bits.length;
			for (int i = 0; i < end; i++)
				c += BYTE_COUNTS[bits[i] & 0xFF];
			count = c;
		}
		return count;
	}

	public final int getRecomputedCount() {
		int c = 0;
		int end = bits.length;
		for (int i = 0; i < end; i++)
			c += BYTE_COUNTS[bits[i] & 0xFF];
		return c;
	}

	private static final byte[] BYTE_COUNTS = { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2,
			2, 3, 2, 3, 3, 4, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
			1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4,
			4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4,
			3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3,
			3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6,
			4, 5, 5, 6, 5, 6, 6, 7, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4,
			4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4,
			3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5,
			5, 6, 5, 6, 6, 7, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
			3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 3, 4, 4, 5, 4, 5,
			5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7,
			6, 7, 7, 8 };

	private static String CODEC = "BitVector";

	private final static int VERSION_PRE = -1;

	private final static int VERSION_START = 0;

	private final static int VERSION_CURRENT = VERSION_START;

	public final void write(Directory d, String name) throws IOException {
		IndexOutput output = d.createOutput(name);
		try {
			output.writeInt(-2);
			CodecUtil.writeHeader(output, CODEC, VERSION_CURRENT);
			if (isSparse()) {
				writeDgaps(output);
			} else {
				writeBits(output);
			}
		} finally {
			output.close();
		}
	}

	private void writeBits(IndexOutput output) throws IOException {
		output.writeInt(size());
		output.writeInt(count());
		output.writeBytes(bits, bits.length);
	}

	private void writeDgaps(IndexOutput output) throws IOException {
		output.writeInt(-1);
		output.writeInt(size());
		output.writeInt(count());
		int last = 0;
		int n = count();
		int m = bits.length;
		for (int i = 0; i < m && n > 0; i++) {
			if (bits[i] != 0) {
				output.writeVInt(i - last);
				output.writeByte(bits[i]);
				last = i;
				n -= BYTE_COUNTS[bits[i] & 0xFF];
			}
		}
	}

	private boolean isSparse() {

		final int setCount = count();
		if (setCount == 0) {
			return true;
		}

		final int avgGapLength = bits.length / setCount;

		final int expectedDGapBytes;
		if (avgGapLength <= (1 << 7)) {
			expectedDGapBytes = 1;
		} else if (avgGapLength <= (1 << 14)) {
			expectedDGapBytes = 2;
		} else if (avgGapLength <= (1 << 21)) {
			expectedDGapBytes = 3;
		} else if (avgGapLength <= (1 << 28)) {
			expectedDGapBytes = 4;
		} else {
			expectedDGapBytes = 5;
		}

		final int bytesPerSetBit = expectedDGapBytes + 1;

		final long expectedBits = 32 + 8 * bytesPerSetBit * count();

		final long factor = 10;
		return factor * expectedBits < size();
	}

	public BitVector(Directory d, String name) throws IOException {
		IndexInput input = d.openInput(name);

		try {
			final int firstInt = input.readInt();
			final int version;
			if (firstInt == -2) {

				version = CodecUtil.checkHeader(input, CODEC, VERSION_START,
						VERSION_START);
				size = input.readInt();
			} else {
				version = VERSION_PRE;
				size = firstInt;
			}
			if (size == -1) {
				readDgaps(input);
			} else {
				readBits(input);
			}
		} finally {
			input.close();
		}
	}

	private void readBits(IndexInput input) throws IOException {
		count = input.readInt();
		bits = new byte[getNumBytes(size)];
		input.readBytes(bits, 0, bits.length);
	}

	private void readDgaps(IndexInput input) throws IOException {
		size = input.readInt();
		count = input.readInt();
		bits = new byte[(size >> 3) + 1];
		int last = 0;
		int n = count();
		while (n > 0) {
			last += input.readVInt();
			bits[last] = input.readByte();
			n -= BYTE_COUNTS[bits[last] & 0xFF];
		}
	}
}
