/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

final class TermsHashPerThread extends InvertedDocConsumerPerThread {

	final TermsHash termsHash;
	final TermsHashConsumerPerThread consumer;
	final TermsHashPerThread nextPerThread;

	final CharBlockPool charPool;
	final IntBlockPool intPool;
	final ByteBlockPool bytePool;
	final boolean primary;
	final DocumentsWriter.DocState docState;

	public TermsHashPerThread(DocInverterPerThread docInverterPerThread,
			final TermsHash termsHash, final TermsHash nextTermsHash,
			final TermsHashPerThread primaryPerThread) {
		docState = docInverterPerThread.docState;

		this.termsHash = termsHash;
		this.consumer = termsHash.consumer.addThread(this);

		if (nextTermsHash != null) {

			charPool = new CharBlockPool(termsHash.docWriter);
			primary = true;
		} else {
			charPool = primaryPerThread.charPool;
			primary = false;
		}

		intPool = new IntBlockPool(termsHash.docWriter);
		bytePool = new ByteBlockPool(termsHash.docWriter.byteBlockAllocator);

		if (nextTermsHash != null)
			nextPerThread = nextTermsHash.addThread(docInverterPerThread, this);
		else
			nextPerThread = null;
	}

	@Override
	InvertedDocConsumerPerField addField(
			DocInverterPerField docInverterPerField, final FieldInfo fieldInfo) {
		return new TermsHashPerField(docInverterPerField, this, nextPerThread,
				fieldInfo);
	}

	@Override
	synchronized public void abort() {
		reset(true);
		try {
			consumer.abort();
		} finally {
			if (nextPerThread != null) {
				nextPerThread.abort();
			}
		}
	}

	@Override
	public void startDocument() throws IOException {
		consumer.startDocument();
		if (nextPerThread != null)
			nextPerThread.consumer.startDocument();
	}

	@Override
	public DocumentsWriter.DocWriter finishDocument() throws IOException {
		final DocumentsWriter.DocWriter doc = consumer.finishDocument();

		final DocumentsWriter.DocWriter doc2;
		if (nextPerThread != null)
			doc2 = nextPerThread.consumer.finishDocument();
		else
			doc2 = null;
		if (doc == null)
			return doc2;
		else {
			doc.setNext(doc2);
			return doc;
		}
	}

	void reset(boolean recyclePostings) {
		intPool.reset();
		bytePool.reset();

		if (primary)
			charPool.reset();
	}
}
