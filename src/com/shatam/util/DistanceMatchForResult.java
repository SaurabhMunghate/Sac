/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;

public class DistanceMatchForResult {

	static String foundAddress = "";
	static String inputAddresses = "";

	private static boolean isMatchGoodEnough(String name1,
			final AddressStruct a, final AbstractIndexType it, String value,
			String caseVal, int distanceCriteria, BoostAddress boostAddress)
			throws Exception {
		JaroWinkler algorithm = new JaroWinkler();
		String inputStreetAbrv = "";
		String inputCity = "";
		String q1 = a.getShatamQueryString();
		String numberWords = "zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth"
				.toUpperCase();
		q1 = q1.replaceAll("(" + numberWords + ")\\^5 ", "");

		/**
		 * To check distance criteria
		 */
		float threshold = 0f;
		if (distanceCriteria >= 70 && distanceCriteria <= 100) {
			threshold = distanceCriteria / 100f;
		} else {
			threshold = 90f / 100;
		}

		StringBuffer buf = new StringBuffer();

		String street = getCompleteStreet(a).replace("  ", " ").trim();

		foundAddress = street;

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
		
		//w 90th st > w 90 st 
		if (Util.match(street.toLowerCase(), "\\d(th|st|nd|rd)") != null				
				&& Util.match(inputAddress.toLowerCase(), "\\d(th|st|nd|rd)") == null){
			street = street
					.toLowerCase()
					.replaceAll("(\\d)(th|st|nd|rd)", "$1");
		}
		
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

			// m = Pattern.compile("([A-Z0-9]+)_CITY\\^4",
			// Pattern.CASE_INSENSITIVE).matcher(" " + q1);
			m = Pattern.compile(
					"([A-Z0-9]+)_CITY\\" + boostAddress.getCityWeight(),
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

			name1 = it.encode(name1.toLowerCase());
		}		
		switch (caseVal) {
		case "approxMatching":

			if (value.equalsIgnoreCase("street")) {					
				if (inputAddress != null) {
					if (algorithm.getSimilarity(inputStreetAbrv.toLowerCase()
							.trim(), street.toLowerCase().trim()) > 0.9){						
						return true;
					}
				}
			} else {

				if (inputCity != null) {

					// if (algorithm.getSimilarity(inputCity.toLowerCase(),
					// name1) > 0.9)
					if (algorithm.getSimilarity(inputCity.toLowerCase(), name1) > threshold)
						return true;					
				}
			}

			break;

		case "contains":

			if (value.equalsIgnoreCase("street")) {
				float f = algorithm.getSimilarity(inputStreetAbrv.trim().toLowerCase(),
						street.toLowerCase());			
				if (inputStreetAbrv.toLowerCase().contains(street.toLowerCase()) && f > 0.84) {
					return true;
				} else {
					return false;
				}
			} else {

				if (inputCity != null) {

					// if (algorithm.getSimilarity(inputCity.toLowerCase(),
					// name1) > 0.9)
					if (algorithm.getSimilarity(inputCity.toLowerCase(), name1) > threshold)
						return true;
					
				}
			}

		default:
			boolean result = false;

			if (it.getFieldName().contains("k1")
					|| it.getFieldName().contains("k2")
					|| it.getFieldName().contains("k3"))
				result = isMatchGoodEnough1(name1, a, it, value, boostAddress);
			else
				result = false;			
			return result;

		}
		return false;
	}

