package com.exist.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;


import com.shatam.data.ZipCodes;
import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.util.U;
import com.shatam.util.Util;

public class GenerateCache  {
	public static void main(String args[]) throws Exception {

		doCache();
	}
public static	HashMap<String,String> FIPS=new HashMap<>();

public static	HashMap<String, HashSet<String>> cities = Util.getCity();


public static HashMap<String,String> getfips() throws Exception{
	HashMap<String,String> FIPS=new HashMap<>();
//	U.log("path="+System.getProperty("user.dir")+"\\FIPS.csv");//  HashSet<String> city = zipToCity.get(zip);
	//U.log(cities.size());
	cities = Util.getCity();
	//U.log(cities.size());
	
	
	
	
	CsvListReader csvReader = new CsvListReader(new FileReader("FIPS.csv" ), CsvPreference.STANDARD_PREFERENCE);
	try{
	U.ftpUploadFile();
	}
	catch(Exception e){
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String ROOT = System.getProperty("user.dir");
		String PATH = new File(ROOT).getParent() + "/LOG/";
		File file = new File(PATH + dateFormat.format(date) + ".txt");
		BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
		out.write("***"+e.getMessage());
		out.close();
	}
	List<String> caRow = null;
	while ((caRow = csvReader.read()) != null) {
		//System.out.println(caRow.get(0).trim() + "::ROW2::" + caRow.size());
		
			//U.log("kirti misal="+caRow.get(0).trim());
			FIPS.put(caRow.get(0).trim(), caRow.get(5).trim());
			
			
		
	}
	return FIPS;
}
	public static void doCache() throws Exception {
		// AbstractIndexType it = AbstractIndexType.NORMAL;
		FIPS=getfips();
		//U.log(FIPS.size());
		Iterator iterator = U.STATE_MAP.keySet().iterator();

		while (iterator.hasNext()) {
			String state = (String) iterator.next();
		//	 String state="CA";

			for (AbstractIndexType it : AbstractIndexType.TYPES) {

				for (final String dataSource : new String[] { U.USPS,U.TIGER }) {

					String readerKey = state + it.getFieldName() + "-"
							+ dataSource;
					//U.log(readerKey);
					
					ShatamIndexReader reader = new ShatamIndexReader(it, state,
							dataSource,false );
					if(reader==null)
					{
						//U.log("Reader null");
					}
					ShatamIndexUtil.readerMap.put(readerKey, reader);
				}
			}
			// // break;
		}

	}

}
