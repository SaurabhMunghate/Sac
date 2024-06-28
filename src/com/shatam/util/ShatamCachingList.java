/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.shatam.model.AddressStruct;

public class ShatamCachingList {

	static HashMap<String, List<AddressStruct>> addressMap = null;

	public static void newBuilder() {

		if (addressMap == null) {
			addressMap = new HashMap<>();
		}
	}

	public static void cleanup() {
		addressMap = null;
	}

	public static void put(String key, List<AddressStruct> addressStruct) {
		addressMap.put(key, addressStruct);
	}

	public static long size() {
		if (addressMap == null) {
			return -1;
		}
		return addressMap.size();
	}

	public static String k1_reference = null;

	public static List<AddressStruct> get(String key) throws ExecutionException {
		return addressMap.get(key);
	}

	public static void main(String[] args) throws ExecutionException,
			InterruptedException {
		long start = System.currentTimeMillis();
		List<String> list = new ArrayList<>();
		list.add("1");
		list.add("2");
		list.add("3");
		System.out.println(list);
		List<String> list1 = new ArrayList<>();
		list1.addAll(list);
		System.out.println(list1);
		long end = System.currentTimeMillis();

	}

}
