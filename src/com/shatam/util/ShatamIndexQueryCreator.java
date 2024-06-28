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
import java.util.Set;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.shatamindex.search.Query;

public class ShatamIndexQueryCreator {

	public static String createAddressQuery(String add, String state,
			AbstractIndexType indexType) throws Exception {

		StringBuffer buf = null;
		String[] arr = add.split("[^\\d\\w\\-/]");

		for (String part : arr) {
			if (StrUtil.isEmpty(part))
				continue;

			String v = AbbrReplacement.getFullName(part, state);
			if (v.matches("[\\-_]")) {
				continue;
			}

			if (StrUtil.isEmpty(v)) {
				v = part;
			}

			v = indexType.encode(v);

			buf.append(" ").append(v);

			boolean potentialStreetName = v.equals(part)
					|| v.equals(AbstractIndexType.standardize(part));
			if (potentialStreetName) {
				if (StrUtil.isNum(buf.toString().trim())) {
					int iBuf = Integer.parseInt(buf.toString().trim());
					buf.append(U._getNumSuf(iBuf));
				}
				buf.append(U.STREET_ENHANCE);
			}

		}

		String str = buf.toString().trim();
		return str;

	}

	public static ShatamIndexQueryStruct addressQuery(String add, String city,
			String zip, AbstractIndexType indexType, String state)
			throws Exception {

		ShatamIndexQueryStruct shatamIndexQueryStruct = new ShatamIndexQueryStruct(
				add, city, zip, state, indexType);

		if (StrUtil.isEmpty(add)) {
			return shatamIndexQueryStruct;
		}
		StringBuffer buf = new StringBuffer();

		add = add.trim().toLowerCase();

		{
			String[] hnArr = extractHN(add);

			if (hnArr != null) {
				shatamIndexQueryStruct.setHouseNumber(hnArr[0]);
				add = hnArr[1];
			}
		}

		String[] arr = add.split("[^\\d\\w\\-/]");

		for (String part : arr) {
			if (StrUtil.isEmpty(part))
				continue;

			String v = AbbrReplacement.getFullName(part, state);
			if (v.matches("[\\-_]")) {
				continue;
			}

			if (StrUtil.isEmpty(v)) {
				v = part;
			}

			v = indexType.encode(v);

			buf.append(" ").append(v);

			if (Util.match(part, "\\d+(th|nd|st|rd)") != null) {
				buf.append(" " + part.replaceAll("th|nd|st|rd", ""));
			}

			boolean potentialStreetName = v.equals(part)
					|| v.equals(AbstractIndexType.standardize(part));

			try {
				if (potentialStreetName) {
					if (StrUtil.isNum(v.trim())) {

						int iBuf = Integer.parseInt(v.trim());

						if (((int) Math.log10(iBuf) + 1) < 4
								&& Util.match(add, "rr\\s*\\d+") == null
								&& Util.match(add, "county road \\d{3}") == null)
							buf.append(U._getNumSuf(iBuf));

						if (Util.match(buf.toString(), "\\d+(th|nd|st|rd)") != null)
							buf.append(" " + v.trim());

					}
					buf.append(U.STREET_ENHANCE);
				}
			} catch (Exception e) {

			}

		}

		String str = buf.toString().trim();

		shatamIndexQueryStruct.setNormalizedStreetName(str.replace(
				U.STREET_ENHANCE, ""));

		str = str.replace("OR ", "");

		shatamIndexQueryStruct.setQuery(str);

		return shatamIndexQueryStruct;

	}

	private static ArrayList<String> HN_MAP = new ArrayList<String>();
	static {

		HN_MAP.add("(\\d+\\s*[&-]+\\s*\\d+ )");

		HN_MAP.add("^(\\d+/\\d+) ");

		HN_MAP.add("^(\\d+ and \\d+) ");

		HN_MAP.add("^(\\d+ \\& \\d+) ");

		HN_MAP.add("^(\\d+\\-\\d+-\\d+) ");

		HN_MAP.add("^(\\d+ \\d/\\d)");
		HN_MAP.add("^(\\d+\\-\\d+) ");

		HN_MAP.add("^(\\d+\\-\\w)");

		HN_MAP.add("^([e|w|n|s]\\s*\\d+\\w+) ");

		HN_MAP.add("^[e|w|n|s] (\\w+\\d+) ");
		HN_MAP.add("^(\\d+\\w+) ");
		HN_MAP.add("^(\\w+\\d+) ");

		HN_MAP.add("^(\\d+) ");

		HN_MAP.add("^lot (\\d+) ");
		HN_MAP.add("^tl (\\d+),");

		HN_MAP.add(" (\\d+) [\\w]+ [\\w]+");

		HN_MAP.add("^(\\d+)[^A-Za-z0-9] ");
	}

