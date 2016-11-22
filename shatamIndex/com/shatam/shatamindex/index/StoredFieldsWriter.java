/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.store.RAMOutputStream;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.RamUsageEstimator;

final class StoredFieldsWriter {

	FieldsWriter fieldsWriter;
	final DocumentsWriter docWriter;
	final FieldInfos fieldInfos;
	int lastDocID;

	PerDoc[] docFreeList = new PerDoc[1];
	int freeCount;

	public StoredFieldsWriter(DocumentsWriter docWriter, FieldInfos fieldInfos) {
		this.docWriter = docWriter;
		this.fieldInfos = fieldInfos;
	}

	public StoredFieldsWriterPerThread addThread(
			DocumentsWriter.DocState docState) throws IOException {
		return new StoredFieldsWriterPerThread(docState, this);
	}

	synchronized public void flush(SegmentWriteState state) throws IOException {
		if (state.numDocs > lastDocID) {
			initFieldsWriter();
			fill(state.numDocs);
		}

		if (fieldsWriter != null) {
			fieldsWriter.close();
			fieldsWriter = null;
			lastDocID = 0;

			String fieldsIdxName = IndexFileNames.segmentFileName(
					state.segmentName, IndexFileNames.FIELDS_INDEX_EXTENSION);
			if (4 + ((long) state.numDocs) * 8 != state.directory
					.fileLength(fieldsIdxName)) {
				throw new RuntimeException("after flush: fdx size mismatch: "
						+ state.numDocs + " docs vs "
						+ state.directory.fileLength(fieldsIdxName)
						+ " length in bytes of " + fieldsIdxName
						+ " file exists?="
						+ state.directory.fileExists(fieldsIdxName));
			}
		}
	}

	private synchronized void initFieldsWriter() throws IOException {
		if (fieldsWriter == null) {
			fieldsWriter = new FieldsWriter(docWriter.directory,
					docWriter.getSegment(), fieldInfos);
			lastDocID = 0;
		}
	}

	int allocCount;

	synchronized PerDoc getPerDoc() {
		if (freeCount == 0) {
			allocCount++;
			if (allocCount > docFreeList.length) {

				assert allocCount == 1 + docFreeList.length;
				docFreeList = new PerDoc[ArrayUtil.oversize(allocCount,
						RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
			}
			return new PerDoc();
		} else {
			return docFreeList[--freeCount];
		}
	}

	synchronized void abort() {
		if (fieldsWriter != null) {
			fieldsWriter.abort();
			fieldsWriter = null;
			lastDocID = 0;
		}
	}

	void fill(int docID) throws IOException {

		while (lastDocID < docID) {
			fieldsWriter.skipDocument();
			lastDocID++;
		}
	}

	synchronized void finishDocument(PerDoc perDoc) throws IOException {
		assert docWriter.writer
				.testPoint("StoredFieldsWriter.finishDocument start");
		initFieldsWriter();

		fill(perDoc.docID);

		fieldsWriter.flushDocument(perDoc.numStoredFields, perDoc.fdt);
		lastDocID++;
		perDoc.reset();
		free(perDoc);
		assert docWriter.writer
				.testPoint("StoredFieldsWriter.finishDocument end");
	}

	synchronized void free(PerDoc perDoc) {
		assert freeCount < docFreeList.length;
		assert 0 == perDoc.numStoredFields;
		assert 0 == perDoc.fdt.length();
		assert 0 == perDoc.fdt.getFilePointer();
		docFreeList[freeCount++] = perDoc;
	}

	class PerDoc extends DocumentsWriter.DocWriter {
		final DocumentsWriter.PerDocBuffer buffer = docWriter.newPerDocBuffer();
		RAMOutputStream fdt = new RAMOutputStream(buffer);
		int numStoredFields;

		void reset() {
			fdt.reset();
			buffer.recycle();
			numStoredFields = 0;
		}

		@Override
		void abort() {
			reset();
			free(this);
		}

		@Override
		public long sizeInBytes() {
			return buffer.getSizeInBytes();
		}

		@Override
		public void finish() throws IOException {
			finishDocument(this);
		}
	}
}
