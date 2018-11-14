/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import com.shatam.io.ShatamIndexUtil;
import com.shatam.util.BoostAddress;

public class CustomAddressCorrector {

	public MultiMap corrUsingAppropriateIndex(MultiMap multimap,
			String maxresult, String hitscore, String noOfJobs,
			String dataSource, boolean flag, int distanceCriteria,boolean deepSearchEnable, BoostAddress boostAddress) throws Exception {
		@SuppressWarnings("deprecation")
		MultiMap returnoutput = new MultiHashMap();
		//long s = System.currentTimeMillis();
		ShatamIndexUtil shatamIndexUtil = new ShatamIndexUtil();
		returnoutput = shatamIndexUtil.correctAddresses(multimap, null, null,
				maxresult, hitscore, noOfJobs, dataSource, flag, distanceCriteria,deepSearchEnable, boostAddress);
		//long e = System.currentTimeMillis();
		return returnoutput;

	}
}
