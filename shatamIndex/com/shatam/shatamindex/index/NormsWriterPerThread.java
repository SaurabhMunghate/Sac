/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */

package com.shatam.shatamindex.index;

final class NormsWriterPerThread extends InvertedDocEndConsumerPerThread {
	final NormsWriter normsWriter;
	final DocumentsWriter.DocState docState;

	public NormsWriterPerThread(DocInverterPerThread docInverterPerThread,
			NormsWriter normsWriter) {
		this.normsWriter = normsWriter;
		docState = docInverterPerThread.docState;
	}

	@Override
	InvertedDocEndConsumerPerField addField(
			DocInverterPerField docInverterPerField, final FieldInfo fieldInfo) {
		return new NormsWriterPerField(docInverterPerField, this, fieldInfo);
	}

	@Override
	void abort() {
	}

	@Override
	void startDocument() {
	}

	@Override
	void finishDocument() {
	}

	boolean freeRAM() {
		return false;
	}
}
