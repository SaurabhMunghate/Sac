/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class USStates {
	public static String abbr(String name) {
		name = name.trim().toLowerCase();
		for (String objname : map.keySet()) {
			String sname = objname.trim().toLowerCase();
			if (sname.equalsIgnoreCase(name))
				return map.get(objname);
		}
		return null;
	}

	public static Set<String> getAllFull() {
		return map.keySet();
	}

	public static Collection<String> getAllAbbr() {
		return map.values();
	}

	private static HashMap<String, String> map = new HashMap<String, String>();
	static {
		map.put("Alabama", "AL");
		map.put("Alaska", "AK");
		map.put("Arizona", "AZ");
		map.put("Arkansas", "AR");
		map.put("California", "CA");
		map.put("Colorado", "CO");
		map.put("Connecticut", "CT");
		map.put("Delaware", "DE");
		map.put("Florida", "FL");
		map.put("Georgia", "GA");
		map.put("Hawaii", "HI");
		map.put("Idaho", "ID");
		map.put("Illinois", "IL");
		map.put("Indiana", "IN");
		map.put("Iowa", "IA");
		map.put("Kansas", "KS");
		map.put("Kentucky", "KY");
		map.put("Louisiana", "LA");
		map.put("Maine", "ME");
		map.put("Maryland", "MD");
		map.put("Massachusetts", "MA");
		map.put("Michigan", "MI");
		map.put("Minnesota", "MN");
		map.put("Mississippi", "MS");
		map.put("Missouri", "MO");
		map.put("Montana", "MT");
		map.put("Nebraska", "NE");
		map.put("Nevada", "NV");
		map.put("New Hampshire", "NH");
		map.put("New Jersey", "NJ");
		map.put("New Mexico", "NM");
		map.put("New York", "NY");
		map.put("North Carolina", "NC");
		map.put("North Dakota", "ND");
		map.put("Ohio", "OH");
		map.put("Oklahoma", "OK");
		map.put("Oregon", "OR");
		map.put("Pennsylvania", "PA");
		map.put("Rhode Island", "RI");
		map.put("South Carolina", "SC");
		map.put("South Dakota", "SD");
		map.put("Tennessee", "TN");
		map.put("Texas", "TX");
		map.put("Utah", "UT");
		map.put("Vermont", "VT");
		map.put("Virginia", "VA");
		map.put("Washington", "WA");
		map.put("West Virginia", "WV");
		map.put("Wisconsin", "WI");
		map.put("Wyoming", "WY");
		map.put("District Of Columbia", "DC");
		map.put("British Columbia", "BC");
		map.put("Manitoba", "MB");
	}
}
