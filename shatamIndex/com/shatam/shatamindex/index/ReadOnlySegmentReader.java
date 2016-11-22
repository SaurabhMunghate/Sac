/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

class ReadOnlySegmentReader extends SegmentReader {

	static void noWrite() {
		throw new UnsupportedOperationException(
				"This IndexReader cannot make any changes to the index (it was opened with readOnly = true)");
	}

	@Override
	protected void acquireWriteLock() {
		noWrite();
	}

	@Override
	public boolean isDeleted(int n) {
		return deletedDocs != null && deletedDocs.get(n);
	}
}
