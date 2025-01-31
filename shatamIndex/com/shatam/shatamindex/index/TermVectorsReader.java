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
import java.util.Arrays;

import com.shatam.shatamindex.store.BufferedIndexInput;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.IOUtils;

class TermVectorsReader implements Cloneable, Closeable {

	static final int FORMAT_VERSION = 2;

	static final int FORMAT_VERSION2 = 3;

	static final int FORMAT_UTF8_LENGTH_IN_BYTES = 4;

	static final int FORMAT_CURRENT = FORMAT_UTF8_LENGTH_IN_BYTES;

	static final int FORMAT_SIZE = 4;

	static final byte STORE_POSITIONS_WITH_TERMVECTOR = 0x1;
	static final byte STORE_OFFSET_WITH_TERMVECTOR = 0x2;

	private FieldInfos fieldInfos;

	private IndexInput tvx;
	private IndexInput tvd;
	private IndexInput tvf;
	private int size;
	private int numTotalDocs;

	private int docStoreOffset;

	private final int format;

	TermVectorsReader(Directory d, String segment, FieldInfos fieldInfos)
			throws CorruptIndexException, IOException {
		this(d, segment, fieldInfos, BufferedIndexInput.BUFFER_SIZE);
	}

	TermVectorsReader(Directory d, String segment, FieldInfos fieldInfos,
			int readBufferSize) throws CorruptIndexException, IOException {
		this(d, segment, fieldInfos, readBufferSize, -1, 0);
	}

	TermVectorsReader(Directory d, String segment, FieldInfos fieldInfos,
			int readBufferSize, int docStoreOffset, int size)
			throws CorruptIndexException, IOException {
		boolean success = false;

		try {
			String idxName = IndexFileNames.segmentFileName(segment,
					IndexFileNames.VECTORS_INDEX_EXTENSION);
			tvx = d.openInput(idxName, readBufferSize);
			format = checkValidFormat(idxName, tvx);
			String fn = IndexFileNames.segmentFileName(segment,
					IndexFileNames.VECTORS_DOCUMENTS_EXTENSION);
			tvd = d.openInput(fn, readBufferSize);
			final int tvdFormat = checkValidFormat(fn, tvd);
			fn = IndexFileNames.segmentFileName(segment,
					IndexFileNames.VECTORS_FIELDS_EXTENSION);
			tvf = d.openInput(fn, readBufferSize);
			final int tvfFormat = checkValidFormat(fn, tvf);

			assert format == tvdFormat;
			assert format == tvfFormat;

			if (format >= FORMAT_VERSION2) {
				numTotalDocs = (int) (tvx.length() >> 4);
			} else {
				assert (tvx.length() - FORMAT_SIZE) % 8 == 0;
				numTotalDocs = (int) (tvx.length() >> 3);
			}

			if (-1 == docStoreOffset) {
				this.docStoreOffset = 0;
				this.size = numTotalDocs;
				assert size == 0 || numTotalDocs == size;
			} else {
				this.docStoreOffset = docStoreOffset;
				this.size = size;

				assert numTotalDocs >= size + docStoreOffset : "numTotalDocs="
						+ numTotalDocs + " size=" + size + " docStoreOffset="
						+ docStoreOffset;
			}

			this.fieldInfos = fieldInfos;
			success = true;
		} finally {

			if (!success) {
				close();
			}
		}
	}

	IndexInput getTvdStream() {
		return tvd;
	}

	IndexInput getTvfStream() {
		return tvf;
	}

	final private void seekTvx(final int docNum) throws IOException {
		if (format < FORMAT_VERSION2)
			tvx.seek((docNum + docStoreOffset) * 8L + FORMAT_SIZE);
		else
			tvx.seek((docNum + docStoreOffset) * 16L + FORMAT_SIZE);
	}

	boolean canReadRawDocs() {
		return format >= FORMAT_UTF8_LENGTH_IN_BYTES;
	}

	final void rawDocs(int[] tvdLengths, int[] tvfLengths, int startDocID,
			int numDocs) throws IOException {

		if (tvx == null) {
			Arrays.fill(tvdLengths, 0);
			Arrays.fill(tvfLengths, 0);
			return;
		}

		if (format < FORMAT_VERSION2)
			throw new IllegalStateException(
					"cannot read raw docs with older term vector formats");

		seekTvx(startDocID);

		long tvdPosition = tvx.readLong();
		tvd.seek(tvdPosition);

		long tvfPosition = tvx.readLong();
		tvf.seek(tvfPosition);

		long lastTvdPosition = tvdPosition;
		long lastTvfPosition = tvfPosition;

		int count = 0;
		while (count < numDocs) {
			final int docID = docStoreOffset + startDocID + count + 1;
			assert docID <= numTotalDocs;
			if (docID < numTotalDocs) {
				tvdPosition = tvx.readLong();
				tvfPosition = tvx.readLong();
			} else {
				tvdPosition = tvd.length();
				tvfPosition = tvf.length();
				assert count == numDocs - 1;
			}
			tvdLengths[count] = (int) (tvdPosition - lastTvdPosition);
			tvfLengths[count] = (int) (tvfPosition - lastTvfPosition);
			count++;
			lastTvdPosition = tvdPosition;
			lastTvfPosition = tvfPosition;
		}
	}

