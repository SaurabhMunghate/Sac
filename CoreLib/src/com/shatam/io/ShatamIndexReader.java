package com.shatam.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.codec.language.Metaphone;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.queryParser.QueryParser;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.ScoreDoc;
import com.shatam.shatamindex.search.TopDocs;
import com.shatam.shatamindex.search.TopScoreDocCollector;
import com.shatam.shatamindex.store.FSDirectory;
import com.shatam.shatamindex.store.NIOFSDirectory;
import com.shatam.shatamindex.util.Version;
import com.shatam.interpolator.Interpolate;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.ShatamIndexQueryCreator;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class ShatamIndexReader {
	private static final int MAX_HITS = 6;

public static IndexSearcher searcher;
public static	 QueryParser parser;

	AbstractIndexType indexType;
	private String state = null;

	public ShatamIndexReader(AbstractIndexType indexType, String state,
			String dataSource) throws Exception {
		File shatamIndexPath = ShatamIndexUtil.createIndexPath(state,
				dataSource);

		U.log("shatamIndexReader shatamIndexPath:" + shatamIndexPath);
	IndexReader	ir = IndexReader.open(FSDirectory .open(shatamIndexPath), true);//priviously here used FSDirectory :-20 july/NIOFSDirectory:-give bad effects on windows
		searcher = new IndexSearcher(ir);
		parser = new QueryParser(Version.SHATAM_35, indexType.getFieldName(),
				ShatamIndexUtil.getAnalyzer());

		this.indexType = indexType;
		this.state = state;
	}

	// static Metaphone SNDX = new Metaphone();

	/*public ArrayList<AddressStruct> searchAddress(String address, String city,
			String zip, String unitType, String unitNumber) throws Exception {
		long s = System.currentTimeMillis();
		
		
		
		ShatamIndexQueryStruct shatamIndexQueryStruct = ShatamIndexQueryCreator
				.createQuery(address, city, zip, state, indexType);
		
		long e = System.currentTimeMillis();
		
		U.log("shatamIndexQueryStruct  time:: :::  ::"+ (e - s));
		
		
		s = System.currentTimeMillis();
		ArrayList<AddressStruct> addresses = searchIndex(address,
				shatamIndexQueryStruct, unitType, unitNumber);
		
		e = System.currentTimeMillis();
		
		U.log("searchIndex time:: :::  ::"+ (e - s));

		return addresses;
	}// searchAddress()
*/
	public ArrayList<AddressStruct> searchIndex(String address,
			ShatamIndexQueryStruct shatamIndexQueryStruct,
			String unitTypeFromInputAddress, String unitNumber,Query query )
			throws Exception {
	    //	System.gc();
	    //	long s = System.currentTimeMillis();
		
		  U.log("Start searchIndex............");

		ArrayList<AddressStruct> addresses = new ArrayList<AddressStruct>();

		HashMap<String, AddressStruct> exists = new HashMap<String, AddressStruct>();
	
	
		//Query query = shatamIndexQueryStruct.createQueryObj(parser);
	
	
	 //   long e = System.currentTimeMillis();
		
	//	U.log("createQueryObj time:: :::  ::"+ (e - s));
		if (query == null)
		{
			U.log("Kirti");
			return addresses;
		
		}
		
		
		//new code of lucene added by kirti misal
		
	   // TopScoreDocCollector collector = TopScoreDocCollector.create(MAX_HITS, true);
	  
	   	
	   		
		
		long s = System.currentTimeMillis();
		
		TopDocs results = searcher.search(query, MAX_HITS);
		
        long e = System.currentTimeMillis();
		
		U.log("searcher.search time:: :::  ::"+ (e - s));
		ScoreDoc[] hits =results.scoreDocs;
        U.log(hits.length);
      
		// float prevScore = -1;
		s = System.currentTimeMillis();
		for (ScoreDoc hit : hits) {
		
		Document doc = searcher.doc(hit.doc);

			AddressStruct addStruct = new AddressStruct(state);
			addStruct.inputAddress = address;
			addStruct.setQueryStruct(shatamIndexQueryStruct);

		
			
			if (StrUtil.isEmpty(address)) {
				addStruct.hitScore = 0;
			} else {
				addStruct.hitScore = hit.score;
			}

			addStruct.setHouseNumber(shatamIndexQueryStruct.getHouseNumber());

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
				
				
				//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
				              
				//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
				if (matcher.isResultMatched()) 
				{
					U.log("Test Pass");
					addresses.add(addStruct);
				}

				exists.put(addStructKey, addStruct);
			}

			U.log("FOUND Addresses:-"+addStruct.get(AddColumns.CITY).toUpperCase()+"::::"+addStruct.getFoundName());

		//	if (addresses.size() > U.MAX_SEARCH_RESULTS_ALLOWED)
			if (addresses.size() == ShatamIndexUtil.maxresult)
				break;

		}// for hit
		 e = System.currentTimeMillis();
			
			U.log("processing hits time for one addresses:: :::  ::"+ (e - s));

		return addresses;

	}// searchIndex()

	public static void close() throws IOException {
		searcher.close();
	//	ir.close();
	}

}
