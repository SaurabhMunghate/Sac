/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

public class JsonSchema implements Comparable<JsonSchema> {

	public String key;
	public String address;
	public String house_number = "";
	public String prefix_direction = "";
	public String prefix_qualifier = "";
	public String prefix_type = "";
	public String street_name = "";
	public String suffix_type = "";
	public String suffix_direction = "";
	public String city = "";
	public String zip = "";
	public String state = "";
	public String fipsCode = "";
	public String errorCode = "";
	public String datasource = "";
	public String message = "";
	public float score;

	@Override
	public int compareTo(JsonSchema objectAdStruct) {
		Double hitsc = Double.parseDouble(this.score + "");
		Double destsc = Double.parseDouble(objectAdStruct.score + "");
		return destsc.compareTo(hitsc);

	}
}
