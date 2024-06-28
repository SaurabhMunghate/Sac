/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.json.JSONArray;

import com.shatam.util.U;
import com.shatam.zip.search.ZipSearch;

public class TestDemo {
	MultiMap addMap = new MultiHashMap();

	ArrayList<String> addList = new ArrayList<String>();
	boolean flag = true;

	public void addArray() {

		addList.add("AK");
		addList.add("AL");
		addList.add("AR");
		addList.add("AS");
		addList.add("AZ");
		addList.add("CA");
		addList.add("CO");
		addList.add("CT");
		addList.add("DC");
		addList.add("DE");
		addList.add("FL");
		addList.add("FM");
		addList.add("GA");
		addList.add("GU");
		addList.add("HI");
		addList.add("IA");
		addList.add("ID");
		addList.add("IL");
		addList.add("IN");
		addList.add("KS");
		addList.add("KY");
		addList.add("LA");
		addList.add("MA");
		addList.add("MD");
		addList.add("ME");
		addList.add("MH");
		addList.add("MI");
		addList.add("MN");
		addList.add("MO");
		addList.add("MP");
		addList.add("MS");
		addList.add("MT");
		addList.add("NC");
		addList.add("ND");
		addList.add("NE");
		addList.add("NH");
		addList.add("NJ");
		addList.add("NM");
		addList.add("NV");
		addList.add("NY");
		addList.add("OH");
		addList.add("OK");
		addList.add("OR");
		addList.add("PA");
		addList.add("PR");
		addList.add("PW");
		addList.add("RI");
		addList.add("SC");
		addList.add("SD");
		addList.add("TN");
		addList.add("TX");
		addList.add("UT");
		addList.add("VA");
		addList.add("VI");
		addList.add("VT");
		addList.add("WA");
		addList.add("WI");
		addList.add("WV");
		addList.add("WY");
	}

	public HashMap<String, ArrayList<InputJsonSchema>> getAddress(
			InputJsonSchema textEntered, String state) {

		if (addList.contains(state.trim().toUpperCase())) {
			addMap.put(state, textEntered);
		}
		if (!addMap.containsKey(state)) {
			if (flag == false) {
				addMap.put("no state", textEntered);
				flag = true;
			}else if(!textEntered.hasValidZip){			
				//System.out.println("invalidzip");
				addMap.put("invalidzip", textEntered);
			}
			else {
				addMap.put("missing", textEntered);
			}
		}
		return (HashMap<String, ArrayList<InputJsonSchema>>) addMap;
	}

	public HashMap<String, ArrayList<InputJsonSchema>> sortAddress(
			String textEntered) throws Exception {
		HashMap<String, ArrayList<InputJsonSchema>> addList = new HashMap<String, ArrayList<InputJsonSchema>>();
		org.json.JSONArray arr = new org.json.JSONArray(textEntered);
		long strtTime = System.currentTimeMillis();
		int groupId = 1;
		for (int i = 0; i < arr.length(); i++) {
			org.json.JSONArray innerArr = arr.getJSONArray(i);
			ZipSearch d = new ZipSearch();
			if (innerArr.length() == 6) {
				List<Object> rscst = d.getJSONArrays(innerArr);
				if (rscst != null && rscst.size() == 2) {					
					List<org.json.JSONArray> innerArrList = (List) rscst.get(1);
					//Looping here.
					for (JSONArray ar1 : innerArrList) {
						if (ar1.length() == 6) {
							InputJsonSchema obj = new InputJsonSchema();
							obj.address1 = ar1.getString(1);
							obj.address2 = ar1.getString(2);
							obj.city = ar1.getString(3);
							obj.state = ar1.getString(4).toUpperCase();
							obj.zip = ar1.getString(5);
							obj.key = ar1.getString(0);
							//If the zip belong to one city Then no need to assign group id.
							if(innerArrList.size()>1)
							obj.groupId = groupId;
							flag = false;
							addList = getAddress(obj, obj.state);
						}
					}
					groupId++;
					continue;
				}else if (rscst != null && rscst.size() == 1) {							
						InputJsonSchema obj = new InputJsonSchema();
						obj.address1 = innerArr.getString(1);
						obj.address2 = innerArr.getString(2);
						obj.city = innerArr.getString(3);
						obj.state = innerArr.getString(4);
						obj.zip = innerArr.getString(5);
						obj.key = innerArr.getString(0);	
						obj.hasValidZip = false;
						flag = true;
						addList = getAddress(obj, obj.state);
						continue;
				}
			}
			if (innerArr.length() == 6) {
				InputJsonSchema obj = new InputJsonSchema();
				obj.address1 = innerArr.getString(1);
				obj.address2 = innerArr.getString(2);
				obj.city = innerArr.getString(3);
				obj.state = innerArr.getString(4);
				obj.zip = innerArr.getString(5);
				obj.key = innerArr.getString(0);
				flag = false;
				addList = getAddress(obj, obj.state);

			} else {
				flag = true;
				org.json.JSONArray innerArr1 = arr.getJSONArray(i);
				InputJsonSchema obj = new InputJsonSchema();
				obj.address1 = "CHECK JSON FORMAT";
				obj.address2 = "";
				obj.city = "";
				obj.state = "";
				obj.zip = "";
				if (innerArr1.length() > 1)
					obj.key = innerArr.getString(0);
				else
					obj.key = "";
				addList = getAddress(obj, obj.state);
			}

		}
		long endTime = System.currentTimeMillis();

		return addList;
	}
}