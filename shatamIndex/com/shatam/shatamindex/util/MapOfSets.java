/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class MapOfSets<K, V> {

	private final Map<K, Set<V>> theMap;

	public MapOfSets(Map<K, Set<V>> m) {
		theMap = m;
	}

	public Map<K, Set<V>> getMap() {
		return theMap;
	}

	public int put(K key, V val) {
		final Set<V> theSet;
		if (theMap.containsKey(key)) {
			theSet = theMap.get(key);
		} else {
			theSet = new HashSet<V>(23);
			theMap.put(key, theSet);
		}
		theSet.add(val);
		return theSet.size();
	}

	public int putAll(K key, Collection<? extends V> vals) {
		final Set<V> theSet;
		if (theMap.containsKey(key)) {
			theSet = theMap.get(key);
		} else {
			theSet = new HashSet<V>(23);
			theMap.put(key, theSet);
		}
		theSet.addAll(vals);
		return theSet.size();
	}

}
