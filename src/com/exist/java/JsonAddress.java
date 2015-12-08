package com.exist.java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;

import com.shatam.util.U;

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
			// System.out.println(sCurrentLine);
			textEntered = sCurrentLine;
			// textEntered
			// ="[[\"205 207 MADISON AVENUE  ST. MARYS\",\" \",\"SAINT MARYS\",\"GA\",\"31558\",\"536\"]]";
			JsonAddress md = new JsonAddress();

			// GenerateCache.doCache();
			outputObj = md.jsonAddress(textEntered, hitscore, maxResults,
					noOfJobs, dataSource, false);

		}
		if (br != null)
			br.close();
	}

	public org.json.JSONArray jsonAddress(String textEntered, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag)
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
	//	textEntered=textEntered.replaceAll("CHECK|check", "check ");
		HashMap<String, ArrayList<InputJsonSchema>> addressMap = td
				.sortAddress(textEntered);
		
		ArrayList<InputJsonSchema> addList = null;
		Set<String> addSet = addressMap.keySet();
		int noOfOutput=0;
		int inputAddressCount=0;
		long s = System.currentTimeMillis();
		for (String m : addSet) {
		//	U.log(m);
			if(!m.contains("no state")&&!m.contains("missing")){
			addList = addressMap.get(m);
			inputAddressCount+=addList.size();
			outputObj = jph.processJsonFileforSAC(addList, hitscore,
					maxResults, noOfJobs, dataSource, flag);
	//	 U.log(outputObj.toString());
			objOutput.put(outputObj);
noOfOutput+=outputObj.length();
			}
			else{
				if(m.contains("missing")){
				addList = addressMap.get(m);
				//U.log("@@@@@@@@@@"+addList.size());
				inputAddressCount+=addList.size();
				for (int i = 0; i < addList.size(); i++) {
					//U.log("found no state address");
					InputJsonSchema innerArr = addList.get(i);
					
					String key = innerArr.key;
					
					String arr[]={key,"Some fields are missing","","","","","","","","","","","","",""};
					outputObj=new JSONArray(Arrays.asList(arr));
					outputObj=new JSONArray(Arrays.asList(outputObj));
					outputObj=new JSONArray(Arrays.asList(outputObj));
					//U.log("::::::::::::"+outputObj.length());
					objOutput.put(outputObj);
					noOfOutput+=outputObj.length();
				}
			}
				if(m.contains("no state")){
					addList = addressMap.get(m);
					//U.log("@@@@@@@@@@>>>>>"+addList.size());
					inputAddressCount+=addList.size();
					for (int i = 0; i < addList.size(); i++) {
						//U.log("found no state address:::");
						
						InputJsonSchema innerArr = addList.get(i);
						String key = innerArr.key;
						//if(outputObj.get(key)!=null)
						{
						String arr[]={key,"State is invalid","","","","","","","","","","","","",""};
						outputObj=new JSONArray(Arrays.asList(arr));
						outputObj=new JSONArray(Arrays.asList(outputObj));
						outputObj=new JSONArray(Arrays.asList(outputObj));
					//	U.log("::::::::::::"+outputObj.length());
						objOutput.put(outputObj);
						noOfOutput+=outputObj.length();
						}
				}
			}
		}
		}
		long e = System.currentTimeMillis();
		flag = true;
		
	//	U.log(objOutput);
		U.log("Total SAC TIME::" + (e - s));
		U.log("Number of INPUT addresses=="+inputAddressCount);
		U.log("Number of OUTPUT addresses==="+noOfOutput);  
		return objOutput;

	}
}
