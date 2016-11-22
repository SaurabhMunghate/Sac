/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.index.IndexReader.FieldOption;
import com.shatam.shatamindex.index.MergePolicy.MergeAbortedException;
import com.shatam.shatamindex.index.PayloadProcessorProvider.PayloadProcessor;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.ReaderUtil;

final class SegmentMerger {
	private Directory directory;
	private String segment;
	private int termIndexInterval = IndexWriterConfig.DEFAULT_TERM_INDEX_INTERVAL;

	private List<IndexReader> readers = new ArrayList<IndexReader>();
	private final FieldInfos fieldInfos;

	private int mergedDocs;

	private final CheckAbort checkAbort;

	private final static int MAX_RAW_MERGE_DOCS = 4192;

	private SegmentWriteState segmentWriteState;

	private final PayloadProcessorProvider payloadProcessorProvider;

	SegmentMerger(Directory dir, int termIndexInterval, String name,
			MergePolicy.OneMerge merge,
			PayloadProcessorProvider payloadProcessorProvider,
			FieldInfos fieldInfos) {
		this.payloadProcessorProvider = payloadProcessorProvider;
		directory = dir;
		this.fieldInfos = fieldInfos;
		segment = name;
		if (merge != null) {
			checkAbort = new CheckAbort(merge, directory);
		} else {
			checkAbort = new CheckAbort(null, null) {
				@Override
				public void work(double units) throws MergeAbortedException {

				}
			};
		}
		this.termIndexInterval = termIndexInterval;
	}

	public FieldInfos fieldInfos() {
		return fieldInfos;
	}

	final void add(IndexReader reader) {
		ReaderUtil.gatherSubReaders(readers, reader);
	}

	final int merge() throws CorruptIndexException, IOException {

		mergedDocs = mergeFields();
		mergeTerms();
		mergeNorms();

		if (fieldInfos.hasVectors())
			mergeVectors();

		return mergedDocs;
	}

	final Collection<String> createCompoundFile(String fileName,
			final SegmentInfo info) throws IOException {

		Collection<String> files = info.files();
		CompoundFileWriter cfsWriter = new CompoundFileWriter(directory,
				fileName, checkAbort);
		for (String file : files) {
			assert !IndexFileNames.matchesExtension(file,
					IndexFileNames.DELETES_EXTENSION) : ".del file is not allowed in .cfs: "
					+ file;
			assert !IndexFileNames.isSeparateNormsFile(file) : "separate norms file (.s[0-9]+) is not allowed in .cfs: "
					+ file;
			cfsWriter.addFile(file);
		}

		cfsWriter.close();

		return files;
	}

	private static void addIndexed(IndexReader reader, FieldInfos fInfos,
			Collection<String> names, boolean storeTermVectors,
			boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector, boolean storePayloads,
			IndexOptions indexOptions) throws IOException {
		for (String field : names) {
			fInfos.add(field, true, storeTermVectors,
					storePositionWithTermVector, storeOffsetWithTermVector,
					!reader.hasNorms(field), storePayloads, indexOptions);
		}
	}

	private SegmentReader[] matchingSegmentReaders;
	private int[] rawDocLengths;
	private int[] rawDocLengths2;
	private int matchedCount;

	public int getMatchedSubReaderCount() {
		return matchedCount;
	}

	private void setMatchingSegmentReaders() {

		int numReaders = readers.size();
		matchingSegmentReaders = new SegmentReader[numReaders];

		for (int i = 0; i < numReaders; i++) {
			IndexReader reader = readers.get(i);
			if (reader instanceof SegmentReader) {
				SegmentReader segmentReader = (SegmentReader) reader;
				boolean same = true;
				FieldInfos segmentFieldInfos = segmentReader.fieldInfos();
				int numFieldInfos = segmentFieldInfos.size();
				for (int j = 0; j < numFieldInfos; j++) {
					if (!fieldInfos.fieldName(j).equals(
							segmentFieldInfos.fieldName(j))) {
						same = false;
						break;
					}
				}
				if (same) {
					matchingSegmentReaders[i] = segmentReader;
					matchedCount++;
				}
			}
		}

		rawDocLengths = new int[MAX_RAW_MERGE_DOCS];
		rawDocLengths2 = new int[MAX_RAW_MERGE_DOCS];
	}

