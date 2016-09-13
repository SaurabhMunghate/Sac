/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.memdb;

public abstract class IndexType {
	protected int indexBy = -1;

	public IndexType(int indexBy) {
		this.indexBy = indexBy;
	}

	public abstract boolean shouldAddRow(String[] rowStrings);

	public abstract void addToIndex(String[] rowStrings, int size);

	public abstract int[] find(String val);
}
