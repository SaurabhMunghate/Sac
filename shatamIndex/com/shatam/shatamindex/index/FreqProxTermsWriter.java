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
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.util.BitVector;
import com.shatam.shatamindex.util.CollectionUtil;
import com.shatam.shatamindex.util.UnicodeUtil;

final class FreqProxTermsWriter extends TermsHashConsumer {

	@Override
	public TermsHashConsumerPerThread addThread(TermsHashPerThread perThread) {
		return new FreqProxTermsWriterPerThread(perThread);
	}

	private static int compareText(final char[] text1, int pos1,
			final char[] text2, int pos2) {
		while (true) {
			final char c1 = text1[pos1++];
			final char c2 = text2[pos2++];
			if (c1 != c2) {
				if (0xffff == c2)
					return 1;
				else if (0xffff == c1)
					return -1;
				else
					return c1 - c2;
			} else if (0xffff == c1)
				return 0;
		}
	}

	@Override
	void abort() {
	}

	@Override
	public void flush(
			Map<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> threadsAndFields,
			final SegmentWriteState state) throws IOException {

		List<FreqProxTermsWriterPerField> allFields = new ArrayList<FreqProxTermsWriterPerField>();

		for (Map.Entry<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> entry : threadsAndFields
				.entrySet()) {

			Collection<TermsHashConsumerPerField> fields = entry.getValue();

			for (final TermsHashConsumerPerField i : fields) {
				final FreqProxTermsWriterPerField perField = (FreqProxTermsWriterPerField) i;
				if (perField.termsHashPerField.numPostings > 0)
					allFields.add(perField);
			}
		}

		CollectionUtil.quickSort(allFields);
		final int numAllFields = allFields.size();

		final FormatPostingsFieldsConsumer consumer = new FormatPostingsFieldsWriter(
				state, fieldInfos);

		try {
			int start = 0;
			while (start < numAllFields) {
				final FieldInfo fieldInfo = allFields.get(start).fieldInfo;
				final String fieldName = fieldInfo.name;

				int end = start + 1;
				while (end < numAllFields
						&& allFields.get(end).fieldInfo.name.equals(fieldName))
					end++;

				FreqProxTermsWriterPerField[] fields = new FreqProxTermsWriterPerField[end
						- start];
				for (int i = start; i < end; i++) {
					fields[i - start] = allFields.get(i);

					if (fieldInfo.indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
						fieldInfo.storePayloads |= fields[i - start].hasPayloads;
					}
				}

				appendPostings(fieldName, state, fields, consumer);

				for (int i = 0; i < fields.length; i++) {
					TermsHashPerField perField = fields[i].termsHashPerField;
					int numPostings = perField.numPostings;
					perField.reset();
					perField.shrinkHash(numPostings);
					fields[i].reset();
				}

				start = end;
			}

			for (Map.Entry<TermsHashConsumerPerThread, Collection<TermsHashConsumerPerField>> entry : threadsAndFields
					.entrySet()) {
				FreqProxTermsWriterPerThread perThread = (FreqProxTermsWriterPerThread) entry
						.getKey();
				perThread.termsHashPerThread.reset(true);
			}
		} finally {
			consumer.finish();
		}
	}

	private byte[] payloadBuffer;

	void appendPostings(String fieldName, SegmentWriteState state,
			FreqProxTermsWriterPerField[] fields,
			FormatPostingsFieldsConsumer consumer)
			throws CorruptIndexException, IOException {

		int numFields = fields.length;

		final FreqProxFieldMergeState[] mergeStates = new FreqProxFieldMergeState[numFields];

		for (int i = 0; i < numFields; i++) {
			FreqProxFieldMergeState fms = mergeStates[i] = new FreqProxFieldMergeState(
					fields[i]);

			assert fms.field.fieldInfo == fields[0].fieldInfo;

			boolean result = fms.nextTerm();
			assert result;
		}

		final FormatPostingsTermsConsumer termsConsumer = consumer
				.addField(fields[0].fieldInfo);
		final Term protoTerm = new Term(fieldName);

		FreqProxFieldMergeState[] termStates = new FreqProxFieldMergeState[numFields];

		final IndexOptions currentFieldIndexOptions = fields[0].fieldInfo.indexOptions;

		final Map<Term, Integer> segDeletes;
		if (state.segDeletes != null && state.segDeletes.terms.size() > 0) {
			segDeletes = state.segDeletes.terms;
		} else {
			segDeletes = null;
		}

		try {

			while (numFields > 0) {

				termStates[0] = mergeStates[0];
				int numToMerge = 1;

				for (int i = 1; i < numFields; i++) {
					final char[] text = mergeStates[i].text;
					final int textOffset = mergeStates[i].textOffset;
					final int cmp = compareText(text, textOffset,
							termStates[0].text, termStates[0].textOffset);

					if (cmp < 0) {
						termStates[0] = mergeStates[i];
						numToMerge = 1;
					} else if (cmp == 0)
						termStates[numToMerge++] = mergeStates[i];
				}

				final FormatPostingsDocsConsumer docConsumer = termsConsumer
						.addTerm(termStates[0].text, termStates[0].textOffset);

				final int delDocLimit;
				if (segDeletes != null) {
					final Integer docIDUpto = segDeletes.get(protoTerm
							.createTerm(termStates[0].termText()));
					if (docIDUpto != null) {
						delDocLimit = docIDUpto;
					} else {
						delDocLimit = 0;
					}
				} else {
					delDocLimit = 0;
				}

				try {

					while (numToMerge > 0) {

						FreqProxFieldMergeState minState = termStates[0];
						for (int i = 1; i < numToMerge; i++)
							if (termStates[i].docID < minState.docID)
								minState = termStates[i];

						final int termDocFreq = minState.termFreq;

						final FormatPostingsPositionsConsumer posConsumer = docConsumer
								.addDoc(minState.docID, termDocFreq);

						if (minState.docID < delDocLimit) {

							if (state.deletedDocs == null) {
								state.deletedDocs = new BitVector(state.numDocs);
							}
							state.deletedDocs.set(minState.docID);
						}

						final ByteSliceReader prox = minState.prox;

						if (currentFieldIndexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {

							try {
								int position = 0;
								for (int j = 0; j < termDocFreq; j++) {
									final int code = prox.readVInt();
									position += code >> 1;

									final int payloadLength;
									if ((code & 1) != 0) {

										payloadLength = prox.readVInt();

										if (payloadBuffer == null
												|| payloadBuffer.length < payloadLength)
											payloadBuffer = new byte[payloadLength];

										prox.readBytes(payloadBuffer, 0,
												payloadLength);

									} else
										payloadLength = 0;

									posConsumer.addPosition(position,
											payloadBuffer, 0, payloadLength);
								}
							} finally {
								posConsumer.finish();
							}
						}

						if (!minState.nextDoc()) {

							int upto = 0;
							for (int i = 0; i < numToMerge; i++)
								if (termStates[i] != minState)
									termStates[upto++] = termStates[i];
							numToMerge--;
							assert upto == numToMerge;

							if (!minState.nextTerm()) {

								upto = 0;
								for (int i = 0; i < numFields; i++)
									if (mergeStates[i] != minState)
										mergeStates[upto++] = mergeStates[i];
								numFields--;
								assert upto == numFields;
							}
						}
					}
				} finally {
					docConsumer.finish();
				}
			}
		} finally {
			termsConsumer.finish();
		}
	}

	final UnicodeUtil.UTF8Result termsUTF8 = new UnicodeUtil.UTF8Result();
}
