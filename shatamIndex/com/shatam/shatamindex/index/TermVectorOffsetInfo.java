/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Serializable;

public class TermVectorOffsetInfo implements Serializable {

	public transient static final TermVectorOffsetInfo[] EMPTY_OFFSET_INFO = new TermVectorOffsetInfo[0];
	private int startOffset;
	private int endOffset;

	public TermVectorOffsetInfo() {
	}

	public TermVectorOffsetInfo(int startOffset, int endOffset) {
		this.endOffset = endOffset;
		this.startOffset = startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TermVectorOffsetInfo))
			return false;

		final TermVectorOffsetInfo termVectorOffsetInfo = (TermVectorOffsetInfo) o;

		if (endOffset != termVectorOffsetInfo.endOffset)
			return false;
		if (startOffset != termVectorOffsetInfo.startOffset)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = startOffset;
		result = 29 * result + endOffset;
		return result;
	}
}
