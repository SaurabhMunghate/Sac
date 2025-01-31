/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.json.JSONArray;
import org.json.JSONException;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.search.Query;
import com.shatam.util.AbbrReplacement;
import com.shatam.util.BoostAddress;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.OutputStatusCode;
import com.shatam.util.ShatamCachingSingle;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;
import com.shatam.zip.search.RecordSelector;

public class ThreadedSAC {

	static int avg = 0;

	public org.json.JSONArray processByParts(ArrayList<InputJsonSchema> arr,
			final String hitscor, String maxResult, String noOfJobs,
			String dataSource, boolean flag, int distanceCriteria,boolean deepSearchEnable, BoostAddress boostAddress) throws Exception {

		String hitscore = hitscor;
		final String maxResults = maxResult;
		org.json.JSONArray outputArr = new JSONArray();
		MultiMap multiMap = null;
		//
		Map<Integer, List<String>> groupMap = new HashMap<>();
		try {			
			multiMap = addMultiplMap(arr,groupMap);
		} catch (Exception e1) {

			e1.printStackTrace();
		}

		CustomAddressCorrector customAddressCorrector = new CustomAddressCorrector();
		MultiMap finalresult = null;
		try {
			U.log("try-corrUsingAppropriateIndex");
			finalresult = customAddressCorrector.corrUsingAppropriateIndex(
					multiMap, maxResults, hitscor, noOfJobs, dataSource, flag, distanceCriteria,deepSearchEnable, boostAddress);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		HashMap<String, String> fipsMap = new HashMap<>();
		MultiMap combined = new MultiValueMap();
		{
			combined.putAll(finalresult);
		}
		long sortSt = System.currentTimeMillis();
		Set<String> keys1 = finalresult.keySet();
		Set<String> keys = new HashSet<>();
		keys.addAll(keys1);
		Set<String> keys2 = multiMap.keySet();
		if (keys1.size() != keys2.size()) {
			U.log("keys1.size() != keys2.size()");
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
					semimultiMap.put(k, address1);
					semimultiMap.put(k, address2);
					semimultiMap.put(k, city);
					semimultiMap.put(k, state);
					semimultiMap.put(k, zip);
					semimultiMap.put(k, addkey);
				}
			}
			try {
				U.log("try------"+finalresult);
				finalresult = customAddressCorrector.corrUsingAppropriateIndex(
						semimultiMap, maxResults, hitscor, noOfJobs,
						dataSource, flag, distanceCriteria,deepSearchEnable, boostAddress);
			} catch (Exception e) {
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
			U.log("else----------------");
			RecordSelector mppro = new RecordSelector(); 
			if(groupMap.size()>0){				
				mppro.sortAndgetHighScoreRecords(combined,groupMap);
			}
			for (String key : keys) { U.log("thisus---------------------------------");
				ArrayList<AddressStruct> addStruct = null;
				ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
				List list = (List) combined.get(key);
				if (list == null) {
					//U.log("Found some issues to getting key of inputed addresses");
					continue;
				}
				addStruct = (ArrayList<AddressStruct>) list.get(0);
				String addkey = (String) list.get(1);
				List inputList = (List) list.get(2);
				Boolean statePresent = true;
				if (inputList != null && inputList.size() != 0) {
					if (inputList.get(6).toString().length() < 1) {

					}
				}
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
					schemaobj.message = "";
					schemaobj.secondary_designator = "";
					schemaobj.secondary_number = "";
					listOutput.add(schemaobj);
				} else { 
					if (addStruct.size() > 0) {
						for (int returnOutputSize = 0; returnOutputSize < addStruct
								.size(); returnOutputSize++) {
							AddressStruct current = addStruct
									.get(returnOutputSize);
							if (current.inputAddress.equals("No Match Found")) {
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
								schemaobj.message = "";
								schemaobj.secondary_designator = "";
								schemaobj.secondary_number = "";
								listOutput.add(schemaobj);
								continue;
							}

							float jaroPer = calculateScoring(hitscore, current) * 100;
							if (addStruct.size() > 1 && returnOutputSize == 0) {
								if (current.hitScore > 5) {
									if (current.hitScore >= 6)
										current.hitScore = 6;
									avg = 6;
								} else
									avg = 5;
							}

							if (addStruct.size() > 1 && avg == 6
									&& current.hitScore >= 6) {
								current.hitScore = 6;
							}
							
							JsonSchema schemaobj = new JsonSchema();
							schemaobj.key = addkey;
							schemaobj.address = getCompleteStreet(current);
							U.log(schemaobj.address);
							if (!StrUtil.isEmpty(current.unitNumber)) {
								schemaobj.secondary_designator = current
										.getUnitType();
								schemaobj.secondary_number = current.unitNumber;
							}

							schemaobj.house_number = current.getHouseNumber();
							schemaobj.prefix_direction = standrdForm(
									current.get(AddColumns.PREDIRABRV),
									current.getState());
							schemaobj.prefix_qualifier = standrdForm(
									current.get(AddColumns.PREQUALABR),
									current.getState());
							schemaobj.prefix_type = standrdForm(
									current.get(AddColumns.PRETYPABRV),
									current.getState());
							schemaobj.street_name = makeStandardText(current
									.get(AddColumns.NAME));
							schemaobj.suffix_type = standrdForm(
									current.get(AddColumns.SUFTYPABRV),
									current.getState());
							schemaobj.suffix_direction = standrdForm(
									current.get(AddColumns.SUFDIRABRV),
									current.getState());
							schemaobj.city = makeStandardText(current
									.get(AddColumns.CITY));
							if (current.getState() != null)
								schemaobj.state = current.getState();
							else
								schemaobj.state = "";
							// String hashcode = current.get(AddColumns.ZIP)
							// + current.getState();

							if (current.getState() != null) {
								// String fipsCode = GenerateCache.FIPS
								// .get(hashcode.hashCode() + "");
								String fipsCode = U.getStateCode(current
										.getState())
										+ current.get(AddColumns.COUNTYNO);
								// if (fipsCode == null)
								// fipsCode = U.STATE_MAP.get(current
								// .getState());

								if (fipsCode.length() == 3)
									fipsCode = "0" + fipsCode;
								schemaobj.fipsCode = fipsCode;
							} else {
								schemaobj.fipsCode = "";
							}

							schemaobj.zip = current.get(AddColumns.ZIP);
							try {
								schemaobj.errorCode = OutputStatusCode
										.getStatusCode(
												inputList,
												getCompleteStreetWithoutUnit(
														current).toString(),
												schemaobj.city.toString(),
												schemaobj.zip.toString());
							} catch (Exception e) {
								schemaobj.errorCode = "";
							}

							if (addStruct.size() == 1) {
								if (current.hitScore > 5)
									current.hitScore = 5;
								avg = 5;
							}
							schemaobj.score = 100 * (current.hitScore / avg);

							if (schemaobj.score < 80 && current.hitScore > 2) {
								float p = calculateMatchingRate(inputList,
										current);

								if (p != 0) {
									schemaobj.score = p;
								}
							}

							if (jaroPer > schemaobj.score)
								schemaobj.score = jaroPer;

							if (jaroPer > 80)
								schemaobj.score = jaroPer;

							schemaobj.datasource = current
									.get(AddColumns.DATASOURCE);

							listOutput.add(schemaobj);

							schemaobj.message = "";

							if (schemaobj.city.split(" ").length == 1) {
								String cityWithoutSpace = schemaobj.city
										.replaceAll(" ", "");
								String cityobserverCity = com.shatam.util.Util.cityObserver
										.get(cityWithoutSpace.toUpperCase()
												.trim() + schemaobj.zip);

								if (cityobserverCity != null) {
									schemaobj.message = "City name standardized";
									schemaobj.city = makeStandardText(cityobserverCity
											.toLowerCase());
								}
							}

						}
					} else {

						AddressStruct struct = new AddressStruct("");
						struct.inputAddress = "No Match Found";
//						ShatamCachingSingle.put(
//								ShatamCachingSingle.k1_reference, struct);
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
						schemaobj.message = "";
						schemaobj.secondary_designator = "";
						schemaobj.secondary_number = "";
						listOutput.add(schemaobj);
					}
				}
				org.json.JSONArray jsonColl = new JSONArray();

				try {
					jsonColl = generateJson(listOutput);
				} catch (JSONException e) {

					U.log("Error is in output listing");
					e.printStackTrace();
				}

				outputArr.put(jsonColl);

			}
		}
U.log("outputArr :-"+outputArr);
		return outputArr;
	}

	public HashMap<String, String> getFips() {
		HashMap<String, String> fipsMap = new HashMap<>();

		return fipsMap;
	}

	public static float calculateScoring(String score, AddressStruct addstruct)
			throws Exception {
		float per = 0;
		int persentage;

		String foundaddress = DistanceMatchForResult
				.getCompleteStreet(addstruct).toLowerCase()
				.replace(addstruct.unitNumber, "");
		String inputAddress = addstruct.inputAddress.replace(
				addstruct.getHouseNumber(), "");
		if (com.shatam.util.Util
				.match(inputAddress.toLowerCase(),
						"(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
				&& com.shatam.util.Util.match(inputAddress.toLowerCase(),
						"\\d(th|st|nd|rd)") != null)
			inputAddress = inputAddress.toLowerCase().replaceAll(
					"\\d(th|st|nd|rd)", "");

		String foundcity = addstruct.get(AddColumns.CITY).toLowerCase();
		String inputCity = addstruct.getQueryCity();
		String state = addstruct.getState();
		String foundZip = addstruct.get(AddColumns.ZIP);
		String inputZip = addstruct.getQueryZip();
		StringBuffer buf = new StringBuffer();
		for (String s : inputAddress.split(" ")) {
			String result = standrdForm(s, addstruct.getState());
			buf.append(result);
			buf.append(" ");
		}
		String inputStreetAbrv = buf.toString().replace("  ", " ")
				.toLowerCase();
		String completeInputAddress = inputStreetAbrv.trim() + " "
				+ inputCity.trim() + " " + state.trim() + " " + inputZip.trim();
		String completeOutputAddress = foundaddress.trim() + " "
				+ foundcity.trim() + " " + state.trim() + " " + foundZip.trim();
		JaroWinkler algorithm = new JaroWinkler();
		float Matchingper = algorithm.getSimilarity(completeInputAddress
				.toLowerCase().replace("  ", " "), completeOutputAddress
				.toLowerCase().replace("  ", " "));
		return Matchingper;
	}

	public MultiMap addMultiplMap(ArrayList<InputJsonSchema> array)
			throws JSONException, JsonGenerationException,
			JsonMappingException, IOException {
		MultiMap multiMap = new MultiValueMap();
		Map<Integer,List<String>> groupMap = new HashMap<>();
		for (int i = 0; i < array.size(); i++) {
			InputJsonSchema innerArr = array.get(i);
			String state = innerArr.state.toUpperCase();
			ArrayList<AddressStruct> addStruct = null;
			String value = evaluateInputAddress(innerArr.toString());
			if (value == null) {
				
				String address2 = innerArr.address2.replaceAll(
						"[$&+,:;=?@#|*%()^!.~-]", "");
				String ival = Integer.toString(i);
				if(innerArr.groupId>0){
					
					List<String> l = groupMap.get(innerArr.groupId);
					if(l==null){
						l = new ArrayList<>();
					}
					l.add(ival);
					groupMap.put(innerArr.groupId, l);
					
				}
				multiMap.put(ival, innerArr.address1);
				multiMap.put(ival, address2);
				multiMap.put(ival, innerArr.city);
				multiMap.put(ival, state);
				multiMap.put(ival, innerArr.zip);
				multiMap.put(ival, innerArr.key);

			}
		}
		
	//	U.log(groupMap +"--");		
		return multiMap;
	}

	public MultiMap addMultiplMap(ArrayList<InputJsonSchema> array,Map<Integer,List<String>> groupMap)
			throws JSONException, JsonGenerationException,
			JsonMappingException, IOException {
		MultiMap multiMap = new MultiValueMap();
		for (int i = 0; i < array.size(); i++) {
			InputJsonSchema innerArr = array.get(i);
			String state = innerArr.state.toUpperCase();
			ArrayList<AddressStruct> addStruct = null;
			String value = evaluateInputAddress(innerArr.toString());
			if (value == null) {				
				String address2 = innerArr.address2.replaceAll(
						"[$&+,:;=?@#|*%()^!.~-]", "");
				String ival = Integer.toString(i);
				if(innerArr.groupId>0){					
					List<String> l = groupMap.get(innerArr.groupId);
					if(l==null){
						l = new ArrayList<>();
					}
					l.add(ival);
					groupMap.put(innerArr.groupId, l);					
				}
				multiMap.put(ival, innerArr.address1);
				multiMap.put(ival, address2);
				multiMap.put(ival, innerArr.city);
				multiMap.put(ival, state);
				multiMap.put(ival, innerArr.zip);
				multiMap.put(ival, innerArr.key);

			}
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
	}

	public String getCompleteStreetWithoutUnit(AddressStruct addstruct)
			throws Exception {
		StringBuffer buf = new StringBuffer();

		buf.append(addstruct.getHouseNumber());

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREDIRABRV),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREQUALABR),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PRETYPABRV),
						addstruct.getState()));

		buf.append(" "
				+ makeStandardText(addstruct.get(AddColumns.NAME).toUpperCase()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFTYPABRV),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFDIRABRV),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFQUALABR),
						addstruct.getState()));

		if (!StrUtil.isEmpty(addstruct.unitNumber)) {
			if (buf.length() > 0) {
				buf.append(" ");
			}

		}

		buf.trimToSize();

		return buf.toString().trim().replaceAll("\\s+", " ");
	}

	public String getCompleteStreet(AddressStruct addstruct) throws Exception {
		StringBuffer buf = new StringBuffer();

		buf.append(addstruct.getHouseNumber());

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREDIRABRV),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREQUALABR),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PRETYPABRV),
						addstruct.getState()));

		buf.append(" "
				+ makeStandardText(addstruct.get(AddColumns.NAME).toUpperCase()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFTYPABRV),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFDIRABRV),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFQUALABR),
						addstruct.getState()));

		if (!StrUtil.isEmpty(addstruct.unitNumber)) {
			if (buf.length() > 0) {
				buf.append(" ");
			}
			buf.append(addstruct.getUnitType());
			buf.append(" ").append(addstruct.unitNumber);
		}

		buf.trimToSize();

		return buf.toString().trim().replaceAll("\\s+", " ");
	}

	private JSONArray addToJson(ArrayList<AddressStruct> addStruct,
			String hitscore, String maxResults, String key) throws Exception {

		ArrayList<JsonSchema> listOutput = new ArrayList<JsonSchema>();
		if (addStruct.size() > 0) {

			for (int i = 0; i < Integer.parseInt(maxResults); i++) {
				AddressStruct current = addStruct.get(i);
				JsonSchema schemaobj = new JsonSchema();
				schemaobj.key = key;
				schemaobj.address = getCompleteStreet(current);
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

		long jsoncallstart = System.currentTimeMillis();
		org.json.JSONArray jsonColl = generateJson(listOutput);
		long jsonend = System.currentTimeMillis();

		return jsonColl;
	}

	private org.json.JSONArray generateJson(ArrayList<JsonSchema> listOutput)
			throws JSONException {

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
				arrr.add(obj.secondary_designator);
				arrr.add(obj.secondary_number);
				
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

				String inputAddressQuery = com.shatam.util.Util.match(
						query.toString().replaceAll("k1:", ""),
						"(.*?[a-z]+\\^5.*?)[a-z]+_city", 0).replaceAll(
						"[a-z]+_city|\\^5\\.0|\\d+", "");

				String inputHashKey = ((inputAddressQuery
						.replaceAll("\\^5", "") + city.toLowerCase()).replace(
						" ", ""));

				String outputHashKeyStreetName = ((addStruct.get(
						AddColumns.NAME).toLowerCase().replace(" ", "")) + foundCity
						.toLowerCase());

				String outputHashKeyAddress = ((addStruct.get(
						AddColumns.PREDIRABRV).toLowerCase()
						+ addStruct.get(AddColumns.NAME).toLowerCase()
						+ addStruct.get(AddColumns.SUFTYPABRV).toLowerCase() + addStruct
						.get(AddColumns.SUFDIRABRV).toLowerCase()).replace(" ",
						"") + foundCity.toLowerCase());

				if (DistanceMatchForResult.jaroMatch(inputHashKey,
						outputHashKeyStreetName) > 0.84) {
					return (DistanceMatchForResult.jaroMatch(inputHashKey,
							outputHashKeyStreetName)) * 100;
				}
				if (DistanceMatchForResult.jaroMatch(inputHashKey,
						outputHashKeyAddress) > 0.84) {
					return (DistanceMatchForResult.jaroMatch(inputHashKey,
							outputHashKeyAddress)) * 100;
				}
			} else {

			}
		} catch (Exception e) {

			return 0;
		}
		return 0;
	}

	public static String standrdForm(String s, String state) throws Exception {

		String val;
		if (s.trim().length() > 0) {
			val = abbrv.get(s.toUpperCase());

		} else
			val = AbbrReplacement.getFullAddress(s, state);

		if (val == null)
			val = AbbrReplacement.getFullAddress(s, state);

		return val;
	}

	public static String makeStandardText(String val) {

		if (val != null && val.length() > 0) {
			val = val.replace("  ", " ");
			String capital = null, wd;
			String[] words = val.trim().split(" ");

			for (String word : words) {

				wd = word.substring(0, 1).toUpperCase() + word.substring(1);
				capital = (capital == null) ? wd : capital + " " + wd;
			}

			val = capital;

			if (capital == null) {

				val = "";

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
