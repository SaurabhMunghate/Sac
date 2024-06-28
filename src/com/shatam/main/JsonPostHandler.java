/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

import com.data.main.Logger;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.BoostAddress;
import com.shatam.util.U;

public class JsonPostHandler extends AbstractHandler {
	public String state;
	public String errMsg = null;
	public String hitscore1 = null;
	public String maxResults11 = null;
	public String noOfJobs1 = null;
	public String disable_enable = "enable";
	public ThreadedSAC threadedSAC = new ThreadedSAC();
//	public int distanceCriteria = 0;
	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		ServletInputStream input = request.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		String strLine;
		StringBuffer buf = new StringBuffer();
		while ((strLine = br.readLine()) != null) {
			buf.append(strLine);
		}
		String data1 = buf.toString();
		input.close();
		org.json.JSONArray outputObj = null;
		try {
			org.json.JSONObject inputJSon = new JSONObject(data1);
			data1 = null;
			if (!inputJSon.has("address"))
				throw new Exception(
						"Check Json Format ,Could not find 'address' key In Input Json!");

			if (!inputJSon.has("count"))
				throw new Exception(
						"Check Json Format ,Could not find 'count' key In Input Json !");

			String inputData = inputJSon.get("address").toString().trim();
			String count = inputJSon.get("count").toString().trim();
			String noOfJobs = inputJSon.get("jobs").toString().trim();
			String dataSource = inputJSon.get("data").toString().trim();
			disable_enable = inputJSon.get("log").toString().trim();

			/**
			 * @author Sawan
			 */
			String distCriteria = "";
//			try{
//				distCriteria = inputJSon.get("distance_criteria").toString().trim();
//			}catch(JSONException e){}
			
			String cityWeight = "";
			try{
				cityWeight = inputJSon.get("city_weight").toString().trim();
			}catch(JSONException e){}
			
			String zipWeight = "";
			try{
				zipWeight = inputJSon.get("zip_weight").toString().trim();
			}catch(JSONException e){}
			
			//For Exact Match Search
			String dSearchEnable = "0";
			boolean deepSearchEnable=false;
			
			try{
				dSearchEnable = inputJSon.get("deepSearchEnable").toString().trim();
				if(dSearchEnable.equals("1")){
					deepSearchEnable=true;
				}
				
			}catch(JSONException e){}				
			
			if(cityWeight.isEmpty() || cityWeight.length() == 0 || cityWeight == null)cityWeight = "4";
			if(zipWeight.isEmpty() || zipWeight.length() == 0 || zipWeight == null)zipWeight = "4";
			
			if(distCriteria.trim().isEmpty() || distCriteria.trim().length()<=1 || distCriteria.trim().length()>3 || distCriteria == null)distCriteria="0";
			
			int distanceCriteria = Integer.parseInt(distCriteria);
			int cityBoost = Integer.parseInt(cityWeight);
			int zipBoost = Integer.parseInt(zipWeight);
			/***/
			
			String value = evaluateJson(inputData, count, noOfJobs);

			if (value != null)
				throw new Exception(value);

			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			String ROOT = System.getProperty("user.dir");
			String PATH = new File(ROOT).getParent() + "/LOG/";

			File file = new File(PATH + dateFormat.format(date) + ".txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
			InetAddress IP = InetAddress.getLocalHost();
			if (disable_enable.toLowerCase().contains("enable")) {
				out.write("\n\n************************Input Addresses********="
						+ IP.getHostAddress()
						+ "=***************************\n"
						+ inputData
						+ "\n************************Output Addresses***************************");
			}
			JsonAddress md = new JsonAddress();
			try {
				outputObj = md.jsonAddress(inputData, "", count, noOfJobs,
						dataSource, false,distanceCriteria,deepSearchEnable, cityBoost, zipBoost); //, distanceCriteria, 4, 3
			} catch (Exception e) {
				U.log("Output obj null'");
				e.getMessage();
				out.write("\n" + e.getMessage());
			}
			if (disable_enable.toLowerCase().contains("enable")) {
				out.write("\n" + outputObj.toString());
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getLocalizedMessage().contains(
					"A JSONObject text must begin with"))
				errMsg = "Check Json Format ,Could not find 'address' key or  'count' key In Input Json !";
			try {
				Logger.put(String.class.getName(), e.toString(), "IP Error");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		if (errMsg != null) {
			String err = errMsg;
			errMsg = null;
			response.getWriter().println(err);
		}
		if (outputObj != null)
			response.getWriter().println(outputObj);

	}

	private String evaluateJson(String inputData, String count, String jobs)
			throws Exception {
		char ch = inputData.charAt(0);
		if (ch != '[') {
			inputData = inputData.substring(1, inputData.length() - 1);
		}
		try {
			if (jobs.length() == 0)
				throw new InvalidJsonException(
						"Check Input Of Jobs Value!It should be greater than 0");
			String jobs1 = match(jobs, "[$&+,:;=?@#|*%()^!.~-]");
			String jobs2 = match(jobs, "[a-zA-Z]");
			if (jobs1 != null || jobs2 != null)
				throw new InvalidJsonException(
						"Check Input Of Jobs Value!It Should Be In Number Format. ");
			if (count.length() > 1 || count.length() == 0)
				throw new InvalidJsonException(
						"Check Maximum Addresses Count value! It should be 1, 2, 3 ");
			String count2 = match(count, "[$&+,:;=?@#|*%()^!.~-]");
			String count3 = match(count, "[a-zA-Z]");
			if (count2 != null || count3 != null)
				throw new InvalidJsonException(
						"Check Maximum Addresses Count value! It should be 1, 2, 3 ");
			int val = Integer.parseInt(count.trim());
			if (val > 3)
				throw new InvalidJsonException(
						"Check Maximum Addresses Count value! It should be 1, 2, 3 ");
			if (val <= 0)
				throw new InvalidJsonException(
						"Check Maximum Addresses Count value! It should be 1, 2, 3 ");
			org.json.JSONArray arr = null;
			try {
				arr = new org.json.JSONArray(inputData);
			} catch (Exception e) {
				U.log(":::" + inputData);
			}
			org.json.JSONArray innerArr = arr.getJSONArray(0);
		} catch (InvalidJsonException e) {
			e.printStackTrace();
			errMsg = e.getMessage();
			return e.getMessage();
		} catch (JSONException e) {
			e.printStackTrace();
			errMsg = "Check Json Format ,Entered Json Is Not 2D Json Array !";
			return e.getMessage();
		}

		return null;
	}

	private String evaluateInputAddress(String inputData) {
		try {
			inputData = inputData.replace("\\", "").trim();
			org.json.JSONArray innerArr = new org.json.JSONArray(inputData);
			if (innerArr.length() != 6) {
				throw new InvalidJsonException(
						"Invalid Json Array Exception In Address , JSON Array Length Should be 6.");
			}
			String state = innerArr.getString(3).toUpperCase();
			if (!U.STATE_MAP.containsKey(state.trim())) {
				String state2 = USStates.abbr(state.trim());
				if (state2 == null) {
					throw new InvalidJsonException(
							"Invalid State Abbreviation In Address ");
				} else {
					state = state2;
				}
			}
			String city = innerArr.getString(2);
			String address1 = innerArr.getString(0);
			address1 = address1.replaceAll("\\d+", "").trim();
			if (address1.length() == 0) {
				throw new InvalidJsonException(
						"Invalid Address1 In Address , Either Address1 contains only digits or is blank in ");
			}
			String zip = innerArr.getString(4);
			String city2 = match(city, "\\d+");
			if (city2 != null) {
				throw new InvalidJsonException("Invalid City Name In Address");
			}
			city2 = match(city, "[$&+,:;=?@#|*%()^!.~-]");
			if (city2 != null) {
				throw new InvalidJsonException("Invalid City Name In Address");
			}
			String zip2 = match(zip, "[a-zA-Z]");
			String zip3 = match(zip, "[$&+,:;=?@#|*%()^!.~-]");
			if (zip2 != null || zip.trim().length() > 5 || zip3 != null)
				throw new InvalidJsonException("Invalid Zip In Address ");
		} catch (InvalidJsonException e) {

			return e.getMessage();
		} catch (JSONException e) {

		}

		return null;
	}

	private String match(String txt, String findPattern) {

		Matcher m = Pattern.compile(findPattern, Pattern.CASE_INSENSITIVE)
				.matcher(txt);
		while (m.find()) {
			String val = txt.substring(m.start(), m.end());
			val = val.trim();
			val = val.replaceAll("\\s+", " ").trim();
			return val;
		}
		return null;
	}

	public org.json.JSONArray processJsonFileforSAC(
			ArrayList<InputJsonSchema> addList, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag, int distanceCriteria,boolean deepSearchEnable, BoostAddress boostAddress)
			throws Exception {
		hitscore1 = hitscore;
		//maxResults1 = maxResults;
		//long sactime1 = System.currentTimeMillis();
		org.json.JSONArray outputArry = threadedSAC.processByParts(addList,
				hitscore1, maxResults, noOfJobs, dataSource, flag, distanceCriteria,deepSearchEnable, boostAddress);
		//long sactime2 = System.currentTimeMillis();
		//String text = "\nTotal one state SAC time=" + (sactime2 - sactime1);
		//U.writeFile(text);
		return outputArry;

	}

	private org.json.JSONArray addToJson(ArrayList<AddressStruct> addStruct,
			String hitscore, String maxResults, String key)
			throws JSONException {

		ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
		if (addStruct.size() > 0) {
			if (addStruct.size() < 3)
				maxResults = addStruct.size() + "";

			for (int i = 0; i < Integer.parseInt(maxResults); i++) {
				AddressStruct current = addStruct.get(i);
				JsonSchema schemaobj = new JsonSchema();
				schemaobj.key = key;
				schemaobj.address = current.toOnlyStreet().toString()
						.toUpperCase();
				schemaobj.house_number = current.getHouseNumber();
				schemaobj.prefix_direction = current.get(AddColumns.PREDIRABRV);
				schemaobj.prefix_qualifier = current.get(AddColumns.PREQUALABR);
				schemaobj.prefix_type = current.get(AddColumns.PRETYPABRV);
				schemaobj.street_name = current.get(AddColumns.NAME);
				schemaobj.suffix_type = current.get(AddColumns.SUFTYPABRV);
				schemaobj.suffix_direction = current.get(AddColumns.SUFDIRABRV);
				schemaobj.city = current.get(AddColumns.CITY);
				if (current.getState() != null)
					schemaobj.state = current.getState();
				else
					schemaobj.state = "";

				schemaobj.zip = current.get(AddColumns.ZIP);
				if (current.hitScore > 5)
					current.hitScore = 5;
				schemaobj.score = 100 * (current.hitScore / 5);
				listOutput.add(schemaobj);

			}
		} else {
			JsonSchema schemaobj = new JsonSchema();
			schemaobj.address = "No Match Found";
			schemaobj.house_number = "";
			schemaobj.prefix_direction = "";
			schemaobj.prefix_qualifier = "";
			schemaobj.prefix_type = "";
			schemaobj.street_name = "";
			schemaobj.suffix_type = "";
			schemaobj.suffix_direction = "";
			schemaobj.city = "";
			schemaobj.state = "";
			schemaobj.zip = "";
			schemaobj.score = 0;
			listOutput.add(schemaobj);
		}

		Collections.sort(listOutput);

		org.json.JSONArray jsonColl = generateJson(listOutput);

		return jsonColl;
	}

	private org.json.JSONArray generateJson(ArrayList<JsonSchema> listOutput)
			throws JSONException {

		org.json.JSONArray jsonColl = new org.json.JSONArray();
		for (JsonSchema obj : listOutput) {

			org.json.JSONArray arrr = new org.json.JSONArray();
			arrr.put(obj.key);
			arrr.put(obj.address);
			arrr.put(obj.house_number);
			arrr.put(obj.prefix_direction);
			arrr.put(obj.prefix_qualifier);

			arrr.put(obj.prefix_type);
			arrr.put(obj.street_name);

			arrr.put(obj.suffix_type);
			arrr.put(obj.suffix_direction);
			arrr.put(obj.city);
			arrr.put(obj.state);
			arrr.put(obj.zip);
			arrr.put(obj.score);

			jsonColl.put(arrr);

		}

		return jsonColl;

	}
}

class InvalidJsonException extends Exception {

	private String message = null;

	public InvalidJsonException() {
		super();
	}

	public InvalidJsonException(String message) {
		super(message);
		this.message = message;
	}

	public InvalidJsonException(Throwable cause) {
		super(cause);
	}

	@Override
	public String toString() {
		return message;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
