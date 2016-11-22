/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import java.io.Serializable;

import com.shatam.shatamindex.util.AttributeImpl;

public class FlagsAttributeImpl extends AttributeImpl implements
		FlagsAttribute, Cloneable, Serializable {
	private int flags = 0;

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	@Override
	public void clear() {
		flags = 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof FlagsAttributeImpl) {
			return ((FlagsAttributeImpl) other).flags == flags;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return flags;
	}

	@Override
	public void copyTo(AttributeImpl target) {
		FlagsAttribute t = (FlagsAttribute) target;
		t.setFlags(flags);
	}
}
