/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

class InputJsonSchema implements Comparable<InputJsonSchema> {

	String address1;
	String address2;
	String city;
	String state;
	String zip;
	String key;

	@Override
	public int compareTo(InputJsonSchema input) {

		return this.state.compareTo(input.state);

	}

}
