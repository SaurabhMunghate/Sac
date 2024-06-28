/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.document.Fieldable;

final class DocFieldProcessorPerField {

	final DocFieldConsumerPerField consumer;
	final FieldInfo fieldInfo;

	DocFieldProcessorPerField next;
	int lastGen = -1;

	int fieldCount;
	Fieldable[] fields = new Fieldable[1];

	public DocFieldProcessorPerField(
			final DocFieldProcessorPerThread perThread,
			final FieldInfo fieldInfo) {
		this.consumer = perThread.consumer.addField(fieldInfo);
		this.fieldInfo = fieldInfo;
	}

	public void abort() {
		consumer.abort();
	}
}
