/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
import com.shatam.io.AbstractIndexType;
import com.shatam.shatamindex.queryParser.QueryParser;
import com.shatam.shatamindex.search.Query;

public class ShatamIndexQueryStruct {

	private String houseNumber = "";
	private String query = null;
	private String address = null;
	private String city = null;
	private String zip = null;
	private String state = null;
	private AbstractIndexType indexType;

	private String normalizedStreetName;
	private String normalizedCity;
	private String normalizedZip;

	public void setHouseNumber(String hn) {
		houseNumber = hn;
	}

	public String getHouseNumber() {
		return houseNumber;
	}

	public String getQuery() {
		return query;
	}

	public String getAddress() {
		return address;
	}

	public String getState() {
		return state;
	}

	public AbstractIndexType getIndexType() {
		return indexType;
	}

	public ShatamIndexQueryStruct(String add, String city, String zip,
			String state2, AbstractIndexType indexType2) {

		address = add;
		this.city = city;
		this.zip = zip;
		state = state2;
		indexType = indexType2;
	}

	public ShatamIndexQueryStruct() {

	}

	public Query createQueryObj(QueryParser parser) throws Exception {
		Query q = null;
		synchronized (parser) {
			try {

				q = parser.parse(query);

				if (q == null) {
					U.log("OMG queryyyyy@@@@@=null");
				}

			} catch (Exception e) {
				U.log("q=======================" + q);
				U.log("ERROR:" + e);
				U.log("ERROR : queryString:" + query);
				U.log("ERROR : address:" + address);

			}
		}

		return q;
	}

	public void setQuery(String q) throws Exception {

		this.query = q;

	}

	public String getCity() {

		return city;
	}

	public String getZip() {

		return zip;
	}

	public void setNormalizedStreetName(String stName) {
		this.normalizedStreetName = stName;

	}

	public void setNormalizedCity(String city) {
		this.normalizedCity = city;

	}

	public void setNormalizedZip(String zip) {
		this.normalizedZip = zip;

	}

	public String getNormalizedStreetName() {
		return normalizedStreetName;
	}

	public String getNormalizedCity() {
		return normalizedCity;
	}

	public String getNormalizedZip() {
		return normalizedZip;
	}

}