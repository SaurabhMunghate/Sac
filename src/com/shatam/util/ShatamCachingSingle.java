/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import com.shatam.model.AddressStruct;

public class ShatamCachingSingle {

	static HashMap<String, AddressStruct> addressMap = null;

	public static void newBuilder() {

		if (addressMap == null) {
			addressMap = new HashMap<>();
		}
	}

	public static void cleanup() {
		addressMap = null;
	}

	public static void put(String key, AddressStruct addressStruct) {
		addressMap.put(key, addressStruct);
	}

	public static long size() {
		if (addressMap == null) {
			return -1;
		}
		return addressMap.size();
	}

	public static String k1_reference = null;

	public static AddressStruct get(String key) throws ExecutionException {
		return addressMap.get(key);

	}

	public static void main(String[] args) throws ExecutionException,
			InterruptedException {

		long start = System.currentTimeMillis();

		long end = System.currentTimeMillis();

	}

}
