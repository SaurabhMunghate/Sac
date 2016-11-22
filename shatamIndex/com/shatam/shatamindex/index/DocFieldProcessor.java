/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

final class DocFieldProcessor extends DocConsumer {

	final DocumentsWriter docWriter;
	final FieldInfos fieldInfos;
	final DocFieldConsumer consumer;
	final StoredFieldsWriter fieldsWriter;

	public DocFieldProcessor(DocumentsWriter docWriter,
			DocFieldConsumer consumer) {
		this.docWriter = docWriter;
		this.consumer = consumer;
		fieldInfos = docWriter.getFieldInfos();
		consumer.setFieldInfos(fieldInfos);
		fieldsWriter = new StoredFieldsWriter(docWriter, fieldInfos);
	}

	@Override
	public void flush(Collection<DocConsumerPerThread> threads,
			SegmentWriteState state) throws IOException {

		Map<DocFieldConsumerPerThread, Collection<DocFieldConsumerPerField>> childThreadsAndFields = new HashMap<DocFieldConsumerPerThread, Collection<DocFieldConsumerPerField>>();
		for (DocConsumerPerThread thread : threads) {
			DocFieldProcessorPerThread perThread = (DocFieldProcessorPerThread) thread;
			childThreadsAndFields.put(perThread.consumer, perThread.fields());
			perThread.trimFields(state);
		}

		fieldsWriter.flush(state);
		consumer.flush(childThreadsAndFields, state);

		final String fileName = IndexFileNames.segmentFileName(
				state.segmentName, IndexFileNames.FIELD_INFOS_EXTENSION);
		fieldInfos.write(state.directory, fileName);
	}

	@Override
	public void abort() {
		try {
			fieldsWriter.abort();
		} finally {
			consumer.abort();
		}
	}

	@Override
	public boolean freeRAM() {
		return consumer.freeRAM();
	}

	@Override
	public DocConsumerPerThread addThread(DocumentsWriterThreadState threadState)
			throws IOException {
		return new DocFieldProcessorPerThread(threadState, this);
	}
}
