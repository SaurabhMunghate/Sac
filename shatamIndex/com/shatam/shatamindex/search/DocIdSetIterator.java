/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

public abstract class DocIdSetIterator {

	public static final int NO_MORE_DOCS = Integer.MAX_VALUE;

	public abstract int docID();

	public abstract int nextDoc() throws IOException;

	public abstract int advance(int target) throws IOException;

}