	private int checkValidFormat(String fn, IndexInput in)
			throws CorruptIndexException, IOException {
		int format = in.readInt();
		if (format > FORMAT_CURRENT) {
			throw new IndexFormatTooNewException(in, format, 1, FORMAT_CURRENT);
		}
		return format;
	}

	public void close() throws IOException {
		IOUtils.close(tvx, tvd, tvf);
	}

	int size() {
		return size;
	}

	public void get(int docNum, String field, TermVectorMapper mapper)
			throws IOException {
		if (tvx != null) {
			int fieldNumber = fieldInfos.fieldNumber(field);

			seekTvx(docNum);

			long tvdPosition = tvx.readLong();

			tvd.seek(tvdPosition);
			int fieldCount = tvd.readVInt();

			int number = 0;
			int found = -1;
			for (int i = 0; i < fieldCount; i++) {
				if (format >= FORMAT_VERSION)
					number = tvd.readVInt();
				else
					number += tvd.readVInt();

				if (number == fieldNumber)
					found = i;
			}

			if (found != -1) {

				long position;
				if (format >= FORMAT_VERSION2)
					position = tvx.readLong();
				else
					position = tvd.readVLong();
				for (int i = 1; i <= found; i++)
					position += tvd.readVLong();

				mapper.setDocumentNumber(docNum);
				readTermVector(field, position, mapper);
			} else {

			}
		} else {

		}
	}

	TermFreqVector get(int docNum, String field) throws IOException {

		ParallelArrayTermVectorMapper mapper = new ParallelArrayTermVectorMapper();
		get(docNum, field, mapper);

		return mapper.materializeVector();
	}

	final private String[] readFields(int fieldCount) throws IOException {
		int number = 0;
		String[] fields = new String[fieldCount];

		for (int i = 0; i < fieldCount; i++) {
			if (format >= FORMAT_VERSION)
				number = tvd.readVInt();
			else
				number += tvd.readVInt();

			fields[i] = fieldInfos.fieldName(number);
		}

		return fields;
	}

	final private long[] readTvfPointers(int fieldCount) throws IOException {

		long position;
		if (format >= FORMAT_VERSION2)
			position = tvx.readLong();
		else
			position = tvd.readVLong();

		long[] tvfPointers = new long[fieldCount];
		tvfPointers[0] = position;

		for (int i = 1; i < fieldCount; i++) {
			position += tvd.readVLong();
			tvfPointers[i] = position;
		}

		return tvfPointers;
	}

	TermFreqVector[] get(int docNum) throws IOException {
		TermFreqVector[] result = null;
		if (tvx != null) {

			seekTvx(docNum);
			long tvdPosition = tvx.readLong();

			tvd.seek(tvdPosition);
			int fieldCount = tvd.readVInt();

			if (fieldCount != 0) {
				final String[] fields = readFields(fieldCount);
				final long[] tvfPointers = readTvfPointers(fieldCount);
				result = readTermVectors(docNum, fields, tvfPointers);
			}
		} else {

		}
		return result;
	}

	public void get(int docNumber, TermVectorMapper mapper) throws IOException {

		if (tvx != null) {

			seekTvx(docNumber);
			long tvdPosition = tvx.readLong();

			tvd.seek(tvdPosition);
			int fieldCount = tvd.readVInt();

			if (fieldCount != 0) {
				final String[] fields = readFields(fieldCount);
				final long[] tvfPointers = readTvfPointers(fieldCount);
				mapper.setDocumentNumber(docNumber);
				readTermVectors(fields, tvfPointers, mapper);
			}
		} else {

		}
	}

	private SegmentTermVector[] readTermVectors(int docNum, String fields[],
			long tvfPointers[]) throws IOException {
		SegmentTermVector res[] = new SegmentTermVector[fields.length];
		for (int i = 0; i < fields.length; i++) {
			ParallelArrayTermVectorMapper mapper = new ParallelArrayTermVectorMapper();
			mapper.setDocumentNumber(docNum);
			readTermVector(fields[i], tvfPointers[i], mapper);
			res[i] = (SegmentTermVector) mapper.materializeVector();
		}
		return res;
	}

	private void readTermVectors(String fields[], long tvfPointers[],
			TermVectorMapper mapper) throws IOException {
		for (int i = 0; i < fields.length; i++) {
			readTermVector(fields[i], tvfPointers[i], mapper);
		}
	}

