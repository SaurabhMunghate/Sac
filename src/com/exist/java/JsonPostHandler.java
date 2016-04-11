package com.exist.java;

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

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.rmi.CORBA.Util;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.data.main.Logger;

import com.shatam.data.PostSACJarInfoToServer;
import com.shatam.io.ShatamIndexReader;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.Paths;
import com.shatam.util.U;

public class JsonPostHandler extends AbstractHandler {
	public String state;
	public String errMsg = null;

	public String hitscore1 = null;
	public String maxResults1 = null;
	public String noOfJobs1 = null;
public static String disable_enable="enable";
	public ThreadedSAC threadedSAC = new ThreadedSAC();

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		
		 /* Queue queue = QueueFactory.getDefaultQueue();
	        queue.add(TaskOptions.Builder.withUrl("/postData"));*/
		
		
		
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
			//U.log(Thread.currentThread().getName()+"=data1=="+data1);
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
			 disable_enable=inputJSon.get("log").toString().trim();
			String value = evaluateJson(inputData, count, noOfJobs);

			if (value != null)
				throw new Exception(value);

			// write etxt in log file
			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String ROOT = System.getProperty("user.dir");
			String PATH = new File(ROOT).getParent() + "/LOG/";
			File file = new File(PATH + dateFormat.format(date) + ".txt");
			// U.log("file log name==="+file.getPath());
			BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
			InetAddress IP = InetAddress.getLocalHost();
		//	Long writeStartTime=System.currentTimeMillis();
			if(disable_enable.toLowerCase().contains("enable")){
			out.write("\n\n************************Input Addresses********="
					+ IP.getHostAddress()
					+ "=***************************\n"
					+ inputData
					+ "\n************************Output Addresses***************************");
			//U.log("FILE is writed");
			}
		//	Long writeEndTime=System.currentTimeMillis();
			
			//U.log("Time==="+(writeEndTime-writeStartTime));
			
			JsonAddress md = new JsonAddress();
			try {
				outputObj = md.jsonAddress(inputData, "", count, noOfJobs,
						dataSource, false);
			} catch (Exception e) {
				U.log("Output obj null'");
				e.getMessage();
				out.write("\n" + e.getMessage());
			}
//			writeStartTime=System.currentTimeMillis();
			if(disable_enable.toLowerCase().contains("enable"))
			{
			out.write("\n" + outputObj.toString());
			}
//			writeEndTime=System.currentTimeMillis();
			
//			U.log("Time2==="+(writeEndTime-writeStartTime));
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

		// * String out= outputObj.toString(); out=out.replaceAll("\"", "\\\"");

		if (errMsg != null) {
			String err = errMsg;
			errMsg = null;
			response.getWriter().println(err);
		}

		
		if (outputObj != null)
			response.getWriter().println(outputObj);
		
		// System.out.println("Final OutPut:" + outputObj);

	}

	private String evaluateJson(String inputData, String count, String jobs)
			throws Exception {
		// TODO Auto-generated method stub
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

		//	U.log(inputData);
			org.json.JSONArray arr =null;
			try{
			 arr = new org.json.JSONArray(inputData);
			 
			 //Posting Address Count info to server.
			 PostSACJarInfoToServer postinfo = new PostSACJarInfoToServer(arr.length());
			 postinfo.post();
			}
			catch(Exception e){
				U.log(":::"+inputData);
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
	}// match

	public org.json.JSONArray processJsonFileforSAC(
			ArrayList<InputJsonSchema> addList, String hitscore,
			String maxResults, String noOfJobs, String dataSource, boolean flag)
			throws Exception {
		hitscore1 = hitscore;
		maxResults1 = maxResults;
		// noOfJobs1=noOfJobs;
		// U.log("Processing Started :::");

		// U.log(hitscore);
		// U.log(maxResults);
		// U.log(noOfJobs);
		// U.log(dataSource);
		long sactime1 = System.currentTimeMillis();

		// Added New
		// org.json.JSONArray JSonAddressArray = new
		// org.json.JSONArray(textEntered);

		// /CustomAddressCorrector.NORMAL= CustomAddressCorrector.METAPHONE=
		// CustomAddressCorrector.SOUNDEX=CustomAddressCorrector.DOUBLE_METAPHONE=
		// CustomAddressCorrector.REFINED_SOUNDEX=0;

		/*
		 * ExecutorService executorService =
		 * Executors.newFixedThreadPool(Integer.parseInt(noOfJobs));
		 * 
		 * executorService.execute(new Runnable() { public void run() {
		 * 
		 * //new ArrayList<InputJsonSchema>(addresses.subList(threadJobStart[i],
		 * threadJobEnd[i])) // for(int i=0;i<threadJobStart.length;i++){
		 * 
		 * try {
		 */
		// SimpleThreadFactory simpleThreadFactory=new SimpleThreadFactory();
		// ExecutorService executorService = Executors.newFixedThreadPool(50 );


		
	

		org.json.JSONArray outputArry = threadedSAC.processByParts(addList,
				hitscore1, maxResults1, noOfJobs, dataSource, flag);

		// outputArry.put(outputArry1);
		/*
		 * } catch (Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 * 
		 * // // } // // } });
		 * 
		 * executorService.shutdown(); while (!executorService.isTerminated()) {
		 * 
		 * }
		 */
		// change in logic
		// outputArry=SimpleThreadPool.simpletrheadpoolCalling(addresses,
		// hitscore,maxResults,noOfJobs);

		long sactime2 = System.currentTimeMillis();
		// U.log("Total SACccc time"+(sactime2-sactime1));
		String text = "\nTotal one state SAC time=" + (sactime2 - sactime1);
		U.writeFile(text);
		// ShatamIndexReader.close();
		//U.log("NORMAL="+CustomAddressCorrector.NORMAL+"::"+"METAPHONE="+CustomAddressCorrector.METAPHONE+"::"+"SOUNDEX="+CustomAddressCorrector.SOUNDEX+"::"+"DOUBLE_METAPHONE="+CustomAddressCorrector.DOUBLE_METAPHONE+"::"+"REFINED_SOUNDEX="+CustomAddressCorrector.REFINED_SOUNDEX);
//U.log("KIRTI MISAL");
		// //U.log(outputArry);

		return outputArry;
	
}
		// return null;

	

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
		// TODO Auto-generated method stub

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
