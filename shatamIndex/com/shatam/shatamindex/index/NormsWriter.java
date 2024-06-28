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
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.IOUtils;

final class NormsWriter extends InvertedDocEndConsumer {

	private final byte defaultNorm = Similarity.getDefault().encodeNormValue(
			1.0f);
	private FieldInfos fieldInfos;

	@Override
	public InvertedDocEndConsumerPerThread addThread(
			DocInverterPerThread docInverterPerThread) {
		return new NormsWriterPerThread(docInverterPerThread, this);
	}

	@Override
	public void abort() {
	}

	void files(Collection<String> files) {
	}

	@Override
	void setFieldInfos(FieldInfos fieldInfos) {
		this.fieldInfos = fieldInfos;
	}

	@Override
	public void flush(
			Map<InvertedDocEndConsumerPerThread, Collection<InvertedDocEndConsumerPerField>> threadsAndFields,
			SegmentWriteState state) throws IOException {

		final Map<FieldInfo, List<NormsWriterPerField>> byField = new HashMap<FieldInfo, List<NormsWriterPerField>>();

		for (final Map.Entry<InvertedDocEndConsumerPerThread, Collection<InvertedDocEndConsumerPerField>> entry : threadsAndFields
				.entrySet()) {
			final Collection<InvertedDocEndConsumerPerField> fields = entry
					.getValue();
			final Iterator<InvertedDocEndConsumerPerField> fieldsIt = fields
					.iterator();

			while (fieldsIt.hasNext()) {
				final NormsWriterPerField perField = (NormsWriterPerField) fieldsIt
						.next();

				if (perField.upto > 0) {

					List<NormsWriterPerField> l = byField
							.get(perField.fieldInfo);
					if (l == null) {
						l = new ArrayList<NormsWriterPerField>();
						byField.put(perField.fieldInfo, l);
					}
					l.add(perField);
				} else

					fieldsIt.remove();
			}
		}

		final String normsFileName = IndexFileNames.segmentFileName(
				state.segmentName, IndexFileNames.NORMS_EXTENSION);
		IndexOutput normsOut = state.directory.createOutput(normsFileName);
		boolean success = false;
		try {
			normsOut.writeBytes(SegmentNorms.NORMS_HEADER, 0,
					SegmentNorms.NORMS_HEADER.length);

			final int numField = fieldInfos.size();

			int normCount = 0;

			for (int fieldNumber = 0; fieldNumber < numField; fieldNumber++) {

				final FieldInfo fieldInfo = fieldInfos.fieldInfo(fieldNumber);

				List<NormsWriterPerField> toMerge = byField.get(fieldInfo);
				int upto = 0;
				if (toMerge != null) {

					final int numFields = toMerge.size();

					normCount++;

					final NormsWriterPerField[] fields = new NormsWriterPerField[numFields];
					int[] uptos = new int[numFields];

					for (int j = 0; j < numFields; j++)
						fields[j] = toMerge.get(j);

					int numLeft = numFields;

					while (numLeft > 0) {

						assert uptos[0] < fields[0].docIDs.length : " uptos[0]="
								+ uptos[0]
								+ " len="
								+ (fields[0].docIDs.length);

						int minLoc = 0;
						int minDocID = fields[0].docIDs[uptos[0]];

						for (int j = 1; j < numLeft; j++) {
							final int docID = fields[j].docIDs[uptos[j]];
							if (docID < minDocID) {
								minDocID = docID;
								minLoc = j;
							}
						}

						assert minDocID < state.numDocs;

						for (; upto < minDocID; upto++)
							normsOut.writeByte(defaultNorm);

						normsOut.writeByte(fields[minLoc].norms[uptos[minLoc]]);
						(uptos[minLoc])++;
						upto++;

						if (uptos[minLoc] == fields[minLoc].upto) {
							fields[minLoc].reset();
							if (minLoc != numLeft - 1) {
								fields[minLoc] = fields[numLeft - 1];
								uptos[minLoc] = uptos[numLeft - 1];
							}
							numLeft--;
						}
					}

					for (; upto < state.numDocs; upto++)
						normsOut.writeByte(defaultNorm);
				} else if (fieldInfo.isIndexed && !fieldInfo.omitNorms) {
					normCount++;

					for (; upto < state.numDocs; upto++)
						normsOut.writeByte(defaultNorm);
				}

				assert 4 + normCount * state.numDocs == normsOut
						.getFilePointer() : ".nrm file size mismatch: expected="
						+ (4 + normCount * state.numDocs)
						+ " actual="
						+ normsOut.getFilePointer();
			}
			success = true;
		} finally {
			if (success) {
				IOUtils.close(normsOut);
			} else {
				IOUtils.closeWhileHandlingException(normsOut);
			}
		}
	}
}
