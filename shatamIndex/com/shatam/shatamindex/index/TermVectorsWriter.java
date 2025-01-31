/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.StringHelper;
import com.shatam.shatamindex.util.UnicodeUtil;

import java.io.IOException;

final class TermVectorsWriter {

	private IndexOutput tvx = null, tvd = null, tvf = null;
	private FieldInfos fieldInfos;
	final UnicodeUtil.UTF8Result[] utf8Results = new UnicodeUtil.UTF8Result[] {
			new UnicodeUtil.UTF8Result(), new UnicodeUtil.UTF8Result() };

	public TermVectorsWriter(Directory directory, String segment,
			FieldInfos fieldInfos) throws IOException {
		boolean success = false;
		try {

			tvx = directory.createOutput(IndexFileNames.segmentFileName(
					segment, IndexFileNames.VECTORS_INDEX_EXTENSION));
			tvx.writeInt(TermVectorsReader.FORMAT_CURRENT);
			tvd = directory.createOutput(IndexFileNames.segmentFileName(
					segment, IndexFileNames.VECTORS_DOCUMENTS_EXTENSION));
			tvd.writeInt(TermVectorsReader.FORMAT_CURRENT);
			tvf = directory.createOutput(IndexFileNames.segmentFileName(
					segment, IndexFileNames.VECTORS_FIELDS_EXTENSION));
			tvf.writeInt(TermVectorsReader.FORMAT_CURRENT);
			success = true;
		} finally {
			if (!success) {
				IOUtils.closeWhileHandlingException(tvx, tvd, tvf);
			}
		}

		this.fieldInfos = fieldInfos;
	}

	public final void addAllDocVectors(TermFreqVector[] vectors)
			throws IOException {

		tvx.writeLong(tvd.getFilePointer());
		tvx.writeLong(tvf.getFilePointer());

		if (vectors != null) {
			final int numFields = vectors.length;
			tvd.writeVInt(numFields);

			long[] fieldPointers = new long[numFields];

			for (int i = 0; i < numFields; i++) {
				fieldPointers[i] = tvf.getFilePointer();

				final int fieldNumber = fieldInfos.fieldNumber(vectors[i]
						.getField());

				tvd.writeVInt(fieldNumber);

				final int numTerms = vectors[i].size();
				tvf.writeVInt(numTerms);

				final TermPositionVector tpVector;

				final byte bits;
				final boolean storePositions;
				final boolean storeOffsets;

				if (vectors[i] instanceof TermPositionVector) {

					tpVector = (TermPositionVector) vectors[i];
					storePositions = tpVector.size() > 0
							&& tpVector.getTermPositions(0) != null;
					storeOffsets = tpVector.size() > 0
							&& tpVector.getOffsets(0) != null;
					bits = (byte) ((storePositions ? TermVectorsReader.STORE_POSITIONS_WITH_TERMVECTOR
							: 0) + (storeOffsets ? TermVectorsReader.STORE_OFFSET_WITH_TERMVECTOR
							: 0));
				} else {
					tpVector = null;
					bits = 0;
					storePositions = false;
					storeOffsets = false;
				}

				tvf.writeVInt(bits);

				final String[] terms = vectors[i].getTerms();
				final int[] freqs = vectors[i].getTermFrequencies();

				int utf8Upto = 0;
				utf8Results[1].length = 0;

				for (int j = 0; j < numTerms; j++) {

					UnicodeUtil.UTF16toUTF8(terms[j], 0, terms[j].length(),
							utf8Results[utf8Upto]);

					int start = StringHelper.bytesDifference(
							utf8Results[1 - utf8Upto].result,
							utf8Results[1 - utf8Upto].length,
							utf8Results[utf8Upto].result,
							utf8Results[utf8Upto].length);
					int length = utf8Results[utf8Upto].length - start;
					tvf.writeVInt(start);
					tvf.writeVInt(length);
					tvf.writeBytes(utf8Results[utf8Upto].result, start, length);
					utf8Upto = 1 - utf8Upto;

					final int termFreq = freqs[j];

					tvf.writeVInt(termFreq);

					if (storePositions) {
						final int[] positions = tpVector.getTermPositions(j);
						if (positions == null)
							throw new IllegalStateException(
									"Trying to write positions that are null!");
						assert positions.length == termFreq;

						int lastPosition = 0;
						for (int k = 0; k < positions.length; k++) {
							final int position = positions[k];
							tvf.writeVInt(position - lastPosition);
							lastPosition = position;
						}
					}

					if (storeOffsets) {
						final TermVectorOffsetInfo[] offsets = tpVector
								.getOffsets(j);
						if (offsets == null)
							throw new IllegalStateException(
									"Trying to write offsets that are null!");
						assert offsets.length == termFreq;

						int lastEndOffset = 0;
						for (int k = 0; k < offsets.length; k++) {
							final int startOffset = offsets[k].getStartOffset();
							final int endOffset = offsets[k].getEndOffset();
							tvf.writeVInt(startOffset - lastEndOffset);
							tvf.writeVInt(endOffset - startOffset);
							lastEndOffset = endOffset;
						}
					}
				}
			}

			if (numFields > 1) {
				long lastFieldPointer = fieldPointers[0];
				for (int i = 1; i < numFields; i++) {
					final long fieldPointer = fieldPointers[i];
					tvd.writeVLong(fieldPointer - lastFieldPointer);
					lastFieldPointer = fieldPointer;
				}
			}
		} else
			tvd.writeVInt(0);
	}

	final void addRawDocuments(TermVectorsReader reader, int[] tvdLengths,
			int[] tvfLengths, int numDocs) throws IOException {
		long tvdPosition = tvd.getFilePointer();
		long tvfPosition = tvf.getFilePointer();
		long tvdStart = tvdPosition;
		long tvfStart = tvfPosition;
		for (int i = 0; i < numDocs; i++) {
			tvx.writeLong(tvdPosition);
			tvdPosition += tvdLengths[i];
			tvx.writeLong(tvfPosition);
			tvfPosition += tvfLengths[i];
		}
		tvd.copyBytes(reader.getTvdStream(), tvdPosition - tvdStart);
		tvf.copyBytes(reader.getTvfStream(), tvfPosition - tvfStart);
		assert tvd.getFilePointer() == tvdPosition;
		assert tvf.getFilePointer() == tvfPosition;
	}

	final void close() throws IOException {

		IOUtils.close(tvx, tvd, tvf);
	}
}
