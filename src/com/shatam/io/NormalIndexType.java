/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.io;

public class NormalIndexType extends AbstractIndexType {
	String fielName = null;

	public NormalIndexType(String field) {
		fielName = field;
	}

	public String getFieldName() {
		return fielName;

	}

	public String innerEncode(String v) {
		return v;
	}

}