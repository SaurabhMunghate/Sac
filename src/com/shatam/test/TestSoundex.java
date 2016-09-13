/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.test;

import org.apache.commons.codec.language.Metaphone;

import com.shatam.util.U;

public class TestSoundex {
	public static void main(String[] ar) {

		Metaphone SNDX = new Metaphone();
		String s = SNDX.encode("Khobragade");
		U.log(s);
	}
}
