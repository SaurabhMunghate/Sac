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

import com.shatam.shatamindex.store.IndexOutput;

class DefaultSkipListWriter extends MultiLevelSkipListWriter {
	private int[] lastSkipDoc;
	private int[] lastSkipPayloadLength;
	private long[] lastSkipFreqPointer;
	private long[] lastSkipProxPointer;

	private IndexOutput freqOutput;
	private IndexOutput proxOutput;

	private int curDoc;
	private boolean curStorePayloads;
	private int curPayloadLength;
	private long curFreqPointer;
	private long curProxPointer;

	DefaultSkipListWriter(int skipInterval, int numberOfSkipLevels,
			int docCount, IndexOutput freqOutput, IndexOutput proxOutput) {
		super(skipInterval, numberOfSkipLevels, docCount);
		this.freqOutput = freqOutput;
		this.proxOutput = proxOutput;

		lastSkipDoc = new int[numberOfSkipLevels];
		lastSkipPayloadLength = new int[numberOfSkipLevels];
		lastSkipFreqPointer = new long[numberOfSkipLevels];
		lastSkipProxPointer = new long[numberOfSkipLevels];
	}

	void setFreqOutput(IndexOutput freqOutput) {
		this.freqOutput = freqOutput;
	}

	void setProxOutput(IndexOutput proxOutput) {
		this.proxOutput = proxOutput;
	}

	void setSkipData(int doc, boolean storePayloads, int payloadLength) {
		this.curDoc = doc;
		this.curStorePayloads = storePayloads;
		this.curPayloadLength = payloadLength;
		this.curFreqPointer = freqOutput.getFilePointer();
		if (proxOutput != null)
			this.curProxPointer = proxOutput.getFilePointer();
	}

	@Override
	protected void resetSkip() {
		super.resetSkip();
		Arrays.fill(lastSkipDoc, 0);
		Arrays.fill(lastSkipPayloadLength, -1);
		Arrays.fill(lastSkipFreqPointer, freqOutput.getFilePointer());
		if (proxOutput != null)
			Arrays.fill(lastSkipProxPointer, proxOutput.getFilePointer());
	}

	@Override
	protected void writeSkipData(int level, IndexOutput skipBuffer)
			throws IOException {

		if (curStorePayloads) {
			int delta = curDoc - lastSkipDoc[level];
			if (curPayloadLength == lastSkipPayloadLength[level]) {

				skipBuffer.writeVInt(delta * 2);
			} else {

				skipBuffer.writeVInt(delta * 2 + 1);
				skipBuffer.writeVInt(curPayloadLength);
				lastSkipPayloadLength[level] = curPayloadLength;
			}
		} else {

			skipBuffer.writeVInt(curDoc - lastSkipDoc[level]);
		}
		skipBuffer
				.writeVInt((int) (curFreqPointer - lastSkipFreqPointer[level]));
		skipBuffer
				.writeVInt((int) (curProxPointer - lastSkipProxPointer[level]));

		lastSkipDoc[level] = curDoc;

		lastSkipFreqPointer[level] = curFreqPointer;
		lastSkipProxPointer[level] = curProxPointer;
	}

}
