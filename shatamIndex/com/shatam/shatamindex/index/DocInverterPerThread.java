/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.OffsetAttribute;
import com.shatam.shatamindex.util.AttributeSource;

final class DocInverterPerThread extends DocFieldConsumerPerThread {
	final DocInverter docInverter;
	final InvertedDocConsumerPerThread consumer;
	final InvertedDocEndConsumerPerThread endConsumer;
	final SingleTokenAttributeSource singleToken = new SingleTokenAttributeSource();

	static class SingleTokenAttributeSource extends AttributeSource {
		final CharTermAttribute termAttribute;
		final OffsetAttribute offsetAttribute;

		private SingleTokenAttributeSource() {
			termAttribute = addAttribute(CharTermAttribute.class);
			offsetAttribute = addAttribute(OffsetAttribute.class);
		}

		public void reinit(String stringValue, int startOffset, int endOffset) {
			termAttribute.setEmpty().append(stringValue);
			offsetAttribute.setOffset(startOffset, endOffset);
		}
	}

	final DocumentsWriter.DocState docState;

	final FieldInvertState fieldState = new FieldInvertState();

	final ReusableStringReader stringReader = new ReusableStringReader();

	public DocInverterPerThread(
			DocFieldProcessorPerThread docFieldProcessorPerThread,
			DocInverter docInverter) {
		this.docInverter = docInverter;
		docState = docFieldProcessorPerThread.docState;
		consumer = docInverter.consumer.addThread(this);
		endConsumer = docInverter.endConsumer.addThread(this);
	}

	@Override
	public void startDocument() throws IOException {
		consumer.startDocument();
		endConsumer.startDocument();
	}

	@Override
	public DocumentsWriter.DocWriter finishDocument() throws IOException {

		endConsumer.finishDocument();
		return consumer.finishDocument();
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
	public DocFieldConsumerPerField addField(FieldInfo fi) {
		return new DocInverterPerField(this, fi);
	}
}
