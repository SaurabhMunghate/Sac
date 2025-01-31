/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.store.RAMOutputStream;

abstract class MultiLevelSkipListWriter {

	private int numberOfSkipLevels;

	private int skipInterval;

	private RAMOutputStream[] skipBuffer;

	protected MultiLevelSkipListWriter(int skipInterval, int maxSkipLevels,
			int df) {
		this.skipInterval = skipInterval;

		numberOfSkipLevels = df == 0 ? 0 : (int) Math.floor(Math.log(df)
				/ Math.log(skipInterval));

		if (numberOfSkipLevels > maxSkipLevels) {
			numberOfSkipLevels = maxSkipLevels;
		}
	}

	protected void init() {
		skipBuffer = new RAMOutputStream[numberOfSkipLevels];
		for (int i = 0; i < numberOfSkipLevels; i++) {
			skipBuffer[i] = new RAMOutputStream();
		}
	}

	protected void resetSkip() {

		if (skipBuffer == null) {
			init();
		} else {
			for (int i = 0; i < skipBuffer.length; i++) {
				skipBuffer[i].reset();
			}
		}
	}

	protected abstract void writeSkipData(int level, IndexOutput skipBuffer)
			throws IOException;

	void bufferSkip(int df) throws IOException {
		int numLevels;

		for (numLevels = 0; (df % skipInterval) == 0
				&& numLevels < numberOfSkipLevels; df /= skipInterval) {
			numLevels++;
		}

		long childPointer = 0;

		for (int level = 0; level < numLevels; level++) {
			writeSkipData(level, skipBuffer[level]);

			long newChildPointer = skipBuffer[level].getFilePointer();

			if (level != 0) {

				skipBuffer[level].writeVLong(childPointer);
			}

			childPointer = newChildPointer;
		}
	}

	long writeSkip(IndexOutput output) throws IOException {
		long skipPointer = output.getFilePointer();
		if (skipBuffer == null || skipBuffer.length == 0)
			return skipPointer;

		for (int level = numberOfSkipLevels - 1; level > 0; level--) {
			long length = skipBuffer[level].getFilePointer();
			if (length > 0) {
				output.writeVLong(length);
				skipBuffer[level].writeTo(output);
			}
		}
		skipBuffer[0].writeTo(output);

		return skipPointer;
	}

}
