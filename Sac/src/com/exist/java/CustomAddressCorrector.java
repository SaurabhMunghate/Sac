package com.exist.java;

//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
//import java.util.List;
//import java.util.Set;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.json.JSONArray;
import org.json.JSONException;

import com.data.main.Logger;
import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.U;

public class CustomAddressCorrector {


	/**
	 * @param args
	 */
	// public static ArrayList<Integer> timesAll = new ArrayList<Integer>();
	// public static ArrayList<String> addressAll = new ArrayList<String>();
	


    
	public  MultiMap corrUsingAppropriateIndex(
			MultiMap multimap,String maxresult,String hitscore ,String noOfJobs) throws Exception {
		   @SuppressWarnings("deprecation")
		MultiMap returnoutput=new MultiHashMap();
		U.log("Size of input multimap=="+multimap.size());
		   int i=0;
          for (AbstractIndexType it : AbstractIndexType.TYPES) {
           if(i==0){
			for (final String dataSource : new String[] {U.USPS }) {
				
				 U.log("  *** " + it.getFieldName() + " / " + dataSource);
				long s = System.currentTimeMillis();
				ShatamIndexUtil shatamIndexUtil=new ShatamIndexUtil();
				returnoutput = shatamIndexUtil
						.correctAddresses(multimap,
								it, dataSource,maxresult,hitscore,noOfJobs);
				long e = System.currentTimeMillis();
				U.log("Inside loop ::"+ (e - s));
				
			i++;	
			return returnoutput;             
			}}}
          return returnoutput;              
	}
}
