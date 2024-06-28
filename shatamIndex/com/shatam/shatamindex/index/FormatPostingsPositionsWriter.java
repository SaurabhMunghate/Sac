/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.IOUtils;

import java.io.Closeable;
import java.io.IOException;

final class FormatPostingsPositionsWriter extends
		FormatPostingsPositionsConsumer implements Closeable {

	final FormatPostingsDocsWriter parent;
	final IndexOutput out;

	boolean omitTermFreqAndPositions;
	boolean storePayloads;
	int lastPayloadLength = -1;

	FormatPostingsPositionsWriter(SegmentWriteState state,
			FormatPostingsDocsWriter parent) throws IOException {
		this.parent = parent;
		omitTermFreqAndPositions = parent.omitTermFreqAndPositions;
		if (parent.parent.parent.fieldInfos.hasProx()) {

			out = parent.parent.parent.dir.createOutput(IndexFileNames
					.segmentFileName(parent.parent.parent.segment,
							IndexFileNames.PROX_EXTENSION));
			parent.skipListWriter.setProxOutput(out);
		} else

			out = null;
	}

	int lastPosition;

	@Override
	void addPosition(int position, byte[] payload, int payloadOffset,
			int payloadLength) throws IOException {
		assert !omitTermFreqAndPositions : "omitTermFreqAndPositions is true";
		assert out != null;

		final int delta = position - lastPosition;
		lastPosition = position;

		if (storePayloads) {
			if (payloadLength != lastPayloadLength) {
				lastPayloadLength = payloadLength;
				out.writeVInt((delta << 1) | 1);
				out.writeVInt(payloadLength);
			} else
				out.writeVInt(delta << 1);
			if (payloadLength > 0)
				out.writeBytes(payload, payloadLength);
		} else
			out.writeVInt(delta);
	}

	void setField(FieldInfo fieldInfo) {
		omitTermFreqAndPositions = fieldInfo.indexOptions == IndexOptions.DOCS_ONLY;
		storePayloads = omitTermFreqAndPositions ? false
				: fieldInfo.storePayloads;
	}

	@Override
	void finish() {
		lastPosition = 0;
		lastPayloadLength = -1;
	}

	public void close() throws IOException {
		IOUtils.close(out);
	}
}
