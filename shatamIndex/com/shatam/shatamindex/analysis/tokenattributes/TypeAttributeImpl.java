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

public class TypeAttributeImpl extends AttributeImpl implements TypeAttribute,
		Cloneable, Serializable {
	private String type;

	public TypeAttributeImpl() {
		this(DEFAULT_TYPE);
	}

	public TypeAttributeImpl(String type) {
		this.type = type;
	}

	public String type() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void clear() {
		type = DEFAULT_TYPE;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (other instanceof TypeAttributeImpl) {
			final TypeAttributeImpl o = (TypeAttributeImpl) other;
			return (this.type == null ? o.type == null : this.type
					.equals(o.type));
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (type == null) ? 0 : type.hashCode();
	}

	@Override
	public void copyTo(AttributeImpl target) {
		TypeAttribute t = (TypeAttribute) target;
		t.setType(type);
	}
}
