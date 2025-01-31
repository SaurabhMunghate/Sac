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
import java.util.Iterator;
import java.util.Map;

final class TermsHash extends InvertedDocConsumer {

	final TermsHashConsumer consumer;
	final TermsHash nextTermsHash;
	final DocumentsWriter docWriter;

	boolean trackAllocations;

	public TermsHash(final DocumentsWriter docWriter, boolean trackAllocations,
			final TermsHashConsumer consumer, final TermsHash nextTermsHash) {
		this.docWriter = docWriter;
		this.consumer = consumer;
		this.nextTermsHash = nextTermsHash;
		this.trackAllocations = trackAllocations;
	}

	@Override
	InvertedDocConsumerPerThread addThread(
			DocInverterPerThread docInverterPerThread) {
		return new TermsHashPerThread(docInverterPerThread, this,
				nextTermsHash, null);
	}

	TermsHashPerThread addThread(DocInverterPerThread docInverterPerThread,
			TermsHashPerThread primaryPerThread) {
		return new TermsHashPerThread(docInverterPerThread, this,
				nextTermsHash, primaryPerThread);
	}

	@Override
	void setFieldInfos(FieldInfos fieldInfos) {
		this.fieldInfos = fieldInfos;
		consumer.setFieldInfos(fieldInfos);
	}

	@Override
	public void abort() {
		try {
			consumer.abort();
		} finally {
			if (nextTermsHash != null) {
				nextTermsHash.abort();
			}
		}
	}

	@Override
	synchronized void flush(
			Map<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>> threadsAndFields,
			final SegmentWriteState state) throws IOException {
		Map<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> childThreadsAndFields = new HashMap<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>>();
		Map<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>> nextThreadsAndFields;

		if (nextTermsHash != null)
			nextThreadsAndFields = new HashMap<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>>();
		else
			nextThreadsAndFields = null;

		for (final Map.Entry<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>> entry : threadsAndFields
				.entrySet()) {

			TermsHashPerThread perThread = (TermsHashPerThread) entry.getKey();

			Collection<InvertedDocConsumerPerField> fields = entry.getValue();

			Iterator<InvertedDocConsumerPerField> fieldsIt = fields.iterator();
			Collection<TermsHashConsumerPerField> childFields = new HashSet<TermsHashConsumerPerField>();
			Collection<InvertedDocConsumerPerField> nextChildFields;

			if (nextTermsHash != null)
				nextChildFields = new HashSet<InvertedDocConsumerPerField>();
			else
				nextChildFields = null;

			while (fieldsIt.hasNext()) {
				TermsHashPerField perField = (TermsHashPerField) fieldsIt
						.next();
				childFields.add(perField.consumer);
				if (nextTermsHash != null)
					nextChildFields.add(perField.nextPerField);
			}

			childThreadsAndFields.put(perThread.consumer, childFields);
			if (nextTermsHash != null)
				nextThreadsAndFields.put(perThread.nextPerThread,
						nextChildFields);
		}

		consumer.flush(childThreadsAndFields, state);

		if (nextTermsHash != null)
			nextTermsHash.flush(nextThreadsAndFields, state);
	}

	@Override
	synchronized public boolean freeRAM() {
		return false;
	}
}
