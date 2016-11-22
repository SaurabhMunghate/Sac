/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.PrintStream;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.util.BitVector;

public class SegmentWriteState {
	public final PrintStream infoStream;
	public final Directory directory;
	public final String segmentName;
	public final FieldInfos fieldInfos;
	public final int numDocs;
	public boolean hasVectors;

	public final BufferedDeletes segDeletes;

	public BitVector deletedDocs;

	public final int termIndexInterval;

	public final int skipInterval = 16;

	public final int maxSkipLevels = 10;

	public SegmentWriteState(PrintStream infoStream, Directory directory,
			String segmentName, FieldInfos fieldInfos, int numDocs,
			int termIndexInterval, BufferedDeletes segDeletes) {
		this.infoStream = infoStream;
		this.segDeletes = segDeletes;
		this.directory = directory;
		this.segmentName = segmentName;
		this.fieldInfos = fieldInfos;
		this.numDocs = numDocs;
		this.termIndexInterval = termIndexInterval;
	}
}
