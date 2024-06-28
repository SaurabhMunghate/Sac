/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import java.io.Serializable;

import com.shatam.shatamindex.index.Payload;
import com.shatam.shatamindex.util.AttributeImpl;

public class PayloadAttributeImpl extends AttributeImpl implements
		PayloadAttribute, Cloneable, Serializable {
	private Payload payload;

	public PayloadAttributeImpl() {
	}

	public PayloadAttributeImpl(Payload payload) {
		this.payload = payload;
	}

	public Payload getPayload() {
		return this.payload;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	@Override
	public void clear() {
		payload = null;
	}

	@Override
	public Object clone() {
		PayloadAttributeImpl clone = (PayloadAttributeImpl) super.clone();
		if (payload != null) {
			clone.payload = (Payload) payload.clone();
		}
		return clone;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (other instanceof PayloadAttribute) {
			PayloadAttributeImpl o = (PayloadAttributeImpl) other;
			if (o.payload == null || payload == null) {
				return o.payload == null && payload == null;
			}

			return o.payload.equals(payload);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (payload == null) ? 0 : payload.hashCode();
	}

	@Override
	public void copyTo(AttributeImpl target) {
		PayloadAttribute t = (PayloadAttribute) target;
		t.setPayload((payload == null) ? null : (Payload) payload.clone());
	}

}
