/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.io;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.standard.StandardAnalyzer;
import com.shatam.shatamindex.util.Version;
import com.shatam.util.ShatamCachingList;
import com.shatam.util.ShatamCachingSingle;
import com.shatam.util.Paths;
import com.shatam.util.ShatamIndexQueryCreator;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class ShatamIndexUtil {

	int counterMisingAddresses = 0;

	public static void main(String args[]) {

	}

	public static int NORMAL = 0, METAPHONE = 0, SOUNDEX = 0,
			DOUBLE_METAPHONE = 0, REFINED_SOUNDEX = 0, Tiger = 0;
	public static AddressStruct adStructBackUp = null;

	public static boolean isDebug = false;
	public static HashMap<String, ShatamIndexReader> readerMap = new HashMap<>();
	public static HashMap<String, ShatamIndexWriter> writerMap;

	//public static int maxresult;

	public ShatamIndexWriter getWriter(final String fips,
			final String dataSource) throws Exception {
		writerMap = new HashMap<String, ShatamIndexWriter>();

		String state = fips.substring(0, 2);
		ShatamIndexWriter writer = writerMap.get(state);

		if (writer == null) {
			writer = new ShatamIndexWriter(state, dataSource);
			writerMap.put(state, writer);
		}
		return writer;
	}

	public static void closeWriters() throws Exception {
		for (ShatamIndexWriter writer : writerMap.values()) {
			writer.close();
		}
	}

	public MultiMap output;
	static long ss, ee;
	static long ss1, ee1;
	static ArrayList<Long> queryTime = new ArrayList<Long>();
	static ArrayList<Long> searcherTime = new ArrayList<Long>();
    static Object lock = new Object();
	public MultiMap correctAddresses(MultiMap multimap1,
			final AbstractIndexType indexType1, final String dataSource1,
			String maxresult, String hitscore, String noOfJobs,
			String dataOfType, boolean flag) throws Exception {
		long start = System.currentTimeMillis();
		output = new MultiHashMap();
		NORMAL = 0;
		METAPHONE = 0;
		SOUNDEX = 0;
		DOUBLE_METAPHONE = 0;
		REFINED_SOUNDEX = 0;
		Tiger = 0;
		start = System.currentTimeMillis();

		if (ShatamCachingSingle.size() > 4000) {
			U.log(ShatamCachingSingle.size());
			ShatamCachingSingle.cleanup();
		}
		if (ShatamCachingList.size() > 4000) {
			U.log(ShatamCachingList.size());
			ShatamCachingList.cleanup();
		}

		String lastDataSource = null;
		String k1DataSource = null;
		String[] arrayOfSource = new String[2];
		;
		if (dataOfType.contains("USPS and TIGER")) {
			arrayOfSource = new String[2];
			arrayOfSource[0] = "USPS";
			arrayOfSource[1] = "TIGER";
			lastDataSource = "TIGER";
			k1DataSource = "USPS";

		} else {
			if (dataOfType.contains("USPS")) {
				arrayOfSource = new String[1];

				arrayOfSource[0] = "USPS";
				lastDataSource = "USPS";
				k1DataSource = "USPS";
			} else {
				if (dataOfType.contains("TIGER")) {
					arrayOfSource = new String[1];

					arrayOfSource[0] = "TIGER";
					lastDataSource = "TIGER";
					k1DataSource = "TIGER";
				}

			}
		}
		ShatamIndexReader.mapOfAddresses = null;
		ShatamIndexReader.mapOfAddresses = new HashMap<>();
		ShatamIndexReader.mapaddressesWithoutZipTest = new HashMap<>();
		String[] queryType = { "normal" };

		for (String quryType : queryType) {

			for (AbstractIndexType it : AbstractIndexType.TYPES) {

				for (final String dataSource : arrayOfSource) {

					final MultiMap nonmatchedAddresses = new MultiHashMap();
					final String source = lastDataSource;
					final AbstractIndexType indexType = it;

					final MultiMap multimap = multimap1;

					//ShatamIndexUtil.maxresult = Integer.parseInt(maxresult);
					ArrayList<AddressStruct> resultAddsDisplay = new ArrayList<AddressStruct>();

					ss = System.currentTimeMillis();
					ShatamIndexQueryCreator shatamIndexQueryCreator = new ShatamIndexQueryCreator();
					final MultiMap queryMultimap = shatamIndexQueryCreator
							.createQuery(multimap, dataSource, "", "", "",
									indexType, readerMap, flag);

					ee = System.currentTimeMillis();

					queryTime.add((ee - ss));

					Set<String> keys = queryMultimap.keySet();

					int i = -1;
					final long startTime = System.currentTimeMillis();
					for (final String key : keys) {
						i++;

						List list = (List) queryMultimap.get(key);
						ShatamIndexQueryStruct shatamIndexQueryStruct = (ShatamIndexQueryStruct) list
								.get(0);
						String unitType = (String) list.get(1);

						String unitNumber = (String) list.get(2);

						String address = (String) list.get(3);

						Query query = (Query) list.get(4);

						if (query == null) {
							U.log("OMG queryyyyy=null");
						}
						if (address == null) {
							U.log("SAC couldn't parse NULL input");
						}
						String state = (String) list.get(6);
						String city = shatamIndexQueryStruct.getCity();
						String zip = shatamIndexQueryStruct.getZip();

						long s = System.currentTimeMillis();
						ArrayList<AddressStruct> resultAdds = null;

						ArrayList<AddressStruct> addresses = new ArrayList<>();
						ss1 = System.currentTimeMillis();
						String readerKey = state + indexType.getFieldName()
								+ "-" + dataSource;
						ShatamIndexReader reader = readerMap.get(readerKey);
						if (reader == null) {
							U.log("OMG reader=null");
						}
						synchronized (lock) {

							try {

								addresses = reader.searchIndex(address,
										shatamIndexQueryStruct, unitType,
										unitNumber, query, key,
										indexType.getFieldName(), source,
										dataSource, k1DataSource,Integer.parseInt(maxresult));

							} catch (Exception e1) {
								U.log("exception in read addresses==" + e1);
								e1.printStackTrace();
							}
							ee1 = System.currentTimeMillis();
							searcherTime.add((ee1 - ss1));

							if (addresses.size() == 0
									&& dataSource.contains(source)
									&& (indexType.getFieldName())
											.contains("k5")) {
								output.put(key, addresses);
								output.put(key, (String) list.get(5));
								output.put(key, list);
							}
							if (addresses.size() == Integer.parseInt(maxresult)) {
								output.put(key, addresses);
								output.put(key, (String) list.get(5));
								output.put(key, list);
								if (dataSource.contains("TIGER"))
									Tiger++;
								indexervalue(indexType);
							} else {
								List<String> list1 = (List<String>) multimap
										.get(key);
								nonmatchedAddresses.put(key, list1.get(0));
								nonmatchedAddresses.put(key, list1.get(1));
								nonmatchedAddresses.put(key, list1.get(2));
								nonmatchedAddresses.put(key, list1.get(3));
								nonmatchedAddresses.put(key, list1.get(4));
								nonmatchedAddresses.put(key, list1.get(5));
							}
							long e = System.currentTimeMillis();
						}
						long endTime = System.currentTimeMillis();
						long finalTime = (endTime - startTime);

					}
					if (nonmatchedAddresses.size() > 0) {

						multimap1 = nonmatchedAddresses;

					} else {

						return output;
					}

				}

			}
		}

		long end = System.currentTimeMillis();

		return output;

	}

	public static void indexervalue(AbstractIndexType indextype) {

		if (indextype.getFieldName().contains("k1"))
			NORMAL++;
		if (indextype.getFieldName().contains("k2"))
			METAPHONE++;
		if (indextype.getFieldName().contains("k3"))
			SOUNDEX++;
		if (indextype.getFieldName().contains("k4"))
			DOUBLE_METAPHONE++;
		if (indextype.getFieldName().contains("k5"))
			REFINED_SOUNDEX++;
	}

	public static Analyzer getAnalyzer() {
		return new StandardAnalyzer(Version.SHATAM_35);
	}

	public static File createIndexPath(final String ipState,
			final String dataSource) throws Exception {
		String state = ipState;
		if (StrUtil.containsString(state)) {
			state = U.STATE_MAP.get(state.toUpperCase());
		}
		if (StrUtil.isEmpty(state) || state.length() != 2) {
			throw new Exception("Bad state ipState:" + ipState);
		}
		if (Paths.READ_DATA == null)
			Paths.READ_DATA = Paths.DATA_ROOT;
		return new File(Paths.combine(Paths.READ_DATA, dataSource + "_Index",
				state));
	}

}
