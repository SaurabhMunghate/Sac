/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.util.IOUtils;

final class FormatPostingsFieldsWriter extends FormatPostingsFieldsConsumer {

	final Directory dir;
	final String segment;
	TermInfosWriter termsOut;
	final FieldInfos fieldInfos;
	FormatPostingsTermsWriter termsWriter;
	final DefaultSkipListWriter skipListWriter;
	final int totalNumDocs;

	public FormatPostingsFieldsWriter(SegmentWriteState state,
			FieldInfos fieldInfos) throws IOException {
		dir = state.directory;
		segment = state.segmentName;
		totalNumDocs = state.numDocs;
		this.fieldInfos = fieldInfos;
		boolean success = false;
		try {
			termsOut = new TermInfosWriter(dir, segment, fieldInfos,
					state.termIndexInterval);

			skipListWriter = new DefaultSkipListWriter(termsOut.skipInterval,
					termsOut.maxSkipLevels, totalNumDocs, null, null);

			termsWriter = new FormatPostingsTermsWriter(state, this);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(termsOut, termsWriter);
			}
		}
	}

	@Override
	FormatPostingsTermsConsumer addField(FieldInfo field) {
		termsWriter.setField(field);
		return termsWriter;
	}

	@Override
	void finish() throws IOException {
		IOUtils.close(termsOut, termsWriter);
	}
}
