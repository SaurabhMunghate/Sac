/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

final class DocumentsWriterThreadState {

	boolean isIdle = true;
	int numThreads = 1;
	final DocConsumerPerThread consumer;
	final DocumentsWriter.DocState docState;

	final DocumentsWriter docWriter;

	public DocumentsWriterThreadState(DocumentsWriter docWriter)
			throws IOException {
		this.docWriter = docWriter;
		docState = new DocumentsWriter.DocState();
		docState.maxFieldLength = docWriter.maxFieldLength;
		docState.infoStream = docWriter.infoStream;
		docState.similarity = docWriter.similarity;
		docState.docWriter = docWriter;
		consumer = docWriter.consumer.addThread(this);
	}

	void doAfterFlush() {
		numThreads = 0;
	}
}
