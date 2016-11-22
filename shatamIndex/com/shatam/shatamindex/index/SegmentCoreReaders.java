/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.IOUtils;

final class SegmentCoreReaders {

	private final AtomicInteger ref = new AtomicInteger(1);

	final String segment;
	final FieldInfos fieldInfos;
	final IndexInput freqStream;
	final IndexInput proxStream;
	final TermInfosReader tisNoIndex;

	final Directory dir;
	final Directory cfsDir;
	final int readBufferSize;
	final int termsIndexDivisor;

	private final SegmentReader owner;

	TermInfosReader tis;
	FieldsReader fieldsReaderOrig;
	TermVectorsReader termVectorsReaderOrig;
	CompoundFileReader cfsReader;
	CompoundFileReader storeCFSReader;

	SegmentCoreReaders(SegmentReader owner, Directory dir, SegmentInfo si,
			int readBufferSize, int termsIndexDivisor) throws IOException {
		segment = si.name;
		this.readBufferSize = readBufferSize;
		this.dir = dir;

		boolean success = false;

		try {
			Directory dir0 = dir;
			if (si.getUseCompoundFile()) {
				cfsReader = new CompoundFileReader(dir,
						IndexFileNames.segmentFileName(segment,
								IndexFileNames.COMPOUND_FILE_EXTENSION),
						readBufferSize);
				dir0 = cfsReader;
			}
			cfsDir = dir0;

			fieldInfos = new FieldInfos(cfsDir, IndexFileNames.segmentFileName(
					segment, IndexFileNames.FIELD_INFOS_EXTENSION));

			this.termsIndexDivisor = termsIndexDivisor;
			TermInfosReader reader = new TermInfosReader(cfsDir, segment,
					fieldInfos, readBufferSize, termsIndexDivisor);
			if (termsIndexDivisor == -1) {
				tisNoIndex = reader;
			} else {
				tis = reader;
				tisNoIndex = null;
			}

			freqStream = cfsDir.openInput(IndexFileNames.segmentFileName(
					segment, IndexFileNames.FREQ_EXTENSION), readBufferSize);

			if (fieldInfos.hasProx()) {
				proxStream = cfsDir
						.openInput(IndexFileNames.segmentFileName(segment,
								IndexFileNames.PROX_EXTENSION), readBufferSize);
			} else {
				proxStream = null;
			}
			success = true;
		} finally {
			if (!success) {
				decRef();
			}
		}

		this.owner = owner;
	}

	synchronized TermVectorsReader getTermVectorsReaderOrig() {
		return termVectorsReaderOrig;
	}

	synchronized FieldsReader getFieldsReaderOrig() {
		return fieldsReaderOrig;
	}

	synchronized void incRef() {
		ref.incrementAndGet();
	}

	synchronized Directory getCFSReader() {
		return cfsReader;
	}

	synchronized TermInfosReader getTermsReader() {
		if (tis != null) {
			return tis;
		} else {
			return tisNoIndex;
		}
	}

	synchronized boolean termsIndexIsLoaded() {
		return tis != null;
	}

	synchronized void loadTermsIndex(SegmentInfo si, int termsIndexDivisor)
			throws IOException {
		if (tis == null) {
			Directory dir0;
			if (si.getUseCompoundFile()) {

				if (cfsReader == null) {
					cfsReader = new CompoundFileReader(dir,
							IndexFileNames.segmentFileName(segment,
									IndexFileNames.COMPOUND_FILE_EXTENSION),
							readBufferSize);
				}
				dir0 = cfsReader;
			} else {
				dir0 = dir;
			}

			tis = new TermInfosReader(dir0, segment, fieldInfos,
					readBufferSize, termsIndexDivisor);
		}
	}

	synchronized void decRef() throws IOException {

		if (ref.decrementAndGet() == 0) {
			IOUtils.close(tis, tisNoIndex, freqStream, proxStream,
					termVectorsReaderOrig, fieldsReaderOrig, cfsReader,
					storeCFSReader);
			tis = null;

			if (owner != null) {
				owner.notifyReaderFinishedListeners();
			}
		}
	}

	synchronized void openDocStores(SegmentInfo si) throws IOException {

		assert si.name.equals(segment);

		if (fieldsReaderOrig == null) {
			final Directory storeDir;
			if (si.getDocStoreOffset() != -1) {
				if (si.getDocStoreIsCompoundFile()) {
					assert storeCFSReader == null;
					storeCFSReader = new CompoundFileReader(
							dir,
							IndexFileNames.segmentFileName(
									si.getDocStoreSegment(),
									IndexFileNames.COMPOUND_FILE_STORE_EXTENSION),
							readBufferSize);
					storeDir = storeCFSReader;
					assert storeDir != null;
				} else {
					storeDir = dir;
					assert storeDir != null;
				}
			} else if (si.getUseCompoundFile()) {

				if (cfsReader == null) {
					cfsReader = new CompoundFileReader(dir,
							IndexFileNames.segmentFileName(segment,
									IndexFileNames.COMPOUND_FILE_EXTENSION),
							readBufferSize);
				}
				storeDir = cfsReader;
				assert storeDir != null;
			} else {
				storeDir = dir;
				assert storeDir != null;
			}

			final String storesSegment;
			if (si.getDocStoreOffset() != -1) {
				storesSegment = si.getDocStoreSegment();
			} else {
				storesSegment = segment;
			}

			fieldsReaderOrig = new FieldsReader(storeDir, storesSegment,
					fieldInfos, readBufferSize, si.getDocStoreOffset(),
					si.docCount);

			if (si.getDocStoreOffset() == -1
					&& fieldsReaderOrig.size() != si.docCount) {
				throw new CorruptIndexException(
						"doc counts differ for segment " + segment
								+ ": fieldsReader shows "
								+ fieldsReaderOrig.size()
								+ " but segmentInfo shows " + si.docCount);
			}

			if (si.getHasVectors()) {
				termVectorsReaderOrig = new TermVectorsReader(storeDir,
						storesSegment, fieldInfos, readBufferSize,
						si.getDocStoreOffset(), si.docCount);
			}
		}
	}

	@Override
	public String toString() {
		return "SegmentCoreReader(owner=" + owner + ")";
	}
}