	private void readTermVector(String field, long tvfPointer,
			TermVectorMapper mapper) throws IOException {

		tvf.seek(tvfPointer);

		int numTerms = tvf.readVInt();

		if (numTerms == 0)
			return;

		boolean storePositions;
		boolean storeOffsets;

		if (format >= FORMAT_VERSION) {
			byte bits = tvf.readByte();
			storePositions = (bits & STORE_POSITIONS_WITH_TERMVECTOR) != 0;
			storeOffsets = (bits & STORE_OFFSET_WITH_TERMVECTOR) != 0;
		} else {
			tvf.readVInt();
			storePositions = false;
			storeOffsets = false;
		}
		mapper.setExpectations(field, numTerms, storeOffsets, storePositions);
		int start = 0;
		int deltaLength = 0;
		int totalLength = 0;
		byte[] byteBuffer;
		char[] charBuffer;
		final boolean preUTF8 = format < FORMAT_UTF8_LENGTH_IN_BYTES;

		if (preUTF8) {
			charBuffer = new char[10];
			byteBuffer = null;
		} else {
			charBuffer = null;
			byteBuffer = new byte[20];
		}

		for (int i = 0; i < numTerms; i++) {
			start = tvf.readVInt();
			deltaLength = tvf.readVInt();
			totalLength = start + deltaLength;

			final String term;

			if (preUTF8) {

				if (charBuffer.length < totalLength) {
					charBuffer = ArrayUtil.grow(charBuffer, totalLength);
				}
				tvf.readChars(charBuffer, start, deltaLength);
				term = new String(charBuffer, 0, totalLength);
			} else {

				if (byteBuffer.length < totalLength) {
					byteBuffer = ArrayUtil.grow(byteBuffer, totalLength);
				}
				tvf.readBytes(byteBuffer, start, deltaLength);
				term = new String(byteBuffer, 0, totalLength, "UTF-8");
			}
			int freq = tvf.readVInt();
			int[] positions = null;
			if (storePositions) {

				if (mapper.isIgnoringPositions() == false) {
					positions = new int[freq];
					int prevPosition = 0;
					for (int j = 0; j < freq; j++) {
						positions[j] = prevPosition + tvf.readVInt();
						prevPosition = positions[j];
					}
				} else {

					//
					for (int j = 0; j < freq; j++) {
						tvf.readVInt();
					}
				}
			}
			TermVectorOffsetInfo[] offsets = null;
			if (storeOffsets) {

				if (mapper.isIgnoringOffsets() == false) {
					offsets = new TermVectorOffsetInfo[freq];
					int prevOffset = 0;
					for (int j = 0; j < freq; j++) {
						int startOffset = prevOffset + tvf.readVInt();
						int endOffset = startOffset + tvf.readVInt();
						offsets[j] = new TermVectorOffsetInfo(startOffset,
								endOffset);
						prevOffset = endOffset;
					}
				} else {
					for (int j = 0; j < freq; j++) {
						tvf.readVInt();
						tvf.readVInt();
					}
				}
			}
			mapper.map(term, freq, offsets, positions);
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		final TermVectorsReader clone = (TermVectorsReader) super.clone();

		if (tvx != null && tvd != null && tvf != null) {
			clone.tvx = (IndexInput) tvx.clone();
			clone.tvd = (IndexInput) tvd.clone();
			clone.tvf = (IndexInput) tvf.clone();
		}

		return clone;
	}
}

class ParallelArrayTermVectorMapper extends TermVectorMapper {

	private String[] terms;
	private int[] termFreqs;
	private int positions[][];
	private TermVectorOffsetInfo offsets[][];
	private int currentPosition;
	private boolean storingOffsets;
	private boolean storingPositions;
	private String field;

	@Override
	public void setExpectations(String field, int numTerms,
			boolean storeOffsets, boolean storePositions) {
		this.field = field;
		terms = new String[numTerms];
		termFreqs = new int[numTerms];
		this.storingOffsets = storeOffsets;
		this.storingPositions = storePositions;
		if (storePositions)
			this.positions = new int[numTerms][];
		if (storeOffsets)
			this.offsets = new TermVectorOffsetInfo[numTerms][];
	}

	@Override
	public void map(String term, int frequency, TermVectorOffsetInfo[] offsets,
			int[] positions) {
		terms[currentPosition] = term;
		termFreqs[currentPosition] = frequency;
		if (storingOffsets) {
			this.offsets[currentPosition] = offsets;
		}
		if (storingPositions) {
			this.positions[currentPosition] = positions;
		}
		currentPosition++;
	}

	public TermFreqVector materializeVector() {
		SegmentTermVector tv = null;
		if (field != null && terms != null) {
			if (storingPositions || storingOffsets) {
				tv = new SegmentTermPositionVector(field, terms, termFreqs,
						positions, offsets);
			} else {
				tv = new SegmentTermVector(field, terms, termFreqs);
			}
		}
		return tv;
	}
}
