/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.index.FreqProxTermsWriterPerField.FreqProxPostingsArray;

final class FreqProxFieldMergeState {

	final FreqProxTermsWriterPerField field;
	final int numPostings;
	final CharBlockPool charPool;
	final int[] termIDs;
	final FreqProxPostingsArray postings;
	int currentTermID;

	char[] text;
	int textOffset;

	private int postingUpto = -1;

	final ByteSliceReader freq = new ByteSliceReader();
	final ByteSliceReader prox = new ByteSliceReader();

	int docID;
	int termFreq;

	public FreqProxFieldMergeState(FreqProxTermsWriterPerField field) {
		this.field = field;
		this.charPool = field.perThread.termsHashPerThread.charPool;
		this.numPostings = field.termsHashPerField.numPostings;
		this.termIDs = field.termsHashPerField.sortPostings();
		this.postings = (FreqProxPostingsArray) field.termsHashPerField.postingsArray;
	}

	boolean nextTerm() throws IOException {
		postingUpto++;
		if (postingUpto == numPostings)
			return false;

		currentTermID = termIDs[postingUpto];
		docID = 0;

		final int textStart = postings.textStarts[currentTermID];
		text = charPool.buffers[textStart >> DocumentsWriter.CHAR_BLOCK_SHIFT];
		textOffset = textStart & DocumentsWriter.CHAR_BLOCK_MASK;

		field.termsHashPerField.initReader(freq, currentTermID, 0);
		if (field.fieldInfo.indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
			field.termsHashPerField.initReader(prox, currentTermID, 1);

		boolean result = nextDoc();
		assert result;

		return true;
	}

	public String termText() {
		int upto = textOffset;
		while (text[upto] != 0xffff) {
			upto++;
		}
		return new String(text, textOffset, upto - textOffset);
	}

	public boolean nextDoc() throws IOException {
		if (freq.eof()) {
			if (postings.lastDocCodes[currentTermID] != -1) {

				docID = postings.lastDocIDs[currentTermID];
				if (field.indexOptions != IndexOptions.DOCS_ONLY)
					termFreq = postings.docFreqs[currentTermID];
				postings.lastDocCodes[currentTermID] = -1;
				return true;
			} else

				return false;
		}

		final int code = freq.readVInt();
		if (field.indexOptions == IndexOptions.DOCS_ONLY)
			docID += code;
		else {
			docID += code >>> 1;
			if ((code & 1) != 0)
				termFreq = 1;
			else
				termFreq = freq.readVInt();
		}

		assert docID != postings.lastDocIDs[currentTermID];

		return true;
	}
}
