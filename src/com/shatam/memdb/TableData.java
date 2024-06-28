/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.memdb;

import java.util.ArrayList;

import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class TableData {
	private ArrayList<String[]> data = new ArrayList<String[]>();

	private IndexType indexType;

	public TableData(IndexType indexType) {
		this.indexType = indexType;
	}

	public void add(Object[] rowObjects, int maxColns) {

		String[] rowStrings = StrUtil.convertToStringArr(rowObjects, maxColns);

		if (this.indexType != null) {
			if (!indexType.shouldAddRow(rowStrings)) {

				return;
			}

			indexType.addToIndex(rowStrings, data.size());

		}

		data.add(rowStrings);

	}

	public ArrayList<String[]> searchAndFind(String val) throws Exception {
		ArrayList<String[]> retList = new ArrayList<String[]>();
		if (val == null)
			return retList;

		if (this.indexType == null)
			throw new Exception("Not indexed");

		val = val.trim().toLowerCase();

		int[] foundAt = indexType.find(val);
		for (int i : foundAt) {
			retList.add(data.get(i));
		}

		return retList;
	}

	public int size() {

		return data.size();
	}

	public String[] getRowAt(int rowNumber) {

		return data.get(rowNumber);
	}

	public void close() {

		data.clear();
		System.gc();
	}
}
