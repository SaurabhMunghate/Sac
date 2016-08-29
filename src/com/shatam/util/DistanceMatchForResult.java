package com.shatam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.shatam.data.ZipCodes;
import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.sun.jndi.url.iiopname.iiopnameURLContextFactory;

public class DistanceMatchForResult {
	// Commented by me

	/*
	 * private static boolean isMatchGoodEnough(final String name1, final
	 * AddressStruct a, final AbstractIndexType it) throws Exception {
	 * 
	 * if (name1.length() < 2) return false;
	 * 
	 * final String q1 = a.getshatamIndexQueryString(); final String q2 =
	 * q1.replace(U.STREET_ENHANCE, "").replace(U.CITY_ENHANCE, "").replace("_",
	 * ""); final String name2 = it.encode(name1);
	 * 
	 * if (name2.length() < 2) return false;
	 * 
	 * //U.log("is matchfound==="+q1+"::"+name1);
	 * 
	 * 
	 * if (q1.toLowerCase().contains(name1.toLowerCase()) ||
	 * q1.toLowerCase().contains(name2.toLowerCase())) return true; if
	 * (q2.toLowerCase().contains(name1) || q2.toLowerCase().contains(name2))
	 * return true;
	 * 
	 * final String name3 = findLargestString(name1.split(" ")); final String
	 * name4 = findLargestString(name2.split(" "));
	 * 
	 * if (q1.toLowerCase().contains(name3) || q1.toLowerCase().contains(name4))
	 * return true; if (q2.toLowerCase().contains(name3) ||
	 * q2.toLowerCase().contains(name4)) return true;
	 * 
	 * 
	 * 
	 * return false; }
	 */// isMatchGoodEnough()
		// ******************************************************************************************************************************
	static String foundAddress = "";
	static String inputAddresses = "";

