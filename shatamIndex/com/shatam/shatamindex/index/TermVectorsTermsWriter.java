/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.store.RAMOutputStream;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.RamUsageEstimator;

import java.io.IOException;
import java.util.Collection;

import java.util.Map;

final class TermVectorsTermsWriter extends TermsHashConsumer {

	final DocumentsWriter docWriter;
	PerDoc[] docFreeList = new PerDoc[1];
	int freeCount;
	IndexOutput tvx;
	IndexOutput tvd;
	IndexOutput tvf;
	int lastDocID;
	boolean hasVectors;

	public TermVectorsTermsWriter(DocumentsWriter docWriter) {
		this.docWriter = docWriter;
	}

	@Override
	public TermsHashConsumerPerThread addThread(
			TermsHashPerThread termsHashPerThread) {
		return new TermVectorsTermsWriterPerThread(termsHashPerThread, this);
	}

	@Override
	synchronized void flush(
			Map<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> threadsAndFields,
			final SegmentWriteState state) throws IOException {
		if (tvx != null) {

			fill(state.numDocs);
			IOUtils.close(tvx, tvf, tvd);
			tvx = tvd = tvf = null;
			assert state.segmentName != null;
			String idxName = IndexFileNames.segmentFileName(state.segmentName,
					IndexFileNames.VECTORS_INDEX_EXTENSION);
			if (4 + ((long) state.numDocs) * 16 != state.directory
					.fileLength(idxName)) {
				throw new RuntimeException("after flush: tvx size mismatch: "
						+ state.numDocs + " docs vs "
						+ state.directory.fileLength(idxName)
						+ " length in bytes of " + idxName + " file exists?="
						+ state.directory.fileExists(idxName));
			}

			lastDocID = 0;
			state.hasVectors = hasVectors;
			hasVectors = false;
		}

		for (Map.Entry<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> entry : threadsAndFields
				.entrySet()) {
			for (final TermsHashConsumerPerField field : entry.getValue()) {
				TermVectorsTermsWriterPerField perField = (TermVectorsTermsWriterPerField) field;
				perField.termsHashPerField.reset();
				perField.shrinkHash();
			}

			TermVectorsTermsWriterPerThread perThread = (TermVectorsTermsWriterPerThread) entry
					.getKey();
			perThread.termsHashPerThread.reset(true);
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

	void fill(int docID) throws IOException {
		if (lastDocID < docID) {
			final long tvfPosition = tvf.getFilePointer();
			while (lastDocID < docID) {
				tvx.writeLong(tvd.getFilePointer());
				tvd.writeVInt(0);
				tvx.writeLong(tvfPosition);
				lastDocID++;
			}
		}
	}

	synchronized void initTermVectorsWriter() throws IOException {
		if (tvx == null) {
			boolean success = false;
			try {

				hasVectors = true;
				tvx = docWriter.directory.createOutput(IndexFileNames
						.segmentFileName(docWriter.getSegment(),
								IndexFileNames.VECTORS_INDEX_EXTENSION));
				tvd = docWriter.directory.createOutput(IndexFileNames
						.segmentFileName(docWriter.getSegment(),
								IndexFileNames.VECTORS_DOCUMENTS_EXTENSION));
				tvf = docWriter.directory.createOutput(IndexFileNames
						.segmentFileName(docWriter.getSegment(),
								IndexFileNames.VECTORS_FIELDS_EXTENSION));

				tvx.writeInt(TermVectorsReader.FORMAT_CURRENT);
				tvd.writeInt(TermVectorsReader.FORMAT_CURRENT);
				tvf.writeInt(TermVectorsReader.FORMAT_CURRENT);
				success = true;
			} finally {
				if (!success) {
					IOUtils.closeWhileHandlingException(tvx, tvd, tvf);
				}
			}
			lastDocID = 0;
		}
	}

	synchronized void finishDocument(PerDoc perDoc) throws IOException {

		assert docWriter.writer
				.testPoint("TermVectorsTermsWriter.finishDocument start");

		initTermVectorsWriter();

		fill(perDoc.docID);

		tvx.writeLong(tvd.getFilePointer());
		tvx.writeLong(tvf.getFilePointer());
		tvd.writeVInt(perDoc.numVectorFields);
		if (perDoc.numVectorFields > 0) {
			for (int i = 0; i < perDoc.numVectorFields; i++) {
				tvd.writeVInt(perDoc.fieldNumbers[i]);
			}
			assert 0 == perDoc.fieldPointers[0];
			long lastPos = perDoc.fieldPointers[0];
			for (int i = 1; i < perDoc.numVectorFields; i++) {
				long pos = perDoc.fieldPointers[i];
				tvd.writeVLong(pos - lastPos);
				lastPos = pos;
			}
			perDoc.perDocTvf.writeTo(tvf);
			perDoc.numVectorFields = 0;
		}

		assert lastDocID == perDoc.docID : "lastDocID=" + lastDocID
				+ " perDoc.docID=" + perDoc.docID;

		lastDocID++;

		perDoc.reset();
		free(perDoc);
		assert docWriter.writer
				.testPoint("TermVectorsTermsWriter.finishDocument end");
	}

	@Override
	public void abort() {
		hasVectors = false;
		try {
			IOUtils.closeWhileHandlingException(tvx, tvd, tvf);
		} catch (IOException e) {

			throw new RuntimeException(e);
		}

		try {
			docWriter.directory.deleteFile(IndexFileNames.segmentFileName(
					docWriter.getSegment(),
					IndexFileNames.VECTORS_INDEX_EXTENSION));
		} catch (IOException ignored) {
		}

		try {
			docWriter.directory.deleteFile(IndexFileNames.segmentFileName(
					docWriter.getSegment(),
					IndexFileNames.VECTORS_DOCUMENTS_EXTENSION));
		} catch (IOException ignored) {
		}

		try {
			docWriter.directory.deleteFile(IndexFileNames.segmentFileName(
					docWriter.getSegment(),
					IndexFileNames.VECTORS_FIELDS_EXTENSION));
		} catch (IOException ignored) {
		}

		tvx = tvd = tvf = null;
		lastDocID = 0;
	}

	synchronized void free(PerDoc doc) {
		assert freeCount < docFreeList.length;
		docFreeList[freeCount++] = doc;
	}

	class PerDoc extends DocumentsWriter.DocWriter {

		final DocumentsWriter.PerDocBuffer buffer = docWriter.newPerDocBuffer();
		RAMOutputStream perDocTvf = new RAMOutputStream(buffer);

		int numVectorFields;

		int[] fieldNumbers = new int[1];
		long[] fieldPointers = new long[1];

		void reset() {
			perDocTvf.reset();
			buffer.recycle();
			numVectorFields = 0;
		}

		@Override
		void abort() {
			reset();
			free(this);
		}

		void addField(final int fieldNumber) {
			if (numVectorFields == fieldNumbers.length) {
				fieldNumbers = ArrayUtil.grow(fieldNumbers);
			}
			if (numVectorFields == fieldPointers.length) {
				fieldPointers = ArrayUtil.grow(fieldPointers);
			}
			fieldNumbers[numVectorFields] = fieldNumber;
			fieldPointers[numVectorFields] = perDocTvf.getFilePointer();
			numVectorFields++;
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
