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

abstract class DocConsumer {
	abstract DocConsumerPerThread addThread(DocumentsWriterThreadState perThread)
			throws IOException;

	abstract void flush(final Collection<DocConsumerPerThread> threads,
			final SegmentWriteState state) throws IOException;

	abstract void abort();

	abstract boolean freeRAM();
}
