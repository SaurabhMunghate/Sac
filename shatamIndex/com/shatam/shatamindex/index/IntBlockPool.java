/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

final class IntBlockPool {

	public int[][] buffers = new int[10][];

	int bufferUpto = -1;
	public int intUpto = DocumentsWriter.INT_BLOCK_SIZE;

	public int[] buffer;
	public int intOffset = -DocumentsWriter.INT_BLOCK_SIZE;

	final private DocumentsWriter docWriter;

	public IntBlockPool(DocumentsWriter docWriter) {
		this.docWriter = docWriter;
	}

	public void reset() {
		if (bufferUpto != -1) {
			if (bufferUpto > 0)

				docWriter.recycleIntBlocks(buffers, 1, 1 + bufferUpto);

			bufferUpto = 0;
			intUpto = 0;
			intOffset = 0;
			buffer = buffers[0];
		}
	}

	public void nextBuffer() {
		if (1 + bufferUpto == buffers.length) {
			int[][] newBuffers = new int[(int) (buffers.length * 1.5)][];
			System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
			buffers = newBuffers;
		}
		buffer = buffers[1 + bufferUpto] = docWriter.getIntBlock();
		bufferUpto++;

		intUpto = 0;
		intOffset += DocumentsWriter.INT_BLOCK_SIZE;
	}
}
