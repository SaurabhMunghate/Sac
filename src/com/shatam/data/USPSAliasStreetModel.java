/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.data;

import java.util.Collection;
import java.util.HashMap;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;

public class USPSAliasStreetModel {
	private String realStreetKey;
	private String aliasStreetKey;
	private static HashMap<String, HashMap<String, USPSAliasStreetModel>> realStreetToAliasesMap = new HashMap<String, HashMap<String, USPSAliasStreetModel>>();

	public static Collection<USPSAliasStreetModel> getAliases(
			USPSStreetsModel realStreetModel) throws Exception {
		if (realStreetToAliasesMap.size() == 0) {
			_readAndStore();
		}

		HashMap<String, USPSAliasStreetModel> map = realStreetToAliasesMap
				.get(realStreetModel.getKey());
		if (map == null)
			map = new HashMap<String, USPSAliasStreetModel>();
		return map.values();
	}

	private static void _readAndStore() throws Exception {
		USPSUtil.readFile(USPSUtil.CITY_STATE_FILE_NAME, 'A', 129,
				new USPSUtil._USPSFileCallback() {

					@Override
					public void callback(byte[] data) throws Exception {

						USPSAliasStreetModel rec = new USPSAliasStreetModel(
								data);

						HashMap<String, USPSAliasStreetModel> aliasesMap = realStreetToAliasesMap
								.get(rec.realStreetKey);
						if (aliasesMap == null) {
							aliasesMap = new HashMap<String, USPSAliasStreetModel>();
							realStreetToAliasesMap.put(rec.realStreetKey,
									aliasesMap);
						}
						aliasesMap.put(rec.aliasStreetKey, rec);
					}
				});

	}

	public USPSAliasStreetModel(byte[] data) {
		zip = new String(data, 1, 5).trim().toUpperCase();

		aliasPreDirAbbr = new String(data, 6, 2).trim().toUpperCase();
		aliasStreetName = new String(data, 8, 28).trim().toUpperCase();
		aliasStreetSuffixAbbr = new String(data, 36, 4).trim().toUpperCase();
		aliasStreetPostDirAbbr = new String(data, 40, 2).trim().toUpperCase();

		realPreDirAbbr = new String(data, 42, 2).trim().toUpperCase();
		realStreetName = new String(data, 44, 28).trim().toUpperCase();
		realStreetSuffixAbbr = new String(data, 72, 4).trim().toUpperCase();
		realStreetPostDirAbbr = new String(data, 76, 2).trim().toUpperCase();

		aliasTypeCode = new String(data, 78, 1).trim().toUpperCase();

		{
			StringBuffer writtenKey = new StringBuffer();
			writtenKey.append(realPreDirAbbr).append("-");
			writtenKey.append(realStreetName).append("-");
			writtenKey.append(realStreetSuffixAbbr).append("-");
			writtenKey.append(realStreetPostDirAbbr).append("-");
			writtenKey.append(zip).append("-");
			realStreetKey = writtenKey.toString().trim().toUpperCase();

		}

		{
			StringBuffer writtenKey = new StringBuffer();
			writtenKey.append(aliasPreDirAbbr).append("-");
			writtenKey.append(aliasStreetName).append("-");
			writtenKey.append(aliasStreetSuffixAbbr).append("-");
			writtenKey.append(aliasStreetPostDirAbbr).append("-");
			writtenKey.append(zip).append("-");
			aliasStreetKey = writtenKey.toString().trim().toUpperCase();
		}

	}

	String zip;
	String aliasPreDirAbbr;
	String aliasStreetName;
	String aliasStreetPostDirAbbr;
	String aliasStreetSuffixAbbr;

	String realPreDirAbbr;
	String realStreetName;
	String realStreetPostDirAbbr;
	String realStreetSuffixAbbr;

	private String aliasTypeCode;

	public AddressStruct createAddressStruct(String state, String city)
			throws Exception {
		AddressStruct s = new AddressStruct(state);
		s.put(AddColumns.PREDIRABRV, aliasPreDirAbbr);
		s.put(AddColumns.PREQUALABR, "");
		s.put(AddColumns.PRETYPABRV, "");
		s.put(AddColumns.NAME, aliasStreetName);
		s.put(AddColumns.SUFTYPABRV, aliasStreetSuffixAbbr);
		s.put(AddColumns.SUFDIRABRV, aliasStreetPostDirAbbr);
		s.put(AddColumns.SUFQUALABR, "");
		s.put(AddColumns.CITY, city);
		s.put(AddColumns.ZIP, zip);

		return s;

	}

}