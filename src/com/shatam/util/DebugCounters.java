/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.HashMap;
import java.util.Map.Entry;

public class DebugCounters {
	private HashMap<String, Integer> map = new HashMap<String, Integer>();

	public void increment(String k) {
		Integer v = map.get(k);
		if (v == null)
			v = new Integer(0);
		map.put(k, v.intValue() + 1);
	}

	public void disp() {
		for (Entry<String, Integer> pair : map.entrySet()) {
			U.log(pair.getKey() + " = " + pair.getValue());
		}
	}

}
