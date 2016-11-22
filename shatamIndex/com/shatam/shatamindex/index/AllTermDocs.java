/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.util.BitVector;

class AllTermDocs extends AbstractAllTermDocs {

	protected BitVector deletedDocs;

	protected AllTermDocs(SegmentReader parent) {
		super(parent.maxDoc());
		synchronized (parent) {
			this.deletedDocs = parent.deletedDocs;
		}
	}

	@Override
	public boolean isDeleted(int doc) {
		return deletedDocs != null && deletedDocs.get(doc);
	}
}