	private static boolean isMatchGoodEnough(String name1,
			final AddressStruct a, final AbstractIndexType it, String value,
			String caseVal) throws Exception {
		JaroWinkler algorithm = new JaroWinkler();
		String inputStreetAbrv = "";
		String inputCity = "";
		String q1 = a.getLuceneQueryString();
		String numberWords = "zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth"
				.toUpperCase();
		q1 = q1.replaceAll("(" + numberWords + ")\\^5 ", "");
		// U.log("q1=="+q1);

		StringBuffer buf = new StringBuffer();

		// U.log("HouceNumber=="+a.getHouseNumber());
		// U.log("Name1=="+name1);
		String street = getCompleteStreet(a).replace("  ", " ").trim();
		//U.log("CompleteName1==" + street);
		foundAddress = street;
	// U.log(a.inputAddress+"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"+a.getHouseNumber());
		String inputAddress = a.inputAddress.replace(a.getHouseNumber(), "");
		if (Util.match(street.toLowerCase(),
				"(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
				&& Util.match(inputAddress.toLowerCase(),
						"(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
				&& Util.match(inputAddress.toLowerCase(), "\\d(th|st|nd|rd)") != null)
			inputAddress = inputAddress.toLowerCase().replaceAll(
					"\\d(th|st|nd|rd)", "");

		if (Util.match(street.toLowerCase(), "\\d(th|st|nd|rd)") != null
				&& Util.match(inputAddress.toLowerCase(),
						"(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
				&& Util.match(inputAddress.toLowerCase(), "\\d(th|st|nd|rd)") != null)
			inputAddress = inputAddress
					.toLowerCase()
					.replaceAll(
							"(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)",
							"");

		if (value.equalsIgnoreCase("street")) {
			for (String s : inputAddress.split(" ")) {
				String result = standrdForm(s, a.getState());
				buf.append(result);
				buf.append(" ");
			}
			inputStreetAbrv = buf.toString().replace("  ", " ");
			inputAddresses = inputStreetAbrv;
		} else {

			Matcher m = null;
			int groupNum = 1;

			m = Pattern.compile("([A-Z0-9]+)_CITY\\^4",
					Pattern.CASE_INSENSITIVE).matcher(" " + q1);

			if (groupNum <= 0) {
				if (m.find()) {
					inputCity = m.group();
				} else {
					inputCity = null;
				}
			}

			if (m.find(groupNum)) {
				inputCity = m.group(groupNum).trim();
			} else {
				inputCity = null;
			}

			//U.log("FOUND city==" + inputCity);
			name1 = it.encode(name1.toLowerCase());
		}

		switch (caseVal) {
		case "approxMatching":

			if (value.equalsIgnoreCase("street")) {

				//U.log(inputStreetAbrv + "==:::inputAddress==" + inputAddress);

				if (inputAddress != null) {
/*
					U.log("street:::::::::::::::::::"
							+ algorithm.getSimilarity(inputStreetAbrv
									.toLowerCase().trim(), street.toLowerCase()
									.trim()));*/
					if (algorithm.getSimilarity(inputStreetAbrv.toLowerCase()
							.trim(), street.toLowerCase().trim()) > 0.9)
						return true;

				}
			} else {

				 //U.log(it.encode(name1.toLowerCase())+"=encode value of city=="+inputCity);
				if (inputCity != null) {

					/*U.log("city:::::::::::::::::::"
							+ algorithm.getSimilarity(inputCity.toLowerCase(),
									name1.toLowerCase()));*/
					if (algorithm.getSimilarity(inputCity.toLowerCase(), name1) > 0.9)
						return true;
					int jaroLastTime = (int) System.currentTimeMillis();
				}
			}

			break;

		case "contains":
			// U.log("in case CONTAINS");
			//U.log(inputStreetAbrv + "==inputAddress==" + street);
			if (value.equalsIgnoreCase("street")) {

				float f = algorithm.getSimilarity(inputStreetAbrv.trim(),
						street);
				// U.log("matching value=="+f);
				if (inputStreetAbrv.contains(street) && f > 0.84) {
					// U.log("_________________yes contain street");
					return true;
				} else {
					return false;
				}
			} else {
				// U.log(inputCity+"==inputcity=="+name1);
				/*
				 * if(inputCity.contains(name1))return true; else{ return false;
				 * }
				 */
				if (inputCity != null) {

				//	U.log("city:::::::::::::::::::"
				//			+ algorithm.getSimilarity(inputCity.toLowerCase(),
				//					name1));
					if (algorithm.getSimilarity(inputCity.toLowerCase(), name1) > 0.9)
						return true;
					int jaroLastTime = (int) System.currentTimeMillis();
				}
			}

		default:
			boolean result=false;
			
			if(it.getFieldName().contains("k1")||it.getFieldName().contains("k2")||it.getFieldName().contains("k3"))result=isMatchGoodEnough1(name1, a, it, value);
			else
				result=false;
			
			return result;

		}
		return false;
	}

	private static boolean isMatchGoodEnough1(String name1,
			final AddressStruct a, final AbstractIndexType it, String value)
			throws Exception {
		String inputAddress = a.inputAddress.replace(a.getHouseNumber(), "")
				.trim();
		JaroWinkler algorithm = new JaroWinkler();
		// U.log(":::::::::::::::::::"+algorithm.getSimilarity(s.toLowerCase(),name3.toLowerCase()));

		// U.log("inputed address======="+inputAddress+"::");

		// if(name1.toLowerCase().contains("canyon"))Thread.sleep(7000);

		// U.log("name1:- " + name1);

		 if(name1.contains("RR"))
		 name1=name1.replaceAll("\\d+", "");

		if (name1.length() < 1)
			return false;

		String q1 = a.getLuceneQueryString();
	//	 U.log("Q1: " + q1);
		if (q1.contains("-"))
			return true;
		// @@@@@@@@@@@@@@@@@
		/*
		 * if (name1.contains("PO BOX") && value.contains("street") &&
		 * a.get(AddColumns.CITY).toUpperCase().contains("BAKERSFIELD") &&
		 * q1.contains("BAKERSFIELD")) { return true; }
		 */

		// U.log("Distance match for res___Q1: " + q1);

		final String q2 = q1.replace(U.STREET_ENHANCE, "")
				.replace(U.CITY_ENHANCE, "").replace("_", "");

		// U.log("Q2: " + q2);
		String q3 = q2.replaceAll("(\\d+)(TH|ST|RD)", "$1");
		// U.log("q3===="+q3);
		// name1=name1.replaceAll("\\d+", "");
		// U.log("Name1=="+name1);
		String name2 = it.encode(name1);
	//	 U.log("Name2=="+name2);
		if (name2.length() < 3)
			return false;

		if (value.equalsIgnoreCase("street")) {

			if (inputAddress.split(" ").length > 3
					&& name1.split(" ").length < 2) {
			//	U.log(inputAddresses+"::::::::::::::::::::::::::"+algorithm.getSimilarity(inputAddresses.toLowerCase(),foundAddress.toLowerCase()));
				if (algorithm.getSimilarity(inputAddresses.toLowerCase(),
						foundAddress.toLowerCase()) > 0.74)
					return true;

				return false;
			}
			if (name1.toLowerCase().contains(a.getQueryCity().toLowerCase()))
				return false;
		}

		if (q1.contains(name1) || q1.contains(name2))
			return true;

		if (q2.contains(name1) || q2.contains(name2))
			return true;

		if (q3.contains(name1) || q3.contains(name2))
			return true;

		// Additional code
		/*
		 * name1=name1.replaceAll("\\d+", ""); name2 = it.encode(name1); if
		 * (name2.length() < 2) return false; if (q1.contains(name1) ||
		 * q1.contains(name2)) return true; if (q2.contains(name1) ||
		 * q2.contains(name2)) return true;
		 */

		String name3 = findLargestString(name1.split(" "));
		// U.log("name3==" + name3);
		if (value.equalsIgnoreCase("street")) {

			if (inputAddress.split(" ").length > 1
					&& name3.split(" ").length < 2) {
				// U.log("hello shatam");
				return false;
			}
		}
		String name4 = findLargestString(name2.split(" "));

		// U.log("name3==" + name3);
		// U.log("Name4==" + name4);

		name3 = name3.replaceAll("\\d+", "-null-");
		name4 = name4.replaceAll("\\d+", "-null-");

		// U.log(name3.contains(Util.match(q1,
		// "([A-Z]+)_CITY\\^4",1).toLowerCase())+"match if city=="+Util.match(q1,
		// "([A-Z]+)_CITY\\^4",1).toLowerCase());
		if (value.equalsIgnoreCase("street"))
			if (name3.toLowerCase().contains(a.getQueryCity().toLowerCase()))
				return false;// added at 19th nov 2015
								// "80","113  OAK ST"," ","YUKON","PA","15698"

		// name4=name3.substring(1);

		// U.log("Q1=" + q1);

		if (name3.trim().length() > 1)
			if (q1.toLowerCase().contains(name3))
				return true;

		if (name4.trim().length() > 1)
			if (q1.contains(name4))
				return true;

		if (name3.trim().length() > 1)
			if (q2.contains(name3))
				return true;

		if (name4.trim().length() > 1)
			if (q2.contains(name4))
				return true;

		String s = "";
		Matcher m = null;
		int groupNum = 1;
		if (value.equalsIgnoreCase("street")) {
			m = Pattern.compile("([A-Z]+|[a-z]+)[\\~]?[\\^5]?",
					Pattern.CASE_INSENSITIVE).matcher(" " + q1);
		} else {
			m = Pattern.compile("([A-Z0-9]+)_CITY\\^4",
					Pattern.CASE_INSENSITIVE).matcher(" " + q1);

		}

		if (groupNum <= 0) {
			if (m.find()) {
				s = m.group();
			} else {
				s = null;
			}
		}

		if (m.find(groupNum)) {
			s = m.group(groupNum).trim();
		} else {
			s = null;
		}

		// U.log("MATCHER=="+s);
		int jaroTime = (int) System.currentTimeMillis();
		if (s != null) {

			 //U.log(":::::::::::::::::::"+algorithm.getSimilarity(s.toLowerCase(),name3.toLowerCase()));
			if (algorithm.getSimilarity(s.toLowerCase(), name3.toLowerCase()) > 0.84)
				return true;
			int jaroLastTime = (int) System.currentTimeMillis();
			// U.log("jaroalgo time=="+(jaroLastTime-jaroTime));
		}

		// Create HEre another matcher usesing Double Metaphone..

		// Here I writtena another logic of matching the addresses...

		// //U.log("Query String:  " + q1);
		// //U.log("After Enchase it Query String:  " + q2);
		// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
		/*
		 * Pattern pat = null; StringBuffer buf = new StringBuffer();
		 * 
		 * if (value.equalsIgnoreCase("street")) { // pat =
		 * Pattern.compile("[A-Z]+\\^5"); // pat =
		 * Pattern.compile("[A-Z]+~1\\^5"); // //U.log("Q1::: " + q1);
		 * 
		 * pat = Pattern.compile("\\d+[A-Z]+~1|[A-Z]+~1");
		 * 
		 * Matcher mat = pat.matcher(q1);
		 * 
		 * while (mat.find()) { buf.append(mat.group());
		 * 
		 * } if (buf.toString().isEmpty()) { q1 =
		 * q1.replace(a.get(AddColumns.ZIP), ""); String cty =
		 * a.get(AddColumns.CITY).replaceAll("\\s+", ""); q1 =
		 * q1.replace(cty.toUpperCase(), "");
		 * 
		 * q1 = q1.replace(U.STREET_ENHANCE, "") .replace(U.CITY_ENHANCE,
		 * "").replace("_", ""); q1 = q1.replaceAll("CITY|ZIP", "");
		 * 
		 * String street = removePrefix_Suf(q1, "AZ").toUpperCase(); street =
		 * street.replace("?", ""); if (street.length() > 1) { if
		 * (street.contains(name1) || name1.contains(street)) return true; }
		 * //U.log("Street Found ::" + street);
		 * 
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if (value.equalsIgnoreCase("city")) { pat =
		 * Pattern.compile("[A-Z]+_CITY~\\^4");
		 * 
		 * // Alveys try to use the Interface because its faster than other //
		 * class.....
		 * 
		 * Matcher mat = pat.matcher(q1); // StringBuffer buf = new
		 * StringBuffer(); while (mat.find()) {
		 * 
		 * buf.append(mat.group());
		 * 
		 * } }
		 * 
		 * // //U.log(it.getFieldName().contains("k1")); //
		 * //U.log(!buf.toString().isEmpty()); if
		 * (it.getFieldName().contains("k1") && !buf.toString().isEmpty()) {
		 * 
		 * // System.out.println("Name Printing Here.." + buf.toString()); //
		 * System.out.println("Found Name: " + name1); String q3 = new
		 * Soundex().encode(buf.toString());
		 * 
		 * String name5 = new Soundex().encode(name1);
		 * 
		 * // q3=q3.substring(1); // //U.log("name5:  " + name5); // Kirti made
		 * changes if (name5.trim().length() != 0) { name5 = name5.substring(1);
		 * } // //U.log("Q3::::  " + q3);
		 * 
		 * // //U.log("name5:  " + name5);
		 * 
		 * // //U.log("Q3: " + q3); // // //U.log("name5: " + name5);
		 * 
		 * String numStreet = extractPattern(q1, "\\d+[A-Z]+~1"); //
		 * //U.log("NumStreet: " + numStreet); if (numStreet.length() == 0) {
		 * 
		 * if (q3.contains(name5) || q3.contains(name5)) {
		 * 
		 * return true; } }
		 * 
		 * if (value.equalsIgnoreCase("street")) {
		 * 
		 * String str = buf.toString().replace("~1", "").trim(); int dist =
		 * StringUtils.getLevenshteinDistance(name1, str);
		 * 
		 * System.out.println("Dist: " + dist);
		 * 
		 * if (dist == 1) return checkDiffChar(name1, str); // return true; //
		 * Original Addresses...
		 * 
		 * }
		 * 
		 * }
		 * 
		 * // buf=null;
		 * 
		 * //U.log("isMatchGoodEnough q1:" + q1);
		 * //U.log("isMatchGoodEnough q2:" + q2);
		 * //U.log("isMatchGoodEnough name1:" + name1);
		 * //U.log("isMatchGoodEnough name2:" + name2);
		 */
		// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
		return false;
	}// isMatchGoodEnough()

	private static String removePrefix_Suf(String streetName, String state) {
		// = add.split("\\s");

		// add = "5723  44th ln";
		streetName = streetName.toLowerCase();
		String splitAdd[] = streetName.split("[^\\d\\w\\-/]");
		splitAdd = streetName.split("\\s");
		List<String> abbrArr = new ArrayList<String>();

		for (int i = 0; i < splitAdd.length; i++) {

			String str = AbbrReplacement.getAbbr(splitAdd[i], state);

			str = AbbrReplacement.getsuffixFull(str, state);
			if (str != null) {
				if (!abbrArr.contains(str)) {
					abbrArr.add(str);
					String strAbbri = AbbrReplacement.getAbbr(str, "AZ");
					if (!splitAdd[i].equalsIgnoreCase(strAbbri)) {
						strAbbri = splitAdd[i];
					}

					// //U.log("\nstrabbr====="+strAbbri);

					streetName = streetName.replace(strAbbri, "");

				}
			}

		}

		for (int i = 0; i < splitAdd.length; i++) {
			// //U.log("Passed String: " + splitAdd[i]);
			String str = AbbrReplacement.getAbbr(splitAdd[i], "AZ");

			str = AbbrReplacement.getsuffixFull(str, "AZ");
			if (str != null) {
				if (!abbrArr.contains(str)) {
					abbrArr.add(str);

					String strAbbri = AbbrReplacement.getAbbr(str, "AZ");
					if (!splitAdd[i].equalsIgnoreCase(strAbbri)) {
						strAbbri = splitAdd[i];
					}
					streetName = streetName.replace(strAbbri, "");

				}
			}

		}

		// String [] abbPreAndSuf = abbrArr.toArray(new String[abbrArr.size()]);

		return streetName;

	}

	private static String extractPattern(String str, String pattarn) {

		StringBuffer buf = new StringBuffer();
		Pattern pat = Pattern.compile(pattarn);
		Matcher mat = pat.matcher(str);

		// StringBuffer buf = new StringBuffer();
		while (mat.find()) {

			buf.append(mat.group());

		}

		return buf.toString();

	}

	private static boolean checkDiffChar(CharSequence cs1, CharSequence cs2) {

		if ((cs1 == null) || (cs2 == null)) {
			throw new IllegalArgumentException("String must not be null");
		}
		for (int i = 1; (i < cs1.length()) && (i < cs2.length()); i++) {
			if (cs1.charAt(i) != cs2.charAt(i)) {
				return false;
			}
		}
		return false;
	}

	// **********************************************************************************************************************************

	private static final double RETRY_IF_LOWER_THAN_SCORE = 2.0;
	private AddressStruct addStruct = null;
	private AbstractIndexType indexType = null;

	public DistanceMatchForResult(AddressStruct a, final AbstractIndexType it) {
		addStruct = a;
		indexType = it;
	}

	public boolean isResultMatched(String caseVal, String key) throws Exception {
		float score = score(addStruct);

		if (score > 3) {
			// U.log("Hitscore are greater than 3");
			// return true;
		}

		final String foundStreet = addStruct.getFoundName();
		final String foundCity = addStruct.get(AddColumns.CITY).toUpperCase();
		final String foundZip = addStruct.get(AddColumns.ZIP).toUpperCase();
		// U.log("C Found :" + foundStreet + " , " +
		// foundCity+", "+DistanceMatchForResult.isMatchGoodEnough(foundStreet,
		// addStruct, indexType,"street",caseVal));

		if (DistanceMatchForResult.isMatchGoodEnough(foundStreet, addStruct,
				indexType, "street", caseVal)) {

			// U.log("street Match***************");
			// I have removed zipchk condition on 19th nov 2015
			if (DistanceMatchForResult.isMatchGoodEnough(foundCity, addStruct,
					indexType, "city", caseVal))// ||
												// DistanceMatchForResult.isMatchGoodEnough(foundZip,
												// addStruct,
												// indexType,"zip",caseVal)
			{
				 //U.log("D Found :" + foundStreet + " , " + foundCity);
				ShatamIndexReader.addressesWithoutZipTest = new ArrayList<>();
				ShatamIndexReader.addressesWithoutZipTest.add(addStruct);
				ShatamIndexReader.mapaddressesWithoutZipTest.put(key,
						ShatamIndexReader.addressesWithoutZipTest);

				if (chkForZip(foundZip, foundCity, addStruct))
					return true;

			}

		}

		return false;
	}

	public static boolean chkForZip(String foundZip, String foundCity,
			AddressStruct addStruct) {
		String inputCity = addStruct.getQueryCity();
		// U.log("inputcity=="+inputCity);
		String inputZip = addStruct.getQueryZip();
		// U.log(foundZip+"***********************inputzip=="+inputZip);
		if (inputZip.replace(" ", "").length() == 5
				&& !inputZip.contains(foundZip.trim())) {
			//U.log("sizes of cities==="+Util.zipToCity.size());
			HashSet<String> cities = Util.zipToCity.get(inputZip);
			 //U.log("sizes of cities==="+cities.size());
			if (cities != null)
				for (String city : cities) {
					 //U.log(city);
					if (city.toLowerCase().contains(foundCity.toLowerCase()))
						return false;
					else {
						return true;
					}

				}
		}
		return true;
	}

	/*
	 * private static int levenshteinDistance(String s, String t) { //
	 * degenerate cases if (s == t) return 0; if (s.length() == 0) return
	 * t.length(); if (t.length() == 0) return s.length();
	 * 
	 * // create two work vectors of integer distances int[] v0 = new
	 * int[t.length() + 1]; int[] v1 = new int[t.length() + 1];
	 * 
	 * // initialize v0 (the previous row of distances) // this row is A[0][i]:
	 * edit distance for an empty s // the distance is just the number of
	 * characters to delete from t for (int i = 0; i < v0.length; i++) v0[i] =
	 * i;
	 * 
	 * for (int i = 0; i < s.length(); i++) { // calculate v1 (current row
	 * distances) from the previous row v0
	 * 
	 * // first element of v1 is A[i+1][0] // edit distance is delete (i+1)
	 * chars from s to match empty t v1[0] = i + 1;
	 * 
	 * // use formula to fill in the rest of the row for (int j = 0; j <
	 * t.length(); j++) { int cost = (s.charAt(i) == t.charAt(j)) ? 0 : 1; v1[j
	 * + 1] = minimum(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost); }
	 * 
	 * // copy v1 (current row) to v0 (previous row) for next iteration for (int
	 * j = 0; j < v0.length; j++) v0[j] = v1[j]; }
	 * 
	 * return v1[t.length()]; }
	 * 
	 * private static int minimum(int i, int j, int k) { return Math.min(i,
	 * Math.min(j, k)); }
	 */
	private static String findLargestString(String[] arr) {
		String max = "";
		for (String s : arr) {
			if (StrUtil.isNum(s))
				return s;

			if (s.equalsIgnoreCase("highway") || s.equalsIgnoreCase("road")
					|| s.equalsIgnoreCase("CREEK")
					|| s.equalsIgnoreCase("LAKES"))
				continue;

			if (max.length() < s.length()) {
				max = s;
			}
		}
		// U.log("**********************************"+max);
		return max;
	}

	private static float score(AddressStruct a) {
		return a != null ? a.hitScore : 0;
	}

	public static float jaroMatch(String s1, String s2) {

		JaroWinkler algorithm = new JaroWinkler();

		if (algorithm.getSimilarity(s1, s2) > 0.80)
			return algorithm.getSimilarity(s1, s2);
		else {
			return 0;
		}

	}

	public static String standrdForm(String s, String state) throws Exception {
		// String val=AddressStruct.getshortForm(s, state, col);
		// U.log(s+"state=="+state);
		String val;
		if (s.trim().length() > 0) {

			val = abbrv.get(s.toUpperCase());
			// U.log(s+"==val=="+val);
		} else
			val = AbbrReplacement.getFullAddress(s, state);

		if (val == null)
			val = AbbrReplacement.getFullAddress(s, state);
		// U.log(val);
		return val;
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

	public static String getCompleteStreet(AddressStruct addstruct)
			throws Exception {
		StringBuffer buf = new StringBuffer();
		// U.log("complete address====="+buf.toString());
		String name = "";
		StringBuffer nbuf = new StringBuffer();
		for (String s : addstruct.get(AddColumns.NAME).toString().split(" ")) {
			String result = standrdForm(s, addstruct.getState());
			nbuf.append(result);
			nbuf.append(" ");
		}
		name = nbuf.toString().replace("  ", " ");

		// U.log(addstruct.getState()+"==1wcomplete address====="+buf.toString());
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREDIRABRV),
						addstruct.getState()));
		// U.log("1complete address====="+buf.toString());
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREQUALABR),
						addstruct.getState()));
		// U.log("2complete address====="+buf.toString());
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PRETYPABRV),
						addstruct.getState()));
		// U.log("3complete address====="+buf.toString());
		buf.append(" " + name);
		// U.log("4complete address====="+buf.toString());
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFTYPABRV),
						addstruct.getState()));
		// U.log("5complete address====="+buf.toString());
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFDIRABRV),
						addstruct.getState()));
		// U.log("6complete address====="+buf.toString());
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.SUFQUALABR),
						addstruct.getState()));
		// U.log("7complete address====="+buf.toString());
		// U.log("complete address====="+buf.toString());
		if (!StrUtil.isEmpty(addstruct.unitNumber)) {
			if (buf.length() > 0) {
				buf.append(" ");
			}
			//buf.append(addstruct.getUnitType());
			//buf.append(" ").append(addstruct.unitNumber);
			//U.log("Unitnumber: " + addstruct.unitNumber);
		}

		buf.trimToSize();
		return buf.toString().trim().replace("  ", " ");
	}

}// class DistanceMatchForResult