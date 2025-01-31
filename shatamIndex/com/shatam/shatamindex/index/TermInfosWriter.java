/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Closeable;
import java.io.IOException;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.UnicodeUtil;

final class TermInfosWriter implements Closeable {

	public static final int FORMAT = -3;

	public static final int FORMAT_VERSION_UTF8_LENGTH_IN_BYTES = -4;

	public static final int FORMAT_CURRENT = FORMAT_VERSION_UTF8_LENGTH_IN_BYTES;

	private FieldInfos fieldInfos;
	private IndexOutput output;
	private TermInfo lastTi = new TermInfo();
	private long size;

	int indexInterval = 128;

	int skipInterval = 16;

	int maxSkipLevels = 10;

	private long lastIndexPointer;
	private boolean isIndex;
	private byte[] lastTermBytes = new byte[10];
	private int lastTermBytesLength = 0;
	private int lastFieldNumber = -1;

	private TermInfosWriter other;
	private UnicodeUtil.UTF8Result utf8Result = new UnicodeUtil.UTF8Result();

	TermInfosWriter(Directory directory, String segment, FieldInfos fis,
			int interval) throws IOException {
		initialize(directory, segment, fis, interval, false);
		boolean success = false;
		try {
			other = new TermInfosWriter(directory, segment, fis, interval, true);
			other.other = this;
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(output, other);
			}
		}
	}

	private TermInfosWriter(Directory directory, String segment,
			FieldInfos fis, int interval, boolean isIndex) throws IOException {
		initialize(directory, segment, fis, interval, isIndex);
	}

	private void initialize(Directory directory, String segment,
			FieldInfos fis, int interval, boolean isi) throws IOException {
		indexInterval = interval;
		fieldInfos = fis;
		isIndex = isi;
		output = directory.createOutput(segment + (isIndex ? ".tii" : ".tis"));
		boolean success = false;
		try {
			output.writeInt(FORMAT_CURRENT);
			output.writeLong(0);
			output.writeInt(indexInterval);
			output.writeInt(skipInterval);
			output.writeInt(maxSkipLevels);
			assert initUTF16Results();
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(output);
			}
		}
	}

	void add(Term term, TermInfo ti) throws IOException {
		UnicodeUtil.UTF16toUTF8(term.text, 0, term.text.length(), utf8Result);
		add(fieldInfos.fieldNumber(term.field), utf8Result.result,
				utf8Result.length, ti);
	}

	UnicodeUtil.UTF16Result utf16Result1;
	UnicodeUtil.UTF16Result utf16Result2;

	private boolean initUTF16Results() {
		utf16Result1 = new UnicodeUtil.UTF16Result();
		utf16Result2 = new UnicodeUtil.UTF16Result();
		return true;
	}

	private int compareToLastTerm(int fieldNumber, byte[] termBytes,
			int termBytesLength) {

		if (lastFieldNumber != fieldNumber) {
			final int cmp = fieldInfos.fieldName(lastFieldNumber).compareTo(
					fieldInfos.fieldName(fieldNumber));

			if (cmp != 0 || lastFieldNumber != -1)
				return cmp;
		}

		UnicodeUtil.UTF8toUTF16(lastTermBytes, 0, lastTermBytesLength,
				utf16Result1);
		UnicodeUtil.UTF8toUTF16(termBytes, 0, termBytesLength, utf16Result2);
		final int len;
		if (utf16Result1.length < utf16Result2.length)
			len = utf16Result1.length;
		else
			len = utf16Result2.length;

		for (int i = 0; i < len; i++) {
			final char ch1 = utf16Result1.result[i];
			final char ch2 = utf16Result2.result[i];
			if (ch1 != ch2)
				return ch1 - ch2;
		}
		if (utf16Result1.length == 0 && lastFieldNumber == -1) {

			return -1;
		}
		return utf16Result1.length - utf16Result2.length;
	}

	void add(int fieldNumber, byte[] termBytes, int termBytesLength, TermInfo ti)
			throws IOException {

		assert compareToLastTerm(fieldNumber, termBytes, termBytesLength) < 0
				|| (isIndex && termBytesLength == 0 && lastTermBytesLength == 0) : "Terms are out of order: field="
				+ fieldInfos.fieldName(fieldNumber)
				+ " (number "
				+ fieldNumber
				+ ")"
				+ " lastField="
				+ fieldInfos.fieldName(lastFieldNumber)
				+ " (number "
				+ lastFieldNumber
				+ ")"
				+ " text="
				+ new String(termBytes, 0, termBytesLength, "UTF-8")
				+ " lastText="
				+ new String(lastTermBytes, 0, lastTermBytesLength, "UTF-8");

		assert ti.freqPointer >= lastTi.freqPointer : "freqPointer out of order ("
				+ ti.freqPointer + " < " + lastTi.freqPointer + ")";
		assert ti.proxPointer >= lastTi.proxPointer : "proxPointer out of order ("
				+ ti.proxPointer + " < " + lastTi.proxPointer + ")";

		if (!isIndex && size % indexInterval == 0)
			other.add(lastFieldNumber, lastTermBytes, lastTermBytesLength,
					lastTi);

		writeTerm(fieldNumber, termBytes, termBytesLength);

		output.writeVInt(ti.docFreq);
		output.writeVLong(ti.freqPointer - lastTi.freqPointer);
		output.writeVLong(ti.proxPointer - lastTi.proxPointer);

		if (ti.docFreq >= skipInterval) {
			output.writeVInt(ti.skipOffset);
		}

		if (isIndex) {
			output.writeVLong(other.output.getFilePointer() - lastIndexPointer);
			lastIndexPointer = other.output.getFilePointer();
		}

		lastFieldNumber = fieldNumber;
		lastTi.set(ti);
		size++;
	}

	private void writeTerm(int fieldNumber, byte[] termBytes,
			int termBytesLength) throws IOException {

		int start = 0;
		final int limit = termBytesLength < lastTermBytesLength ? termBytesLength
				: lastTermBytesLength;
		while (start < limit) {
			if (termBytes[start] != lastTermBytes[start])
				break;
			start++;
		}

		final int length = termBytesLength - start;
		output.writeVInt(start);
		output.writeVInt(length);
		output.writeBytes(termBytes, start, length);
		output.writeVInt(fieldNumber);
		if (lastTermBytes.length < termBytesLength) {
			lastTermBytes = ArrayUtil.grow(lastTermBytes, termBytesLength);
		}
		System.arraycopy(termBytes, start, lastTermBytes, start, length);
		lastTermBytesLength = termBytesLength;
	}

	public void close() throws IOException {
		try {
			output.seek(4);
			output.writeLong(size);
		} finally {
			try {
				output.close();
			} finally {
				if (!isIndex) {
					other.close();
				}
			}
		}

	}

}
