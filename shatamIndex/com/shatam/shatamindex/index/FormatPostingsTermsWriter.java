/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Closeable;
import java.io.IOException;

final class FormatPostingsTermsWriter extends FormatPostingsTermsConsumer
		implements Closeable {

	final FormatPostingsFieldsWriter parent;
	final FormatPostingsDocsWriter docsWriter;
	final TermInfosWriter termsOut;
	FieldInfo fieldInfo;

	FormatPostingsTermsWriter(SegmentWriteState state,
			FormatPostingsFieldsWriter parent) throws IOException {
		this.parent = parent;
		termsOut = parent.termsOut;
		docsWriter = new FormatPostingsDocsWriter(state, this);
	}

	void setField(FieldInfo fieldInfo) {
		this.fieldInfo = fieldInfo;
		docsWriter.setField(fieldInfo);
	}

	char[] currentTerm;
	int currentTermStart;

	long freqStart;
	long proxStart;

	@Override
	FormatPostingsDocsConsumer addTerm(char[] text, int start) {
		currentTerm = text;
		currentTermStart = start;

		freqStart = docsWriter.out.getFilePointer();
		if (docsWriter.posWriter.out != null)
			proxStart = docsWriter.posWriter.out.getFilePointer();

		parent.skipListWriter.resetSkip();

		return docsWriter;
	}

	@Override
	void finish() {
	}

	public void close() throws IOException {
		docsWriter.close();
	}
}
