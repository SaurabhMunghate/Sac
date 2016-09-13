/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.data.search;

import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddressStruct;
import com.shatam.util.AddressCorrector;
import com.shatam.util.U;

public class Search {

	public static void main(String[] args) throws Exception {

		String address1 = " 625 ORE STREET        ";
		String address2 = "";
		String city = "BOWMANSTOWN";
		String state = "PA";
		String zip = "18030";
		long start = System.currentTimeMillis();
		AddressStruct addStruct = AddressCorrector.corrUsingAppropriateIndex(
				address1, "", city, state, zip);
		U.log("FOUND ADDRESS:::" + addStruct.toFullAddressString());
		long end = System.currentTimeMillis();
		U.log(start - end);
		U.log("FOUND:" + addStruct.toOnlyStreet());
		U.log(" addStruct:" + AbstractIndexType.NORMAL.buildQuery(addStruct));
		U.log(" hitScore:" + addStruct.hitScore);

	}

}
