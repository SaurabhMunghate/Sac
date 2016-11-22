/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import com.shatam.shatamindex.search.NumericRangeQuery;
import com.shatam.shatamindex.search.PrefixQuery;
import com.shatam.shatamindex.search.TermRangeQuery;
import com.shatam.shatamindex.util.NumericUtils;

import java.util.Date;
import java.util.Calendar;

@Deprecated
public class DateField {

	private DateField() {
	}

	private static int DATE_LEN = Long.toString(
			1000L * 365 * 24 * 60 * 60 * 1000, Character.MAX_RADIX).length();

	public static String MIN_DATE_STRING() {
		return timeToString(0);
	}

	public static String MAX_DATE_STRING() {
		char[] buffer = new char[DATE_LEN];
		char c = Character.forDigit(Character.MAX_RADIX - 1,
				Character.MAX_RADIX);
		for (int i = 0; i < DATE_LEN; i++)
			buffer[i] = c;
		return new String(buffer);
	}

	public static String dateToString(Date date) {
		return timeToString(date.getTime());
	}

	public static String timeToString(long time) {
		if (time < 0)
			throw new RuntimeException("time '" + time
					+ "' is too early, must be >= 0");

		String s = Long.toString(time, Character.MAX_RADIX);

		if (s.length() > DATE_LEN)
			throw new RuntimeException("time '" + time
					+ "' is too late, length of string "
					+ "representation must be <= " + DATE_LEN);

		if (s.length() < DATE_LEN) {
			StringBuilder sb = new StringBuilder(s);
			while (sb.length() < DATE_LEN)
				sb.insert(0, 0);
			s = sb.toString();
		}

		return s;
	}

	public static long stringToTime(String s) {
		return Long.parseLong(s, Character.MAX_RADIX);
	}

	public static Date stringToDate(String s) {
		return new Date(stringToTime(s));
	}
}
