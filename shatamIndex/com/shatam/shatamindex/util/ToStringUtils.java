/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public final class ToStringUtils {

	private ToStringUtils() {
	}

	public static String boost(float boost) {
		if (boost != 1.0f) {
			return "^" + Float.toString(boost);
		} else
			return "";
	}

	public static void byteArray(StringBuilder buffer, byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			buffer.append("b[").append(i).append("]=").append(bytes[i]);
			if (i < bytes.length - 1) {
				buffer.append(',');
			}

		}
	}

}
