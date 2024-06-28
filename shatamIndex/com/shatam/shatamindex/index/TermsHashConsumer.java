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

abstract class TermsHashConsumer {
	abstract TermsHashConsumerPerThread addThread(TermsHashPerThread perThread);

	abstract void flush(
			Map<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> threadsAndFields,
			final SegmentWriteState state) throws IOException;

	abstract void abort();

	FieldInfos fieldInfos;

	void setFieldInfos(FieldInfos fieldInfos) {
		this.fieldInfos = fieldInfos;
	}
}
