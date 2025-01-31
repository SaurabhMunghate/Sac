/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Serializable;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.util.ArrayUtil;

public class Payload implements Serializable, Cloneable {

	protected byte[] data;

	protected int offset;

	protected int length;

	public Payload() {

	}

	public Payload(byte[] data) {
		this(data, 0, data.length);
	}

	public Payload(byte[] data, int offset, int length) {
		if (offset < 0 || offset + length > data.length) {
			throw new IllegalArgumentException();
		}
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	public void setData(byte[] data) {
		setData(data, 0, data.length);
	}

	public void setData(byte[] data, int offset, int length) {
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	public byte[] getData() {
		return this.data;
	}

	public int getOffset() {
		return this.offset;
	}

	public int length() {
		return this.length;
	}

	public byte byteAt(int index) {
		if (0 <= index && index < this.length) {
			return this.data[this.offset + index];
		}
		throw new ArrayIndexOutOfBoundsException(index);
	}

	public byte[] toByteArray() {
		byte[] retArray = new byte[this.length];
		System.arraycopy(this.data, this.offset, retArray, 0, this.length);
		return retArray;
	}

	public void copyTo(byte[] target, int targetOffset) {
		if (this.length > target.length + targetOffset) {
			throw new ArrayIndexOutOfBoundsException();
		}
		System.arraycopy(this.data, this.offset, target, targetOffset,
				this.length);
	}

	@Override
	public Object clone() {
		try {

			Payload clone = (Payload) super.clone();

			if (offset == 0 && length == data.length) {

				clone.data = data.clone();
			} else {

				clone.data = this.toByteArray();
				clone.offset = 0;
			}
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof Payload) {
			Payload other = (Payload) obj;
			if (length == other.length) {
				for (int i = 0; i < length; i++)
					if (data[offset + i] != other.data[other.offset + i])
						return false;
				return true;
			} else
				return false;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return ArrayUtil.hashCode(data, offset, offset + length);
	}
}
