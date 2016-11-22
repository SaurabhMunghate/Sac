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
import java.util.HashMap;
import java.util.HashSet;

import java.util.Map;

final class DocInverter extends DocFieldConsumer {

	final InvertedDocConsumer consumer;
	final InvertedDocEndConsumer endConsumer;

	public DocInverter(InvertedDocConsumer consumer,
			InvertedDocEndConsumer endConsumer) {
		this.consumer = consumer;
		this.endConsumer = endConsumer;
	}

	@Override
	void setFieldInfos(FieldInfos fieldInfos) {
		super.setFieldInfos(fieldInfos);
		consumer.setFieldInfos(fieldInfos);
		endConsumer.setFieldInfos(fieldInfos);
	}

	@Override
	void flush(
			Map<DocFieldConsumerPerThread, Collection<DocFieldConsumerPerField>> threadsAndFields,
			SegmentWriteState state) throws IOException {

		Map<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>> childThreadsAndFields = new HashMap<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>>();
		Map<InvertedDocEndConsumerPerThread, Collection<InvertedDocEndConsumerPerField>> endChildThreadsAndFields = new HashMap<InvertedDocEndConsumerPerThread, Collection<InvertedDocEndConsumerPerField>>();

		for (Map.Entry<DocFieldConsumerPerThread, Collection<DocFieldConsumerPerField>> entry : threadsAndFields
				.entrySet()) {
			DocInverterPerThread perThread = (DocInverterPerThread) entry
					.getKey();

			Collection<InvertedDocConsumerPerField> childFields = new HashSet<InvertedDocConsumerPerField>();
			Collection<InvertedDocEndConsumerPerField> endChildFields = new HashSet<InvertedDocEndConsumerPerField>();
			for (final DocFieldConsumerPerField field : entry.getValue()) {
				DocInverterPerField perField = (DocInverterPerField) field;
				childFields.add(perField.consumer);
				endChildFields.add(perField.endConsumer);
			}

			childThreadsAndFields.put(perThread.consumer, childFields);
			endChildThreadsAndFields.put(perThread.endConsumer, endChildFields);
		}

		consumer.flush(childThreadsAndFields, state);
		endConsumer.flush(endChildThreadsAndFields, state);
	}

	@Override
	void abort() {
		try {
			consumer.abort();
		} finally {
			endConsumer.abort();
		}
	}

	@Override
	public boolean freeRAM() {
		return consumer.freeRAM();
	}

	@Override
	public DocFieldConsumerPerThread addThread(
			DocFieldProcessorPerThread docFieldProcessorPerThread) {
		return new DocInverterPerThread(docFieldProcessorPerThread, this);
	}
}
