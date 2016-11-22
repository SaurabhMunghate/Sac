/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.io.Reader;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.tokenattributes.OffsetAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.PositionIncrementAttribute;
import com.shatam.shatamindex.document.Fieldable;

final class DocInverterPerField extends DocFieldConsumerPerField {

	final private DocInverterPerThread perThread;
	final private FieldInfo fieldInfo;
	final InvertedDocConsumerPerField consumer;
	final InvertedDocEndConsumerPerField endConsumer;
	final DocumentsWriter.DocState docState;
	final FieldInvertState fieldState;

	public DocInverterPerField(DocInverterPerThread perThread,
			FieldInfo fieldInfo) {
		this.perThread = perThread;
		this.fieldInfo = fieldInfo;
		docState = perThread.docState;
		fieldState = perThread.fieldState;
		this.consumer = perThread.consumer.addField(this, fieldInfo);
		this.endConsumer = perThread.endConsumer.addField(this, fieldInfo);
	}

	@Override
	void abort() {
		try {
			consumer.abort();
		} finally {
			endConsumer.abort();
		}
	}

	@Override
	public void processFields(final Fieldable[] fields, final int count)
			throws IOException {

		fieldState.reset(docState.doc.getBoost());

		final int maxFieldLength = docState.maxFieldLength;

		final boolean doInvert = consumer.start(fields, count);

		for (int i = 0; i < count; i++) {

			final Fieldable field = fields[i];

			if (field.isIndexed() && doInvert) {

				if (i > 0)
					fieldState.position += docState.analyzer == null ? 0
							: docState.analyzer
									.getPositionIncrementGap(fieldInfo.name);

				if (!field.isTokenized()) {
					String stringValue = field.stringValue();
					final int valueLength = stringValue.length();
					perThread.singleToken.reinit(stringValue, 0, valueLength);
					fieldState.attributeSource = perThread.singleToken;
					consumer.start(field);

					boolean success = false;
					try {
						consumer.add();
						success = true;
					} finally {
						if (!success)
							docState.docWriter.setAborting();
					}
					fieldState.offset += valueLength;
					fieldState.length++;
					fieldState.position++;
				} else {
					final TokenStream stream;
					final TokenStream streamValue = field.tokenStreamValue();

					if (streamValue != null)
						stream = streamValue;
					else {

						final Reader reader;
						final Reader readerValue = field.readerValue();

						if (readerValue != null)
							reader = readerValue;
						else {
							String stringValue = field.stringValue();
							if (stringValue == null)
								throw new IllegalArgumentException(
										"field must have either TokenStream, String or Reader value");
							perThread.stringReader.init(stringValue);
							reader = perThread.stringReader;
						}

						stream = docState.analyzer.reusableTokenStream(
								fieldInfo.name, reader);
					}

					stream.reset();

					final int startLength = fieldState.length;

					try {
						boolean hasMoreTokens = stream.incrementToken();

						fieldState.attributeSource = stream;

						OffsetAttribute offsetAttribute = fieldState.attributeSource
								.addAttribute(OffsetAttribute.class);
						PositionIncrementAttribute posIncrAttribute = fieldState.attributeSource
								.addAttribute(PositionIncrementAttribute.class);

						consumer.start(field);

						for (;;) {

							if (!hasMoreTokens)
								break;

							final int posIncr = posIncrAttribute
									.getPositionIncrement();
							fieldState.position += posIncr;
							if (fieldState.position > 0) {
								fieldState.position--;
							}

							if (posIncr == 0)
								fieldState.numOverlap++;

							boolean success = false;
							try {

								consumer.add();
								success = true;
							} finally {
								if (!success)
									docState.docWriter.setAborting();
							}
							fieldState.position++;
							if (++fieldState.length >= maxFieldLength) {
								if (docState.infoStream != null)
									docState.infoStream
											.println("maxFieldLength "
													+ maxFieldLength
													+ " reached for field "
													+ fieldInfo.name
													+ ", ignoring following tokens");
								break;
							}

							hasMoreTokens = stream.incrementToken();
						}

						stream.end();

						fieldState.offset += offsetAttribute.endOffset();
					} finally {
						stream.close();
					}
				}

				fieldState.offset += docState.analyzer == null ? 0
						: docState.analyzer.getOffsetGap(field);
				fieldState.boost *= field.getBoost();
			}

			fields[i] = null;
		}

		consumer.finish();
		endConsumer.finish();
	}
}
