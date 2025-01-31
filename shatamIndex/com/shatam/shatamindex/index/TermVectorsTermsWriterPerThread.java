/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.util.UnicodeUtil;

final class TermVectorsTermsWriterPerThread extends TermsHashConsumerPerThread {

	final TermVectorsTermsWriter termsWriter;
	final TermsHashPerThread termsHashPerThread;
	final DocumentsWriter.DocState docState;

	TermVectorsTermsWriter.PerDoc doc;

	public TermVectorsTermsWriterPerThread(
			TermsHashPerThread termsHashPerThread,
			TermVectorsTermsWriter termsWriter) {
		this.termsWriter = termsWriter;
		this.termsHashPerThread = termsHashPerThread;
		docState = termsHashPerThread.docState;
	}

	final ByteSliceReader vectorSliceReader = new ByteSliceReader();

	final UnicodeUtil.UTF8Result utf8Results[] = {
			new UnicodeUtil.UTF8Result(), new UnicodeUtil.UTF8Result() };

	@Override
	public void startDocument() {
		assert clearLastVectorFieldName();
		if (doc != null) {
			doc.reset();
			doc.docID = docState.docID;
		}
	}

	@Override
	public DocumentsWriter.DocWriter finishDocument() {
		try {
			return doc;
		} finally {
			doc = null;
		}
	}

	@Override
	public TermsHashConsumerPerField addField(
			TermsHashPerField termsHashPerField, FieldInfo fieldInfo) {
		return new TermVectorsTermsWriterPerField(termsHashPerField, this,
				fieldInfo);
	}

	@Override
	public void abort() {
		if (doc != null) {
			doc.abort();
			doc = null;
		}
	}

	final boolean clearLastVectorFieldName() {
		lastVectorFieldName = null;
		return true;
	}

	String lastVectorFieldName;

	final boolean vectorFieldsInOrder(FieldInfo fi) {
		try {
			if (lastVectorFieldName != null)
				return lastVectorFieldName.compareTo(fi.name) < 0;
			else
				return true;
		} finally {
			lastVectorFieldName = fi.name;
		}
	}
}
