/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.HashSet;

public class TLIDModel {
	public static final String DELIMETER_1 = " == ";
	public static final String DELIMETER_2 = " -- ";
	private String zip;
	private String city;
	public StringBuffer addressKey = null;

	private HashSet<String> fromToRanges = new HashSet<String>();

	public String getZip() {
		return zip;
	}

	public String getCity() {
		return city;
	}

	public StringBuffer combineFromToRanges() {
		StringBuffer buf = new StringBuffer();
		for (String s : fromToRanges) {
			if (buf.length() > 0)
				buf.append(DELIMETER_1);
			buf.append(s);
		}

		return buf;
	}

	public TLIDModel(String city, String zip) {
		this.city = city;
		this.zip = zip;
	}

	public void addFromTo(String from, String to) {
		from = from.toLowerCase().trim();
		to = to.toLowerCase().trim();
		fromToRanges.add(from + DELIMETER_2 + to);
	}
}
