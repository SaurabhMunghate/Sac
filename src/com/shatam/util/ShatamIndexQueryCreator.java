package com.shatam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.jfree.util.UnitType;

import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.io.SimpleThreadFactory;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.search.Query;

public class ShatamIndexQueryCreator {
	// MultiMap queryMultimap;
	// public String city;
	// public String state;
	// public String zip;
	
	
	// static Metaphone SNDX = new Metaphone();
	public static String createAddressQuery(String add, String state,
			AbstractIndexType indexType) throws Exception {

		StringBuffer buf = null;
		String[] arr = add.split("[^\\d\\w\\-/]");
	//	U.log("::::::::"+arr.length);
		// int partIndex = -1;
		for (String part : arr) {
			if (StrUtil.isEmpty(part))
				continue;

			// partIndex++;

			String v = AbbrReplacement.getFullName(part, state);
			if (v.matches("[\\-_]")) {
				continue;
			}

			if (StrUtil.isEmpty(v)) {
				v = part;
			}

			v = indexType.encode(v);

			// if (v.equals("po") || v.equals("box")){
			// continue;
			// }

			buf.append(" ").append(v);

			//U.log("addressQuery v:" + v + " part:" + part);
			boolean potentialStreetName = v.equals(part)
					|| v.equals(AbstractIndexType.standardize(part));
			if (potentialStreetName) {
				if (StrUtil.isNum(buf.toString().trim())) {
					int iBuf = Integer.parseInt(buf.toString().trim());
					buf.append(U._getNumSuf(iBuf));
				}
				buf.append(U.STREET_ENHANCE);
			}

		}// for part : arr

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
//U.log(add);
		// get the house Num
		{
			String[] hnArr = extractHN(add);
		//	U.log("addressQuery  add:"+add + " hnArr:"+hnArr[0]);
			if (hnArr != null) {
				shatamIndexQueryStruct.setHouseNumber(hnArr[0]);
				add = hnArr[1];
			}
		}
		//U.log("addressQuery Before:"+add);
		/*
		 * String[] extractAptArr = StrUtil.extractApartment(add, ""); if
		 * (extractAptArr.length==2 && extractAptArr[0].equals("po box")){
		 * //U.log("addressQuery Before:"+add); add =
		 * StrUtil.removeUnitFromAddress(add); //if (StrUtil.isEmpty(add)) add =
		 * "A"; //U.log("addressQuery After:"+add); }
		 */
	//U.log("Before replacement====="+add);
		String[] arr = add.split("[^\\d\\w\\-/]");
		// int partIndex = -1;
		for (String part : arr) {
			if (StrUtil.isEmpty(part))
				continue;

			// partIndex++;

			String v = AbbrReplacement.getFullName(part, state);
			if (v.matches("[\\-_]")) {
				continue;
			}

			if (StrUtil.isEmpty(v)) {
				v = part;
			}

			v = indexType.encode(v);

			// if (v.equals("po") || v.equals("box")){
			// continue;
			// }

			buf.append(" ").append(v);

			//added on 19th nov 2015 -"11","956 36 1/2 AVENUE EAST"," ","MOLINE","IL","61244" 
			if(Util.match(part, "\\d+(th|nd|st|rd)")!=null){
				buf.append(" "+part.replaceAll("th|nd|st|rd", ""));
			}
			
		//U.log("addressQuery v:" + v + " part:" + part);
			boolean potentialStreetName = v.equals(part)
					|| v.equals(AbstractIndexType.standardize(part));
		//	U.log(potentialStreetName+":::::::::::::::::"+StrUtil.isNum(buf.toString().trim()));
			try{
			if (potentialStreetName) {
				if (StrUtil.isNum(v.trim())) {
				
					int iBuf = Integer.parseInt(v.trim());
					//U.log((int) Math.log10(iBuf) + 1);
					//U.log(Util.match(add, "rr\\s*\\d+")+":::::"+Util.match(add, "county road \\d{3}"));
					if(((int) Math.log10(iBuf) + 1)<4&&Util.match(add, "rr\\s*\\d+")==null&&Util.match(add, "county road \\d{3}")==null)
					buf.append(U._getNumSuf(iBuf));
					
				//	U.log("1=="+buf.toString());
					
					if(Util.match(buf.toString(), "\\d+(th|nd|st|rd)")!=null)buf.append(" "+v.trim());
					
					//U.log("2=="+buf.toString());
				}
				buf.append(U.STREET_ENHANCE);
			}
			}
catch(Exception e){
				
			}

		}// for part : arr

		String str = buf.toString().trim();
	// U.log("  A LQC str:"+str);
		 
		 
		 
		// //U.log("  B LQC str:"+str.replace(U.STREET_ENHANCE, ""));
		shatamIndexQueryStruct.setNormalizedStreetName(str.replace(
				U.STREET_ENHANCE, ""));

		
		str=str.replace("OR ", "");
		
		
		shatamIndexQueryStruct.setQuery(str);

	//U.log("shatamIndexQueryStruct.query:"+shatamIndexQueryStruct.query);

		// //U.log("shatamIndexQueryStruct.query :"+shatamIndexQueryStruct.query
		// +" indexType:"+indexType);
		
		
		
		
		return shatamIndexQueryStruct;

	}// addressQuery()

