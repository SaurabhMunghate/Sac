/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

class SegmentTermPositionVector extends SegmentTermVector implements
		TermPositionVector {
	protected int[][] positions;
	protected TermVectorOffsetInfo[][] offsets;
	public static final int[] EMPTY_TERM_POS = new int[0];

	public SegmentTermPositionVector(String field, String terms[],
			int termFreqs[], int[][] positions, TermVectorOffsetInfo[][] offsets) {
		super(field, terms, termFreqs);
		this.offsets = offsets;
		this.positions = positions;
	}

	public TermVectorOffsetInfo[] getOffsets(int index) {
		TermVectorOffsetInfo[] result = TermVectorOffsetInfo.EMPTY_OFFSET_INFO;
		if (offsets == null)
			return null;
		if (index >= 0 && index < offsets.length) {
			result = offsets[index];
		}
		return result;
	}

	public int[] getTermPositions(int index) {
		int[] result = EMPTY_TERM_POS;
		if (positions == null)
			return null;
		if (index >= 0 && index < positions.length) {
			result = positions[index];
		}

		return result;
	}
}