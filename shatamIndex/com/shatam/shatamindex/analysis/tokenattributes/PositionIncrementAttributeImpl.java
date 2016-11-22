/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import java.io.Serializable;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.util.AttributeImpl;

public class PositionIncrementAttributeImpl extends AttributeImpl implements
		PositionIncrementAttribute, Cloneable, Serializable {
	private int positionIncrement = 1;

	public void setPositionIncrement(int positionIncrement) {
		if (positionIncrement < 0)
			throw new IllegalArgumentException(
					"Increment must be zero or greater: " + positionIncrement);
		this.positionIncrement = positionIncrement;
	}

	public int getPositionIncrement() {
		return positionIncrement;
	}

	@Override
	public void clear() {
		this.positionIncrement = 1;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (other instanceof PositionIncrementAttributeImpl) {
			return positionIncrement == ((PositionIncrementAttributeImpl) other).positionIncrement;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return positionIncrement;
	}

	@Override
	public void copyTo(AttributeImpl target) {
		PositionIncrementAttribute t = (PositionIncrementAttribute) target;
		t.setPositionIncrement(positionIncrement);
	}

}
