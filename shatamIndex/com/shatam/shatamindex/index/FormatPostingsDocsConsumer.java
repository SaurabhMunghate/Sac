/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

abstract class FormatPostingsDocsConsumer {

	abstract FormatPostingsPositionsConsumer addDoc(int docID, int termDocFreq)
			throws IOException;

	abstract void finish() throws IOException;
}
