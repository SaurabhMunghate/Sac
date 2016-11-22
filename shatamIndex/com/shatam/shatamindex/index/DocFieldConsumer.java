/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

abstract class DocFieldConsumer {

	FieldInfos fieldInfos;

	abstract void flush(
			Map<DocFieldConsumerPerThread, Collection<DocFieldConsumerPerField>> threadsAndFields,
			SegmentWriteState state) throws IOException;

	abstract void abort();

	abstract DocFieldConsumerPerThread addThread(
			DocFieldProcessorPerThread docFieldProcessorPerThread)
			throws IOException;

	abstract boolean freeRAM();

	void setFieldInfos(FieldInfos fieldInfos) {
		this.fieldInfos = fieldInfos;
	}
}
