/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.store.IndexInput;

final class SegmentTermEnum extends TermEnum implements Cloneable {
	private IndexInput input;
	FieldInfos fieldInfos;
	long size;
	long position = -1;
	private boolean first = true;

	private TermBuffer termBuffer = new TermBuffer();
	private TermBuffer prevBuffer = new TermBuffer();
	private TermBuffer scanBuffer = new TermBuffer();

	private TermInfo termInfo = new TermInfo();

	private int format;
	private boolean isIndex = false;
	long indexPointer = 0;
	int indexInterval;
	int skipInterval;
	int maxSkipLevels;
	private int formatM1SkipInterval;

	SegmentTermEnum(IndexInput i, FieldInfos fis, boolean isi)
			throws CorruptIndexException, IOException {
		input = i;
		fieldInfos = fis;
		isIndex = isi;
		maxSkipLevels = 1;

		int firstInt = input.readInt();
		if (firstInt >= 0) {

			format = 0;
			size = firstInt;

			indexInterval = 128;
			skipInterval = Integer.MAX_VALUE;
		} else {

			format = firstInt;

			if (format < TermInfosWriter.FORMAT_CURRENT)
				throw new IndexFormatTooNewException(input, format, -1,
						TermInfosWriter.FORMAT_CURRENT);

			size = input.readLong();

			if (format == -1) {
				if (!isIndex) {
					indexInterval = input.readInt();
					formatM1SkipInterval = input.readInt();
				}

				skipInterval = Integer.MAX_VALUE;
			} else {
				indexInterval = input.readInt();
				skipInterval = input.readInt();
				if (format <= TermInfosWriter.FORMAT) {

					maxSkipLevels = input.readInt();
				}
			}
			assert indexInterval > 0 : "indexInterval=" + indexInterval
					+ " is negative; must be > 0";
			assert skipInterval > 0 : "skipInterval=" + skipInterval
					+ " is negative; must be > 0";
		}
		if (format > TermInfosWriter.FORMAT_VERSION_UTF8_LENGTH_IN_BYTES) {
			termBuffer.setPreUTF8Strings();
			scanBuffer.setPreUTF8Strings();
			prevBuffer.setPreUTF8Strings();
		}
	}

	@Override
	protected Object clone() {
		SegmentTermEnum clone = null;
		try {
			clone = (SegmentTermEnum) super.clone();
		} catch (CloneNotSupportedException e) {
		}

		clone.input = (IndexInput) input.clone();
		clone.termInfo = new TermInfo(termInfo);

		clone.termBuffer = (TermBuffer) termBuffer.clone();
		clone.prevBuffer = (TermBuffer) prevBuffer.clone();
		clone.scanBuffer = new TermBuffer();

		return clone;
	}

	final void seek(long pointer, long p, Term t, TermInfo ti)
			throws IOException {
		input.seek(pointer);
		position = p;
		termBuffer.set(t);
		prevBuffer.reset();
		termInfo.set(ti);
		first = p == -1;
	}

	@Override
	public final boolean next() throws IOException {
		if (position++ >= size - 1) {
			prevBuffer.set(termBuffer);
			termBuffer.reset();
			return false;
		}

		prevBuffer.set(termBuffer);
		termBuffer.read(input, fieldInfos);

		termInfo.docFreq = input.readVInt();
		termInfo.freqPointer += input.readVLong();
		termInfo.proxPointer += input.readVLong();

		if (format == -1) {

			if (!isIndex) {
				if (termInfo.docFreq > formatM1SkipInterval) {
					termInfo.skipOffset = input.readVInt();
				}
			}
		} else {
			if (termInfo.docFreq >= skipInterval)
				termInfo.skipOffset = input.readVInt();
		}

		if (isIndex)
			indexPointer += input.readVLong();

		return true;
	}

	final int scanTo(Term term) throws IOException {
		scanBuffer.set(term);
		int count = 0;
		if (first) {

			next();
			first = false;
			count++;
		}
		while (scanBuffer.compareTo(termBuffer) > 0 && next()) {
			count++;
		}
		return count;
	}

	@Override
	public final Term term() {
		return termBuffer.toTerm();
	}

	final Term prev() {
		return prevBuffer.toTerm();
	}

	final TermInfo termInfo() {
		return new TermInfo(termInfo);
	}

	final void termInfo(TermInfo ti) {
		ti.set(termInfo);
	}

	@Override
	public final int docFreq() {
		return termInfo.docFreq;
	}

	final long freqPointer() {
		return termInfo.freqPointer;
	}

	final long proxPointer() {
		return termInfo.proxPointer;
	}

	@Override
	public final void close() throws IOException {
		input.close();
	}
}
