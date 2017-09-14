/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.queryParser.QueryParser;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.ScoreDoc;
import com.shatam.shatamindex.search.TopDocs;
import com.shatam.shatamindex.store.FSDirectory;
import com.shatam.shatamindex.util.Version;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.ShatamCachingList;
import com.shatam.util.ShatamCachingSingle;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class ShatamIndexReader {
	private static int MAX_HITS = 11;

	public IndexSearcher searcher;
	public QueryParser parser;
	public AbstractIndexType indexType;
	private String state = null;

	public ShatamIndexReader(AbstractIndexType indexType, String state,
			String dataSource, boolean flag) throws Exception {

		if (flag == false) {

			File shatamIndexPath = ShatamIndexUtil.createIndexPath(state,
					dataSource);

			IndexReader ir = IndexReader.open(
					FSDirectory.open(shatamIndexPath), true);
			if (ir == null) {
				U.log("It seems like Data directory Missing");
			}

			searcher = new IndexSearcher(ir);
			parser = new QueryParser(Version.SHATAM_35,
					indexType.getFieldName(), ShatamIndexUtil.getAnalyzer());

			this.indexType = indexType;
			this.state = state;
			flag = true;
		}

	}

	static HashMap<String, ArrayList<AddressStruct>> mapOfAddresses = new HashMap<>();
	public static ArrayList<AddressStruct> addressesWithoutZipTest = new ArrayList<AddressStruct>();
	public static HashMap<String, ArrayList<AddressStruct>> mapaddressesWithoutZipTest = new HashMap<>();

	public ArrayList<AddressStruct> searchIndex(String address,
			ShatamIndexQueryStruct shatamIndexQueryStruct,
			String unitTypeFromInputAddress, String unitNumber, Query query,
			String key, String indextype, String finalsource,
			String dataSource, String k1dataSource,int maxResult)

	throws Exception {

		ArrayList<AddressStruct> addresses = new ArrayList<AddressStruct>();
		ArrayList<AddressStruct> firstApperanceaddresses = new ArrayList<AddressStruct>();
		ArrayList<AddressStruct> addressesk1 = new ArrayList<AddressStruct>();
		HashMap<String, AddressStruct> exists = new HashMap<String, AddressStruct>();

		if (shatamIndexQueryStruct == null) {
			U.log("OMG shatamIndexQueryStruct ==null");
			return addresses;

		}
		if (query == null) {
			U.log("OMG qury ==null");
			return addresses;

		}
		if (indextype == "k1") {
			ShatamCachingSingle.k1_reference = query.toString()+state;
		}
		if (maxResult == 1) {
			ShatamCachingSingle.newBuilder();
			if (ShatamCachingSingle.size() > 0) {
				AddressStruct newStruct = new AddressStruct(state);
				AddressStruct oldStruct = ShatamCachingSingle.get(query
						.toString()+state);
				if (oldStruct != null) {
					if (oldStruct.inputAddress != null) {
						if (oldStruct.inputAddress.equals("No Match Found")) {
							newStruct.inputAddress = oldStruct.inputAddress;
							addresses.add(newStruct);
							return addresses;
						}
					}

					for (AddColumns col : AddColumns.values()) {
						newStruct.put(col, oldStruct.get(col));
					}
					newStruct.setHouseNumber(shatamIndexQueryStruct
							.getHouseNumber());
					newStruct.inputAddress = address;
					newStruct.setQueryStruct(shatamIndexQueryStruct);
					newStruct.unitTypeFromInputAddress = unitTypeFromInputAddress;
					newStruct.unitNumber = unitNumber;
					newStruct._hnDistance = 0;
					if (StrUtil.isEmpty(address)) {
						newStruct.hitScore = 0;
					} else {
						newStruct.hitScore = oldStruct.hitScore;
					}
					addresses.add(newStruct);
					if (addresses.size() > 0)
						return addresses;

				}
			}
		}

		if (maxResult > 1) {
			ShatamCachingList.newBuilder();

			List<AddressStruct> oldlist = ShatamCachingList.get(query
					.toString()+state);
			if (oldlist != null && oldlist.size() == maxResult) {		
				ArrayList<AddressStruct> newList = new ArrayList<>();
				for (AddressStruct old : oldlist) {

					AddressStruct newAddStruct = new AddressStruct(state);

					for (AddColumns col : AddColumns.values()) {
						newAddStruct.put(col, old.get(col));

					}
					newAddStruct.setHouseNumber(shatamIndexQueryStruct
							.getHouseNumber());
					newAddStruct.inputAddress = address;
					newAddStruct.setQueryStruct(shatamIndexQueryStruct);
					newAddStruct.unitTypeFromInputAddress = unitTypeFromInputAddress;
					newAddStruct.unitNumber = unitNumber;
					newAddStruct._hnDistance = 0;

					if (StrUtil.isEmpty(address)) {
						newAddStruct.hitScore = 0;
					} else {
						newAddStruct.hitScore = old.hitScore;
					}
					newList.add(newAddStruct);					
				}

				return newList;
			}
		}

		if (indextype != "k1")
			MAX_HITS = 3;

		TopDocs results = searcher.search(query, MAX_HITS);
		ScoreDoc[] hits = results.scoreDocs;
		boolean flag = false;
		int i = 0;

		for (String caseV : new String[] { "contains", "approxMatching",
				"defaults" }) {

			if (indextype != "k1") {
				if (caseV.contains("contains")
						|| caseV.contains("approxMatching")) {
					continue;
				}
			}
			for (ScoreDoc hit : hits) {
				i++;
				Document doc = searcher.doc(hit.doc);
				AddressStruct addStruct = new AddressStruct(state);
				addStruct.inputAddress = address;
				addStruct.setQueryStruct(shatamIndexQueryStruct);
				if (StrUtil.isEmpty(address)) {
					addStruct.hitScore = 0;
				} else {
					addStruct.hitScore = hit.score;
				}
				addStruct.setHouseNumber(shatamIndexQueryStruct
						.getHouseNumber());
				for (AddColumns col : AddColumns.values()) {
					addStruct.put(col, doc.get(col.name()));

				}

				String addStructKey = AbstractIndexType.NORMAL
						.buildQuery(addStruct);
				{
					addStruct.unitTypeFromInputAddress = unitTypeFromInputAddress;
					addStruct.unitNumber = unitNumber;
					addStruct._hnDistance = 0;
					DistanceMatchForResult matcher = new DistanceMatchForResult(
							addStruct, indexType);
					if (maxResult == 1) {
						{
							if (matcher.isResultMatched(caseV, key)) {
								ShatamCachingSingle.put(query.toString()+state,
										addStruct);
								addresses.add(addStruct);

							} else {
								if (finalsource.contains(dataSource)
										&& indextype.contains("k5")
										&& addresses.size() == 0) {
									if (mapaddressesWithoutZipTest.get(key) != null)
										addresses = mapaddressesWithoutZipTest
												.get(key);
								}
							}

						}
					}

					else {
						if (indexType.getFieldName().contains("k1")
								&& dataSource.contains(k1dataSource)) {
							if (addressesk1.size() != maxResult)
								addressesk1.add(addStruct);
							if (addressesk1.size() == maxResult)
								mapOfAddresses.put(key, addressesk1);
						}

						if (!indexType.getFieldName().contains("k1")
								&& dataSource.contains(k1dataSource)
								&& mapOfAddresses.get(key) == null) {
							if (addressesk1.size() != maxResult)
								addressesk1.add(addStruct);
							if (addressesk1.size() == maxResult)
								mapOfAddresses.put(key, addressesk1);
						}

						firstApperanceaddresses.add(addStruct);
						if (flag == false) {
							if (matcher.isResultMatched(caseV, key)) {
								addresses.add(addStruct);
								if (i == MAX_HITS) {
									addresses.add(firstApperanceaddresses
											.get(0));
									if (addresses.size() != maxResult)
										addresses.add(firstApperanceaddresses
												.get(1));
								}
								flag = true;
							}
						} else {
							addresses.add(addStruct);
						}
					}
					exists.put(addStructKey, addStruct);
				}
				if (addresses.size() == maxResult) {
					break;
				}
			}
			if (maxResult > 1
					&& addresses.size() == maxResult) {
				ShatamCachingList.put(query.toString()+state, addresses);
			}
			if (addresses.size() == maxResult) {
				break;
			}
		}
		if (finalsource.contains(dataSource) && indextype.contains("k5")
				&& addresses.size() == 0 && maxResult > 1) {
			addresses = mapOfAddresses.get(key);
			ShatamCachingList.put(query.toString()+state, addresses);
		}

		if (addresses == null) {
			addresses = new ArrayList<>();
		}

		return addresses;

	}

	public void close() throws IOException {
		searcher.close();

	}

}