	private int mergeFields() throws CorruptIndexException, IOException {

		for (IndexReader reader : readers) {
			if (reader instanceof SegmentReader) {
				SegmentReader segmentReader = (SegmentReader) reader;
				FieldInfos readerFieldInfos = segmentReader.fieldInfos();
				int numReaderFieldInfos = readerFieldInfos.size();
				for (int j = 0; j < numReaderFieldInfos; j++) {
					fieldInfos.add(readerFieldInfos.fieldInfo(j));
				}
			} else {
				addIndexed(
						reader,
						fieldInfos,
						reader.getFieldNames(FieldOption.TERMVECTOR_WITH_POSITION_OFFSET),
						true, true, true, false,
						IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				addIndexed(
						reader,
						fieldInfos,
						reader.getFieldNames(FieldOption.TERMVECTOR_WITH_POSITION),
						true, true, false, false,
						IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				addIndexed(
						reader,
						fieldInfos,
						reader.getFieldNames(FieldOption.TERMVECTOR_WITH_OFFSET),
						true, false, true, false,
						IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				addIndexed(reader, fieldInfos,
						reader.getFieldNames(FieldOption.TERMVECTOR), true,
						false, false, false,
						IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				addIndexed(reader, fieldInfos,
						reader.getFieldNames(FieldOption.OMIT_POSITIONS),
						false, false, false, false, IndexOptions.DOCS_AND_FREQS);
				addIndexed(
						reader,
						fieldInfos,
						reader.getFieldNames(FieldOption.OMIT_TERM_FREQ_AND_POSITIONS),
						false, false, false, false, IndexOptions.DOCS_ONLY);
				addIndexed(reader, fieldInfos,
						reader.getFieldNames(FieldOption.STORES_PAYLOADS),
						false, false, false, true,
						IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				addIndexed(reader, fieldInfos,
						reader.getFieldNames(FieldOption.INDEXED), false,
						false, false, false,
						IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				fieldInfos.add(reader.getFieldNames(FieldOption.UNINDEXED),
						false);
			}
		}
		fieldInfos.write(directory, segment + ".fnm");

		int docCount = 0;

		setMatchingSegmentReaders();

		final FieldsWriter fieldsWriter = new FieldsWriter(directory, segment,
				fieldInfos);

		try {
			int idx = 0;
			for (IndexReader reader : readers) {
				final SegmentReader matchingSegmentReader = matchingSegmentReaders[idx++];
				FieldsReader matchingFieldsReader = null;
				if (matchingSegmentReader != null) {
					final FieldsReader fieldsReader = matchingSegmentReader
							.getFieldsReader();
					if (fieldsReader != null && fieldsReader.canReadRawDocs()) {
						matchingFieldsReader = fieldsReader;
					}
				}
				if (reader.hasDeletions()) {
					docCount += copyFieldsWithDeletions(fieldsWriter, reader,
							matchingFieldsReader);
				} else {
					docCount += copyFieldsNoDeletions(fieldsWriter, reader,
							matchingFieldsReader);
				}
			}
		} finally {
			fieldsWriter.close();
		}

		final String fileName = IndexFileNames.segmentFileName(segment,
				IndexFileNames.FIELDS_INDEX_EXTENSION);
		final long fdxFileLength = directory.fileLength(fileName);

		if (4 + ((long) docCount) * 8 != fdxFileLength)

			throw new RuntimeException(
					"mergeFields produced an invalid result: docCount is "
							+ docCount
							+ " but fdx file size is "
							+ fdxFileLength
							+ " file="
							+ fileName
							+ " file exists?="
							+ directory.fileExists(fileName)
							+ "; now aborting this merge to prevent index corruption");

		segmentWriteState = new SegmentWriteState(null, directory, segment,
				fieldInfos, docCount, termIndexInterval, null);
		return docCount;
	}

	private int copyFieldsWithDeletions(final FieldsWriter fieldsWriter,
			final IndexReader reader, final FieldsReader matchingFieldsReader)
			throws IOException, MergeAbortedException, CorruptIndexException {
		int docCount = 0;
		final int maxDoc = reader.maxDoc();
		if (matchingFieldsReader != null) {

			for (int j = 0; j < maxDoc;) {
				if (reader.isDeleted(j)) {

					++j;
					continue;
				}

				int start = j, numDocs = 0;
				do {
					j++;
					numDocs++;
					if (j >= maxDoc)
						break;
					if (reader.isDeleted(j)) {
						j++;
						break;
					}
				} while (numDocs < MAX_RAW_MERGE_DOCS);

				IndexInput stream = matchingFieldsReader.rawDocs(rawDocLengths,
						start, numDocs);
				fieldsWriter.addRawDocuments(stream, rawDocLengths, numDocs);
				docCount += numDocs;
				checkAbort.work(300 * numDocs);
			}
		} else {
			for (int j = 0; j < maxDoc; j++) {
				if (reader.isDeleted(j)) {

					continue;
				}

				Document doc = reader.document(j);
				fieldsWriter.addDocument(doc);
				docCount++;
				checkAbort.work(300);
			}
		}
		return docCount;
	}

	private int copyFieldsNoDeletions(final FieldsWriter fieldsWriter,
			final IndexReader reader, final FieldsReader matchingFieldsReader)
			throws IOException, MergeAbortedException, CorruptIndexException {
		final int maxDoc = reader.maxDoc();
		int docCount = 0;
		if (matchingFieldsReader != null) {

			while (docCount < maxDoc) {
				int len = Math.min(MAX_RAW_MERGE_DOCS, maxDoc - docCount);
				IndexInput stream = matchingFieldsReader.rawDocs(rawDocLengths,
						docCount, len);
				fieldsWriter.addRawDocuments(stream, rawDocLengths, len);
				docCount += len;
				checkAbort.work(300 * len);
			}
		} else {
			for (; docCount < maxDoc; docCount++) {

				Document doc = reader.document(docCount);
				fieldsWriter.addDocument(doc);
				checkAbort.work(300);
			}
		}
		return docCount;
	}

	private final void mergeVectors() throws IOException {
		TermVectorsWriter termVectorsWriter = new TermVectorsWriter(directory,
				segment, fieldInfos);

		try {
			int idx = 0;
			for (final IndexReader reader : readers) {
				final SegmentReader matchingSegmentReader = matchingSegmentReaders[idx++];
				TermVectorsReader matchingVectorsReader = null;
				if (matchingSegmentReader != null) {
					TermVectorsReader vectorsReader = matchingSegmentReader
							.getTermVectorsReader();

					if (vectorsReader != null && vectorsReader.canReadRawDocs()) {
						matchingVectorsReader = vectorsReader;
					}
				}
				if (reader.hasDeletions()) {
					copyVectorsWithDeletions(termVectorsWriter,
							matchingVectorsReader, reader);
				} else {
					copyVectorsNoDeletions(termVectorsWriter,
							matchingVectorsReader, reader);

				}
			}
		} finally {
			termVectorsWriter.close();
		}

		final String fileName = IndexFileNames.segmentFileName(segment,
				IndexFileNames.VECTORS_INDEX_EXTENSION);
		final long tvxSize = directory.fileLength(fileName);

		if (4 + ((long) mergedDocs) * 16 != tvxSize)

			throw new RuntimeException(
					"mergeVectors produced an invalid result: mergedDocs is "
							+ mergedDocs
							+ " but tvx size is "
							+ tvxSize
							+ " file="
							+ fileName
							+ " file exists?="
							+ directory.fileExists(fileName)
							+ "; now aborting this merge to prevent index corruption");
	}

	private void copyVectorsWithDeletions(
			final TermVectorsWriter termVectorsWriter,
			final TermVectorsReader matchingVectorsReader,
			final IndexReader reader) throws IOException, MergeAbortedException {
		final int maxDoc = reader.maxDoc();
		if (matchingVectorsReader != null) {

			for (int docNum = 0; docNum < maxDoc;) {
				if (reader.isDeleted(docNum)) {

					++docNum;
					continue;
				}

				int start = docNum, numDocs = 0;
				do {
					docNum++;
					numDocs++;
					if (docNum >= maxDoc)
						break;
					if (reader.isDeleted(docNum)) {
						docNum++;
						break;
					}
				} while (numDocs < MAX_RAW_MERGE_DOCS);

				matchingVectorsReader.rawDocs(rawDocLengths, rawDocLengths2,
						start, numDocs);
				termVectorsWriter.addRawDocuments(matchingVectorsReader,
						rawDocLengths, rawDocLengths2, numDocs);
				checkAbort.work(300 * numDocs);
			}
		} else {
			for (int docNum = 0; docNum < maxDoc; docNum++) {
				if (reader.isDeleted(docNum)) {

					continue;
				}

				TermFreqVector[] vectors = reader.getTermFreqVectors(docNum);
				termVectorsWriter.addAllDocVectors(vectors);
				checkAbort.work(300);
			}
		}
	}

	private void copyVectorsNoDeletions(
			final TermVectorsWriter termVectorsWriter,
			final TermVectorsReader matchingVectorsReader,
			final IndexReader reader) throws IOException, MergeAbortedException {
		final int maxDoc = reader.maxDoc();
		if (matchingVectorsReader != null) {

			int docCount = 0;
			while (docCount < maxDoc) {
				int len = Math.min(MAX_RAW_MERGE_DOCS, maxDoc - docCount);
				matchingVectorsReader.rawDocs(rawDocLengths, rawDocLengths2,
						docCount, len);
				termVectorsWriter.addRawDocuments(matchingVectorsReader,
						rawDocLengths, rawDocLengths2, len);
				docCount += len;
				checkAbort.work(300 * len);
			}
		} else {
			for (int docNum = 0; docNum < maxDoc; docNum++) {

				TermFreqVector[] vectors = reader.getTermFreqVectors(docNum);
				termVectorsWriter.addAllDocVectors(vectors);
				checkAbort.work(300);
			}
		}
	}

	private SegmentMergeQueue queue = null;

	private final void mergeTerms() throws CorruptIndexException, IOException {

		final FormatPostingsFieldsConsumer fieldsConsumer = new FormatPostingsFieldsWriter(
				segmentWriteState, fieldInfos);

		try {
			queue = new SegmentMergeQueue(readers.size());

			mergeTermInfos(fieldsConsumer);

		} finally {
			try {
				fieldsConsumer.finish();
			} finally {
				if (queue != null) {
					queue.close();
				}
			}
		}
	}

	IndexOptions indexOptions;

	private final void mergeTermInfos(
			final FormatPostingsFieldsConsumer consumer)
			throws CorruptIndexException, IOException {
		int base = 0;
		final int readerCount = readers.size();
		for (int i = 0; i < readerCount; i++) {
			IndexReader reader = readers.get(i);
			TermEnum termEnum = reader.terms();
			SegmentMergeInfo smi = new SegmentMergeInfo(base, termEnum, reader);
			if (payloadProcessorProvider != null) {
				smi.dirPayloadProcessor = payloadProcessorProvider
						.getDirProcessor(reader.directory());
			}
			int[] docMap = smi.getDocMap();
			if (docMap != null) {
				if (docMaps == null) {
					docMaps = new int[readerCount][];
				}
				docMaps[i] = docMap;
			}

			base += reader.numDocs();

			assert reader.numDocs() == reader.maxDoc() - smi.delCount;

			if (smi.next())
				queue.add(smi);
			else
				smi.close();
		}

		SegmentMergeInfo[] match = new SegmentMergeInfo[readers.size()];

		String currentField = null;
		FormatPostingsTermsConsumer termsConsumer = null;

		while (queue.size() > 0) {
			int matchSize = 0;
			match[matchSize++] = queue.pop();
			Term term = match[0].term;
			SegmentMergeInfo top = queue.top();

			while (top != null && term.compareTo(top.term) == 0) {
				match[matchSize++] = queue.pop();
				top = queue.top();
			}

			if (currentField != term.field) {
				currentField = term.field;
				if (termsConsumer != null)
					termsConsumer.finish();
				final FieldInfo fieldInfo = fieldInfos.fieldInfo(currentField);
				termsConsumer = consumer.addField(fieldInfo);
				indexOptions = fieldInfo.indexOptions;
			}

			int df = appendPostings(termsConsumer, match, matchSize);

			checkAbort.work(df / 3.0);

			while (matchSize > 0) {
				SegmentMergeInfo smi = match[--matchSize];
				if (smi.next())
					queue.add(smi);
				else
					smi.close();
			}
		}
	}

	private byte[] payloadBuffer;
	private int[][] docMaps;

	private final int appendPostings(
			final FormatPostingsTermsConsumer termsConsumer,
			SegmentMergeInfo[] smis, int n) throws CorruptIndexException,
			IOException {

		final FormatPostingsDocsConsumer docConsumer = termsConsumer
				.addTerm(smis[0].term.text);
		int df = 0;
		for (int i = 0; i < n; i++) {
			SegmentMergeInfo smi = smis[i];
			TermPositions postings = smi.getPositions();
			assert postings != null;
			int base = smi.base;
			int[] docMap = smi.getDocMap();
			postings.seek(smi.termEnum);

			PayloadProcessor payloadProcessor = null;
			if (smi.dirPayloadProcessor != null) {
				payloadProcessor = smi.dirPayloadProcessor
						.getProcessor(smi.term);
			}

			while (postings.next()) {
				df++;
				int doc = postings.doc();
				if (docMap != null)
					doc = docMap[doc];
				doc += base;

				final int freq = postings.freq();
				final FormatPostingsPositionsConsumer posConsumer = docConsumer
						.addDoc(doc, freq);

				if (indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
					for (int j = 0; j < freq; j++) {
						final int position = postings.nextPosition();
						int payloadLength = postings.getPayloadLength();
						if (payloadLength > 0) {
							if (payloadBuffer == null
									|| payloadBuffer.length < payloadLength)
								payloadBuffer = new byte[payloadLength];
							postings.getPayload(payloadBuffer, 0);
							if (payloadProcessor != null) {
								payloadBuffer = payloadProcessor
										.processPayload(payloadBuffer, 0,
												payloadLength);
								payloadLength = payloadProcessor
										.payloadLength();
							}
						}
						posConsumer.addPosition(position, payloadBuffer, 0,
								payloadLength);
					}
					posConsumer.finish();
				}
			}
		}
		docConsumer.finish();

		return df;
	}

	public boolean getAnyNonBulkMerges() {
		assert matchedCount <= readers.size();
		return matchedCount != readers.size();
	}

	private void mergeNorms() throws IOException {

		int bufferSize = 0;
		for (IndexReader reader : readers) {
			bufferSize = Math.max(bufferSize, reader.maxDoc());
		}

		byte[] normBuffer = null;
		IndexOutput output = null;
		boolean success = false;
		try {
			int numFieldInfos = fieldInfos.size();
			for (int i = 0; i < numFieldInfos; i++) {
				FieldInfo fi = fieldInfos.fieldInfo(i);
				if (fi.isIndexed && !fi.omitNorms) {
					if (output == null) {
						output = directory.createOutput(IndexFileNames
								.segmentFileName(segment,
										IndexFileNames.NORMS_EXTENSION));
						output.writeBytes(SegmentNorms.NORMS_HEADER,
								SegmentNorms.NORMS_HEADER.length);
					}
					if (normBuffer == null) {
						normBuffer = new byte[bufferSize];
					}
					for (IndexReader reader : readers) {
						final int maxDoc = reader.maxDoc();
						reader.norms(fi.name, normBuffer, 0);
						if (!reader.hasDeletions()) {

							output.writeBytes(normBuffer, maxDoc);
						} else {

							for (int k = 0; k < maxDoc; k++) {
								if (!reader.isDeleted(k)) {
									output.writeByte(normBuffer[k]);
								}
							}
						}
						checkAbort.work(maxDoc);
					}
				}
			}
			success = true;
		} finally {
			if (success) {
				IOUtils.close(output);
			} else {
				IOUtils.closeWhileHandlingException(output);
			}
		}
	}

	static class CheckAbort {
		private double workCount;
		private MergePolicy.OneMerge merge;
		private Directory dir;

		public CheckAbort(MergePolicy.OneMerge merge, Directory dir) {
			this.merge = merge;
			this.dir = dir;
		}

		public void work(double units) throws MergePolicy.MergeAbortedException {
			workCount += units;
			if (workCount >= 10000.0) {
				merge.checkAborted(dir);
				workCount = 0;
			}
		}
	}

}
