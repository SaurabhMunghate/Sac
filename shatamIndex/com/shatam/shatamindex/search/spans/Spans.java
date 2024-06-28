/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import java.io.IOException;
import java.util.Collection;

public abstract class Spans {

	public abstract boolean next() throws IOException;

	public abstract boolean skipTo(int target) throws IOException;

	public abstract int doc();

	public abstract int start();

	public abstract int end();

	public abstract Collection<byte[]> getPayload() throws IOException;

	public abstract boolean isPayloadAvailable();

}
