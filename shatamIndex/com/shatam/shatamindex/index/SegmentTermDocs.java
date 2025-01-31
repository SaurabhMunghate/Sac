/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.BitVector;

class SegmentTermDocs implements TermDocs {
	protected SegmentReader parent;
	protected IndexInput freqStream;
	protected int count;
	protected int df;
	protected BitVector deletedDocs;
	int doc = 0;
	int freq;

	private int skipInterval;
	private int maxSkipLevels;
	private DefaultSkipListReader skipListReader;

	private long freqBasePointer;
	private long proxBasePointer;

	private long skipPointer;
	private boolean haveSkipped;

	protected boolean currentFieldStoresPayloads;
	protected IndexOptions indexOptions;

	protected SegmentTermDocs(SegmentReader parent) {
		this.parent = parent;
		this.freqStream = (IndexInput) parent.core.freqStream.clone();
		synchronized (parent) {
			this.deletedDocs = parent.deletedDocs;
		}
		this.skipInterval = parent.core.getTermsReader().getSkipInterval();
		this.maxSkipLevels = parent.core.getTermsReader().getMaxSkipLevels();
	}

	public void seek(Term term) throws IOException {
		TermInfo ti = parent.core.getTermsReader().get(term);
		seek(ti, term);
	}

	public void seek(TermEnum termEnum) throws IOException {
		TermInfo ti;
		Term term;

		if (termEnum instanceof SegmentTermEnum
				&& ((SegmentTermEnum) termEnum).fieldInfos == parent.core.fieldInfos) {
			SegmentTermEnum segmentTermEnum = ((SegmentTermEnum) termEnum);
			term = segmentTermEnum.term();
			ti = segmentTermEnum.termInfo();
		} else {
			term = termEnum.term();
			ti = parent.core.getTermsReader().get(term);
		}

		seek(ti, term);
	}

	void seek(TermInfo ti, Term term) throws IOException {
		count = 0;
		FieldInfo fi = parent.core.fieldInfos.fieldInfo(term.field);
		indexOptions = (fi != null) ? fi.indexOptions
				: IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
		currentFieldStoresPayloads = (fi != null) ? fi.storePayloads : false;
		if (ti == null) {
			df = 0;
		} else {
			df = ti.docFreq;
			doc = 0;
			freqBasePointer = ti.freqPointer;
			proxBasePointer = ti.proxPointer;
			skipPointer = freqBasePointer + ti.skipOffset;
			freqStream.seek(freqBasePointer);
			haveSkipped = false;
		}
	}

	public void close() throws IOException {
		freqStream.close();
		if (skipListReader != null)
			skipListReader.close();
	}

	public final int doc() {
		return doc;
	}

	public final int freq() {
		return freq;
	}

	protected void skippingDoc() throws IOException {
	}

	public boolean next() throws IOException {
		while (true) {
			if (count == df)
				return false;
			final int docCode = freqStream.readVInt();

			if (indexOptions == IndexOptions.DOCS_ONLY) {
				doc += docCode;
				freq = 1;
			} else {
				doc += docCode >>> 1;
				if ((docCode & 1) != 0)
					freq = 1;
				else
					freq = freqStream.readVInt();
			}

			count++;

			if (deletedDocs == null || !deletedDocs.get(doc))
				break;
			skippingDoc();
		}
		return true;
	}

	public int read(final int[] docs, final int[] freqs) throws IOException {
		final int length = docs.length;
		if (indexOptions == IndexOptions.DOCS_ONLY) {
			return readNoTf(docs, freqs, length);
		} else {
			int i = 0;
			while (i < length && count < df) {

				final int docCode = freqStream.readVInt();
				doc += docCode >>> 1;
				if ((docCode & 1) != 0)
					freq = 1;
				else
					freq = freqStream.readVInt();
				count++;

				if (deletedDocs == null || !deletedDocs.get(doc)) {
					docs[i] = doc;
					freqs[i] = freq;
					++i;
				}
			}
			return i;
		}
	}

	private final int readNoTf(final int[] docs, final int[] freqs,
			final int length) throws IOException {
		int i = 0;
		while (i < length && count < df) {

			doc += freqStream.readVInt();
			count++;

			if (deletedDocs == null || !deletedDocs.get(doc)) {
				docs[i] = doc;

				freqs[i] = 1;
				++i;
			}
		}
		return i;
	}

	protected void skipProx(long proxPointer, int payloadLength)
			throws IOException {
	}

	public boolean skipTo(int target) throws IOException {
		if ((target - skipInterval) >= doc && df >= skipInterval) {
			if (skipListReader == null)
				skipListReader = new DefaultSkipListReader(
						(IndexInput) freqStream.clone(), maxSkipLevels,
						skipInterval);

			if (!haveSkipped) {
				skipListReader.init(skipPointer, freqBasePointer,
						proxBasePointer, df, currentFieldStoresPayloads);
				haveSkipped = true;
			}

			int newCount = skipListReader.skipTo(target);
			if (newCount > count) {
				freqStream.seek(skipListReader.getFreqPointer());
				skipProx(skipListReader.getProxPointer(),
						skipListReader.getPayloadLength());

				doc = skipListReader.getDoc();
				count = newCount;
			}
		}

		do {
			if (!next())
				return false;
		} while (target > doc);
		return true;
	}
}
