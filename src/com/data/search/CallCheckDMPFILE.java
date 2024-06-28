/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.data.search;

import java.util.Iterator;

import com.shatam.util.U;

public class CallCheckDMPFILE {

	public static void main(String[] args) throws Exception {

		int matched = 0, cantTell = 0;
		Iterator iter = U.STATE_MAP.entrySet().iterator();
		U.log(matched + ":::::::::Matched +++::::::::::Unmatched +++:::::"
				+ cantTell);
		CheckDMPFile obj = new CheckDMPFile();
		int arr[] = obj.main();
		U.log("Both Correct:-" + arr[0] + ":Can't Tell:" + arr[1]
				+ "::CASS BETTER::" + arr[2] + "::::SHATAM BETTER:::::"
				+ arr[3] + ":::APPROX MATCHED::::" + arr[4]);
		obj = null;
		System.gc();
	}

}
