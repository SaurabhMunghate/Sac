package com.shatam.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.language.Metaphone;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.queryParser.QueryParser;
import com.shatam.shatamindex.search.Explanation;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.ScoreDoc;
import com.shatam.shatamindex.search.TopDocs;
import com.shatam.shatamindex.search.TopScoreDocCollector;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.FSDirectory;
import com.shatam.shatamindex.store.MMapDirectory;
import com.shatam.shatamindex.store.NIOFSDirectory;
import com.shatam.shatamindex.store.RAMDirectory;
import com.shatam.shatamindex.util.Version;
import com.shatam.interpolator.Interpolate;
import com.shatam.memdb.IndexType;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.GoogleCachingList;
import com.shatam.util.GoogleCachingTest;
import com.shatam.util.OutputStatusCode;
import com.shatam.util.ShatamIndexQueryCreator;
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

			// U.log("shatamIndexReader shatamIndexPath:" + shatamIndexPath);
			IndexReader ir = IndexReader.open(
					FSDirectory.open(shatamIndexPath), true);// priviously here
																// used
																// FSDirectory
																// :-20
																// july/NIOFSDirectory:-give
																// bad effects
																// on windows
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

	// static Metaphone SNDX = new Metaphone();

	/*
	 * public ArrayList<AddressStruct> searchAddress(String address, String
	 * city, String zip, String unitType, String unitNumber) throws Exception {
	 * long s = System.currentTimeMillis();
	 * 
	 * 
	 * 
	 * ShatamIndexQueryStruct shatamIndexQueryStruct = ShatamIndexQueryCreator
	 * .createQuery(address, city, zip, state, indexType);
	 * 
	 * long e = System.currentTimeMillis();
	 * 
	 * //U.log("shatamIndexQueryStruct  time:: :::  ::"+ (e - s));
	 * 
	 * 
	 * s = System.currentTimeMillis(); ArrayList<AddressStruct> addresses =
	 * searchIndex(address, shatamIndexQueryStruct, unitType, unitNumber);
	 * 
	 * e = System.currentTimeMillis();
	 * 
	 * //U.log("searchIndex time:: :::  ::"+ (e - s));
	 * 
	 * return addresses; }// searchAddress()
	 */

	static HashMap<String, ArrayList<AddressStruct>> mapOfAddresses = new HashMap<>();
	public static ArrayList<AddressStruct> addressesWithoutZipTest = new ArrayList<AddressStruct>();
	public static HashMap<String, ArrayList<AddressStruct>> mapaddressesWithoutZipTest = new HashMap<>();

	public ArrayList<AddressStruct> searchIndex(String address,
			ShatamIndexQueryStruct shatamIndexQueryStruct,
			String unitTypeFromInputAddress, String unitNumber, Query query,
			String key, String indextype, String finalsource,
			String dataSource, String k1dataSource)

	throws Exception {

		// U.log(key+"=mapofaddresses===****"+mapOfAddresses.size());
		// U.log(finalsource+":::::"+dataSource+":::::"+indextype);
		// System.gc();
		// long s = System.currentTimeMillis();

		// U.log("Start searchIndex............" + indextype);
		// U.log("Query::  " + query);
		ArrayList<AddressStruct> addresses = new ArrayList<AddressStruct>();
		ArrayList<AddressStruct> firstApperanceaddresses = new ArrayList<AddressStruct>();
		ArrayList<AddressStruct> addressesk1 = new ArrayList<AddressStruct>();
		/*
		 * if(mapOfAddresses.get(key)!=null){
		 * U.log("mapofaddresses==="+mapOfAddresses.size());
		 * addresses=mapOfAddresses.get(key); }
		 */

		HashMap<String, AddressStruct> exists = new HashMap<String, AddressStruct>();

		// Query query = shatamIndexQueryStruct.createQueryObj(parser);
		if (shatamIndexQueryStruct == null) {
			U.log("OMG shatamIndexQueryStruct ==null");
			return addresses;

		}

		// long e = System.currentTimeMillis();

		// //U.log("createQueryObj time:: :::  ::"+ (e - s));
		if (query == null) {
			U.log("OMG qury ==null");
			return addresses;

		}
		/*
		 * if(address==null){ U.log("Address is nulll"); }
		 */

		// new code of lucene added by kirti misal

		// TopScoreDocCollector collector =
		// TopScoreDocCollector.create(MAX_HITS, true);
		// U.log("Shatam Index Reader1 :: " +
		// shatamIndexQueryStruct.getHouseNumber());
		if (indextype == "k1") {
			GoogleCachingTest.k1_reference = query.toString();
		}

		// for one result..
		if (ShatamIndexUtil.maxresult == 1) {
			GoogleCachingTest.newBuilder();
			// U.log("My Query: " + query);
			// U.log("Strcut" + shatamIndexQueryStruct.getQuery());
			if (GoogleCachingTest.size() > 0) {

				AddressStruct newStruct = new AddressStruct(state);
				// AddressStruct struct1 =
				// GoogleCachingTest.get(shatamIndexQueryStruct.getQuery());
				AddressStruct oldStruct = GoogleCachingTest.get(query
						.toString());
				if (oldStruct != null) {

					if (oldStruct.inputAddress != null) {

						if (oldStruct.inputAddress.equals("No Match Found")) {
							newStruct.inputAddress = oldStruct.inputAddress;
							addresses.add(newStruct);
							// U.log("From Catch No match: " +
							// GoogleCachingTest.size());
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
					// U.log("ShatamIndexReader First Hit: " + struct.hitScore);
					// U.log("From Catch: " + GoogleCachingTest.size());
					addresses.add(newStruct);

					if (addresses.size() > 0)
						return addresses;

				}
			}
		}

		// more than one result..
		if (ShatamIndexUtil.maxresult > 1) {
			GoogleCachingList.newBuilder();

			List<AddressStruct> oldlist = GoogleCachingList.get(query
					.toString());
			if (oldlist != null) {

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
				// U.log("From Cache..");
				return newList;
			}
		}// More than one result end

		// long s = System.currentTimeMillis();

		if (indextype != "k1")
			MAX_HITS = 3;

		TopDocs results = searcher.search(query, MAX_HITS);

		// long e = System.currentTimeMillis();

		// U.log("searcher.search time:: :::  ::"+ (e - s));
		ScoreDoc[] hits = results.scoreDocs;
		// U.log("Hits length===="+hits.length);
		// GoogleCachingTest.
		// float prevScore = -1;
		// s = System.currentTimeMillis();
		boolean flag = false;
		int i = 0;
		// for(String caseV:new
		// String[]{"defaults","approxMatching","contains"}){

		// for(String caseV:new
		// String[]{"contains","approxMatching","defaults"}){
		for (String caseV : new String[] { "contains", "approxMatching",
				"defaults" }) {
			// for(String caseV:new String[]{"contains"}){
			// U.log(caseV);
			if (indextype != "k1") { // U.log("Inside :: \t" + indextype);
				if (caseV.contains("contains")
						|| caseV.contains("approxMatching")) {
					continue;
				}
			}
			for (ScoreDoc hit : hits) {
				i++;
				// U.log(i);
				// Explanation explanation = searcher.explain(query, hit.doc);
				// System.out.println("explanation------" + caseV);
				Document doc = searcher.doc(hit.doc);

				AddressStruct addStruct = new AddressStruct(state);
				addStruct.inputAddress = address;
				addStruct.setQueryStruct(shatamIndexQueryStruct);

				// [["100660192_225992560","N FEDERAL HWY","","POMPANO BEACH","FL","33060"]]

				if (StrUtil.isEmpty(address)) {
					addStruct.hitScore = 0;
				} else {
					addStruct.hitScore = hit.score;
				}

				addStruct.setHouseNumber(shatamIndexQueryStruct
						.getHouseNumber());

				for (AddColumns col : AddColumns.values()) {
					addStruct.put(col, doc.get(col.name()));

				}// AddColumns col

				String addStructKey = AbstractIndexType.NORMAL
						.buildQuery(addStruct);

				{
					addStruct.unitTypeFromInputAddress = unitTypeFromInputAddress;
					addStruct.unitNumber = unitNumber;
					addStruct._hnDistance = 0;

					DistanceMatchForResult matcher = new DistanceMatchForResult(
							addStruct, indexType);

					// U.log("FOUND ************** Addresses:-"+addStruct.get(AddColumns.CITY).toUpperCase()+"::::"+addStruct.getFoundName());

					// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
					// U.log("Preaddress size=="+addresses.size());
					// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

					if (ShatamIndexUtil.maxresult == 1) {
						// U.log("FOUND Addresses:-"+addStruct.get(AddColumns.CITY).toUpperCase()+"::::"+addStruct.getFoundName());
						int score;
						// U.log(addStruct.hitScore);
						/*
						 * if (addStruct.hitScore >= 5){ score = 5; score = 100
						 * * (score / 5); if(score ==100){
						 * addresses.add(addStruct); } } else
						 */{

							if (matcher.isResultMatched(caseV, key)) {
								// U.log("Test Pass");
								// U.log( OutputStatusCode.getStatusCode(list,
								// addStruct));
								// addStruct.errorCode=OutputStatusCode.getStatusCode(list,
								// addStruct);
								// GoogleCachingTest.put(shatamIndexQueryStruct.getQuery(),
								// addStruct);
								GoogleCachingTest.put(query.toString(),
										addStruct);
								addresses.add(addStruct);
								// U.log("Inside ShatamReader : " +
								// addresses.size());

							} else {
								// U.log(addressesWithoutZipTest.size()+"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"+addresses.size());
								if (finalsource.contains(dataSource)
										&& indextype.contains("k5")
										&& addresses.size() == 0) {
									// U.log("index k5 *********");
									if (mapaddressesWithoutZipTest.get(key) != null)
										addresses = mapaddressesWithoutZipTest
												.get(key);
								}
							}

						}
					}

					else {

						// ********************************************
						// U.log(indexType.getFieldName()+":::::::::::::::::::::SHATAM:::::::::::::::::::::::::::::"+k1dataSource);

						if (indexType.getFieldName().contains("k1")
								&& dataSource.contains(k1dataSource)) {
							// U.log("addressesk1=========="+addressesk1.size());
							if (addressesk1.size() != ShatamIndexUtil.maxresult)
								addressesk1.add(addStruct);

							if (addressesk1.size() == ShatamIndexUtil.maxresult)
								mapOfAddresses.put(key, addressesk1);
						}
						// ******************************************************

						if (!indexType.getFieldName().contains("k1")
								&& dataSource.contains(k1dataSource)
								&& mapOfAddresses.get(key) == null) {

							if (addressesk1.size() != ShatamIndexUtil.maxresult)
								addressesk1.add(addStruct);
							if (addressesk1.size() == ShatamIndexUtil.maxresult)
								mapOfAddresses.put(key, addressesk1);
						}

						/*
						 * if(source.contains("TIGER")&&indextype.contains("k5"))
						 * {
						 * addresses=addressesk1;//mapOfAddressesOfK1.get(key);
						 * } else {
						 */

						// *************************************
						firstApperanceaddresses.add(addStruct);

						if (flag == false) {
							if (matcher.isResultMatched(caseV, key)) {

								// *************************************
								addresses.add(addStruct);
								// *************************************
								if (i == MAX_HITS) {

									addresses.add(firstApperanceaddresses
											.get(0));
									if (addresses.size() != ShatamIndexUtil.maxresult)
										addresses.add(firstApperanceaddresses
												.get(1));
								}// for adding first addresses of same index if
									// match address found in same index

								flag = true;
								// U.log("TRUE flag====="+flag);
							}// test of exact match
						} else {
							// U.log("FALSE flag====="+flag);
							addresses.add(addStruct);
						}// no testing
							// *************************************
							// }

					}// maxresult>1

					exists.put(addStructKey, addStruct);
				}

				/*
				 * if(indextype.contains("k1")){
				 * 
				 * mapOfAddressesOfK1.put(key, addressesk1); }
				 */

				// if (addresses.size() > U.MAX_SEARCH_RESULTS_ALLOWED)

				if (addresses.size() == ShatamIndexUtil.maxresult) {
					break;
				}

			}// for hit

			if (ShatamIndexUtil.maxresult > 1
					&& addresses.size() == ShatamIndexUtil.maxresult) {
				GoogleCachingList.put(query.toString(), addresses);
			}
			// U.log("MaxResult  ::"+ShatamIndexUtil.maxresult);
			// U.log("Addrees Size: " + addresses.size());
			// U.log("addresses size=="+addresses.size());

			if (addresses.size() == ShatamIndexUtil.maxresult) {
				// U.log("Breaking::   "+ ShatamIndexUtil.maxresult);

				break;
			}

			// if(addresses.size()== ShatamIndexUtil.maxresult)break;
			// U.log("@@@@@addresses size");

		} // for cases

		// *************************************
		if (finalsource.contains(dataSource) && indextype.contains("k5")
				&& addresses.size() == 0 && ShatamIndexUtil.maxresult > 1) {
			// U.log(" curve==="+mapOfAddresses.get(key));

			addresses = mapOfAddresses.get(key);
		}

		// *************************************

		/*
		 * if (addresses.size()>0&&addresses.size() !=
		 * ShatamIndexUtil.maxresult&&ShatamIndexUtil.maxresult>1) {
		 * U.log("save map of add"); mapOfAddresses.put(key, addresses); return
		 * addresses; } else{ return addresses; }
		 */

		/*
		 * e = System.currentTimeMillis();
		 * U.log("address length=="+address.length());
		 * //U.log("processing hits time for one addresses:: :::  ::"+ (e - s));
		 */
		if (addresses == null) {
			addresses = new ArrayList<>();
			// U.log(address+"=====addresses is null");
			// Thread.sleep(4000);
		}

		return addresses;

	}// searchIndex()

	public void close() throws IOException {
		searcher.close();
		// ir.close();
	}

}