	private static boolean isMatchGoodEnough1(String name1,
			final AddressStruct a, final AbstractIndexType it, String value,
			BoostAddress boostAddress) throws Exception {
		String inputAddress = a.inputAddress.replace(a.getHouseNumber(), "")
				.trim();
		JaroWinkler algorithm = new JaroWinkler();

		if (name1.contains("RR"))
			name1 = name1.replaceAll("\\d+", "");

		if (name1.length() < 1)
			return false;

		String q1 = a.getShatamQueryString();

		if (q1.contains("-"))
			return true;

		// final String q2 = q1.replace(U.STREET_ENHANCE,
		// "").replace(U.CITY_ENHANCE, "").replace("_", "");
		final String q2 = q1.replace(U.STREET_ENHANCE, "")
				.replace(boostAddress.getCityWeight(), "").replace("_", "");

		String q3 = q2.replaceAll("(\\d+)(TH|ST|RD)", "$1");

		String name2 = it.encode(name1);

		if (name2.length() < 3)
			return false;

		if (value.equalsIgnoreCase("street")) {

			if (inputAddress.split(" ").length > 3
					&& name1.split(" ").length < 2) {

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

		String name3 = findLargestString(name1.split(" "));

		if (value.equalsIgnoreCase("street")) {

			if (inputAddress.split(" ").length > 1
					&& name3.split(" ").length < 2) {

				return false;
			}
		}
		String name4 = findLargestString(name2.split(" "));

		name3 = name3.replaceAll("\\d+", "-null-");
		name4 = name4.replaceAll("\\d+", "-null-");

		if (value.equalsIgnoreCase("street"))
			if (name3.toLowerCase().contains(a.getQueryCity().toLowerCase()))
				return false;
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
			// m =
			// Pattern.compile("([A-Z0-9]+)_CITY\\^4",Pattern.CASE_INSENSITIVE).matcher(" "
			// + q1);
			m = Pattern.compile(
					"([A-Z0-9]+)_CITY\\" + boostAddress.getCityWeight(),
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

		int jaroTime = (int) System.currentTimeMillis();
		if (s != null) {

			if (algorithm.getSimilarity(s.toLowerCase(), name3.toLowerCase()) > 0.84)
				return true;
			int jaroLastTime = (int) System.currentTimeMillis();

		}

		return false;
	}

	private static String removePrefix_Suf(String streetName, String state) {

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

					streetName = streetName.replace(strAbbri, "");

				}
			}

		}

		for (int i = 0; i < splitAdd.length; i++) {

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

		return streetName;

	}

	private static String extractPattern(String str, String pattarn) {

		StringBuffer buf = new StringBuffer();
		Pattern pat = Pattern.compile(pattarn);
		Matcher mat = pat.matcher(str);

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

	private static final double RETRY_IF_LOWER_THAN_SCORE = 2.0;
	private AddressStruct addStruct = null;
	private AbstractIndexType indexType = null;

	public DistanceMatchForResult(AddressStruct a, final AbstractIndexType it) {
		addStruct = a;
		indexType = it;
	}

	public boolean isResultMatched(String caseVal, String key,
			int distanceCriteria, BoostAddress boostAddress) throws Exception {
		float score = score(addStruct);

		final String foundStreet = addStruct.getFoundName();
		final String foundCity = addStruct.get(AddColumns.CITY).toUpperCase();
		final String foundZip = addStruct.get(AddColumns.ZIP).toUpperCase();

		if (DistanceMatchForResult.isMatchGoodEnough(foundStreet, addStruct,
				indexType, "street", caseVal, distanceCriteria, boostAddress)) {

			if (DistanceMatchForResult.isMatchGoodEnough(foundCity, addStruct,
					indexType, "city", caseVal, distanceCriteria, boostAddress)) {

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

		String inputZip = addStruct.getQueryZip();

		if (inputZip.replace(" ", "").length() == 5
				&& !inputZip.contains(foundZip.trim())) {

			HashSet<String> cities = Util.zipToCity.get(inputZip);
			
			if (cities != null)
				for (String city : cities) {

					if (city.toLowerCase().contains(foundCity.toLowerCase()))
						return false;
					else {
						return true;
					}

				}
		}
		return true;
	}

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

		String val;
		if (s.trim().length() > 0) {

			val = abbrv.get(s.toUpperCase());

		} else
			val = AbbrReplacement.getFullAddress(s, state);
		if (val == null)
			val = AbbrReplacement.getFullAddress(s, state);
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
		String name = "";
		StringBuffer nbuf = new StringBuffer();
		for (String s : addstruct.get(AddColumns.NAME).toString().split(" ")) {
			String result = standrdForm(s, addstruct.getState());
			nbuf.append(result);
			nbuf.append(" ");
		}
		name = nbuf.toString().replace("  ", " ");
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREDIRABRV),
						addstruct.getState()));
		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PREQUALABR),
						addstruct.getState()));

		buf.append(" "
				+ standrdForm(addstruct.get(AddColumns.PRETYPABRV),
						addstruct.getState()));

		buf.append(" " + name);

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
		return buf.toString().trim().replace("  ", " ");
	}

}