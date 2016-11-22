/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.util.MapBackedSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilterIndexReader extends IndexReader {

	public static class FilterTermDocs implements TermDocs {
		protected TermDocs in;

		public FilterTermDocs(TermDocs in) {
			this.in = in;
		}

		public void seek(Term term) throws IOException {
			in.seek(term);
		}

		public void seek(TermEnum termEnum) throws IOException {
			in.seek(termEnum);
		}

		public int doc() {
			return in.doc();
		}

		public int freq() {
			return in.freq();
		}

		public boolean next() throws IOException {
			return in.next();
		}

		public int read(int[] docs, int[] freqs) throws IOException {
			return in.read(docs, freqs);
		}

		public boolean skipTo(int i) throws IOException {
			return in.skipTo(i);
		}

		public void close() throws IOException {
			in.close();
		}
	}

	public static class FilterTermPositions extends FilterTermDocs implements
			TermPositions {

		public FilterTermPositions(TermPositions in) {
			super(in);
		}

		public int nextPosition() throws IOException {
			return ((TermPositions) this.in).nextPosition();
		}

		public int getPayloadLength() {
			return ((TermPositions) this.in).getPayloadLength();
		}

		public byte[] getPayload(byte[] data, int offset) throws IOException {
			return ((TermPositions) this.in).getPayload(data, offset);
		}

		public boolean isPayloadAvailable() {
			return ((TermPositions) this.in).isPayloadAvailable();
		}
	}

	public static class FilterTermEnum extends TermEnum {
		protected TermEnum in;

		public FilterTermEnum(TermEnum in) {
			this.in = in;
		}

		@Override
		public boolean next() throws IOException {
			return in.next();
		}

		@Override
		public Term term() {
			return in.term();
		}

		@Override
		public int docFreq() {
			return in.docFreq();
		}

		@Override
		public void close() throws IOException {
			in.close();
		}
	}

	protected IndexReader in;

	public FilterIndexReader(IndexReader in) {
		super();
		this.in = in;
		readerFinishedListeners = new MapBackedSet<ReaderFinishedListener>(
				new ConcurrentHashMap<ReaderFinishedListener, Boolean>());
	}

	@Override
	public Directory directory() {
		ensureOpen();
		return in.directory();
	}

	@Override
	public TermFreqVector[] getTermFreqVectors(int docNumber)
			throws IOException {
		ensureOpen();
		return in.getTermFreqVectors(docNumber);
	}

	@Override
	public TermFreqVector getTermFreqVector(int docNumber, String field)
			throws IOException {
		ensureOpen();
		return in.getTermFreqVector(docNumber, field);
	}

	@Override
	public void getTermFreqVector(int docNumber, String field,
			TermVectorMapper mapper) throws IOException {
		ensureOpen();
		in.getTermFreqVector(docNumber, field, mapper);
	}

	@Override
	public void getTermFreqVector(int docNumber, TermVectorMapper mapper)
			throws IOException {
		ensureOpen();
		in.getTermFreqVector(docNumber, mapper);
	}

	@Override
	public int numDocs() {

		return in.numDocs();
	}

	@Override
	public int maxDoc() {

		return in.maxDoc();
	}

	@Override
	public Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		ensureOpen();
		return in.document(n, fieldSelector);
	}

	@Override
	public boolean isDeleted(int n) {

		return in.isDeleted(n);
	}

	@Override
	public boolean hasDeletions() {
		ensureOpen();
		return in.hasDeletions();
	}

	@Override
	protected void doUndeleteAll() throws CorruptIndexException, IOException {
		in.undeleteAll();
	}

	@Override
	public boolean hasNorms(String field) throws IOException {
		ensureOpen();
		return in.hasNorms(field);
	}

	@Override
	public byte[] norms(String f) throws IOException {
		ensureOpen();
		return in.norms(f);
	}

	@Override
	public void norms(String f, byte[] bytes, int offset) throws IOException {
		ensureOpen();
		in.norms(f, bytes, offset);
	}

	@Override
	protected void doSetNorm(int d, String f, byte b)
			throws CorruptIndexException, IOException {
		in.setNorm(d, f, b);
	}

	@Override
	public TermEnum terms() throws IOException {
		ensureOpen();
		return in.terms();
	}

	@Override
	public TermEnum terms(Term t) throws IOException {
		ensureOpen();
		return in.terms(t);
	}

	@Override
	public int docFreq(Term t) throws IOException {
		ensureOpen();
		return in.docFreq(t);
	}

	@Override
	public TermDocs termDocs() throws IOException {
		ensureOpen();
		return in.termDocs();
	}

	@Override
	public TermDocs termDocs(Term term) throws IOException {
		ensureOpen();
		return in.termDocs(term);
	}

	@Override
	public TermPositions termPositions() throws IOException {
		ensureOpen();
		return in.termPositions();
	}

	@Override
	protected void doDelete(int n) throws CorruptIndexException, IOException {
		in.deleteDocument(n);
	}

	@Override
	protected void doCommit(Map<String, String> commitUserData)
			throws IOException {
		in.commit(commitUserData);
	}

	@Override
	protected void doClose() throws IOException {
		in.close();
	}

	@Override
	public Collection<String> getFieldNames(IndexReader.FieldOption fieldNames) {
		ensureOpen();
		return in.getFieldNames(fieldNames);
	}

	@Override
	public long getVersion() {
		ensureOpen();
		return in.getVersion();
	}

	@Override
	public boolean isCurrent() throws CorruptIndexException, IOException {
		ensureOpen();
		return in.isCurrent();
	}

	@Deprecated
	@Override
	public boolean isOptimized() {
		ensureOpen();
		return in.isOptimized();
	}

	@Override
	public IndexReader[] getSequentialSubReaders() {
		return in.getSequentialSubReaders();
	}

	@Override
	public Map<String, String> getCommitUserData() {
		return in.getCommitUserData();
	}

	@Override
	public Object getCoreCacheKey() {
		return in.getCoreCacheKey();
	}

	@Override
	public Object getDeletesCacheKey() {
		return in.getDeletesCacheKey();
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder("FilterReader(");
		buffer.append(in);
		buffer.append(')');
		return buffer.toString();
	}

	@Override
	public void addReaderFinishedListener(ReaderFinishedListener listener) {
		super.addReaderFinishedListener(listener);
		in.addReaderFinishedListener(listener);
	}

	@Override
	public void removeReaderFinishedListener(ReaderFinishedListener listener) {
		super.removeReaderFinishedListener(listener);
		in.removeReaderFinishedListener(listener);
	}
}
