package com.shatam.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//import com.exist.java.CustomAddressCorrector;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.shatam.shatamindex.analysis.Analyzer;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import org.json.JSONArray;
import org.json.JSONException;

import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.TopScoreDocCollector;
import com.shatam.shatamindex.standard.StandardAnalyzer;
import com.shatam.shatamindex.util.Version;

import com.shatam.memdb.IndexType;
import com.shatam.model.AddressStruct;
import com.shatam.util.AbbrReplacement;
import com.shatam.util.GoogleCachingList;
import com.shatam.util.GoogleCachingTest;
import com.shatam.util.OutputStatusCode;
import com.shatam.util.Paths;
import com.shatam.util.ShatamIndexQueryCreator;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class ShatamIndexUtil {
	// static String address1;
	// static String address2;
	// static String city;
	// static String state;
	// static String zip;
	// static String addkey;
	// static MultiMap output;
	// static MultiMap queryMultimap ;
	// BlockingQueue<Runnable> threadPool = new LinkedBlockingQueue<Runnable>();
	//
	// ThreadPoolExecutor executorService = new ThreadPoolExecutor(50, 2000, 0L,
	// TimeUnit.MILLISECONDS, threadPool);
	int counterMisingAddresses = 0;

	// public static final String FILES_TO_INDEX_DIRECTORY = "filesToIndex";
	// public static final String INDEX_DIRECTORY = "indexDirectory";
	public static void main(String args[]) {

	}

	public static int NORMAL = 0, METAPHONE = 0, SOUNDEX = 0,
			DOUBLE_METAPHONE = 0, REFINED_SOUNDEX = 0, Tiger = 0;
	public static AddressStruct adStructBackUp = null;
	// public static MultiMap nonmatchedAddresses= new MultiHashMap();
	public static boolean isDebug = false;
	public static HashMap<String, ShatamIndexReader> readerMap = new HashMap<>();
	public static HashMap<String, ShatamIndexWriter> writerMap;
	// public static MultiMap output = new MultiHashMap();
	public static int maxresult;

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

	// ArrayList<Long> normalUSPS=new ArrayList<Long>();
	// ArrayList<Long> normalTIGER=new ArrayList<Long>();
	// ArrayList<Long> metaphoneUSPS=new ArrayList<Long>();
	// ArrayList<Long> metaphoneTIGER=new ArrayList<Long>();
	// ArrayList<Long> soundexUSPS=new ArrayList<Long>();
	// ArrayList<Long> soundexTIGER=new ArrayList<Long>();
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
		// cache cleanup
		// U.log("Shatam Index Reader Clenaup1111111111 Launch");
		if (GoogleCachingTest.size() > 4000) {

			U.log(GoogleCachingTest.size());
			GoogleCachingTest.cleanup();
			// U.log("Cleanup called");
		}
		if (GoogleCachingList.size() > 4000) {

			U.log(GoogleCachingList.size());
			GoogleCachingList.cleanup();
			// U.log("Cleanup called");

		}
		// U.log(readerMap.size());
		// readerMap = new HashMap<String, ShatamIndexReader>();
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
			// lastDataSource="USPS";

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
				// U.log("KIRTI MISAL"+":::::::::::::::::");
				// U.log("FOR NOMATCHED ADDRESS");
				for (final String dataSource : arrayOfSource) {
					// U.log("  *** " + it.getFieldName() + " / " + dataSource);
					final MultiMap nonmatchedAddresses = new MultiHashMap();
					final String source = lastDataSource;
					final AbstractIndexType indexType = it;

					// U.log("*************************"+indexType.getFieldName());

					final MultiMap multimap = multimap1;

					ShatamIndexUtil.maxresult = Integer.parseInt(maxresult);
					ArrayList<AddressStruct> resultAddsDisplay = new ArrayList<AddressStruct>();

					// System.gc();
					ss = System.currentTimeMillis();
					ShatamIndexQueryCreator shatamIndexQueryCreator = new ShatamIndexQueryCreator();
					final MultiMap queryMultimap = shatamIndexQueryCreator
							.createQuery(multimap, dataSource, "", "", "",
									indexType, readerMap, flag);

					ee = System.currentTimeMillis();
					// System.out.println("queryMultimap==" + (ee - ss));
					queryTime.add((ee - ss));
					// get all the set of keys
					Set<String> keys = queryMultimap.keySet();
					// System.out.println("Multimap1 size=="+multimap.size()+"\nquerymultimap size=="+queryMultimap.size()+"\n query keys size=="+keys.size());

					/*
					 * SimpleThreadFactory simpleThreadFactory = new
					 * SimpleThreadFactory(); ExecutorService executorService =
					 * Executors.newFixedThreadPool( Integer.parseInt(noOfJobs),
					 * simpleThreadFactory);
					 */

					// executorService.prestartAllCoreThreads();
					int i = -1;
					final long startTime = System.currentTimeMillis();
					for (final String key : keys) {
						i++;

						// U.log("Shtam Address Parser");
						/*
						 * executorService.submit(new Runnable() { public void
						 * run() {
						 */
						/*
						 * System.out
						 * .println(Thread.currentThread().getName());
						 */

						List list = (List) queryMultimap.get(key);
						ShatamIndexQueryStruct shatamIndexQueryStruct = (ShatamIndexQueryStruct) list
								.get(0);
						String unitType = (String) list.get(1);

						String unitNumber = (String) list.get(2);
						// U.log("size of shatamindexQueryStruct==="+shatamIndexQueryStruct.getAddress());
						String address = (String) list.get(3);
						// U.log(address);
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

						// adding fuzzy logic
						/*
						 * if(quryType.contains("Fuzzy")) { readerKey = state +
						 * indexType.getFieldName() + "-" + dataSource; reader =
						 * readerMap.get(readerKey);
						 * 
						 * String buf=shatamIndexQueryStruct.getQuery();
						 * buf=buf.replace("^5", "~"); try {
						 * shatamIndexQueryStruct.setQuery(buf); } catch
						 * (Exception e3) { // TODO Auto-generated catch block
						 * e3.printStackTrace(); }
						 * U.log("Fuzzy created==="+buf); try {
						 * query=shatamIndexQueryStruct
						 * .createQueryObj(reader.parser);
						 * U.log("Fuzzy Query===="+query); } catch (Exception
						 * e2) { // TODO Auto-generated catch block
						 * e2.printStackTrace(); } }
						 */
						// Ending fuzy logic

						ArrayList<AddressStruct> addresses = new ArrayList<>();
						ss1 = System.currentTimeMillis();
						String readerKey = state + indexType.getFieldName()
								+ "-" + dataSource;
						ShatamIndexReader reader = readerMap.get(readerKey);
						if (reader == null) {
							U.log("OMG reader=null");
						}
						synchronized (output) {

							try {
								// U.log("Index Type: "
								// +indexType.getFieldName());
								// U.log("Unit:  "+ unitNumber );
								// U.log("Unit:  "+ query );
								// U.log("ShatamIndexUtil: " + address) ;

								addresses = reader.searchIndex(address,
										shatamIndexQueryStruct, unitType,
										unitNumber, query, key,
										indexType.getFieldName(), source,
										dataSource, k1DataSource);

								// U.log("Results Size: " +addresses.size());

								// U.log("Shatam Index Util:  " +
								// addresses.get(1));
								// U.log("Return Array Size: " +
								// addresses.size());
								/*
								 * if(addresses==null){
								 * 
								 * U.log( (String) list.get(5));
								 * Thread.sleep(40000);
								 * 
								 * //System.exit(1); }
								 */
								/*
								 * addresses = reader.searchIndex(address,
								 * shatamIndexQueryStruct, unitType, unitNumber,
								 * query);
								 */

							} catch (Exception e1) {

								U.log("exception in read addresses==" + e1);
								e1.printStackTrace();

							}

							ee1 = System.currentTimeMillis();
							searcherTime.add((ee1 - ss1));

							// /U.log("Kirti 1===" + addresses.size());
							if (addresses.size() == 0
									&& dataSource.contains(source)
									&& (indexType.getFieldName())
											.contains("k5")) {
								// U.log(source);
								output.put(key, addresses);
								output.put(key, (String) list.get(5));
								output.put(key, list);

								// if(dataSource.contains("TIGER"))Tiger++;

								// *****************************************************************************
								// For finding Address of datasource wise
								/*
								 * try {
								 * writeTextFile(indexType.getFieldName(),dataSource
								 * ,address); } catch (IOException e1) { // TODO
								 * Auto-generated catch block
								 * e1.printStackTrace(); }
								 */
								// *************************************************************************************

							}
							if (addresses.size() == ShatamIndexUtil.maxresult) {

								output.put(key, addresses);

								// List add = output.get(key);
								// U.log("Shatam IndexUtil:  "+add.getHouseNumber()
								// );
								output.put(key, (String) list.get(5));
								output.put(key, list);
								// U.log("Shatam IndexUtil output key: "
								// +output.get("0"));
								// U.log("Shatam IndexUtil output key: "
								// +output.get("1"));
								if (dataSource.contains("TIGER"))
									Tiger++;
								indexervalue(indexType);
								// *****************************************************************************
								// For finding Address of datasource wise
								/*
								 * try {
								 * writeTextFile(indexType.getFieldName(),dataSource
								 * ,address+","+city + ","+ state+ "," +zip); }
								 * catch (IOException e1) { // TODO
								 * Auto-generated catch block
								 * e1.printStackTrace(); }
								 */
								// *****************************************************************************
							}

							else {
								// U.log("No match address ");
								List<String> list1 = (List<String>) multimap
										.get(key);
								/*
								 * U.log("Nomatchaddress 1===" + list1.size() +
								 * ":::" + list1.get(0));
								 */

								nonmatchedAddresses.put(key, list1.get(0));
								nonmatchedAddresses.put(key, list1.get(1));
								nonmatchedAddresses.put(key, list1.get(2));
								nonmatchedAddresses.put(key, list1.get(3));
								nonmatchedAddresses.put(key, list1.get(4));
								nonmatchedAddresses.put(key, list1.get(5));
							}

							long e = System.currentTimeMillis();

							// U.log("readear.search time for one address:: :::  ::"
							// + (e - s));

						}
						long endTime = System.currentTimeMillis();
						long finalTime = (endTime - startTime);
						// U.log("final time==="+finalTime);
					}

					/*
					 * });
					 * 
					 * }
					 * 
					 * 
					 * executorService.shutdown();
					 * executorService.awaitTermination(Long.MAX_VALUE,
					 * TimeUnit.DAYS);
					 */

					// U.log(output.size()+"======NONMATCHED ADDRESS===" +
					// nonmatchedAddresses.size());

					if (nonmatchedAddresses.size() > 0) {
						// callBackCorrectAddresses(nonmatchedAddresses);
						// U.log("**************CALL ANOTHER INDEX********");
						multimap1 = nonmatchedAddresses;
						// U.log("Size Of Multimap: " + multimap1.size());
					} else {
						/*
						 * System.out.println("Normal=" + NORMAL + "::SOUNDEX="
						 * + SOUNDEX + "::DOUBLE_METAPHONE=" + DOUBLE_METAPHONE
						 * + "::METAPHONE=" + METAPHONE + "::REFINED_SOUNDEX=" +
						 * REFINED_SOUNDEX + "::TIGER=" + Tiger);
						 */
						// U.log("OutputMultimap size==="+output.size());

						return output;
					}

				}

			}
		}

		long end = System.currentTimeMillis();
		// DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		// Calendar cal = Calendar.getInstance();
		/*
		 * System.out.println(dateFormat.format(cal.getTime()));
		 * System.out.println("Searching time for all addreses====" + (end -
		 * start)); System.out.println("Normal=" + NORMAL + "::SOUNDEX=" +
		 * SOUNDEX + "::DOUBLE_METAPHONE=" + DOUBLE_METAPHONE + "::METAPHONE=" +
		 * METAPHONE + "::REFINED_SOUNDEX=" + REFINED_SOUNDEX + "::TIGER=" +
		 * Tiger);
		 */
		// long t = 0;
		// for(long time:queryTime){
		// t+=time;
		// //U.log("queryTime=="+time);
		// }
		// long t2 = 0;
		/*
		 * if(searcherTime!=null) for(long time:searcherTime){ t2+=time;
		 * //U.log("searchtime=="+time); }
		 */
		// U.log("total querytime and searchtime==="+t+"::::::::::::"+t2);
		// U.log("OutputMultimap sizeee==="+output.size());
		return output;

	}// correctAddresses

	/*
	 * public static void writeTextFile(String path,String data,String address)
	 * throws IOException { path="C:\\SAC_cach\\Ashish_Cache\\" +
	 * path+data+".txt"; File f=new File(path); if(!f.exists()) {
	 * f.createNewFile(); } FileWriter fileWritter = new FileWriter(f,true);
	 * BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
	 * bufferWritter.newLine(); bufferWritter.write(address);
	 * 
	 * bufferWritter.close();
	 * 
	 * }
	 */
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
