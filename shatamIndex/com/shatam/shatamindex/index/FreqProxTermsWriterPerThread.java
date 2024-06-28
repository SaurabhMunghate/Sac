/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

final class FreqProxTermsWriterPerThread extends TermsHashConsumerPerThread {
	final TermsHashPerThread termsHashPerThread;
	final DocumentsWriter.DocState docState;

	public FreqProxTermsWriterPerThread(TermsHashPerThread perThread) {
		docState = perThread.docState;
		termsHashPerThread = perThread;
	}

	@Override
	public TermsHashConsumerPerField addField(
			TermsHashPerField termsHashPerField, FieldInfo fieldInfo) {
		return new FreqProxTermsWriterPerField(termsHashPerField, this,
				fieldInfo);
	}

	@Override
	void startDocument() {
	}

	@Override
	DocumentsWriter.DocWriter finishDocument() {
		return null;
	}

	@Override
	public void abort() {
	}
}
