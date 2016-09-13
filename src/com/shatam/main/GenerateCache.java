/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.util.U;
import com.shatam.util.Util;

public class GenerateCache {
	public static void main(String args[]) throws Exception {

		doCache();
	}

	public static HashMap<String, String> FIPS = new HashMap<>();

	public static HashMap<String, HashSet<String>> cities = Util.getCity();

	public static HashMap<String, String> getfips() throws Exception {
		HashMap<String, String> FIPS = new HashMap<>();
		cities = Util.getCity();
		CsvListReader csvReader = new CsvListReader(new FileReader("FIPS.csv"),
				CsvPreference.STANDARD_PREFERENCE);
		List<String> caRow = null;
		while ((caRow = csvReader.read()) != null) {

			FIPS.put(caRow.get(0).trim(), caRow.get(5).trim());

		}
		return FIPS;
	}

	public static void doCache() throws Exception {

		FIPS = getfips();
		Iterator iterator = U.STATE_MAP.keySet().iterator();
		while (iterator.hasNext()) {
			String state = (String) iterator.next();
			for (AbstractIndexType it : AbstractIndexType.TYPES) {
				for (final String dataSource : new String[] { U.USPS, U.TIGER }) {
					String readerKey = state + it.getFieldName() + "-"
							+ dataSource;
					ShatamIndexReader reader = new ShatamIndexReader(it, state,
							dataSource, false);
					ShatamIndexUtil.readerMap.put(readerKey, reader);
				}
			}

		}

	}

}
