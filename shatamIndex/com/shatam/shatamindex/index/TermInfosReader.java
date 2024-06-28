/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Closeable;
import java.io.IOException;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.util.BytesRef;
import com.shatam.shatamindex.util.CloseableThreadLocal;
import com.shatam.shatamindex.util.DoubleBarrelLRUCache;

final class TermInfosReader implements Closeable {
	private final Directory directory;
	private final String segment;
	private final FieldInfos fieldInfos;

	private final CloseableThreadLocal<ThreadResources> threadResources = new CloseableThreadLocal<ThreadResources>();
	private final SegmentTermEnum origEnum;
	private final long size;

	private final TermInfosReaderIndex index;
	private final int indexLength;

	private final int totalIndexInterval;

	private final static int DEFAULT_CACHE_SIZE = 1024;

	private final static class TermInfoAndOrd extends TermInfo {
		final long termOrd;

		public TermInfoAndOrd(TermInfo ti, long termOrd) {
			super(ti);
			assert termOrd >= 0;
			this.termOrd = termOrd;
		}
	}

	private static class CloneableTerm extends
			DoubleBarrelLRUCache.CloneableKey {
		private final Term term;

		public CloneableTerm(Term t) {
			this.term = new Term(t.field(), t.text());
		}

		@Override
		public Object clone() {
			return new CloneableTerm(term);
		}

		@Override
		public boolean equals(Object _other) {
			CloneableTerm other = (CloneableTerm) _other;
			return term.equals(other.term);
		}

		@Override
		public int hashCode() {
			return term.hashCode();
		}
	}

	private final DoubleBarrelLRUCache<CloneableTerm, TermInfoAndOrd> termsCache = new DoubleBarrelLRUCache<CloneableTerm, TermInfoAndOrd>(
			DEFAULT_CACHE_SIZE);

	private static final class ThreadResources {
		SegmentTermEnum termEnum;
	}

	TermInfosReader(Directory dir, String seg, FieldInfos fis,
			int readBufferSize, int indexDivisor) throws CorruptIndexException,
			IOException {
		boolean success = false;

		if (indexDivisor < 1 && indexDivisor != -1) {
			throw new IllegalArgumentException(
					"indexDivisor must be -1 (don't load terms index) or greater than 0: got "
							+ indexDivisor);
		}

		try {
			directory = dir;
			segment = seg;
			fieldInfos = fis;

			origEnum = new SegmentTermEnum(directory.openInput(IndexFileNames
					.segmentFileName(segment, IndexFileNames.TERMS_EXTENSION),
					readBufferSize), fieldInfos, false);
			size = origEnum.size;

			if (indexDivisor != -1) {

				totalIndexInterval = origEnum.indexInterval * indexDivisor;
				final String indexFileName = IndexFileNames.segmentFileName(
						segment, IndexFileNames.TERMS_INDEX_EXTENSION);
				final SegmentTermEnum indexEnum = new SegmentTermEnum(
						directory.openInput(indexFileName, readBufferSize),
						fieldInfos, true);
				try {
					index = new TermInfosReaderIndex(indexEnum, indexDivisor,
							dir.fileLength(indexFileName), totalIndexInterval);
					indexLength = index.length();
				} finally {
					indexEnum.close();
				}
			} else {

				totalIndexInterval = -1;
				index = null;
				indexLength = -1;
			}
			success = true;
		} finally {

			if (!success) {
				close();
			}
		}
	}

	public int getSkipInterval() {
		return origEnum.skipInterval;
	}

	public int getMaxSkipLevels() {
		return origEnum.maxSkipLevels;
	}

	public final void close() throws IOException {
		if (origEnum != null)
			origEnum.close();
		threadResources.close();
	}

	final long size() {
		return size;
	}

	private ThreadResources getThreadResources() {
		ThreadResources resources = threadResources.get();
		if (resources == null) {
			resources = new ThreadResources();
			resources.termEnum = terms();
			threadResources.set(resources);
		}
		return resources;
	}

