/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex;

public final class ShatamPackage {

	private ShatamPackage() {
	}

	public static Package get() {
		return ShatamPackage.class.getPackage();
	}
}
