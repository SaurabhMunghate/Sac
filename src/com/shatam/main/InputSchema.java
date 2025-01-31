/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

public class InputSchema implements Comparable<InputSchema> {

	public String address1;
	public String address2;
	public String city;
	public String state;
	public String zip;

	@Override
	public int compareTo(InputSchema objectInput) {

		return objectInput.state.compareTo(this.state);

	}

}
