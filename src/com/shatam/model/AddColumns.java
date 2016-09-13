/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.model;

public enum AddColumns {

	TLID, PREDIRABRV, PREQUALABR, PRETYPABRV, NAME, SUFTYPABRV, SUFDIRABRV, SUFQUALABR, CITY, ZIP, FULLNAME, LFROMADD, LTOADD, RFROMADD, RTOADD, GEO, DATA, COUNTYNO, DATASOURCE;

	public static boolean useInQuery(AddColumns col) {
		if (col == PREDIRABRV)
			return true;
		if (col == PREQUALABR)
			return true;
		if (col == PRETYPABRV)
			return true;
		if (col == NAME)
			return true;
		if (col == SUFTYPABRV)
			return true;
		if (col == SUFDIRABRV)
			return true;
		if (col == SUFQUALABR)
			return true;
		if (col == CITY)
			return true;
		if (col == ZIP)
			return true;

		return false;
	}

}