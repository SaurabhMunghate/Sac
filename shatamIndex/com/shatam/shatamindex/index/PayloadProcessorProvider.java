/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.store.Directory;

public abstract class PayloadProcessorProvider {

	public static abstract class DirPayloadProcessor {

		public abstract PayloadProcessor getProcessor(Term term)
				throws IOException;

	}

	public static abstract class PayloadProcessor {

		public abstract int payloadLength() throws IOException;

		public abstract byte[] processPayload(byte[] payload, int start,
				int length) throws IOException;

	}

	public abstract DirPayloadProcessor getDirProcessor(Directory dir)
			throws IOException;

}
