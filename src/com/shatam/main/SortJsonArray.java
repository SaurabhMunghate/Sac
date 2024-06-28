/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;

import com.shatam.util.U;

public class SortJsonArray {

	public static void main(String[] args) throws Exception {

		File f = new File("D:\\Sample_Addresses_DMP\\StateJsonFiles\\ALL.txt");
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		String state = null;
		StringBuffer buf = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			buf.append(line);
		}
		ArrayList<InputJsonSchema> INPUT_LIST = new ArrayList<InputJsonSchema>();
		String textEntered = buf.toString();
		org.json.JSONArray arr = new org.json.JSONArray(textEntered);
		for (int i = 0; i < arr.length(); i++) {
			org.json.JSONArray innerArr = arr.getJSONArray(i);
			InputJsonSchema obj = new InputJsonSchema();
			obj.address1 = innerArr.getString(0);
			obj.address2 = innerArr.getString(1);
			obj.city = innerArr.getString(2);
			obj.state = innerArr.getString(3);
			obj.zip = innerArr.getString(4);
			obj.key = innerArr.getString(5);
			INPUT_LIST.add(obj);
		}
		Collections.sort(INPUT_LIST);
		for (InputJsonSchema obj : INPUT_LIST) {
			U.log(obj.state);
		}
	}

	public ArrayList<InputJsonSchema> sortInputAddress(String textEntered)
			throws Exception {
		ArrayList<InputJsonSchema> INPUT_LIST = new ArrayList<InputJsonSchema>();
		org.json.JSONArray arr = new org.json.JSONArray(textEntered);
		for (int i = 0; i < arr.length(); i++) {
			org.json.JSONArray innerArr = arr.getJSONArray(i);
			InputJsonSchema obj = new InputJsonSchema();
			obj.key = innerArr.getString(0);
			obj.address1 = innerArr.getString(1);
			obj.address2 = innerArr.getString(2);
			obj.city = innerArr.getString(3);
			obj.state = innerArr.getString(4);
			obj.zip = innerArr.getString(5);
			INPUT_LIST.add(obj);
		}
		Collections.sort(INPUT_LIST);

		return INPUT_LIST;
	}
}
