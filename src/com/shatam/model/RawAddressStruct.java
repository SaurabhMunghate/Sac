/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.model;

import com.shatam.util.StrUtil;

public class RawAddressStruct {
	public String address;
	public String house_number;
	public String prefix_direction;
	public String prefix_qualifier;
	public String prefix_type;
	public String street_name;
	public String suffix_type;
	public String suffix_direction;
	public String city;
	public String zip;
	public String state;
	public float score;
	public double longitude;
	public double latitude;
	public int _hnDistance;

	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (!StrUtil.isEmpty(street_name))
			buf.append("street_name:" + street_name);
		if (!StrUtil.isEmpty(city))
			buf.append(", city:" + city);
		if (!StrUtil.isEmpty(zip))
			buf.append(", zip:" + zip);
		if (!StrUtil.isEmpty(state))
			buf.append(", state:" + state);

		return buf.toString();
	}

	public void parseArea(String area) {
		zip = StrUtil.extractPattern(area, "(\\d{5})");
		area = area.replaceFirst("\\d{5}.*", "").trim();
		area = area.replaceAll("[\\s,\\.]+", " ");

		String[] arr = area.split(" ");
		for (int i = arr.length - 1; i >= 0; i--) {

			if (arr[i].length() == 2) {
				state = arr[i];
				arr[i] = "";
				break;
			}
		}
		city = area.replaceAll(" " + state, "");

	}

}