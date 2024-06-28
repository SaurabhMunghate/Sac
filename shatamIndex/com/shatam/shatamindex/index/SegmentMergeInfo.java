/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.index.PayloadProcessorProvider.DirPayloadProcessor;

final class SegmentMergeInfo {
	Term term;
	int base;
	int ord;
	TermEnum termEnum;
	IndexReader reader;
	int delCount;
	private TermPositions postings;
	private int[] docMap;
	DirPayloadProcessor dirPayloadProcessor;

	SegmentMergeInfo(int b, TermEnum te, IndexReader r) throws IOException {
		base = b;
		reader = r;
		termEnum = te;
		term = te.term();
	}

	int[] getDocMap() {
		if (docMap == null) {
			delCount = 0;

			if (reader.hasDeletions()) {
				int maxDoc = reader.maxDoc();
				docMap = new int[maxDoc];
				int j = 0;
				for (int i = 0; i < maxDoc; i++) {
					if (reader.isDeleted(i)) {
						delCount++;
						docMap[i] = -1;
					} else
						docMap[i] = j++;
				}
			}
		}
		return docMap;
	}

	TermPositions getPositions() throws IOException {
		if (postings == null) {
			postings = reader.termPositions();
		}
		return postings;
	}

	final boolean next() throws IOException {
		if (termEnum.next()) {
			term = termEnum.term();
			return true;
		} else {
			term = null;
			return false;
		}
	}

	final void close() throws IOException {
		try {
			termEnum.close();
		} finally {
			if (postings != null) {
				postings.close();
			}
		}
	}
}
