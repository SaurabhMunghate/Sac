/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.data;

public class USPSCityModel {

	public String zip;
	public String cityStateKey;
	public String zipClassificationCode;
	public String cityStateName;
	public String cityStateNameAbbr;

	public USPSCityModel(byte[] data) {

		zip = new String(data, 1, 5).trim().toUpperCase();
		cityStateKey = new String(data, 1, 6).trim().toUpperCase();
		zipClassificationCode = new String(data, 12, 1).trim().toUpperCase();
		cityStateName = new String(data, 13, 28).trim().toUpperCase();
		cityStateNameAbbr = new String(data, 41, 13).trim().toUpperCase();
	}

}
