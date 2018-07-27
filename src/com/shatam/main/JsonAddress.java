/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpRequest;
import org.json.JSONArray;

import com.shatam.util.BoostAddress;
import com.shatam.util.U;
import com.test.SacLatency;

public class JsonAddress {
	public static boolean flag = false;
	org.json.JSONArray objOutput = new JSONArray();

	public static void main(String[] args) throws Exception {
		String textEntered = null;
		String noOfJobs = "50";
		String dataSource = "USPS and TIGER";
		String hitscore = "";
		String maxResults = "1";
		BufferedReader br = null;
		org.json.JSONArray outputObj = null;
		String sCurrentLine;
		br = new BufferedReader(new FileReader("E:\\AddressFile\\2.txt"));
		while ((sCurrentLine = br.readLine()) != null) {
			textEntered = sCurrentLine;
			JsonAddress md = new JsonAddress();
			//outputObj = md.jsonAddress(textEntered, hitscore, maxResults,
			//		noOfJobs, dataSource, false);
		}
		if (br != null)
			br.close();
	}
	/*
	 * This method is without distance criteria as parameter.
	 */
	public org.json.JSONArray jsonAddress(String textEntered, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag)
			throws Exception {
		return jsonAddress(textEntered, hitscore,maxResults, noOfJobs, dataSource, flag, 90, new BoostAddress()); //90 is default.
	}
	/*
	 * This method is with distance criteria as parameter.
	 */
	public org.json.JSONArray jsonAddress(String textEntered, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag, int distanceCriteria)
			throws Exception {
		return jsonAddress(textEntered, hitscore,maxResults, noOfJobs, dataSource, flag, distanceCriteria, new BoostAddress());
	}
	/*
	 * This method is with city & zip weight as its parameter.
	 */
	public org.json.JSONArray jsonAddress(String textEntered, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag, int cityWeight, int zipWeight)
			throws Exception {
		return jsonAddress(textEntered, hitscore,maxResults, noOfJobs, dataSource, flag, 90, new BoostAddress(cityWeight, zipWeight));//90 is default.
	}
	/*
	 * This method is with distance criteria as parameter along with city & zip weight as its parameter.
	 */
	public org.json.JSONArray jsonAddress(String textEntered, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag, int distanceCriteria, int cityWeight, int zipWeight)
			throws Exception {

		return jsonAddress(textEntered, hitscore,maxResults, noOfJobs, dataSource, flag, distanceCriteria, new BoostAddress(cityWeight, zipWeight));
	}	

	private org.json.JSONArray jsonAddress(String textEntered, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag, int distanceCriteria, BoostAddress boostAddress)
			throws Exception {
		org.json.JSONArray outputObj = null;
		JsonPostHandler jph = new JsonPostHandler();
		char ch = textEntered.charAt(0);
		if (ch != '[') {
			textEntered = textEntered.substring(1, textEntered.length() - 1);
		}
		textEntered = textEntered.replace("\\", "").trim();
		TestDemo td = new TestDemo();
		td.addArray();
		HashMap<String, ArrayList<InputJsonSchema>> addressMap = td
				.sortAddress(textEntered);
		ArrayList<InputJsonSchema> addList = null;
		Set<String> addSet = addressMap.keySet();
		int noOfOutput = 0;
		int inputAddressCount = 0;
		long s = System.currentTimeMillis();
		for (String m : addSet) {
			if (!m.contains("no state") && !m.contains("missing")) {
				addList = addressMap.get(m);
				inputAddressCount += addList.size();
				outputObj = jph.processJsonFileforSAC(addList, hitscore,
						maxResults, noOfJobs, dataSource, flag, distanceCriteria, boostAddress);
				objOutput.put(outputObj);
				noOfOutput += outputObj.length();
			} else {
				if (m.contains("missing")) {
					addList = addressMap.get(m);

					inputAddressCount += addList.size();
					for (int i = 0; i < addList.size(); i++) {
						InputJsonSchema innerArr = addList.get(i);
						String key = innerArr.key;
						String arr[] = { key, "Some fields are missing", "",
								"", "", "", "", "", "", "", "", "", "", "", "" };
						outputObj = new JSONArray(Arrays.asList(arr));
						outputObj = new JSONArray(Arrays.asList(outputObj));
						outputObj = new JSONArray(Arrays.asList(outputObj));
						objOutput.put(outputObj);
						noOfOutput += outputObj.length();
					}
				}
				if (m.contains("no state")) {
					addList = addressMap.get(m);
					inputAddressCount += addList.size();
					for (int i = 0; i < addList.size(); i++) {
						InputJsonSchema innerArr = addList.get(i);
						String key = innerArr.key;
						{
							String arr[] = { key, "State is invalid", "", "",
									"", "", "", "", "", "", "", "", "", "", "" };
							outputObj = new JSONArray(Arrays.asList(arr));
							outputObj = new JSONArray(Arrays.asList(outputObj));
							outputObj = new JSONArray(Arrays.asList(outputObj));
							objOutput.put(outputObj);
							noOfOutput += outputObj.length();
						}
					}
				}
			}
		}
		long e = System.currentTimeMillis();
		flag = true;
		//========= End =================
//		SacLatency.writeLatency(""+(e - s));
		U.log("Total SAC TIME::" + (e - s));
		U.log("Number of INPUT addresses==" + inputAddressCount);
		U.log("Number of OUTPUT addresses===" + noOfOutput);
		return objOutput;

	}
}
