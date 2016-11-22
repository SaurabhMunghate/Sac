/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.Collection;
import java.util.Map;
import java.io.IOException;

abstract class InvertedDocConsumer {

	abstract InvertedDocConsumerPerThread addThread(
			DocInverterPerThread docInverterPerThread);

	abstract void abort();

	abstract void flush(
			Map<InvertedDocConsumerPerThread, Collection<InvertedDocConsumerPerField>> threadsAndFields,
			SegmentWriteState state) throws IOException;

	abstract boolean freeRAM();

	FieldInfos fieldInfos;

	void setFieldInfos(FieldInfos fieldInfos) {
		this.fieldInfos = fieldInfos;
	}
}
