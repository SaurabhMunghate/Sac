/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

public interface TermPositions extends TermDocs {

	int nextPosition() throws IOException;

	int getPayloadLength();

	byte[] getPayload(byte[] data, int offset) throws IOException;

	public boolean isPayloadAvailable();

}