	public static String[] extractHN(String add) {
		String[] res = new String[2];
		add = add.trim().toLowerCase();

		if (add.matches("^rr *\\d+.*")) {

			return new String[] { "", add };
		}

		for (String reg : HN_MAP) {
			String hn = StrUtil.extractPattern(add, reg, 1);

			if (!StrUtil.isEmpty(hn)) {
				res[0] = hn.trim();
				res[1] = add.replaceFirst(hn, "").replaceAll("\\s+", " ")
						.trim();
				return res;
			}
		}
		return null;
	}

	public static MultiMap createQuery(final MultiMap multimap,
			final String dataSource, final String city1, final String zip1,
			final String state1, final AbstractIndexType indexType,
			final HashMap<String, ShatamIndexReader> readerMap,
			final boolean flag, BoostAddress boostAddress) throws Exception {
		MultiMap queryMultimap = new MultiHashMap();
		Query query = null;
		Set<String> keys = multimap.keySet();
		for (String key : keys) {
			List<String> list = (List<String>) multimap.get(key);
			String address1 = list.get(0);
			String address2 = list.get(1);
			String city = list.get(2);
			String state = list.get(3);
			String zip = list.get(4);
			String addkey = list.get(5);

			if (StrUtil.isEmpty(state) || state.length() != 2
					|| !U.STATE_MAP.containsKey(state.toUpperCase())) {
				try {
					throw new Exception("Invalid input: State:" + state
							+ " address1:" + address1 + " city:" + city
							+ " zip:" + zip);

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			long s = System.currentTimeMillis();
			String readerKey = state + indexType.getFieldName() + "-"+ dataSource;

			ShatamIndexReader reader = ShatamIndexUtil.readerMap.get(readerKey);
			if (reader == null) {
				try {
					reader = new ShatamIndexReader(indexType, state,
							dataSource, flag);
				} catch (Exception e1) {

					e1.printStackTrace();
				}
				ShatamIndexUtil.readerMap.put(readerKey, reader);
			}
			long e = System.currentTimeMillis();
			s = System.currentTimeMillis();
			String address = null;
			try {
				address = U.getSearchableAddress(address1, address2);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e = System.currentTimeMillis();
			s = System.currentTimeMillis();
			String[] unitArr = StrUtil.extractApartment(address1, address2);
			e = System.currentTimeMillis();
			int j = 0;
			ShatamIndexQueryStruct shatamIndexQueryStruct = new ShatamIndexQueryStruct();
			try {
				shatamIndexQueryStruct = addressQuery(address, city, zip, indexType, state);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			StringBuffer buf = new StringBuffer(
					shatamIndexQueryStruct.getQuery() == null ? ""
							: shatamIndexQueryStruct.getQuery());

			if (!StrUtil.isEmpty(city)) {
				city = city.replaceAll("[\\s,\\.\\-]+", " ").trim();
				city = city.trim().toLowerCase();
				if (city.matches("^(\\d+) .{4,}")) {

				} else {
					city = city.replaceAll(StrUtil.WORD_DELIMETER, "");
				}

				try {
					city = indexType.encode(city);
				} catch (Exception e1) {

					e1.printStackTrace();
				}
				shatamIndexQueryStruct.setNormalizedCity(city);

				//buf.append(" ").append(city + "_CITY" + U.CITY_ENHANCE);
				buf.append(" ").append(city + "_CITY" + boostAddress.getCityWeight());
			}
			if (!StrUtil.isEmpty(zip)) {
				zip = zip.trim();
				zip = zip.replaceAll("[^\\d]", "");
				if (!StrUtil.isEmpty(zip)) {
					if (zip.length() > 5)
						zip = zip.substring(0, 5);
					else if (zip.length() < 5)
						zip = String.format("%05d", Integer.parseInt(zip));

					shatamIndexQueryStruct.setNormalizedZip(zip);
					//buf.append(" ").append(zip + "_ZIP" + U.ZIP_ENHANCE);
					buf.append(" ").append(zip + "_ZIP" + boostAddress.getZipWeight());
				}
			}

			synchronized (reader) {
				try {
					shatamIndexQueryStruct.setQuery(buf.toString().trim());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				try {
					query = shatamIndexQueryStruct
							.createQueryObj(reader.parser);
					if (query == null) {
						U.log("OMG queryyyyy111=null");
					}
					if (reader == null) {
						U.log("OMG queryyyyyREADER111=null");
					}

				} catch (Exception e1) {
					e1.printStackTrace();
				}
				

				queryMultimap.put(key, shatamIndexQueryStruct);
				queryMultimap.put(key, unitArr[0]);
				queryMultimap.put(key, unitArr[1]);
				queryMultimap.put(key, address);
				queryMultimap.put(key, query);
				queryMultimap.put(key, addkey);
				queryMultimap.put(key, state);
			}
		}//eof for 
		
U.log("queryMultimapqueryMultimap"+queryMultimap);
		ShatamIndexUtil.readerMap = readerMap;
		return queryMultimap;

	}

}
