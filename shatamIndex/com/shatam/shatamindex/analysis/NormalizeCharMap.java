/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.util.HashMap;
import java.util.Map;

public class NormalizeCharMap {

	Map<Character, NormalizeCharMap> submap;
	String normStr;
	int diff;

	public void add(String singleMatch, String replacement) {
		NormalizeCharMap currMap = this;
		for (int i = 0; i < singleMatch.length(); i++) {
			char c = singleMatch.charAt(i);
			if (currMap.submap == null) {
				currMap.submap = new HashMap<Character, NormalizeCharMap>(1);
			}
			NormalizeCharMap map = currMap.submap.get(Character.valueOf(c));
			if (map == null) {
				map = new NormalizeCharMap();
				currMap.submap.put(Character.valueOf(c), map);
			}
			currMap = map;
		}
		if (currMap.normStr != null) {
			throw new RuntimeException(
					"MappingCharFilter: there is already a mapping for "
							+ singleMatch);
		}
		currMap.normStr = replacement;
		currMap.diff = singleMatch.length() - replacement.length();
	}
}
