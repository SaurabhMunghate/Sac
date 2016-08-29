package com.shatam.util;

import java.util.ArrayList;
import java.util.List;

import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.search.Query;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

public class OutputStatusCode {

	/**
	 * @param args
	 */
	

	public static void main(String[] args) {

		// U.log(getStatusCode("w Ridd Cir", "w Ridd Cir"));
	}

	/*
	 * 10 No modification, 11 Street name was modified, 12 Street number was
	 * modified ,13 Zip code was modified, 14 Address not found, 15 City was
	 * modified
	 */
	public static boolean chkMatchingString(String s1, String s2) {

		//U.log(s1 + ":::::::::" + s2);

		JaroWinkler algorithm = new JaroWinkler();
		//U.log("my:" +algorithm.getSimilarity(s1.toLowerCase(), s2.toLowerCase()));
		if (algorithm.getSimilarity(s1.toLowerCase(), s2.toLowerCase()) != 1.0) {
			return false;
		}
		return true;
	}

	// (input,output)
	public static String getStatusCode(List list, String fAddress,String fCity,String fZip) {
String matchingStatus="";
       
		ShatamIndexQueryStruct shatamIndexQueryStruct = (ShatamIndexQueryStruct) list
				.get(0);
		String unitType = (String) list.get(1);

		String unitNumber = (String) list.get(2);
		//U.log("UnitType==="+unitType.length()+"::"+unitNumber);
		String street = (String) list.get(3);

		Query query = (Query) list.get(4);
		String state = (String) list.get(6);
		String city = shatamIndexQueryStruct.getCity();
		String zip = shatamIndexQueryStruct.getZip();

		String foundStreet;
		String foundCity;
		String foundZip;

		// if (addStruct.size() < 3){
		// String maxResults = addStruct.size() + "";
		// }
		// //U.log("Count of Addresses ::"+count++);

		foundStreet = fAddress;//addStruct.toOnlyStreet().toString().toUpperCase();
		foundCity = fCity;//addStruct.get(AddColumns.CITY);
		foundZip =fZip;// addStruct.get(AddColumns.ZIP);

		String inputCity = shatamIndexQueryStruct.getCity();
		String inputZip = shatamIndexQueryStruct.getZip();
		String inputAddress = (String) list.get(3);
		
		//if(unitType.length()>0)
	   //inputAddress = (String) list.get(3)+" "+unitType; // i did changes here
          if(inputAddress==null)
		    inputAddress="";
          if(inputZip==null)
        	  inputZip="";
          if(inputCity==null)
        	  inputCity="";

		if (foundStreet.toLowerCase().contains("no match found")) {

			matchingStatus = "14";
			return matchingStatus;
		}

		if (street1StatusCode(inputAddress, foundStreet) != null) {
			if (matchingStatus.trim().length() > 1) {
				matchingStatus += ",";
			}
			matchingStatus += street1StatusCode(inputAddress+" "+unitNumber, foundStreet);
		}
		/*
		 * if(street2StatusCode(foundZip, foundZip)!=null) {
		 * if(matchingStatus.trim().length()>1){ matchingStatus+=","; }
		 * matchingStatus+=street2StatusCode(input_address, output_address); }
		 */
		if (cityStatusCode(inputCity, foundCity) != null) {
			if (matchingStatus.trim().length() > 1) {
				matchingStatus += ",";
			}
			matchingStatus += cityStatusCode(inputCity, foundCity);
		}
		
		if (zipStatusCode(inputZip, foundZip) != null) {
			if (matchingStatus.trim().length() > 1) {
				matchingStatus += ",";
			}
			matchingStatus += zipStatusCode(inputZip, foundZip);
		}

		if (matchingStatus.trim().length() < 2) {
			matchingStatus = "10";
		}
      
		
		
		
		
		return matchingStatus;
	}
/*public void isPrefixSuffixMatch(String inputAddress,String outputAddress,String state){
	//diffrentiate no. from suffix
	{
				String DIR_REG = "(w|n|s|ne|nw|se|sw|east|west|north|south|northeast|northwest|southeast|southwest)";
				String hn = StrUtil.extractPattern(inputAddress, "(\\d+)" + DIR_REG
						+ " ", 1);

				if (hn == null) {
					// 128mockingbirdln
					hn = StrUtil.extractPattern(inputAddress, "(\\d+)[a-z]{3,}", 1);
				}

				if (hn == null) {
					// west128 st
					hn = StrUtil.extractPattern(inputAddress, DIR_REG + "(\\d+)", 2);
				}
				if (hn != null) {
					inputAddress = inputAddress.replaceFirst(hn, " " + hn + " ").replaceAll(
							"\\s+", " ");
					//U.log("After fixNumericStreetSuffixes===="+inputAddress);
				}
			}
			
		//full form	
	inputAddress=inputAddress.trim().toLowerCase();
	StringBuffer buf = new StringBuffer();
			String[] arr = inputAddress.split("[^\\d\\w\\-/]");
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

				

				// if (v.equals("po") || v.equals("box")){
				// continue;
				// }

				buf.append(" ").append(v);

				// //U.log("addressQuery v:" + v + " part:" + part);
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

}*/
	public static String street1StatusCode(String input_address,
			String output_address) {
String status=null;
//U.log("input==" + input_address+":::::::::::"+output_address);

if (Util.match(output_address.toLowerCase(),
		  "(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
		  && Util.match(input_address.toLowerCase(),
		    "(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
		  && Util.match(input_address.toLowerCase(), "\\d(th|st|nd|rd)") != null)
		 input_address = input_address.toLowerCase().replaceAll(
		   "\\d(th|st|nd|rd)", "");

		if (Util.match(output_address.toLowerCase(), "\\d(th|st|nd|rd)") != null
		  && Util.match(input_address.toLowerCase(),
		    "(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
		  && Util.match(input_address.toLowerCase(), "\\d(th|st|nd|rd)") != null)
		 input_address = input_address
		   .toLowerCase()
		   .replaceAll(
		     "(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)",
		     "");

//		 input_address = AbbrReplacement.getFullAddress(input_address.replace("  ", " "), "")
//				    .toLowerCase().replaceAll(" av\\z", " ave");
		 
		input_address = AbbrReplacement.getFullAddress(input_address, "")
				.toLowerCase();
		output_address = AbbrReplacement.getFullAddress(output_address, "")
				.toLowerCase();
		

		if (chkMatchingString(input_address, output_address) == false)
			status = "11";

		return status;
	}

	public static String street2StatusCode(String input_address,
			String output_address) {
		String status=null;
		input_address = AbbrReplacement.getFullAddress(input_address, "");

		//U.log("input==" + input_address);

		if (chkMatchingString(input_address, output_address) == false)
			status = "11";

		return status;
	}

	public static String cityStatusCode(String input_city, String output_city) {
		String status=null;
		
		/*if(input_city.trim().length()<2){
			status="17";
		}
		else*/
		if (chkMatchingString(input_city, output_city) == false)
			status = "15";

		return status;
	}

	public static String zipStatusCode(String input_zip, String output_zip) {
		String status=null;
		
		/*if(input_zip.trim().length()<2){
			status="16";
		}
		else*/
		if (chkMatchingString(input_zip, output_zip) == false)
			status = "13";
		return status;
	}
	
	
}
