package com.exist.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import com.shatam.util.U;

public class TestDemo {
	MultiMap addMap = new MultiHashMap();
	// ArrayList<InputJsonSchema> AZ = new ArrayList<>();
	// ArrayList<InputJsonSchema> GA = new ArrayList<>();
	ArrayList<String> addList = new ArrayList<String>();
  boolean flag=true;
	public void addArray() {
		/*addList.add("AK");
		addList.add("AL");
		addList.add("AR");
		addList.add("AZ");
		addList.add("CA");
		addList.add("CO");
		addList.add("CT");
		addList.add("DC");
		addList.add("FL");
		addList.add("GA");
		addList.add("HI");
		addList.add("IA");
		addList.add("ID");
		addList.add("IL");
		addList.add("IN");
		//addList.add("IO");
		//addList.add("IS");
		addList.add("KS");
		addList.add("KY");
		addList.add("LA");
		addList.add("MA");
		addList.add("MD");
		addList.add("ME");
		addList.add("MI");
		addList.add("MN");
		addList.add("MO");
		addList.add("MS");
		addList.add("MT");
		addList.add("NA");
		addList.add("NB");
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
		//addList.add("PE");
		addList.add("PI");
		addList.add("PR");
		addList.add("RI");
		addList.add("RK");
		addList.add("SC");
		addList.add("TN");
		addList.add("TX");
		addList.add("UT");
		addList.add("VA");
		addList.add("VI");
		addList.add("VT");
		addList.add("WA");
		addList.add("WI");
		addList.add("WV");
		addList.add("WY");*/
		addList.add("AK");
		addList.add("AL");
		addList.add("AR");

		addList.add("AS");
		addList.add("AZ");//
		addList.add("CA");
		addList.add("CO");
		addList.add("CT");
		addList.add("DC");
		addList.add("DE");//

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

		//addList.add("UM", "74");
		addList.add("UT");
		addList.add("VA");

		addList.add("VI");
		addList.add("VT");
		addList.add("WA");
		addList.add("WI");
		addList.add("WV");
		addList.add("WY");
		
		//U.log("Added Data to Arraylist::" + addList.size());
	}

	public HashMap<String, ArrayList<InputJsonSchema>> getAddress(
			InputJsonSchema textEntered, String state) {

		if (addList.contains(state.trim().toUpperCase())) 
		{
		addMap.put(state, textEntered);
		}
		if (!addMap.containsKey(state)) {
			if(flag==false){
				//U.log("1 no state added");
				addMap.put("no state", textEntered);
				
				flag=true;
			}
			else{
				addMap.put("missing", textEntered);
			}
		}
		//U.log("Inside Map::" + addList.size());
		/*if (addList.contains(state.trim().toUpperCase())) 
		{
			ArrayList<InputJsonSchema> aList = new ArrayList<InputJsonSchema>();
			addMap.put(state, aList);
			addList.remove(state);
		}
		if (!addMap.containsKey(state)) {
			U.log("INVALID STATE::");
			ArrayList<InputJsonSchema> aList = new ArrayList<InputJsonSchema>();
			if(flag==false){
				U.log("1 no state added");
				addMap.put("no state", aList);
				addMap.get("no state").add(textEntered);
				addList.remove("no state");
				flag=true;
			}
			else{
				U.log("1 missing added");
			addMap.put("missing", aList);
			addMap.get("missing").add(textEntered);
			addList.remove("missing");
			flag=false;
			}
		} else {
			addMap.get(state).add(textEntered);
		}*/
		
		
		return (HashMap<String, ArrayList<InputJsonSchema>>) addMap;
	}

	public HashMap<String, ArrayList<InputJsonSchema>> sortAddress(
			String textEntered) throws Exception {
		HashMap<String, ArrayList<InputJsonSchema>> addList = new HashMap<String, ArrayList<InputJsonSchema>>();
		org.json.JSONArray arr = new org.json.JSONArray(textEntered);
		long strtTime=System.currentTimeMillis();
		for (int i = 0; i < arr.length(); i++) {
			org.json.JSONArray innerArr = arr.getJSONArray(i);
			if(innerArr.length()==6){
			InputJsonSchema obj = new InputJsonSchema();
			obj.address1 = innerArr.getString(1);
			obj.address2 = innerArr.getString(2);
			obj.city = innerArr.getString(3);
			obj.state = innerArr.getString(4);
			obj.zip = innerArr.getString(5);
			obj.key = innerArr.getString(0);
			flag=false;
			addList = getAddress(obj, obj.state);
			
			}
			else{
				flag=true;
				org.json.JSONArray innerArr1 = arr.getJSONArray(i);
				InputJsonSchema obj = new InputJsonSchema();
				obj.address1 = "CHECK JSON FORMAT";
				obj.address2 = "";
				obj.city = "";
				obj.state = "";
				obj.zip = "";
				if(innerArr1.length()>1)
				obj.key = innerArr.getString(0);
				else
					obj.key = "";
				
				addList = getAddress(obj, obj.state);
			}
			// U.log(obj.state + "::STATE");
		}
		long endTime=System.currentTimeMillis();
		//U.log("Time required for creating statewise sorting:="+(endTime-strtTime));
		return addList;
	}
}