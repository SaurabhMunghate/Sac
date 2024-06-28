/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.io.Serializable;

public abstract class FieldComparatorSource implements Serializable {

	public abstract FieldComparator newComparator(String fieldname,
			int numHits, int sortPos, boolean reversed) throws IOException;
}
