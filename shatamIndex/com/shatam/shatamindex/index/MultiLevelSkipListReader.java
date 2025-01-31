/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Arrays;

import com.shatam.shatamindex.store.BufferedIndexInput;
import com.shatam.shatamindex.store.IndexInput;

abstract class MultiLevelSkipListReader {

	private int maxNumberOfSkipLevels;

	private int numberOfSkipLevels;

	private int numberOfLevelsToBuffer = 1;

	private int docCount;
	private boolean haveSkipped;

	private IndexInput[] skipStream;
	private long skipPointer[];
	private int skipInterval[];
	private int[] numSkipped;

	private int[] skipDoc;
	private int lastDoc;
	private long[] childPointer;
	private long lastChildPointer;

	private boolean inputIsBuffered;

	public MultiLevelSkipListReader(IndexInput skipStream, int maxSkipLevels,
			int skipInterval) {
		this.skipStream = new IndexInput[maxSkipLevels];
		this.skipPointer = new long[maxSkipLevels];
		this.childPointer = new long[maxSkipLevels];
		this.numSkipped = new int[maxSkipLevels];
		this.maxNumberOfSkipLevels = maxSkipLevels;
		this.skipInterval = new int[maxSkipLevels];
		this.skipStream[0] = skipStream;
		this.inputIsBuffered = (skipStream instanceof BufferedIndexInput);
		this.skipInterval[0] = skipInterval;
		for (int i = 1; i < maxSkipLevels; i++) {

			this.skipInterval[i] = this.skipInterval[i - 1] * skipInterval;
		}
		skipDoc = new int[maxSkipLevels];
	}

	int getDoc() {
		return lastDoc;
	}

	int skipTo(int target) throws IOException {
		if (!haveSkipped) {

			loadSkipLevels();
			haveSkipped = true;
		}

		int level = 0;
		while (level < numberOfSkipLevels - 1 && target > skipDoc[level + 1]) {
			level++;
		}

		while (level >= 0) {
			if (target > skipDoc[level]) {
				if (!loadNextSkip(level)) {
					continue;
				}
			} else {

				if (level > 0
						&& lastChildPointer > skipStream[level - 1]
								.getFilePointer()) {
					seekChild(level - 1);
				}
				level--;
			}
		}

		return numSkipped[0] - skipInterval[0] - 1;
	}

	private boolean loadNextSkip(int level) throws IOException {

		setLastSkipData(level);

		numSkipped[level] += skipInterval[level];

		if (numSkipped[level] > docCount) {

			skipDoc[level] = Integer.MAX_VALUE;
			if (numberOfSkipLevels > level)
				numberOfSkipLevels = level;
			return false;
		}

		skipDoc[level] += readSkipData(level, skipStream[level]);

		if (level != 0) {

			childPointer[level] = skipStream[level].readVLong()
					+ skipPointer[level - 1];
		}

		return true;

	}

	protected void seekChild(int level) throws IOException {
		skipStream[level].seek(lastChildPointer);
		numSkipped[level] = numSkipped[level + 1] - skipInterval[level + 1];
		skipDoc[level] = lastDoc;
		if (level > 0) {
			childPointer[level] = skipStream[level].readVLong()
					+ skipPointer[level - 1];
		}
	}

	void close() throws IOException {
		for (int i = 1; i < skipStream.length; i++) {
			if (skipStream[i] != null) {
				skipStream[i].close();
			}
		}
	}

	void init(long skipPointer, int df) {
		this.skipPointer[0] = skipPointer;
		this.docCount = df;
		Arrays.fill(skipDoc, 0);
		Arrays.fill(numSkipped, 0);
		Arrays.fill(childPointer, 0);

		haveSkipped = false;
		for (int i = 1; i < numberOfSkipLevels; i++) {
			skipStream[i] = null;
		}
	}

	private void loadSkipLevels() throws IOException {
		numberOfSkipLevels = docCount == 0 ? 0 : (int) Math.floor(Math
				.log(docCount) / Math.log(skipInterval[0]));
		if (numberOfSkipLevels > maxNumberOfSkipLevels) {
			numberOfSkipLevels = maxNumberOfSkipLevels;
		}

		skipStream[0].seek(skipPointer[0]);

		int toBuffer = numberOfLevelsToBuffer;

		for (int i = numberOfSkipLevels - 1; i > 0; i--) {

			long length = skipStream[0].readVLong();

			skipPointer[i] = skipStream[0].getFilePointer();
			if (toBuffer > 0) {

				skipStream[i] = new SkipBuffer(skipStream[0], (int) length);
				toBuffer--;
			} else {

				skipStream[i] = (IndexInput) skipStream[0].clone();
				if (inputIsBuffered && length < BufferedIndexInput.BUFFER_SIZE) {
					((BufferedIndexInput) skipStream[i])
							.setBufferSize((int) length);
				}

				skipStream[0].seek(skipStream[0].getFilePointer() + length);
			}
		}

		skipPointer[0] = skipStream[0].getFilePointer();
	}

	protected abstract int readSkipData(int level, IndexInput skipStream)
			throws IOException;

	protected void setLastSkipData(int level) {
		lastDoc = skipDoc[level];
		lastChildPointer = childPointer[level];
	}

	private final static class SkipBuffer extends IndexInput {
		private byte[] data;
		private long pointer;
		private int pos;

		SkipBuffer(IndexInput input, int length) throws IOException {
			super("SkipBuffer on " + input);
			data = new byte[length];
			pointer = input.getFilePointer();
			input.readBytes(data, 0, length);
		}

		@Override
		public void close() throws IOException {
			data = null;
		}

		@Override
		public long getFilePointer() {
			return pointer + pos;
		}

		@Override
		public long length() {
			return data.length;
		}

		@Override
		public byte readByte() throws IOException {
			return data[pos++];
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) throws IOException {
			System.arraycopy(data, pos, b, offset, len);
			pos += len;
		}

		@Override
		public void seek(long pos) throws IOException {
			this.pos = (int) (pos - pointer);
		}

	}
}