	private static ArrayList<String> HN_MAP = new ArrayList<String>();
	static {

		HN_MAP.add("(\\d+\\s*[&-]+\\s*\\d+ )");//8126 - 8128
		
		HN_MAP.add("^(\\d+/\\d+) ");// 1801/1803 NE WICHITA WAY

		HN_MAP.add("^(\\d+ and \\d+) ");// 1301 AND 1303 NEHALEM AVENUE

		HN_MAP.add("^(\\d+ \\& \\d+) ");// 1810 & 1812 ne noble ave

		HN_MAP.add("^(\\d+\\-\\d+-\\d+) "); // 128-130-132 BERTHEL AVE.

		HN_MAP.add("^(\\d+ \\d/\\d)"); // 303 1/2 baptist
		HN_MAP.add("^(\\d+\\-\\d+) "); // 153-01 32nd

		HN_MAP.add("^(\\d+\\-\\w)"); // 160-A N 6TH ST # 3

		HN_MAP.add("^([e|w|n|s]\\s*\\d+\\w+) "); // n 100B main st//make changes 20 nov 2015 ^[e|w|n|s] (\\d+\\w+)
		HN_MAP.add("^[e|w|n|s] (\\w+\\d+) "); // n B100 main st
		HN_MAP.add("^(\\d+\\w+) "); // 100J main st
		HN_MAP.add("^(\\w+\\d+) "); // B100 main st

		HN_MAP.add("^(\\d+) "); // 100 main

		HN_MAP.add("^lot (\\d+) "); // LOT 3 STEWART AVE
		HN_MAP.add("^tl (\\d+),"); // TL 306, WILLOW SPRINGS ROAD

		HN_MAP.add(" (\\d+) [\\w]+ [\\w]+"); // GARDNER, SCOTT 1506 SW 203RD
												// AVENUE
		
		HN_MAP.add("^(\\d+)[^A-Za-z0-9] ");//14217- 
	}

	public static String[] extractHN(String add) {
		String[] res = new String[2];
		add = add.trim().toLowerCase();

		// Solve RR case
		// HN_MAP.add("^rr *\\d+");//1801/1803 NE WICHITA WAY
		if (add.matches("^rr *\\d+.*")) {
			// Rural Routes
			return new String[] { "", add };
		}

		
		
		for (String reg : HN_MAP) {
			String hn = StrUtil.extractPattern(add, reg, 1);

			//U.log(add+"=="+reg + " hn:"+ hn);
			if (!StrUtil.isEmpty(hn)) {

				res[0] = hn.trim();
				// res[0] = res[0].replace("-", "");

				res[1] = add.replaceFirst(hn, "").replaceAll("\\s+", " ").trim();
				return res;
			}
		}
		return null;
	}

	public static MultiMap createQuery(final MultiMap multimap,
			final String dataSource, final String city1, final String zip1,
			final String state1, final AbstractIndexType indexType,
			final HashMap<String, ShatamIndexReader> readerMap,
			final boolean flag) throws Exception {
		MultiMap queryMultimap = new MultiHashMap();
		// //U.log("createQuery add:"+add);
		 Query query = null;
		Set<String> keys = multimap.keySet();
		// U.log("query keys size======"+keys.size());
		
		/*
		 * SimpleThreadFactory simpleThreadFactory = new SimpleThreadFactory();
		 * ExecutorService executorService = Executors.newFixedThreadPool( 10,
		 * simpleThreadFactory);
		 */

		for (String key : keys) {

			/*
			 * executorService.submit(new Runnable() { public void run() {
			 */
			// System.out.println("Query creater==="+Thread.currentThread().getName());
			List<String> list = (List<String>) multimap.get(key);
			String address1 = list.get(0);
			// U.log("Address1::"+address1);
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
					// TODO Auto-generated catch block

					e1.printStackTrace();

				}

			}
			// U.log("Reader Map:: Size :::"+readerMap.size());
			long s = System.currentTimeMillis();
			String readerKey = state + indexType.getFieldName() + "-"
					+ dataSource;

