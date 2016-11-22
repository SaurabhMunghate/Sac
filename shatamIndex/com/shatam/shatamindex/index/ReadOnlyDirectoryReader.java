/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.Directory;

import java.io.IOException;
import java.util.Map;
import java.util.Collection;

class ReadOnlyDirectoryReader extends DirectoryReader {
	ReadOnlyDirectoryReader(Directory directory, SegmentInfos sis,
			IndexDeletionPolicy deletionPolicy, int termInfosIndexDivisor,
			Collection<ReaderFinishedListener> readerFinishedListeners)
			throws IOException {
		super(directory, sis, deletionPolicy, true, termInfosIndexDivisor,
				readerFinishedListeners);
	}

	ReadOnlyDirectoryReader(Directory directory, SegmentInfos infos,
			SegmentReader[] oldReaders, int[] oldStarts,
			Map<String, byte[]> oldNormsCache, boolean doClone,
			int termInfosIndexDivisor,
			Collection<ReaderFinishedListener> readerFinishedListeners)
			throws IOException {
		super(directory, infos, oldReaders, oldStarts, oldNormsCache, true,
				doClone, termInfosIndexDivisor, readerFinishedListeners);
	}

	ReadOnlyDirectoryReader(IndexWriter writer, SegmentInfos infos,
			int termInfosIndexDivisor, boolean applyAllDeletes)
			throws IOException {
		super(writer, infos, termInfosIndexDivisor, applyAllDeletes);
	}

	@Override
	protected void acquireWriteLock() {
		ReadOnlySegmentReader.noWrite();
	}
}
