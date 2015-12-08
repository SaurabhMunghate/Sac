package com.shatam.io;

import java.io.File;
import java.io.OutputStream;
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
			DOUBLE_METAPHONE = 0, REFINED_SOUNDEX = 0,Tiger=0;
	public static AddressStruct adStructBackUp = null;
	// public static MultiMap nonmatchedAddresses= new MultiHashMap();
	public static boolean isDebug = false;
	public static HashMap<String, ShatamIndexReader> readerMap;
	public static HashMap<String, ShatamIndexWriter> writerMap;
	// public static MultiMap output = new MultiHashMap();
	public static int maxresult;

	public static ShatamIndexWriter getWriter(final String fips,
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
	public static  MultiMap output;

	public MultiMap correctAddresses(MultiMap multimap1,
			final AbstractIndexType indexType1, final String dataSource1,
			String maxresult, String hitscore, String noOfJobs)
			throws Exception {
		long start = 0;
		output= new MultiHashMap();
		NORMAL = 0;
		 METAPHONE = 0; SOUNDEX = 0;
				DOUBLE_METAPHONE = 0; REFINED_SOUNDEX = 0;Tiger=0;
		for (AbstractIndexType it : AbstractIndexType.TYPES) {
			for (final String dataSource : new String[] { U.USPS,U.TIGER }) {

				final MultiMap nonmatchedAddresses = new MultiHashMap();

				final AbstractIndexType indexType = it;
				readerMap = new HashMap<String, ShatamIndexReader>();
				final MultiMap multimap = multimap1;

				ShatamIndexUtil.maxresult = Integer.parseInt(maxresult);
				ArrayList<AddressStruct> resultAddsDisplay = new ArrayList<AddressStruct>();

				// System.gc();
				long ss = System.currentTimeMillis();
				ShatamIndexQueryCreator shatamIndexQueryCreator = new ShatamIndexQueryCreator();
				final MultiMap queryMultimap = shatamIndexQueryCreator
						.createQuery(multimap, dataSource, "", "", "",
								indexType, readerMap);

				long ee = System.currentTimeMillis();
				System.out.println("queryMultimap==" + (ee - ss));

				// get all the set of keys
				Set<String> keys = queryMultimap.keySet();
				start = System.currentTimeMillis();
				SimpleThreadFactory simpleThreadFactory = new SimpleThreadFactory();
				ExecutorService executorService = Executors.newFixedThreadPool(
						Integer.parseInt(noOfJobs), simpleThreadFactory);

				// executorService.prestartAllCoreThreads();
				int i = 0;
				for (final String key : keys) {
					i++;

					executorService.submit(new Runnable() {
						public void run() {

							System.out
									.println(Thread.currentThread().getName());
							List list = (List) queryMultimap.get(key);
							ShatamIndexQueryStruct shatamIndexQueryStruct = (ShatamIndexQueryStruct) list
									.get(0);
							String unitType = (String) list.get(1);
							String unitNumber = (String) list.get(2);
							String address = (String) list.get(3);
							Query query = (Query) list.get(4);
							String state = (String) list.get(6);

							String readerKey = state + indexType.getFieldName()
									+ "-" + dataSource;
							ShatamIndexReader reader = readerMap.get(readerKey);
							long s = System.currentTimeMillis();
							ArrayList<AddressStruct> resultAdds = null;

							ArrayList<AddressStruct> addresses = new ArrayList<>();
							try {
								addresses = reader.searchIndex(address,
										shatamIndexQueryStruct, unitType,
										unitNumber, query);
							} catch (Exception e1) {

								U.log("exception in read addresses==" + e1);
								e1.printStackTrace();

							}

							synchronized (output) {
								U.log("Kirti 1===" + addresses.size());
								if(addresses.size()==0&&dataSource.contains("TIGER")&&(indexType.getFieldName()).contains("k3"))
								{
									output.put(key, addresses);
									output.put(key, (String) list.get(5));
//									if(dataSource.contains("TIGER"))Tiger++;
//									indexervalue(indexType);
								}
								if (addresses.size() > 0) {

									output.put(key, addresses);
									output.put(key, (String) list.get(5));
									if(dataSource.contains("TIGER"))Tiger++;
									indexervalue(indexType);
								}

								else {
									U.log("Shit..No match address ");
									List<String> list1 = (List<String>) multimap
											.get(key);
									U.log("Nomatchaddress 1===" + list1.size()
											+ ":::" + list1.get(0));

									nonmatchedAddresses.put(key, list1.get(0));
									nonmatchedAddresses.put(key, list1.get(1));
									nonmatchedAddresses.put(key, list1.get(2));
									nonmatchedAddresses.put(key, list1.get(3));
									nonmatchedAddresses.put(key, list1.get(4));
									nonmatchedAddresses.put(key, list1.get(5));
								}

								long e = System.currentTimeMillis();

								U.log("readear.search time for one address:: :::  ::"
										+ (e - s));

							}
						}

					});

				}

				executorService.shutdown();
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
				U.log("NONMATCHED ADDRESS===" + nonmatchedAddresses.size());
				if (nonmatchedAddresses.size() > 0) {
					// callBackCorrectAddresses(nonmatchedAddresses);
					U.log("**************CALL ANOTHER INDEX********");
					multimap1 = nonmatchedAddresses;

				} else {
					System.out.println("Normal=" + NORMAL + "::SOUNDEX="
							+ SOUNDEX + "::DOUBLE_METAPHONE=" + DOUBLE_METAPHONE
							+ "::METAPHONE=" + METAPHONE + "::REFINED_SOUNDEX="
							+ REFINED_SOUNDEX+"::TIGER="+Tiger);

					return output;
				}

			}

		}

		long end = System.currentTimeMillis();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println(dateFormat.format(cal.getTime()));
		System.out.println("Searching time for all addreses===="
				+ (start - end));
		System.out.println("Normal=" + NORMAL + "::SOUNDEX="
				+ SOUNDEX + "::DOUBLE_METAPHONE=" + DOUBLE_METAPHONE
				+ "::METAPHONE=" + METAPHONE + "::REFINED_SOUNDEX="
				+ REFINED_SOUNDEX+"::TIGER="+Tiger);
		
		

		return output;

	}// correctAddresses

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

		return new File(Paths.combine(Paths.DATA_ROOT, dataSource + "_Index",
				state));
	}

}
