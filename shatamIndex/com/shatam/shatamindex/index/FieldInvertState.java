/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.util.AttributeSource;

public final class FieldInvertState {
	int position;
	int length;
	int numOverlap;
	int offset;
	int maxTermFrequency;
	int uniqueTermCount;
	float boost;
	AttributeSource attributeSource;

	public FieldInvertState() {
	}

	public FieldInvertState(int position, int length, int numOverlap,
			int offset, float boost) {
		this.position = position;
		this.length = length;
		this.numOverlap = numOverlap;
		this.offset = offset;
		this.boost = boost;
	}

	void reset(float docBoost) {
		position = 0;
		length = 0;
		numOverlap = 0;
		offset = 0;
		maxTermFrequency = 0;
		uniqueTermCount = 0;
		boost = docBoost;
		attributeSource = null;
	}

	public int getPosition() {
		return position;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getNumOverlap() {
		return numOverlap;
	}

	public void setNumOverlap(int numOverlap) {
		this.numOverlap = numOverlap;
	}

	public int getOffset() {
		return offset;
	}

	public float getBoost() {
		return boost;
	}

	public void setBoost(float boost) {
		this.boost = boost;
	}

	public int getMaxTermFrequency() {
		return maxTermFrequency;
	}

	public int getUniqueTermCount() {
		return uniqueTermCount;
	}

	public AttributeSource getAttributeSource() {
		return attributeSource;
	}
}
