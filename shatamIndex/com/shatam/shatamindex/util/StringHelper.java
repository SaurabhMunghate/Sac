/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.Comparator;
import java.util.StringTokenizer;

public abstract class StringHelper {

	public static StringInterner interner = new SimpleStringInterner(1024, 8);

	public static String intern(String s) {
		return interner.intern(s);
	}

	public static final int bytesDifference(byte[] bytes1, int len1,
			byte[] bytes2, int len2) {
		int len = len1 < len2 ? len1 : len2;
		for (int i = 0; i < len; i++)
			if (bytes1[i] != bytes2[i])
				return i;
		return len;
	}

	private StringHelper() {
	}

	public static Comparator<String> getVersionComparator() {
		return versionComparator;
	}

	private static Comparator<String> versionComparator = new Comparator<String>() {
		public int compare(String a, String b) {
			StringTokenizer aTokens = new StringTokenizer(a, ".");
			StringTokenizer bTokens = new StringTokenizer(b, ".");

			while (aTokens.hasMoreTokens()) {
				int aToken = Integer.parseInt(aTokens.nextToken());
				if (bTokens.hasMoreTokens()) {
					int bToken = Integer.parseInt(bTokens.nextToken());
					if (aToken != bToken) {
						return aToken < bToken ? -1 : 1;
					}
				} else {

					if (aToken != 0) {
						return 1;
					}
				}
			}

			while (bTokens.hasMoreTokens()) {
				if (Integer.parseInt(bTokens.nextToken()) != 0)
					return -1;
			}

			return 0;
		}
	};
}