			// U.log("READER KEY===="+readerKey);
			ShatamIndexReader reader = ShatamIndexUtil.readerMap.get(readerKey);
			 
			if (reader == null) {
				// U.log("reader null");
				try {
					reader = new ShatamIndexReader(indexType, state,
							dataSource, flag);
				} catch (Exception e1) {

					e1.printStackTrace();
				}

				ShatamIndexUtil.readerMap.put(readerKey, reader);
			}

			long e = System.currentTimeMillis();

			// U.log("loading reader- reader shatam reader  ::" + (e - s));
			s = System.currentTimeMillis();
			String address = null;
			
			try {
				
				address = U.getSearchableAddress(address1, address2);
			} catch (Exception e1) {

				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			e = System.currentTimeMillis();
			//U.log("Searchable address===="+address);
			// U.log("get searchable address time::  ::" + (e - s));

			s = System.currentTimeMillis();
			String[] unitArr = StrUtil.extractApartment(address1, address2);
			
			
			e = System.currentTimeMillis();

			// U.log("extract apartment time :::  ::" + (e - s));

			int j = 0;

			ShatamIndexQueryStruct shatamIndexQueryStruct = new ShatamIndexQueryStruct();
			try {
			//	U.log("Searchable address===="+address);
				shatamIndexQueryStruct = addressQuery(address, city, zip,
						indexType, state);
			} catch (Exception e1) {

				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			StringBuffer buf = new StringBuffer(
					shatamIndexQueryStruct.getQuery() == null ? ""
							: shatamIndexQueryStruct.getQuery());

		//	 U.log("street query:-"+buf.toString());

			if (!StrUtil.isEmpty(city)) {

				city = city.replaceAll("[\\s,\\.\\-]+", " ").trim();
				city = city.trim().toLowerCase();

				if (city.matches("^(\\d+) .{4,}")) {
					// Seems like city is the address
				} else {
					city = city.replaceAll(StrUtil.WORD_DELIMETER, "");
				}

				try {
					city = indexType.encode(city);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				shatamIndexQueryStruct.setNormalizedCity(city);

				// TODO: Enhance every word in the city CITY_ENHANCE
				// city = city.trim().replaceAll(" ",
				// U.CITY_ENHANCE+" ");
				// --- END

				buf.append(" ").append(city + "_CITY" + U.CITY_ENHANCE);

				// U.log("buf:" + buf);
			}

			if (!StrUtil.isEmpty(zip)) {
				zip = zip.trim();
				zip = zip.replaceAll("[^\\d]", "");
				// //U.log("B ZIP:"+zip);
				//
				if (!StrUtil.isEmpty(zip)) {
					// //U.log("A ZIP:"+zip);
					if (zip.length() > 5)
						zip = zip.substring(0, 5);
					else if (zip.length() < 5)
						zip = String.format("%05d", Integer.parseInt(zip));
					// //U.log("B ZIP:"+zip);

					shatamIndexQueryStruct.setNormalizedZip(zip);

					buf.append(" ").append(zip + "_ZIP" + U.ZIP_ENHANCE);
				}
			}

			// U.log("kirti=="+buf.toString().trim());
			/*
			 * try { shatamIndexQueryStruct.setQuery(buf.toString().trim()); }
			 * catch (Exception e1) { // TODO Auto-generated catch block
			 * e1.printStackTrace(); }
			 */

			 synchronized (reader)
			{
				try {
					
					shatamIndexQueryStruct.setQuery(buf.toString().trim());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// U.log("kirti1");
				
				try {

					query = shatamIndexQueryStruct
							.createQueryObj(reader.parser);
					if(query==null){
						U.log("OMG queryyyyy111=null");
					}
					if(reader==null){
						U.log("OMG queryyyyyREADER111=null");
					}
					//
					
				//	U.log("Query==**=="+query.toString());

				} catch (Exception e1) {
					// TODO Auto-generated catch block

					e1.printStackTrace();
				}
//U.log(query.toString());
				queryMultimap.put(key, shatamIndexQueryStruct);
				queryMultimap.put(key, unitArr[0]);
				queryMultimap.put(key, unitArr[1]);
				queryMultimap.put(key, address);
				queryMultimap.put(key, query);
				queryMultimap.put(key, addkey);
				queryMultimap.put(key, state);
			}
		}
		/*
		 * }); }
		 * 
		 * executorService.shutdown();
		 * executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		 * while(!executorService.isTerminated()){
		 * 
		 * }
		 */

		ShatamIndexUtil.readerMap = readerMap;
		return queryMultimap;

	}// createQuery()

}
