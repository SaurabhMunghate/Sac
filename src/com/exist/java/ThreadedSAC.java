package com.exist.java;

//import java.awt.RenderingHints.Key;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
//import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
//import org.geotools.filter.expression.ThisPropertyAccessorFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.omg.CORBA.Current;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
//import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.fst.Util;
import com.shatam.util.AbbrReplacement;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.OutputStatusCode;
import com.shatam.util.ShatamIndexQueryCreator;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class ThreadedSAC {

	// static ArrayList<InputJsonSchema> array = new
	// ArrayList<InputJsonSchema>();

	// public int start;
	// public int end;
	// public String hitscore;
	// public String maxResults;
	// public String key;
	// MultiMap multiMap;// = new MultiValueMap();
	// public org.json.JSONArray outputJson = null;
	// public int count=0;
	// public int elsecount=0;
static int avg=0;
	public org.json.JSONArray processByParts(ArrayList<InputJsonSchema> arr,
			final String hitscor, String maxResult, String noOfJobs,
			String dataSource, boolean flag) throws Exception {

		String hitscore = hitscor;
		final String maxResults = maxResult;
		// final JSONArray outputArr = new JSONArray();
		org.json.JSONArray outputArr = new JSONArray();
		// array = arr;
		int start = 0;
		// U.log("JSON LEnght ::" + arr.size());
		int end = arr.size();// / 5;
		// U.log("JSON LEnght ::" + end);
		MultiMap multiMap = null;
		try {
			multiMap = addMultiplMap(arr);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// ExecutorService executorService = Executors.newFixedThreadPool(4);
		CustomAddressCorrector customAddressCorrector = new CustomAddressCorrector();
		MultiMap finalresult = null;
		try {
			finalresult = customAddressCorrector.corrUsingAppropriateIndex(
					multiMap, maxResults, hitscor, noOfJobs, dataSource, flag);
			// U.log("Final result multimap size===" + finalresult.size());
		} catch (Exception e1) {
			// TODO Auto-generated catch block

			e1.printStackTrace();
		}
		HashMap<String, String> fipsMap = new HashMap<>();

		MultiMap combined = new MultiValueMap();
		{
			combined.putAll(finalresult);
		}

		/*
		 * @SuppressWarnings("unchecked") Set<String> keys =
		 * finalresult.keySet(); long st = System.currentTimeMillis();
		 * U.log("Set Size: " + keys.size());
		 */
		Set<String> keys1 = finalresult.keySet();

		Set<String> keys = new HashSet<>();
		keys.addAll(keys1);
		Set<String> keys2 = multiMap.keySet();

		if (keys1.size() != keys2.size()) {

			long st = System.currentTimeMillis();
			// U.log("Semioutput multimap Size: " + keys1.size());

			MultiMap semimultiMap = new MultiValueMap();

			for (String k : keys2) {

				if (!keys1.contains(keys2)) {
					List<String> list = (List<String>) multiMap.get(k);
					String address1 = list.get(0);
					String address2 = list.get(1);
					String city = list.get(2);
					String state = list.get(3);
					String zip = list.get(4);
					String addkey = list.get(5);
					// do whatever u want
					semimultiMap.put(k, address1);
					semimultiMap.put(k, address2);
					semimultiMap.put(k, city);
					semimultiMap.put(k, state);
					semimultiMap.put(k, zip);
					semimultiMap.put(k, addkey);

				}
			}
			try {
				finalresult = customAddressCorrector.corrUsingAppropriateIndex(
						semimultiMap, maxResults, hitscor, noOfJobs,
						dataSource, flag);
				// U.log("Final result multimap size==" + finalresult.size());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			keys1 = finalresult.keySet();

			keys.addAll(keys1);
			{
				combined.putAll(finalresult);
			}
		}

		if (keys == null) {
			U.log("Some Issues Are Found In Inpute addresses Please Send LOG/log.txt File To SHATAM Team To Verified It.");
		}

		else {
			// U.log("Keys are not Null::" + keys.size());
			for (String key : keys) {

				// this.key = key;

				// executorService.execute(new Runnable() {
				// public void run() {
				// System.out.println(Thread.currentThread().getName());
				ArrayList<AddressStruct> addStruct = null;
				ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
				// list

				List list = (List) combined.get(key);

				if (list == null) {
					U.log("Found some issues to getting key of inputed addresses");

				}
				// list=null;
				// 0 - Contains Address Struct and 1 Contains key.
				addStruct = (ArrayList<AddressStruct>) list.get(0);
				String addkey = (String) list.get(1);
				List inputList = (List) list.get(2);
				Boolean statePresent = true;
				if (inputList != null && inputList.size() != 0) {

					if (inputList.get(6).toString().length() < 1) {
						// U.log("yes state is NULLLLLLLL");
						// statePresent = false;
					}
				}

				// U.log("list size===" + inputList.size());

				long sortSt = System.currentTimeMillis();

				if (addStruct == null) {

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
					schemaobj.fipsCode = "";
					if (statePresent == false)
						schemaobj.errorCode = "14,18";
					else {
						schemaobj.errorCode = "14";
					}
					schemaobj.score = 0;
					schemaobj.datasource = "";
					schemaobj.message ="";
					listOutput.add(schemaobj);
				}
				// addStruct=null;
				else {
					if (addStruct.size() > 0) {

						
						// U.log("Count of Addresses ::"+count++);
						for (int returnOutputSize = 0; returnOutputSize < addStruct
								.size(); returnOutputSize++) {
							AddressStruct current = addStruct
									.get(returnOutputSize);
							float jaroPer =calculateScoring(hitscore, current)*100;
							
							//U.log("HITSCORE==="+current.hitScore);
							 if (addStruct.size() >1&&returnOutputSize==0){
								 
								 if(current.hitScore>5){
									 
									 if(current.hitScore>=6)current.hitScore=6;
									 
									 avg=6; 
								 }
								 
								 else
									 avg=5;
								 
														 }
							
							 if (addStruct.size() >1&&avg==6&&current.hitScore>=6){
									current.hitScore=6;
								 }
							 
							JsonSchema schemaobj = new JsonSchema();
							schemaobj.key = addkey;
							schemaobj.address = getCompleteStreet(current);//current.toOnlyStreet().toString().toUpperCase();
							
							schemaobj.house_number = current.getHouseNumber();

							// ...........
							schemaobj.prefix_direction = standrdForm(
									current.get(AddColumns.PREDIRABRV),
									current.getState());

							schemaobj.prefix_qualifier = standrdForm(
									current.get(AddColumns.PREQUALABR),
									current.getState());

							schemaobj.prefix_type = standrdForm(
									current.get(AddColumns.PRETYPABRV),
									current.getState());
							// ..................

							schemaobj.street_name = makeStandardText(current
									.get(AddColumns.NAME));

							// ...................
							schemaobj.suffix_type = standrdForm(
									current.get(AddColumns.SUFTYPABRV),
									current.getState());
							schemaobj.suffix_direction = standrdForm(
									current.get(AddColumns.SUFDIRABRV),
									current.getState());
							// ....................

							schemaobj.city = makeStandardText(current
									.get(AddColumns.CITY));
							if (current.getState() != null)
								schemaobj.state = current.getState();
							else
								schemaobj.state = "";
							String hashcode = current.get(AddColumns.ZIP)
									+ current.getState();
							// U.log(GenerateCache.FIPS.size()+"==hashcode==="+hashcode+"=="+hashcode.hashCode());

							if (current.getState() != null) {
								String fipsCode = GenerateCache.FIPS
										.get(hashcode.hashCode() + "");
								// U.log("FIPSCODE====="+fipsCode);
								if (fipsCode == null)
									fipsCode = U.STATE_MAP.get(current
											.getState());
								if (fipsCode.length() == 3)
									fipsCode = "0" + fipsCode;
								schemaobj.fipsCode = fipsCode;
							} else {
								schemaobj.fipsCode = "";
							}

							schemaobj.zip = current.get(AddColumns.ZIP);
							try {
								schemaobj.errorCode = OutputStatusCode
										.getStatusCode(inputList, getCompleteStreet(current).toString(),schemaobj.city.toString(),schemaobj.zip.toString());
							} catch (Exception e) {
								schemaobj.errorCode = "";
							}
						//	 U.log(avg+"==current.hitScore=="+current.hitScore+"******"+calculateMatchingRate(inputList,current));
							 
							 if (addStruct.size() ==1){
							if (current.hitScore > 5)
								current.hitScore = 5;
							 avg=5;
							 }

							schemaobj.score = 100 * (current.hitScore / avg);
							
					//		U.log(current.hitScore +"*********"+schemaobj.score);

							if (schemaobj.score < 80 && current.hitScore > 2) {
								float p = calculateMatchingRate(inputList,
										current);
								// U.log(p);
								if (p != 0) {
									// U.log("set score");
									schemaobj.score = p;
								}
							}
							
							if(jaroPer>schemaobj.score)schemaobj.score=jaroPer; //45.53>23.34
							/*will schronce rd iron station nc 28080
							schronce rd iron station nc 28080
							0.8248272*/
							if(jaroPer>80)schemaobj.score=jaroPer; 


							schemaobj.datasource = current
									.get(AddColumns.DATASOURCE);

							listOutput.add(schemaobj);
							
							schemaobj.message ="";
							//U.log(schemaobj.city.split(" ").length);
							if(schemaobj.city.split(" ").length==1){
							String cityWithoutSpace=schemaobj.city.replaceAll(" ","");
							String cityobserverCity=com.shatam.util.Util.cityObserver.get(cityWithoutSpace.toUpperCase().trim());
							// U.log("END condition 2=="+cityobserverCity);
							 
							 if(cityobserverCity!=null){
								 schemaobj.message ="City name standardized";
								 schemaobj.city=makeStandardText(cityobserverCity.toLowerCase());
							 }
							}

						}
					} else {

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
						schemaobj.fipsCode = "";

						if (statePresent == false)
							schemaobj.errorCode = "14,18";
						else {
							schemaobj.errorCode = "14";
						}
						schemaobj.score = 0;
						schemaobj.datasource = "";
						schemaobj.message ="";
						listOutput.add(schemaobj);
					}
				}
				org.json.JSONArray jsonColl = new JSONArray();

				// U.log("Listoutput length==="+listOutput.size());

				try {
					jsonColl = generateJson(listOutput);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					U.log("Error is in output listing");
					e.printStackTrace();
				}

				// long end = System.currentTimeMillis();
				// //U.log("addjson time:-==" + (end - sortSt));
				// U.log("::::::******" + jsonColl.toString());
				outputArr.put(jsonColl);
				//
				// }
				// });

			}
		}
		// executorService.shutdown();
		// while (!executorService.isTerminated()) {
		//
		// }

		long en = System.currentTimeMillis();
		// //U.log("create json ::" + (en - st));
		// U.log("Number of input addresses=="+end);
		// U.log("output size==="+outputArr.length());

		// //U.log("Number of else condition of json=="+elsecount);
		// String text="\ncreate json ::" + (en - st);
		// U.writeFile(text);

		/*
		 * long gcs=System.currentTimeMillis(); System.gc(); long
		 * gce=System.currentTimeMillis();
		 */
		// //U.log("gc time:="+(gce-gcs));

		return outputArr;
	}

	public HashMap<String, String> getFips() {
		HashMap<String, String> fipsMap = new HashMap<>();

		return fipsMap;
	}
	
	public static float calculateScoring(String score ,AddressStruct addstruct) throws Exception{
		float per=0;
		int persentage;
		String foundaddress=DistanceMatchForResult.getCompleteStreet(addstruct).toLowerCase();
		String inputAddress=addstruct.inputAddress.replace(addstruct.getHouseNumber(), "");
		//U.log(":::::::::::"+inputAddress);
		if(com.shatam.util.Util.match(inputAddress.toLowerCase(), "(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)")!=null&&com.shatam.util.Util.match(inputAddress.toLowerCase(), "\\d(th|st|nd|rd)")!=null)
	        inputAddress=inputAddress.toLowerCase().replaceAll("\\d(th|st|nd|rd)", "");

		String foundcity=addstruct.get(AddColumns.CITY).toLowerCase();
		String inputCity =addstruct.getQueryCity();
		String state=addstruct.getState();
		String foundZip=addstruct.get(AddColumns.ZIP);
		String inputZip=addstruct.getQueryZip();
		
		StringBuffer buf= new StringBuffer();
		for(String s:inputAddress.split(" ")){
			String result=standrdForm(s, addstruct.getState());
			buf.append(result);
			buf.append(" ");
		}
		String inputStreetAbrv=buf.toString().replace("  ", " ").toLowerCase();
		
		String completeInputAddress=inputStreetAbrv.trim()+" "+inputCity.trim()+" "+state.trim()+" "+inputZip.trim();
		
		String completeOutputAddress=foundaddress.trim()+" "+foundcity.trim()+" "+state.trim()+" "+foundZip.trim();
		
		//U.log("********************\n"+completeInputAddress.toLowerCase().replace("  ", " ")+"\n"+completeOutputAddress.toLowerCase().replace("  ", " "));
		
		JaroWinkler algorithm = new JaroWinkler();
		   float Matchingper=algorithm.getSimilarity(completeInputAddress.toLowerCase().replace("  ", " "),completeOutputAddress.toLowerCase().replace("  ", " "));
		
	//	   U.log(Matchingper);
		   
		/*if (per < 5.5 && per >= 5)
			per = 5;
		
		
		if(per<=5){
			persentage=(per/5)*100;
		}*/
		
		return Matchingper;
	}
	
	public MultiMap addMultiplMap(ArrayList<InputJsonSchema> array)
			throws JSONException, JsonGenerationException,
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

			} /*
			 * else { String adrkey = innerArr.key; AddressStruct emptyStruct =
			 * new AddressStruct(0); emptyStruct.setHouseNumber("ERROR - " +
			 * value); addStruct = new ArrayList<AddressStruct>();
			 * addStruct.add(emptyStruct); outputJson = addToJson(addStruct,
			 * hitscore, maxResults, adrkey); outputArr.put(outputJson); }
			 */
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
			// U.log(inputData+":::::::EXCEPTION JSON");
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

	public String getCompleteStreet(AddressStruct addstruct) throws Exception {
		StringBuffer buf = new StringBuffer();
		//U.log("complete address====="+buf.toString());
		buf.append(addstruct.getHouseNumber());
		//U.log(addstruct.getState()+"==1wcomplete address====="+buf.toString());
		buf.append(" "+standrdForm(addstruct.get(AddColumns.PREDIRABRV),addstruct.getState()));
		//U.log("1complete address====="+buf.toString());
		buf.append(" "+standrdForm(addstruct.get(AddColumns.PREQUALABR),addstruct.getState()));
		//U.log("2complete address====="+buf.toString());
		buf.append(" "+standrdForm(addstruct.get(AddColumns.PRETYPABRV),addstruct.getState()));
	//	U.log("3complete address====="+buf.toString());
		//buf.append(" "+standrdForm(addstruct.get(AddColumns.NAME),addstruct.getState()));
		buf.append(" "+makeStandardText(addstruct.get(AddColumns.NAME).toUpperCase()));
	//	U.log("4complete address====="+buf.toString());
		buf.append(" "+standrdForm(addstruct.get(AddColumns.SUFTYPABRV),addstruct.getState()));
		//U.log("5complete address====="+buf.toString());
		buf.append(" "+standrdForm(addstruct.get(AddColumns.SUFDIRABRV),addstruct.getState()));
		//U.log("6complete address====="+buf.toString());
		buf.append(" "+standrdForm(addstruct.get(AddColumns.SUFQUALABR),addstruct.getState()));
		//U.log("7complete address====="+buf.toString());
		//U.log("complete address====="+buf.toString());
		if (!StrUtil.isEmpty(addstruct.unitNumber)) {
			if (buf.length() > 0) {
				buf.append(" ");
			}
			buf.append(addstruct.getUnitType());
			buf.append(" ").append(addstruct.unitNumber);
		}

		buf.trimToSize();
		//U.log(buf.toString().trim().replaceAll("\\s+", " "));
		return buf.toString().trim().replaceAll("\\s+", " ");
	}

	private JSONArray addToJson(ArrayList<AddressStruct> addStruct,
			String hitscore, String maxResults, String key) throws Exception {

		ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
		if (addStruct.size() > 0) {
			// if (addStruct.size() < 3)
			// maxResults = addStruct.size() + "";

			for (int i = 0; i < Integer.parseInt(maxResults); i++) {
				AddressStruct current = addStruct.get(i);
				JsonSchema schemaobj = new JsonSchema();
				schemaobj.key = key;
				schemaobj.address = getCompleteStreet(current);// current.toOnlyStreet().toString().toUpperCase();
				schemaobj.house_number = current.getHouseNumber();
				schemaobj.prefix_direction = makeStandardText(current
						.get(AddColumns.PREDIRABRV));
				schemaobj.prefix_qualifier = makeStandardText(current
						.get(AddColumns.PREQUALABR));
				schemaobj.prefix_type = makeStandardText(current
						.get(AddColumns.PRETYPABRV));
				schemaobj.street_name = makeStandardText(current
						.get(AddColumns.NAME));

				schemaobj.suffix_type = makeStandardText(current
						.get(AddColumns.SUFTYPABRV));
				schemaobj.suffix_direction = makeStandardText(current
						.get(AddColumns.SUFDIRABRV));

				schemaobj.city = makeStandardText(current.get(AddColumns.CITY));
				if (current.getState() != null)
					schemaobj.state = current.getState();
				else
					schemaobj.state = "";

				schemaobj.zip = current.get(AddColumns.ZIP);
				schemaobj.errorCode = "";
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
			schemaobj.errorCode = "";
			schemaobj.score = 0;
			listOutput.add(schemaobj);
		}

		long sortSt = System.currentTimeMillis();
		Collections.sort(listOutput);
		long sortEn = System.currentTimeMillis();
		// U.log("sorting listoutput time==" + (sortEn - sortSt));

		long jsoncallstart = System.currentTimeMillis();
		org.json.JSONArray jsonColl = generateJson(listOutput);
		long jsonend = System.currentTimeMillis();
		// U.log("generatejsonCollTime==" + (jsonend - jsoncallstart));

		return jsonColl;
	}

	private org.json.JSONArray generateJson(ArrayList<JsonSchema> listOutput)
			throws JSONException {
		// TODO Auto-generated method stub

		org.json.JSONArray jsonColl = new JSONArray();
		if (listOutput != null) {
			for (JsonSchema obj : listOutput) {
				ArrayList<String> arrr = new ArrayList<>();
				arrr.add(obj.key);
				arrr.add(obj.address);
				arrr.add(obj.house_number);
				arrr.add(obj.prefix_direction);
				arrr.add(obj.prefix_qualifier);

				arrr.add(obj.prefix_type);
				arrr.add(obj.street_name);

				arrr.add(obj.suffix_type);
				arrr.add(obj.suffix_direction);
				arrr.add(obj.city);
				arrr.add(obj.state);
				arrr.add(obj.fipsCode);
				arrr.add(obj.zip);
				arrr.add(obj.errorCode);
				arrr.add(obj.score + "");

				if (obj.datasource.contains("usps")) {

					arrr.add("TRUE");

				} else
					arrr.add("FALSE");
				
				arrr.add(obj.message);

				jsonColl.put(arrr);

			}
		} else {
			U.log("list is null");
			return null;
		}
		return jsonColl;

	}

	public static float calculateMatchingRate(List list, AddressStruct addStruct)
			throws Exception {
		// U.log("*****************"+
		// DistanceMatchForResult.jaroMatch("15 street lane","15 lane"));
		ShatamIndexQueryStruct shatamIndexQueryStruct = (ShatamIndexQueryStruct) list
				.get(0);
		String street = (String) list.get(3);
		Query query = (Query) list.get(4);
		String state = (String) list.get(6);
		String city = shatamIndexQueryStruct.getCity();
		String zip = shatamIndexQueryStruct.getZip();

		String foundStreet;
		String foundCity;
		String foundZip;

		foundStreet = addStruct.toOnlyStreet().toString().toUpperCase();
		foundCity = addStruct.get(AddColumns.CITY);
		foundZip = addStruct.get(AddColumns.ZIP);
		try {
			if (query.toString().contains("k1")) {
				// U.log("query=="+query.toString().replaceAll("k1:", ""));
				String inputAddressQuery = com.shatam.util.Util.match(
						query.toString().replaceAll("k1:", ""),
						"(.*?[a-z]+\\^5.*?)[a-z]+_city", 0).replaceAll(
						"[a-z]+_city|\\^5\\.0|\\d+", "");
				// U.log(query.toString().replaceAll("k1:",
				// "")+"***********************************************"+inputAddressQuery);
				String inputHashKey = ((inputAddressQuery
						.replaceAll("\\^5", "") + city.toLowerCase()).replace(
						" ", ""));
				// U.log("input addresses==="+((inputAddressQuery.replaceAll("\\^5",
				// "")+city.toLowerCase()).replace(" ", "")));
				String outputHashKeyStreetName = ((addStruct.get(
						AddColumns.NAME).toLowerCase().replace(" ", "")) + foundCity
						.toLowerCase());

				String outputHashKeyAddress = ((addStruct.get(
						AddColumns.PREDIRABRV).toLowerCase()
						+ addStruct.get(AddColumns.NAME).toLowerCase()
						+ addStruct.get(AddColumns.SUFTYPABRV).toLowerCase() + addStruct
						.get(AddColumns.SUFDIRABRV).toLowerCase()).replace(" ",
						"") + foundCity.toLowerCase());

				// U.log(((addStruct.get(AddColumns.NAME).toLowerCase().replace(" ",
				// ""))+foundCity.toLowerCase())+"===Street mach=="+
				// DistanceMatchForResult.jaroMatch(inputHashKey,
				// outputHashKeyStreetName));
				if (DistanceMatchForResult.jaroMatch(inputHashKey,
						outputHashKeyStreetName) > 0.84) {
					return (DistanceMatchForResult.jaroMatch(inputHashKey,
							outputHashKeyStreetName)) * 100;
				}
				// U.log(((addStruct.get(AddColumns.PREDIRABRV).toLowerCase()+addStruct.get(AddColumns.NAME).toLowerCase()+addStruct.get(AddColumns.SUFTYPABRV).toLowerCase()+addStruct.get(AddColumns.SUFDIRABRV).toLowerCase()).replace(" ",
				// "")+foundCity.toLowerCase())+"=====Address match==="+
				// DistanceMatchForResult.jaroMatch(inputHashKey,
				// outputHashKeyAddress));
				if (DistanceMatchForResult.jaroMatch(inputHashKey,
						outputHashKeyAddress) > 0.84) {

					return (DistanceMatchForResult.jaroMatch(inputHashKey,
							outputHashKeyAddress)) * 100;
				}

			}

			else {
				// U.log("Errrrrrrrrrror=="+street);
				// String inputAddress=
				// ShatamIndexQueryCreator.createAddressQuery(street, state,
				// AbstractIndexType.TYPES[0]);
				// U.log(inputAddress);
			}
		} catch (Exception e) {
			// U.log("error==="+e);
			return 0;
		}
		return 0;
	}

	public static String standrdForm(String s, String state) throws Exception {
		// String val=AddressStruct.getshortForm(s, state, col);
	//	U.log(s+"==state=="+state);
		String val;
		if (s.trim().length() > 0) {
			val = abbrv.get(s.toUpperCase());
			 //U.log(s+"==val=="+val);
		} else
			val = AbbrReplacement.getFullAddress(s, state);
		
		if (val == null)
			val = AbbrReplacement.getFullAddress(s, state);
		// U.log(val);
		return val;
	}

	public static String makeStandardText(String val) {
//U.log("::::::::::"+val);
		if (val != null && val.length() > 0) {
			val = val.replace("  ", " ");
			String capital = null, wd;
			String[] words = val.trim().split(" ");

			for (String word : words) {
//U.log(":::::::"+word);
				wd = word.substring(0, 1).toUpperCase() + word.substring(1);
				capital = (capital == null) ? wd : capital + " " + wd;
			}

			val = capital;
			// U.log("Capitalized val=="+val);
			if (capital == null) {
				// U.log("address is null");
				val = "";
				// Thread.sleep(2000);
			}

			return val;
		}

		return "";
	}

	public static HashMap<String, String> abbrv = new HashMap<String, String>();
	static {
		abbrv.put("ALLEY", "ALY");
		abbrv.put("ANEX", "ANX");
		abbrv.put("ARCADE", "ARC");
		abbrv.put("AVENUE", "AVE");
		abbrv.put("BAYOU", "BYU");
		abbrv.put("BEACH", "BCH");
		abbrv.put("BEND", "BND");
		abbrv.put("BLUFF", "BLF");
		abbrv.put("BLUFFS", "BLFS");
		abbrv.put("BOTTOM", "BTM");
		abbrv.put("BOULEVARD", "BLVD");
		abbrv.put("BRANCH", "BR");
		abbrv.put("BRIDGE", "BRG");
		abbrv.put("BROOK", "BRK");
		abbrv.put("BROOKS", "BRKS");
		abbrv.put("BURG", "BG");
		abbrv.put("BURGS", "BGS");
		abbrv.put("BYPASS", "BYP");
		abbrv.put("CAMP", "CP");
		abbrv.put("CANYON", "CYN");
		abbrv.put("CAPE", "CPE");
		abbrv.put("CAUSEWAY", "CSWY");
		abbrv.put("CENTER", "CTR");
		abbrv.put("CENTERS", "CTRS");
		abbrv.put("CIRCLE", "CIR");
		abbrv.put("CIRCLES", "CIRS");
		abbrv.put("CLIFF", "CLF");
		abbrv.put("CLIFFS", "CLFS");
		abbrv.put("CLUB", "CLB");
		abbrv.put("COMMON", "CMN");
		abbrv.put("COMMONS", "CMNS");
		abbrv.put("CORNER", "COR");
		abbrv.put("CORNERS", "CORS");
		abbrv.put("COURSE", "CRSE");
		abbrv.put("COURT", "CT");
		abbrv.put("COURTS", "CTS");
		abbrv.put("COVE", "CV");
		abbrv.put("COVES", "CVS");
		abbrv.put("CREEK", "CRK");
		abbrv.put("CRESCENT", "CRES");
		abbrv.put("CREST", "CRST");
		abbrv.put("CROSSING", "XING");
		abbrv.put("CROSSROADS", "XRD");
		abbrv.put("CROSSROAD", "XRDS");
		abbrv.put("CURVE", "CURV");
		abbrv.put("DALE", "DL");
		abbrv.put("DAM", "DM");
		abbrv.put("DIVIDE", "DV");
		abbrv.put("DRIVE", "DR");
		abbrv.put("DRIVES", "DRS");
		abbrv.put("ESTATE", "EST");
		abbrv.put("ESTATES", "ESTS");
		abbrv.put("EXPRESSWAY", "EXPY");
		abbrv.put("EXTENSION", "EXT");
		abbrv.put("EXTENSIONS", "EXTS");
		abbrv.put("FALL", "FALL");
		abbrv.put("FALLS", "FLS");
		abbrv.put("FERRY", "FRY");
		abbrv.put("FIELD", "FLD");
		abbrv.put("FIELDS", "FLDS");
		abbrv.put("FLAT", "FLT");
		abbrv.put("FLATS", "FLTS");
		abbrv.put("FORD", "FRD");
		abbrv.put("FORDS", "FRDS");
		abbrv.put("FOREST", "FRST");
		abbrv.put("FORGE", "FRG");
		abbrv.put("FORGES", "FRGS");
		abbrv.put("FORK", "FRK");
		abbrv.put("FORKS", "FRKS");
		abbrv.put("FORT", "FT");
		abbrv.put("FREEWAY", "FWY");
		abbrv.put("GARDEN", "GDN");
		abbrv.put("GARDENS", "GDNS");
		abbrv.put("GATEWAY", "GTWY");
		abbrv.put("GLEN", "GLN");
		abbrv.put("GLENS", "GLNS");
		abbrv.put("GREEN", "GRN");
		abbrv.put("GREENS", "GRNS");
		abbrv.put("GROVE", "GRV");
		abbrv.put("GROVES", "GRVS");
		abbrv.put("HARBOR", "HBR");
		abbrv.put("HARBORS", "HBRS");
		abbrv.put("HAVEN", "HVN");
		abbrv.put("HEIGHTS", "HTS");
		abbrv.put("HIGHWAY", "HWY");
		abbrv.put("HILL", "HL");
		abbrv.put("HILLS", "HLS");
		abbrv.put("HOLLOW", "HOLW");
		abbrv.put("INLET", "INLT");
		abbrv.put("ISLAND", "IS");
		abbrv.put("ISLANDS", "ISS");
		abbrv.put("ISLE", "ISLE");
		abbrv.put("JUNCTION", "JCT");
		abbrv.put("JUNCTIONS", "JCTS");
		abbrv.put("KEY", "KY");
		abbrv.put("KEYS", "KYS");
		abbrv.put("KNOLL", "KNL");
		abbrv.put("KNOLLS", "KNLS");
		abbrv.put("LAKE", "LK");
		abbrv.put("LAKES", "LKS");
		abbrv.put("LAND", "LAND");
		abbrv.put("LANDING", "LNDG");
		abbrv.put("LANE", "LN");
		abbrv.put("LIGHT", "LGT");
		abbrv.put("LIGHTS", "LGTS");
		abbrv.put("LOAF", "LF");
		abbrv.put("LOCK", "LCK");
		abbrv.put("LOCKS", "LCKS");
		abbrv.put("LODGE", "LDG");
		abbrv.put("LOOP", "LOOP");
		abbrv.put("MALL", "MALL");
		abbrv.put("MANOR", "MNR");
		abbrv.put("MANORS", "MNRS");
		abbrv.put("MEADOW", "MDW");
		abbrv.put("MEADOWS", "MDWS");
		abbrv.put("MEWS", "MEWS");
		abbrv.put("MILL", "ML");
		abbrv.put("MILLS", "MLS");
		abbrv.put("MISSION", "MSN");
		abbrv.put("MOTORWAY", "MTWY");
		abbrv.put("MOUNT", "MT");
		abbrv.put("MOUNTAIN", "MTN");
		abbrv.put("MOUNTAINS", "MTNS");
		abbrv.put("NECK", "NCK");
		abbrv.put("ORCHARD", "ORCH");
		abbrv.put("OVAL", "OVAL");
		abbrv.put("OVERPASS", "OPAS");
		abbrv.put("PARK", "PARK");
		abbrv.put("PARKS", "PARK");
		abbrv.put("PARKWAY", "PKWY");
		abbrv.put("PARKWAYS", "PKWY");
		abbrv.put("PASS", "PASS");
		abbrv.put("PASSAGE", "PSGE");
		abbrv.put("PATH", "PATH");
		abbrv.put("PIKE", "PIKE");
		abbrv.put("PINE", "PNE");
		abbrv.put("PINES", "PNES");
		abbrv.put("PLACE", "PL");
		abbrv.put("PLAIN", "PLN");
		abbrv.put("PLAINS", "PLNS");
		abbrv.put("PLAZA", "PLZ");
		abbrv.put("POINT", "PT");
		abbrv.put("POINTS", "PTS");
		abbrv.put("PORT", "PRT");
		abbrv.put("PORTS", "PRTS");
		abbrv.put("PRAIRIE", "PR");
		abbrv.put("RADIAL", "RADL");
		abbrv.put("RAMP", "RAMP");
		abbrv.put("RANCH", "RNCH");
		abbrv.put("RAPID", "RPD");
		abbrv.put("RAPIDS", "RPDS");
		abbrv.put("REST", "RST");
		abbrv.put("RIDGE", "RDG");
		abbrv.put("RIDGES", "RDGS");
		abbrv.put("RIVER", "RIV");
		abbrv.put("ROAD", "RD");
		abbrv.put("ROADS", "RDS");
		abbrv.put("ROUTE", "RTE");
		abbrv.put("ROW", "ROW");
		abbrv.put("RUE", "RUE");
		abbrv.put("RUN", "RUN");
		abbrv.put("SHOAL", "SHL");
		abbrv.put("SHOALS", "SHLS");
		abbrv.put("SHORE", "SHR");
		abbrv.put("SHORES", "SHRS");
		abbrv.put("SKYWAY", "SKWY");
		abbrv.put("SPRING", "SPG");
		abbrv.put("SPRINGS", "SPGS");
		abbrv.put("SPUR", "SPUR");
		abbrv.put("SPURS", "SPUR");
		abbrv.put("SQUARE", "SQ");
		abbrv.put("SQUARES", "SQS");
		abbrv.put("STATION", "STA");
		abbrv.put("STRAVENUE", "STRA");
		abbrv.put("STREAM", "STRM");
		abbrv.put("STREET", "ST");
		abbrv.put("STREETS", "STS");
		abbrv.put("SUMMIT", "SMT");
		abbrv.put("TERRACE", "TER");
		abbrv.put("THROUGHWAY", "TRWY");
		abbrv.put("TRACE", "TRCE");
		abbrv.put("TRACK", "TRAK");
		abbrv.put("TRAFFICWAY", "TRFY");
		abbrv.put("TRAIL", "TRL");
		abbrv.put("TRAILER", "TRLR");
		abbrv.put("TUNNEL", "TUNL");
		abbrv.put("TURNPIKE", "TPKE");
		abbrv.put("UNDERPASS", "UPAS");
		abbrv.put("UNION", "UN");
		abbrv.put("UNIONS", "UNS");
		abbrv.put("VALLEY", "VLY");
		abbrv.put("VALLEYS", "VLYS");
		abbrv.put("VIADUCT", "VIA");
		abbrv.put("VIEW", "VW");
		abbrv.put("VIEWS", "VWS");
		abbrv.put("VILLAGE", "VLG");
		abbrv.put("VILLAGES", "VLGS");
		abbrv.put("VILLE", "VL");
		abbrv.put("VISTA", "VIS");
		abbrv.put("WALK", "WALK");
		abbrv.put("WALKS", "WALK");
		abbrv.put("WALL", "WALL");
		abbrv.put("WAY", "WAY");
		abbrv.put("WAYS", "WAYS");
		abbrv.put("WELL", "WL");
		abbrv.put("WELLS", "WLS");

	}

}
