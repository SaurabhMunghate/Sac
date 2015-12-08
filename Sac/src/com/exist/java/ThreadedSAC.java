package com.exist.java;

//import java.awt.RenderingHints.Key;
import java.io.IOException;
//import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.json.JSONArray;
import org.json.JSONException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
//import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.U;

public class ThreadedSAC {

	//static ArrayList<InputJsonSchema> array = new ArrayList<InputJsonSchema>();
	
//	public int start;
//	public int end;
//	public String hitscore;
//	public String maxResults;
//	public String key;
//	MultiMap multiMap;// = new MultiValueMap();
//	public org.json.JSONArray outputJson = null;
//	public int count=0;
//	public int elsecount=0;

	public JSONArray processByParts(ArrayList<InputJsonSchema> arr,
			final String hitscor, String maxResult, String noOfJobs)
			throws Exception {
		
	String	hitscore = hitscor;
	final String	maxResults = maxResult;
		 final JSONArray outputArr = new JSONArray();
		//array = arr;
	int	start = 0;
		U.log("JSON LEnght ::" + arr.size());
	int	end = arr.size();// / 5;
		U.log("JSON LEnght ::" + end);
		MultiMap multiMap=	addMultiplMap(arr);

//		ExecutorService executorService = Executors.newFixedThreadPool(4);
		CustomAddressCorrector customAddressCorrector=new CustomAddressCorrector();
		final MultiMap finalresult = customAddressCorrector
				.corrUsingAppropriateIndex(multiMap, maxResults, hitscor,noOfJobs);

		@SuppressWarnings("unchecked")
		Set<String> keys = finalresult.keySet();
		long st = System.currentTimeMillis();
		U.log("output multimap Size: " + keys.size());
		
		for ( final String key : keys) {
		//	this.key = key;

//			executorService.execute(new Runnable() {
//				public void run() {
					//System.out.println(Thread.currentThread().getName());
					ArrayList<AddressStruct> addStruct = null;
					// list

					List list = (List) finalresult.get(key);
					// 0 - Contains Address Struct and 1 Contains key.
					addStruct = (ArrayList<AddressStruct>) list.get(0);
					String addkey = (String) list.get(1);
					
					long sortSt = System.currentTimeMillis();
					ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
					
					if (addStruct.size() > 0) {
//						if (addStruct.size() < 3){
//							String maxResults = addStruct.size() + "";
//						}
					//	U.log("Count of Addresses ::"+count++);
						for(int returnOutputSize=0;returnOutputSize<addStruct.size();returnOutputSize++)
						{
						AddressStruct current = addStruct.get(returnOutputSize);
						
						JsonSchema schemaobj = new JsonSchema();
						schemaobj.key = addkey;
						schemaobj.address = current.toOnlyStreet().toString()
								.toUpperCase();
						schemaobj.house_number = current.getHouseNumber();
						schemaobj.prefix_direction = current
								.get(AddColumns.PREDIRABRV);
						schemaobj.prefix_qualifier = current
								.get(AddColumns.PREQUALABR);
						schemaobj.prefix_type = current
								.get(AddColumns.PRETYPABRV);
						schemaobj.street_name = current.get(AddColumns.NAME);
						schemaobj.suffix_type = current
								.get(AddColumns.SUFTYPABRV);
						schemaobj.suffix_direction = current
								.get(AddColumns.SUFDIRABRV);
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

					}} else {
				
						JsonSchema schemaobj = new JsonSchema();
						schemaobj.key = addkey;
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

					org.json.JSONArray jsonColl = new JSONArray();
					
						
							
						
					
							try {
								jsonColl = generateJson(listOutput);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					
							
					
					//long end = System.currentTimeMillis();
					//U.log("addjson time:-==" + (end - sortSt));
					outputArr.put(jsonColl);

//				}
			
//			});

		}

//		executorService.shutdown();
//		while (!executorService.isTerminated()) {

//		}
          
		long en = System.currentTimeMillis();
	//	U.log("create output json time ::" + (en - st));
        U.log("outputArr size==="+outputArr.length());  
      U.log("Number of input json addresses=="+end);
//      U.log("Number of else condition of json=="+elsecount);
//      String text="\ncreate json ::" + (en - st);
//      U.writeFile(text);
        
        
       /* long gcs=System.currentTimeMillis();
        System.gc();
          long gce=System.currentTimeMillis();*/
    //    U.log("gc time:="+(gce-gcs));
        
        
        
		return outputArr;
	}

	public MultiMap addMultiplMap(ArrayList<InputJsonSchema> array) throws JSONException, JsonGenerationException,
			JsonMappingException, IOException {
		MultiMap multiMap = new MultiValueMap();  
		for (int i = 0; i < array.size(); i++) {
			InputJsonSchema innerArr = array.get(i);
			String state = innerArr.state.toUpperCase();
			ArrayList<AddressStruct> addStruct = null; // synchronized
														// (addStruct) {
			String value = evaluateInputAddress(innerArr.toString());
			if (value == null) {
				String address2 = innerArr.address2.replaceAll(
						"[$&+,:;=?@#|*%()^!.~-]", "");
				String ival = Integer.toString(i);
				multiMap.put(ival, innerArr.address1);
				multiMap.put(ival, address2);
				multiMap.put(ival, innerArr.city);
				multiMap.put(ival, state);
				multiMap.put(ival, innerArr.zip);
				multiMap.put(ival, innerArr.key);
				
			} /*else {
				String adrkey = innerArr.key;
				AddressStruct emptyStruct = new AddressStruct(0);
				emptyStruct.setHouseNumber("ERROR - " + value);
				addStruct = new ArrayList<AddressStruct>();
				addStruct.add(emptyStruct);
				outputJson = addToJson(addStruct, hitscore, maxResults, adrkey);
				outputArr.put(outputJson);
			}*/
		}
		return multiMap;
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

	private JSONArray addToJson(ArrayList<AddressStruct> addStruct,
			String hitscore, String maxResults, String key) throws IOException,
			JSONException {

		ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
		if (addStruct.size() > 0) {
			// if (addStruct.size() < 3)
			// maxResults = addStruct.size() + "";

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

		long sortSt = System.currentTimeMillis();
		Collections.sort(listOutput);
		long sortEn = System.currentTimeMillis();
		U.log("sorting listoutput time==" + (sortEn - sortSt));

		long jsoncallstart = System.currentTimeMillis();
		org.json.JSONArray jsonColl = generateJson(listOutput);
		long jsonend = System.currentTimeMillis();
		U.log("generatejsonCollTime==" + (jsonend - jsoncallstart));

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
