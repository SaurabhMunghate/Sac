/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import com.shatam.shatamindex.document.NumericField;
import com.shatam.shatamindex.search.NumericRangeQuery;
import com.shatam.shatamindex.util.NumericUtils;

@Deprecated
public class NumberTools {

	private static final int RADIX = 36;

	private static final char NEGATIVE_PREFIX = '-';

	private static final char POSITIVE_PREFIX = '0';

	public static final String MIN_STRING_VALUE = NEGATIVE_PREFIX
			+ "0000000000000";

	public static final String MAX_STRING_VALUE = POSITIVE_PREFIX
			+ "1y2p0ij32e8e7";

	public static final int STR_SIZE = MIN_STRING_VALUE.length();

	public static String longToString(long l) {

		if (l == Long.MIN_VALUE) {

			return MIN_STRING_VALUE;
		}

		StringBuilder buf = new StringBuilder(STR_SIZE);

		if (l < 0) {
			buf.append(NEGATIVE_PREFIX);
			l = Long.MAX_VALUE + l + 1;
		} else {
			buf.append(POSITIVE_PREFIX);
		}
		String num = Long.toString(l, RADIX);

		int padLen = STR_SIZE - num.length() - buf.length();
		while (padLen-- > 0) {
			buf.append('0');
		}
		buf.append(num);

		return buf.toString();
	}

	public static long stringToLong(String str) {
		if (str == null) {
			throw new NullPointerException("string cannot be null");
		}
		if (str.length() != STR_SIZE) {
			throw new NumberFormatException("string is the wrong size");
		}

		if (str.equals(MIN_STRING_VALUE)) {
			return Long.MIN_VALUE;
		}

		char prefix = str.charAt(0);
		long l = Long.parseLong(str.substring(1), RADIX);

		if (prefix == POSITIVE_PREFIX) {

		} else if (prefix == NEGATIVE_PREFIX) {
			l = l - Long.MAX_VALUE - 1;
		} else {
			throw new NumberFormatException(
					"string does not begin with the correct prefix");
		}

		return l;
	}
}