	TermInfo get(Term term) throws IOException {
		BytesRef termBytesRef = new BytesRef(term.text);
		return get(term, false, termBytesRef);
	}

	private TermInfo get(Term term, boolean mustSeekEnum, BytesRef termBytesRef)
			throws IOException {
		if (size == 0)
			return null;

		ensureIndexIsRead();

		final CloneableTerm cacheKey = new CloneableTerm(term);

		TermInfoAndOrd tiOrd = termsCache.get(cacheKey);
		ThreadResources resources = getThreadResources();

		if (!mustSeekEnum && tiOrd != null) {
			return tiOrd;
		}

		SegmentTermEnum enumerator = resources.termEnum;
		if (enumerator.term() != null
				&& ((enumerator.prev() != null && term.compareTo(enumerator
						.prev()) > 0) || term.compareTo(enumerator.term()) >= 0)) {
			int enumOffset = (int) (enumerator.position / totalIndexInterval) + 1;
			if (indexLength == enumOffset
					|| index.compareTo(term, termBytesRef, enumOffset) < 0) {

				final TermInfo ti;

				int numScans = enumerator.scanTo(term);
				if (enumerator.term() != null
						&& term.compareTo(enumerator.term()) == 0) {
					ti = enumerator.termInfo();
					if (numScans > 1) {

						if (tiOrd == null) {
							termsCache.put(cacheKey, new TermInfoAndOrd(ti,
									enumerator.position));
						} else {
							assert sameTermInfo(ti, tiOrd, enumerator);
							assert (int) enumerator.position == tiOrd.termOrd;
						}
					}
				} else {
					ti = null;
				}

				return ti;
			}
		}

		final int indexPos;
		if (tiOrd != null) {
			indexPos = (int) (tiOrd.termOrd / totalIndexInterval);
		} else {

			indexPos = index.getIndexOffset(term, termBytesRef);
		}

		index.seekEnum(enumerator, indexPos);
		enumerator.scanTo(term);
		final TermInfo ti;
		if (enumerator.term() != null && term.compareTo(enumerator.term()) == 0) {
			ti = enumerator.termInfo();
			if (tiOrd == null) {
				termsCache.put(cacheKey, new TermInfoAndOrd(ti,
						enumerator.position));
			} else {
				assert sameTermInfo(ti, tiOrd, enumerator);
				assert enumerator.position == tiOrd.termOrd;
			}
		} else {
			ti = null;
		}
		return ti;
	}

	private final boolean sameTermInfo(TermInfo ti1, TermInfo ti2,
			SegmentTermEnum enumerator) {
		if (ti1.docFreq != ti2.docFreq) {
			return false;
		}
		if (ti1.freqPointer != ti2.freqPointer) {
			return false;
		}
		if (ti1.proxPointer != ti2.proxPointer) {
			return false;
		}

		if (ti1.docFreq >= enumerator.skipInterval
				&& ti1.skipOffset != ti2.skipOffset) {
			return false;
		}
		return true;
	}

	private void ensureIndexIsRead() {
		if (index == null) {
			throw new IllegalStateException(
					"terms index was not loaded when this reader was created");
		}
	}

	final long getPosition(Term term) throws IOException {
		if (size == 0)
			return -1;

		ensureIndexIsRead();
		BytesRef termBytesRef = new BytesRef(term.text);
		int indexOffset = index.getIndexOffset(term, termBytesRef);

		SegmentTermEnum enumerator = getThreadResources().termEnum;
		index.seekEnum(enumerator, indexOffset);

		while (term.compareTo(enumerator.term()) > 0 && enumerator.next()) {
		}

		if (term.compareTo(enumerator.term()) == 0)
			return enumerator.position;
		else
			return -1;
	}

	public SegmentTermEnum terms() {
		return (SegmentTermEnum) origEnum.clone();
	}

	public SegmentTermEnum terms(Term term) throws IOException {
		BytesRef termBytesRef = new BytesRef(term.text);
		get(term, true, termBytesRef);
		return (SegmentTermEnum) getThreadResources().termEnum.clone();
	}
}
