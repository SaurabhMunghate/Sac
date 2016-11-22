/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

@SuppressWarnings("dep-ann")
public enum Version {

	@Deprecated
	SHATAM_20,

	@Deprecated
	SHATAM_21,

	@Deprecated
	SHATAM_22,

	@Deprecated
	SHATAM_23,

	@Deprecated
	SHATAM_24,

	@Deprecated
	SHATAM_29,

	SHATAM_30,

	SHATAM_31,

	SHATAM_32,

	SHATAM_33,

	SHATAM_34,

	SHATAM_35,

	@Deprecated
	SHATAM_CURRENT;

	public boolean onOrAfter(Version other) {
		return compareTo(other) >= 0;
	}
}