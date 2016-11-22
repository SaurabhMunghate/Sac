/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.io.Closeable;

public abstract class TermEnum implements Closeable {

	public abstract boolean next() throws IOException;

	public abstract Term term();

	public abstract int docFreq();

	public abstract void close() throws IOException;
}
