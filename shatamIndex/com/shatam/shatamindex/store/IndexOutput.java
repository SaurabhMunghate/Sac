/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.io.Closeable;

public abstract class IndexOutput extends DataOutput implements Closeable {

	public abstract void flush() throws IOException;

	public abstract void close() throws IOException;

	public abstract long getFilePointer();

	public abstract void seek(long pos) throws IOException;

	public abstract long length() throws IOException;

	public void setLength(long length) throws IOException {
	}
}
