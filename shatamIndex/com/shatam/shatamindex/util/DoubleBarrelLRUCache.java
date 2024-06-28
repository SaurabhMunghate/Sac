/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

final public class DoubleBarrelLRUCache<K extends DoubleBarrelLRUCache.CloneableKey, V> {

	public static abstract class CloneableKey {
		@Override
		abstract public Object clone();
	}

	private final Map<K, V> cache1;
	private final Map<K, V> cache2;
	private final AtomicInteger countdown;
	private volatile boolean swapped;
	private final int maxSize;

	public DoubleBarrelLRUCache(int maxSize) {
		this.maxSize = maxSize;
		countdown = new AtomicInteger(maxSize);
		cache1 = new ConcurrentHashMap<K, V>();
		cache2 = new ConcurrentHashMap<K, V>();
	}

	@SuppressWarnings("unchecked")
	public V get(K key) {
		final Map<K, V> primary;
		final Map<K, V> secondary;
		if (swapped) {
			primary = cache2;
			secondary = cache1;
		} else {
			primary = cache1;
			secondary = cache2;
		}

		V result = primary.get(key);
		if (result == null) {

			result = secondary.get(key);
			if (result != null) {

				put((K) key.clone(), result);
			}
		}
		return result;
	}

	public void put(K key, V value) {
		final Map<K, V> primary;
		final Map<K, V> secondary;
		if (swapped) {
			primary = cache2;
			secondary = cache1;
		} else {
			primary = cache1;
			secondary = cache2;
		}
		primary.put(key, value);

		if (countdown.decrementAndGet() == 0) {

			secondary.clear();

			swapped = !swapped;

			countdown.set(maxSize);
		}
	}
}
