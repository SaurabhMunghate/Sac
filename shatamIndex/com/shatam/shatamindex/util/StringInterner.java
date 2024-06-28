/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public class StringInterner {

	public String intern(String s) {
		return s.intern();
	}

	public String intern(char[] arr, int offset, int len) {
		return intern(new String(arr, offset, len));
	}
}
