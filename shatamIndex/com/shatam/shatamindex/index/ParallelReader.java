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
import com.shatam.shatamindex.document.FieldSelectorResult;
import com.shatam.shatamindex.document.Fieldable;
import com.shatam.shatamindex.util.MapBackedSet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParallelReader extends IndexReader {
	private List<IndexReader> readers = new ArrayList<IndexReader>();
	private List<Boolean> decrefOnClose = new ArrayList<Boolean>();
	boolean incRefReaders = false;
	private SortedMap<String, IndexReader> fieldToReader = new TreeMap<String, IndexReader>();
	private Map<IndexReader, Collection<String>> readerToFields = new HashMap<IndexReader, Collection<String>>();
	private List<IndexReader> storedFieldReaders = new ArrayList<IndexReader>();

	private int maxDoc;
	private int numDocs;
	private boolean hasDeletions;

	public ParallelReader() throws IOException {
		this(true);
	}

	public ParallelReader(boolean closeSubReaders) throws IOException {
		super();
		this.incRefReaders = !closeSubReaders;
		readerFinishedListeners = new MapBackedSet<ReaderFinishedListener>(
				new ConcurrentHashMap<ReaderFinishedListener, Boolean>());
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder("ParallelReader(");
		final Iterator<IndexReader> iter = readers.iterator();
		if (iter.hasNext()) {
			buffer.append(iter.next());
		}
		while (iter.hasNext()) {
			buffer.append(", ").append(iter.next());
		}
		buffer.append(')');
		return buffer.toString();
	}

	public void add(IndexReader reader) throws IOException {
		ensureOpen();
		add(reader, false);
	}

	public void add(IndexReader reader, boolean ignoreStoredFields)
			throws IOException {

		ensureOpen();
		if (readers.size() == 0) {
			this.maxDoc = reader.maxDoc();
			this.numDocs = reader.numDocs();
			this.hasDeletions = reader.hasDeletions();
		}

		if (reader.maxDoc() != maxDoc)
			throw new IllegalArgumentException(
					"All readers must have same maxDoc: " + maxDoc + "!="
							+ reader.maxDoc());
		if (reader.numDocs() != numDocs)
			throw new IllegalArgumentException(
					"All readers must have same numDocs: " + numDocs + "!="
							+ reader.numDocs());

		Collection<String> fields = reader
				.getFieldNames(IndexReader.FieldOption.ALL);
		readerToFields.put(reader, fields);
		for (final String field : fields) {
			if (fieldToReader.get(field) == null)
				fieldToReader.put(field, reader);
		}

		if (!ignoreStoredFields)
			storedFieldReaders.add(reader);
		readers.add(reader);

		if (incRefReaders) {
			reader.incRef();
		}
		decrefOnClose.add(Boolean.valueOf(incRefReaders));
	}

	@Override
	public synchronized Object clone() {

		try {
			return doReopen(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected synchronized IndexReader doOpenIfChanged()
			throws CorruptIndexException, IOException {

		return doReopen(false);
	}

	protected IndexReader doReopen(boolean doClone)
			throws CorruptIndexException, IOException {
		ensureOpen();

		boolean reopened = false;
		List<IndexReader> newReaders = new ArrayList<IndexReader>();

		boolean success = false;

		try {
			for (final IndexReader oldReader : readers) {
				IndexReader newReader = null;
				if (doClone) {
					newReader = (IndexReader) oldReader.clone();
					reopened = true;
				} else {
					newReader = IndexReader.openIfChanged(oldReader);
					if (newReader != null) {
						reopened = true;
					} else {
						newReader = oldReader;
					}
				}
				newReaders.add(newReader);
			}
			success = true;
		} finally {
			if (!success && reopened) {
				for (int i = 0; i < newReaders.size(); i++) {
					IndexReader r = newReaders.get(i);
					if (r != readers.get(i)) {
						try {
							r.close();
						} catch (IOException ignore) {

						}
					}
				}
			}
		}

		if (reopened) {
			List<Boolean> newDecrefOnClose = new ArrayList<Boolean>();
			ParallelReader pr = new ParallelReader();
			for (int i = 0; i < readers.size(); i++) {
				IndexReader oldReader = readers.get(i);
				IndexReader newReader = newReaders.get(i);
				if (newReader == oldReader) {
					newDecrefOnClose.add(Boolean.TRUE);
					newReader.incRef();
				} else {

					newDecrefOnClose.add(Boolean.FALSE);
				}
				pr.add(newReader, !storedFieldReaders.contains(oldReader));
			}
			pr.decrefOnClose = newDecrefOnClose;
			pr.incRefReaders = incRefReaders;
			return pr;
		} else {

			return null;
		}
	}

	@Override
	public int numDocs() {

		return numDocs;
	}

	@Override
	public int maxDoc() {

		return maxDoc;
	}

	@Override
	public boolean hasDeletions() {
		ensureOpen();
		return hasDeletions;
	}

	@Override
	public boolean isDeleted(int n) {

		if (readers.size() > 0)
			return readers.get(0).isDeleted(n);
		return false;
	}

	@Override
	protected void doDelete(int n) throws CorruptIndexException, IOException {
		for (final IndexReader reader : readers) {
			reader.deleteDocument(n);
		}
		hasDeletions = true;
	}

	@Override
	protected void doUndeleteAll() throws CorruptIndexException, IOException {
		for (final IndexReader reader : readers) {
			reader.undeleteAll();
		}
		hasDeletions = false;
	}

	@Override
	public Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		ensureOpen();
		Document result = new Document();
		for (final IndexReader reader : storedFieldReaders) {

			boolean include = (fieldSelector == null);
			if (!include) {
				Collection<String> fields = readerToFields.get(reader);
				for (final String field : fields)
					if (fieldSelector.accept(field) != FieldSelectorResult.NO_LOAD) {
						include = true;
						break;
					}
			}
			if (include) {
				List<Fieldable> fields = reader.document(n, fieldSelector)
						.getFields();
				for (Fieldable field : fields) {
					result.add(field);
				}
			}
		}
		return result;
	}

	@Override
	public TermFreqVector[] getTermFreqVectors(int n) throws IOException {
		ensureOpen();
		ArrayList<TermFreqVector> results = new ArrayList<TermFreqVector>();
		for (final Map.Entry<String, IndexReader> e : fieldToReader.entrySet()) {

			String field = e.getKey();
			IndexReader reader = e.getValue();
			TermFreqVector vector = reader.getTermFreqVector(n, field);
			if (vector != null)
				results.add(vector);
		}
		return results.toArray(new TermFreqVector[results.size()]);
	}

	@Override
	public TermFreqVector getTermFreqVector(int n, String field)
			throws IOException {
		ensureOpen();
		IndexReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.getTermFreqVector(n, field);
	}

	@Override
	public void getTermFreqVector(int docNumber, String field,
			TermVectorMapper mapper) throws IOException {
		ensureOpen();
		IndexReader reader = fieldToReader.get(field);
		if (reader != null) {
			reader.getTermFreqVector(docNumber, field, mapper);
		}
	}

	@Override
	public void getTermFreqVector(int docNumber, TermVectorMapper mapper)
			throws IOException {
		ensureOpen();

		for (final Map.Entry<String, IndexReader> e : fieldToReader.entrySet()) {

			String field = e.getKey();
			IndexReader reader = e.getValue();
			reader.getTermFreqVector(docNumber, field, mapper);
		}

	}

	@Override
	public boolean hasNorms(String field) throws IOException {
		ensureOpen();
		IndexReader reader = fieldToReader.get(field);
		return reader == null ? false : reader.hasNorms(field);
	}

	@Override
	public byte[] norms(String field) throws IOException {
		ensureOpen();
		IndexReader reader = fieldToReader.get(field);
		return reader == null ? null : reader.norms(field);
	}

	@Override
	public void norms(String field, byte[] result, int offset)
			throws IOException {
		ensureOpen();
		IndexReader reader = fieldToReader.get(field);
		if (reader != null)
			reader.norms(field, result, offset);
	}

	@Override
	protected void doSetNorm(int n, String field, byte value)
			throws CorruptIndexException, IOException {
		IndexReader reader = fieldToReader.get(field);
		if (reader != null)
			reader.doSetNorm(n, field, value);
	}

	@Override
	public TermEnum terms() throws IOException {
		ensureOpen();
		return new ParallelTermEnum();
	}

	@Override
	public TermEnum terms(Term term) throws IOException {
		ensureOpen();
		return new ParallelTermEnum(term);
	}

	@Override
	public int docFreq(Term term) throws IOException {
		ensureOpen();
		IndexReader reader = fieldToReader.get(term.field());
		return reader == null ? 0 : reader.docFreq(term);
	}

	@Override
	public TermDocs termDocs(Term term) throws IOException {
		ensureOpen();
		return new ParallelTermDocs(term);
	}

	@Override
	public TermDocs termDocs() throws IOException {
		ensureOpen();
		return new ParallelTermDocs();
	}

	@Override
	public TermPositions termPositions(Term term) throws IOException {
		ensureOpen();
		return new ParallelTermPositions(term);
	}

	@Override
	public TermPositions termPositions() throws IOException {
		ensureOpen();
		return new ParallelTermPositions();
	}

	@Override
	public boolean isCurrent() throws CorruptIndexException, IOException {
		ensureOpen();
		for (final IndexReader reader : readers) {
			if (!reader.isCurrent()) {
				return false;
			}
		}

		return true;
	}

	@Deprecated
	@Override
	public boolean isOptimized() {
		ensureOpen();
		for (final IndexReader reader : readers) {
			if (!reader.isOptimized()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public long getVersion() {
		throw new UnsupportedOperationException(
				"ParallelReader does not support this method.");
	}

	IndexReader[] getSubReaders() {
		return readers.toArray(new IndexReader[readers.size()]);
	}

	@Override
	protected void doCommit(Map<String, String> commitUserData)
			throws IOException {
		for (final IndexReader reader : readers)
			reader.commit(commitUserData);
	}

	@Override
	protected synchronized void doClose() throws IOException {
		for (int i = 0; i < readers.size(); i++) {
			if (decrefOnClose.get(i).booleanValue()) {
				readers.get(i).decRef();
			} else {
				readers.get(i).close();
			}
		}
	}

	@Override
	public Collection<String> getFieldNames(IndexReader.FieldOption fieldNames) {
		ensureOpen();
		Set<String> fieldSet = new HashSet<String>();
		for (final IndexReader reader : readers) {
			Collection<String> names = reader.getFieldNames(fieldNames);
			fieldSet.addAll(names);
		}
		return fieldSet;
	}

	private class ParallelTermEnum extends TermEnum {
		private String field;
		private Iterator<String> fieldIterator;
		private TermEnum termEnum;

		public ParallelTermEnum() throws IOException {
			try {
				field = fieldToReader.firstKey();
			} catch (NoSuchElementException e) {

				return;
			}
			if (field != null)
				termEnum = fieldToReader.get(field).terms();
		}

		public ParallelTermEnum(Term term) throws IOException {
			field = term.field();
			IndexReader reader = fieldToReader.get(field);
			if (reader != null)
				termEnum = reader.terms(term);
		}

		@Override
		public boolean next() throws IOException {
			if (termEnum == null)
				return false;

			if (termEnum.next() && termEnum.term().field() == field)
				return true;

			termEnum.close();

			if (fieldIterator == null) {
				fieldIterator = fieldToReader.tailMap(field).keySet()
						.iterator();
				fieldIterator.next();
			}
			while (fieldIterator.hasNext()) {
				field = fieldIterator.next();
				termEnum = fieldToReader.get(field).terms(new Term(field));
				Term term = termEnum.term();
				if (term != null && term.field() == field)
					return true;
				else
					termEnum.close();
			}

			return false;
		}

		@Override
		public Term term() {
			if (termEnum == null)
				return null;

			return termEnum.term();
		}

		@Override
		public int docFreq() {
			if (termEnum == null)
				return 0;

			return termEnum.docFreq();
		}

		@Override
		public void close() throws IOException {
			if (termEnum != null)
				termEnum.close();
		}
	}

	private class ParallelTermDocs implements TermDocs {
		protected TermDocs termDocs;

		public ParallelTermDocs() {
		}

		public ParallelTermDocs(Term term) throws IOException {
			if (term == null)
				termDocs = readers.isEmpty() ? null : readers.get(0).termDocs(
						null);
			else
				seek(term);
		}

		public int doc() {
			return termDocs.doc();
		}

		public int freq() {
			return termDocs.freq();
		}

		public void seek(Term term) throws IOException {
			IndexReader reader = fieldToReader.get(term.field());
			termDocs = reader != null ? reader.termDocs(term) : null;
		}

		public void seek(TermEnum termEnum) throws IOException {
			seek(termEnum.term());
		}

		public boolean next() throws IOException {
			if (termDocs == null)
				return false;

			return termDocs.next();
		}

		public int read(final int[] docs, final int[] freqs) throws IOException {
			if (termDocs == null)
				return 0;

			return termDocs.read(docs, freqs);
		}

		public boolean skipTo(int target) throws IOException {
			if (termDocs == null)
				return false;

			return termDocs.skipTo(target);
		}

		public void close() throws IOException {
			if (termDocs != null)
				termDocs.close();
		}

	}

	private class ParallelTermPositions extends ParallelTermDocs implements
			TermPositions {

		public ParallelTermPositions() {
		}

		public ParallelTermPositions(Term term) throws IOException {
			seek(term);
		}

		@Override
		public void seek(Term term) throws IOException {
			IndexReader reader = fieldToReader.get(term.field());
			termDocs = reader != null ? reader.termPositions(term) : null;
		}

		public int nextPosition() throws IOException {

			return ((TermPositions) termDocs).nextPosition();
		}

		public int getPayloadLength() {
			return ((TermPositions) termDocs).getPayloadLength();
		}

		public byte[] getPayload(byte[] data, int offset) throws IOException {
			return ((TermPositions) termDocs).getPayload(data, offset);
		}

		public boolean isPayloadAvailable() {
			return ((TermPositions) termDocs).isPayloadAvailable();
		}
	}

	@Override
	public void addReaderFinishedListener(ReaderFinishedListener listener) {
		super.addReaderFinishedListener(listener);
		for (IndexReader reader : readers) {
			reader.addReaderFinishedListener(listener);
		}
	}

	@Override
	public void removeReaderFinishedListener(ReaderFinishedListener listener) {
		super.removeReaderFinishedListener(listener);
		for (IndexReader reader : readers) {
			reader.removeReaderFinishedListener(listener);
		}
	}
}
