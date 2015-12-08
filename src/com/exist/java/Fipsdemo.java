package com.exist.java;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.shatam.util.U;

public class Fipsdemo {

	/**
	 * @param args
	 */
	public static	HashMap FIPS=new HashMap();
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		FIPS=getfips();
		U.log(FIPS.size());
		String hashcode="734231710";
		U.log(FIPS.get(hashcode));
		
	}
	
	public static HashMap getfips() throws IOException, InterruptedException{
		HashMap FIPS=new HashMap();
//		U.log("path="+System.getProperty("user.dir")+"\\FIPS.csv");
		
		CsvListReader csvReader = new CsvListReader(new FileReader(System.getProperty("user.dir")+"\\FIPS.csv" ), CsvPreference.STANDARD_PREFERENCE);
		List<String> caRow = null;
		while ((caRow = csvReader.read()) != null) {
			System.out.println(caRow.get(0).trim() + "::ROW2::" + caRow.size());
			//Thread.sleep(4000);
				//U.log("kirti misal="+caRow.get(0).trim());
			
				FIPS.put(caRow.get(0).trim(), caRow.get(5).trim());
				
				
			
		}
		return FIPS;
	}
}
