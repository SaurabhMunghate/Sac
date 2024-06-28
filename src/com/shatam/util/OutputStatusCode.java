/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.List;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

public class OutputStatusCode {

	public static void main(String[] args) {

	}

	public static boolean chkMatchingString(String s1, String s2) {

		JaroWinkler algorithm = new JaroWinkler();
		if (algorithm.getSimilarity(s1.toLowerCase(), s2.toLowerCase()) != 1.0) {
			return false;
		}
		return true;
	}

	public static String getStatusCode(List list, String fAddress,
			String fCity, String fZip) {
		String matchingStatus = "";
		ShatamIndexQueryStruct shatamIndexQueryStruct = (ShatamIndexQueryStruct) list
				.get(0);

		String unitNumber = (String) list.get(2);

		String foundStreet;
		String foundCity;
		String foundZip;
		foundStreet = fAddress;
		foundCity = fCity;
		foundZip = fZip;

		String inputCity = shatamIndexQueryStruct.getCity();
		String inputZip = shatamIndexQueryStruct.getZip();
		String inputAddress = (String) list.get(3);

		if (inputAddress == null)
			inputAddress = "";
		if (inputZip == null)
			inputZip = "";
		if (inputCity == null)
			inputCity = "";

		if (foundStreet.toLowerCase().contains("no match found")) {
			matchingStatus = "14";
			return matchingStatus;
		}
		if (street1StatusCode(inputAddress, foundStreet) != null) {
			if (matchingStatus.trim().length() > 1) {
				matchingStatus += ",";
			}
			matchingStatus += street1StatusCode(
					inputAddress + " " + unitNumber, foundStreet);
		}

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

	public static String street1StatusCode(String input_address,
			String output_address) {
		String status = null;

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
		String status = null;
		input_address = AbbrReplacement.getFullAddress(input_address, "");
		if (chkMatchingString(input_address, output_address) == false)
			status = "11";
		return status;
	}

	public static String cityStatusCode(String input_city, String output_city) {
		String status = null;
		if (chkMatchingString(input_city, output_city) == false)
			status = "15";
		return status;
	}

	public static String zipStatusCode(String input_zip, String output_zip) {
		String status = null;
		if (chkMatchingString(input_zip, output_zip) == false)
			status = "13";
		return status;
	}

}
