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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.store.BufferedIndexInput;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.BitVector;
import com.shatam.shatamindex.util.CloseableThreadLocal;
import com.shatam.shatamindex.util.StringHelper;

public class SegmentReader extends IndexReader implements Cloneable {
	protected boolean readOnly;

	private SegmentInfo si;
	private int readBufferSize;

	CloseableThreadLocal<FieldsReader> fieldsReaderLocal = new FieldsReaderLocal();
	CloseableThreadLocal<TermVectorsReader> termVectorsLocal = new CloseableThreadLocal<TermVectorsReader>();

	BitVector deletedDocs = null;
	AtomicInteger deletedDocsRef = null;
	private boolean deletedDocsDirty = false;
	private boolean normsDirty = false;

	private int pendingDeleteCount;

	private boolean rollbackHasChanges = false;
	private boolean rollbackDeletedDocsDirty = false;
	private boolean rollbackNormsDirty = false;
	private SegmentInfo rollbackSegmentInfo;
	private int rollbackPendingDeleteCount;

	IndexInput singleNormStream;
	AtomicInteger singleNormRef;

	SegmentCoreReaders core;

	private class FieldsReaderLocal extends CloseableThreadLocal<FieldsReader> {
		@Override
		protected FieldsReader initialValue() {
			return (FieldsReader) core.getFieldsReaderOrig().clone();
		}
	}

	Map<String, SegmentNorms> norms = new HashMap<String, SegmentNorms>();

	public static SegmentReader get(boolean readOnly, SegmentInfo si,
			int termInfosIndexDivisor) throws CorruptIndexException,
			IOException {
		return get(readOnly, si.dir, si, BufferedIndexInput.BUFFER_SIZE, true,
				termInfosIndexDivisor);
	}

	public static SegmentReader get(boolean readOnly, Directory dir,
			SegmentInfo si, int readBufferSize, boolean doOpenStores,
			int termInfosIndexDivisor) throws CorruptIndexException,
			IOException {
		SegmentReader instance = readOnly ? new ReadOnlySegmentReader()
				: new SegmentReader();
		instance.readOnly = readOnly;
		instance.si = si;
		instance.readBufferSize = readBufferSize;

		boolean success = false;

		try {
			instance.core = new SegmentCoreReaders(instance, dir, si,
					readBufferSize, termInfosIndexDivisor);
			if (doOpenStores) {
				instance.core.openDocStores(si);
			}
			instance.loadDeletedDocs();
			instance.openNorms(instance.core.cfsDir, readBufferSize);
			success = true;
		} finally {

			if (!success) {
				instance.doClose();
			}
		}
		return instance;
	}

	void openDocStores() throws IOException {
		core.openDocStores(si);
	}

	private boolean checkDeletedCounts() throws IOException {
		final int recomputedCount = deletedDocs.getRecomputedCount();

		assert deletedDocs.count() == recomputedCount : "deleted count="
				+ deletedDocs.count() + " vs recomputed count="
				+ recomputedCount;

		assert si.getDelCount() == recomputedCount : "delete count mismatch: info="
				+ si.getDelCount() + " vs BitVector=" + recomputedCount;

		assert si.getDelCount() <= maxDoc() : "delete count mismatch: "
				+ recomputedCount + ") exceeds max doc (" + maxDoc()
				+ ") for segment " + si.name;

		return true;
	}

	private void loadDeletedDocs() throws IOException {

		if (hasDeletions(si)) {
			deletedDocs = new BitVector(directory(), si.getDelFileName());
			deletedDocsRef = new AtomicInteger(1);
			assert checkDeletedCounts();
			if (deletedDocs.size() != si.docCount) {
				throw new CorruptIndexException(
						"document count mismatch: deleted docs count "
								+ deletedDocs.size() + " vs segment doc count "
								+ si.docCount + " segment=" + si.name);
			}
		} else
			assert si.getDelCount() == 0;
	}

	protected byte[] cloneNormBytes(byte[] bytes) {
		byte[] cloneBytes = new byte[bytes.length];
		System.arraycopy(bytes, 0, cloneBytes, 0, bytes.length);
		return cloneBytes;
	}

	protected BitVector cloneDeletedDocs(BitVector bv) {
		ensureOpen();
		return (BitVector) bv.clone();
	}

	@Override
	public final synchronized Object clone() {
		try {
			return clone(readOnly);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public final synchronized IndexReader clone(boolean openReadOnly)
			throws CorruptIndexException, IOException {
		return reopenSegment(si, true, openReadOnly);
	}

	@Override
	protected synchronized IndexReader doOpenIfChanged()
			throws CorruptIndexException, IOException {
		return reopenSegment(si, false, readOnly);
	}

	@Override
	protected synchronized IndexReader doOpenIfChanged(boolean openReadOnly)
			throws CorruptIndexException, IOException {
		return reopenSegment(si, false, openReadOnly);
	}

	synchronized SegmentReader reopenSegment(SegmentInfo si, boolean doClone,
			boolean openReadOnly) throws CorruptIndexException, IOException {
		ensureOpen();
		boolean deletionsUpToDate = (this.si.hasDeletions() == si
				.hasDeletions())
				&& (!si.hasDeletions() || this.si.getDelFileName().equals(
						si.getDelFileName()));
		boolean normsUpToDate = true;

		boolean[] fieldNormsChanged = new boolean[core.fieldInfos.size()];
		final int fieldCount = core.fieldInfos.size();
		for (int i = 0; i < fieldCount; i++) {
			if (!this.si.getNormFileName(i).equals(si.getNormFileName(i))) {
				normsUpToDate = false;
				fieldNormsChanged[i] = true;
			}
		}

		if (normsUpToDate && deletionsUpToDate && !doClone && openReadOnly
				&& readOnly) {
			return null;
		}

		assert !doClone || (normsUpToDate && deletionsUpToDate);

		SegmentReader clone = openReadOnly ? new ReadOnlySegmentReader()
				: new SegmentReader();

		boolean success = false;
		try {
			core.incRef();
			clone.core = core;
			clone.readOnly = openReadOnly;
			clone.si = si;
			clone.readBufferSize = readBufferSize;
			clone.pendingDeleteCount = pendingDeleteCount;
			clone.readerFinishedListeners = readerFinishedListeners;

			if (!openReadOnly && hasChanges) {

				clone.deletedDocsDirty = deletedDocsDirty;
				clone.normsDirty = normsDirty;
				clone.hasChanges = hasChanges;
				hasChanges = false;
			}

			if (doClone) {
				if (deletedDocs != null) {
					deletedDocsRef.incrementAndGet();
					clone.deletedDocs = deletedDocs;
					clone.deletedDocsRef = deletedDocsRef;
				}
			} else {
				if (!deletionsUpToDate) {

					assert clone.deletedDocs == null;
					clone.loadDeletedDocs();
				} else if (deletedDocs != null) {
					deletedDocsRef.incrementAndGet();
					clone.deletedDocs = deletedDocs;
					clone.deletedDocsRef = deletedDocsRef;
				}
			}

			clone.norms = new HashMap<String, SegmentNorms>();

			for (int i = 0; i < fieldNormsChanged.length; i++) {

				if (doClone || !fieldNormsChanged[i]) {
					final String curField = core.fieldInfos.fieldInfo(i).name;
					SegmentNorms norm = this.norms.get(curField);
					if (norm != null)
						clone.norms.put(curField, (SegmentNorms) norm.clone());
				}
			}

			clone.openNorms(si.getUseCompoundFile() ? core.getCFSReader()
					: directory(), readBufferSize);

			success = true;
		} finally {
			if (!success) {

				clone.decRef();
			}
		}

		return clone;
	}

	@Override
	protected void doCommit(Map<String, String> commitUserData)
			throws IOException {
		if (hasChanges) {
			startCommit();
			boolean success = false;
			try {
				commitChanges(commitUserData);
				success = true;
			} finally {
				if (!success) {
					rollbackCommit();
				}
			}
		}
	}

	private synchronized void commitChanges(Map<String, String> commitUserData)
			throws IOException {
		if (deletedDocsDirty) {
			si.advanceDelGen();

			assert deletedDocs.size() == si.docCount;

			final String delFileName = si.getDelFileName();
			boolean success = false;
			try {
				deletedDocs.write(directory(), delFileName);
				success = true;
			} finally {
				if (!success) {
					try {
						directory().deleteFile(delFileName);
					} catch (Throwable t) {

					}
				}
			}

			si.setDelCount(si.getDelCount() + pendingDeleteCount);
			pendingDeleteCount = 0;
			assert deletedDocs.count() == si.getDelCount() : "delete count mismatch during commit: info="
					+ si.getDelCount() + " vs BitVector=" + deletedDocs.count();
		} else {
			assert pendingDeleteCount == 0;
		}

		if (normsDirty) {
			si.setNumFields(core.fieldInfos.size());
			for (final SegmentNorms norm : norms.values()) {
				if (norm.dirty) {
					norm.reWrite(si);
				}
			}
		}
		deletedDocsDirty = false;
		normsDirty = false;
		hasChanges = false;
	}

	FieldsReader getFieldsReader() {
		return fieldsReaderLocal.get();
	}

	@Override
	protected void doClose() throws IOException {
		termVectorsLocal.close();
		fieldsReaderLocal.close();

		if (deletedDocs != null) {
			deletedDocsRef.decrementAndGet();

			deletedDocs = null;
		}

		for (final SegmentNorms norm : norms.values()) {
			norm.decRef();
		}
		if (core != null) {
			core.decRef();
		}
	}

	static boolean hasDeletions(SegmentInfo si) throws IOException {

		return si.hasDeletions();
	}

	@Override
	public boolean hasDeletions() {

		return deletedDocs != null;
	}

	static boolean usesCompoundFile(SegmentInfo si) throws IOException {
		return si.getUseCompoundFile();
	}

	static boolean hasSeparateNorms(SegmentInfo si) throws IOException {
		return si.hasSeparateNorms();
	}

	@Override
	protected void doDelete(int docNum) {
		if (deletedDocs == null) {
			deletedDocs = new BitVector(maxDoc());
			deletedDocsRef = new AtomicInteger(1);
		}

		if (deletedDocsRef.get() > 1) {
			AtomicInteger oldRef = deletedDocsRef;
			deletedDocs = cloneDeletedDocs(deletedDocs);
			deletedDocsRef = new AtomicInteger(1);
			oldRef.decrementAndGet();
		}
		deletedDocsDirty = true;
		if (!deletedDocs.getAndSet(docNum)) {
			pendingDeleteCount++;
		}
	}

	@Override
	protected void doUndeleteAll() {
		deletedDocsDirty = false;
		if (deletedDocs != null) {
			assert deletedDocsRef != null;
			deletedDocsRef.decrementAndGet();
			deletedDocs = null;
			deletedDocsRef = null;
			pendingDeleteCount = 0;
			si.clearDelGen();
			si.setDelCount(0);
		} else {
			assert deletedDocsRef == null;
			assert pendingDeleteCount == 0;
		}
	}

	List<String> files() throws IOException {
		return new ArrayList<String>(si.files());
	}

	@Override
	public TermEnum terms() {
		ensureOpen();
		return core.getTermsReader().terms();
	}

	@Override
	public TermEnum terms(Term t) throws IOException {
		ensureOpen();
		return core.getTermsReader().terms(t);
	}

	FieldInfos fieldInfos() {
		return core.fieldInfos;
	}

	@Override
	public Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		ensureOpen();
		if (n < 0 || n >= maxDoc()) {
			throw new IllegalArgumentException(
					"docID must be >= 0 and < maxDoc=" + maxDoc()
							+ " (got docID=" + n + ")");
		}
		return getFieldsReader().doc(n, fieldSelector);
	}

	@Override
	public synchronized boolean isDeleted(int n) {
		return (deletedDocs != null && deletedDocs.get(n));
	}

	@Override
	public TermDocs termDocs(Term term) throws IOException {
		if (term == null) {
			return new AllTermDocs(this);
		} else {
			return super.termDocs(term);
		}
	}

	@Override
	public TermDocs termDocs() throws IOException {
		ensureOpen();
		return new SegmentTermDocs(this);
	}

	@Override
	public TermPositions termPositions() throws IOException {
		ensureOpen();
		return new SegmentTermPositions(this);
	}

	@Override
	public int docFreq(Term t) throws IOException {
		ensureOpen();
		TermInfo ti = core.getTermsReader().get(t);
		if (ti != null)
			return ti.docFreq;
		else
			return 0;
	}

	@Override
	public int numDocs() {

		int n = maxDoc();
		if (deletedDocs != null)
			n -= deletedDocs.count();
		return n;
	}

	@Override
	public int maxDoc() {

		return si.docCount;
	}

	@Override
	public Collection<String> getFieldNames(IndexReader.FieldOption fieldOption) {
		ensureOpen();

		Set<String> fieldSet = new HashSet<String>();
		for (int i = 0; i < core.fieldInfos.size(); i++) {
			FieldInfo fi = core.fieldInfos.fieldInfo(i);
			if (fieldOption == IndexReader.FieldOption.ALL) {
				fieldSet.add(fi.name);
			} else if (!fi.isIndexed
					&& fieldOption == IndexReader.FieldOption.UNINDEXED) {
				fieldSet.add(fi.name);
			} else if (fi.indexOptions == IndexOptions.DOCS_ONLY
					&& fieldOption == IndexReader.FieldOption.OMIT_TERM_FREQ_AND_POSITIONS) {
				fieldSet.add(fi.name);
			} else if (fi.indexOptions == IndexOptions.DOCS_AND_FREQS
					&& fieldOption == IndexReader.FieldOption.OMIT_POSITIONS) {
				fieldSet.add(fi.name);
			} else if (fi.storePayloads
					&& fieldOption == IndexReader.FieldOption.STORES_PAYLOADS) {
				fieldSet.add(fi.name);
			} else if (fi.isIndexed
					&& fieldOption == IndexReader.FieldOption.INDEXED) {
				fieldSet.add(fi.name);
			} else if (fi.isIndexed
					&& fi.storeTermVector == false
					&& fieldOption == IndexReader.FieldOption.INDEXED_NO_TERMVECTOR) {
				fieldSet.add(fi.name);
			} else if (fi.storeTermVector == true
					&& fi.storePositionWithTermVector == false
					&& fi.storeOffsetWithTermVector == false
					&& fieldOption == IndexReader.FieldOption.TERMVECTOR) {
				fieldSet.add(fi.name);
			} else if (fi.isIndexed
					&& fi.storeTermVector
					&& fieldOption == IndexReader.FieldOption.INDEXED_WITH_TERMVECTOR) {
				fieldSet.add(fi.name);
			} else if (fi.storePositionWithTermVector
					&& fi.storeOffsetWithTermVector == false
					&& fieldOption == IndexReader.FieldOption.TERMVECTOR_WITH_POSITION) {
				fieldSet.add(fi.name);
			} else if (fi.storeOffsetWithTermVector
					&& fi.storePositionWithTermVector == false
					&& fieldOption == IndexReader.FieldOption.TERMVECTOR_WITH_OFFSET) {
				fieldSet.add(fi.name);
			} else if ((fi.storeOffsetWithTermVector && fi.storePositionWithTermVector)
					&& fieldOption == IndexReader.FieldOption.TERMVECTOR_WITH_POSITION_OFFSET) {
				fieldSet.add(fi.name);
			}
		}
		return fieldSet;
	}

	@Override
	public boolean hasNorms(String field) {
		ensureOpen();
		return norms.containsKey(field);
	}

	@Override
	public byte[] norms(String field) throws IOException {
		ensureOpen();
		final SegmentNorms norm = norms.get(field);
		if (norm == null) {

			return null;
		}
		return norm.bytes();
	}

	@Override
	protected void doSetNorm(int doc, String field, byte value)
			throws IOException {
		SegmentNorms norm = norms.get(field);
		if (norm == null) {

			throw new IllegalStateException("Cannot setNorm for field " + field
					+ ": norms were omitted");
		}

		normsDirty = true;
		norm.copyOnWrite()[doc] = value;
	}

	@Override
	public synchronized void norms(String field, byte[] bytes, int offset)
			throws IOException {

		ensureOpen();
		SegmentNorms norm = norms.get(field);
		if (norm == null) {
			Arrays.fill(bytes, offset, bytes.length, Similarity.getDefault()
					.encodeNormValue(1.0f));
			return;
		}

		norm.bytes(bytes, offset, maxDoc());
	}

	int getPostingsSkipInterval() {
		return core.getTermsReader().getSkipInterval();
	}

	private void openNorms(Directory cfsDir, int readBufferSize)
			throws IOException {
		long nextNormSeek = SegmentNorms.NORMS_HEADER.length;
		int maxDoc = maxDoc();
		for (int i = 0; i < core.fieldInfos.size(); i++) {
			FieldInfo fi = core.fieldInfos.fieldInfo(i);
			if (norms.containsKey(fi.name)) {

				continue;
			}
			if (fi.isIndexed && !fi.omitNorms) {
				Directory d = directory();
				String fileName = si.getNormFileName(fi.number);
				if (!si.hasSeparateNorms(fi.number)) {
					d = cfsDir;
				}

				boolean singleNormFile = IndexFileNames.matchesExtension(
						fileName, IndexFileNames.NORMS_EXTENSION);
				IndexInput normInput = null;
				long normSeek;

				if (singleNormFile) {
					normSeek = nextNormSeek;
					if (singleNormStream == null) {
						singleNormStream = d
								.openInput(fileName, readBufferSize);
						singleNormRef = new AtomicInteger(1);
					} else {
						singleNormRef.incrementAndGet();
					}

					normInput = singleNormStream;
				} else {
					normInput = d.openInput(fileName);

					String version = si.getVersion();
					final boolean isUnversioned = (version == null || StringHelper
							.getVersionComparator().compare(version, "3.2") < 0)
							&& normInput.length() == maxDoc();
					if (isUnversioned) {
						normSeek = 0;
					} else {
						normSeek = SegmentNorms.NORMS_HEADER.length;
					}
				}

				norms.put(fi.name, new SegmentNorms(normInput, fi.number,
						normSeek, this));
				nextNormSeek += maxDoc;
			}
		}
	}

	boolean termsIndexLoaded() {
		return core.termsIndexIsLoaded();
	}

	void loadTermsIndex(int termsIndexDivisor) throws IOException {
		core.loadTermsIndex(si, termsIndexDivisor);
	}

	boolean normsClosed() {
		if (singleNormStream != null) {
			return false;
		}
		for (final SegmentNorms norm : norms.values()) {
			if (norm.refCount > 0) {
				return false;
			}
		}
		return true;
	}

	boolean normsClosed(String field) {
		return norms.get(field).refCount == 0;
	}

	TermVectorsReader getTermVectorsReader() {
		TermVectorsReader tvReader = termVectorsLocal.get();
		if (tvReader == null) {
			TermVectorsReader orig = core.getTermVectorsReaderOrig();
			if (orig == null) {
				return null;
			} else {
				try {
					tvReader = (TermVectorsReader) orig.clone();
				} catch (CloneNotSupportedException cnse) {
					return null;
				}
			}
			termVectorsLocal.set(tvReader);
		}
		return tvReader;
	}

	TermVectorsReader getTermVectorsReaderOrig() {
		return core.getTermVectorsReaderOrig();
	}

	@Override
	public TermFreqVector getTermFreqVector(int docNumber, String field)
			throws IOException {

		ensureOpen();
		FieldInfo fi = core.fieldInfos.fieldInfo(field);
		if (fi == null || !fi.storeTermVector)
			return null;

		TermVectorsReader termVectorsReader = getTermVectorsReader();
		if (termVectorsReader == null)
			return null;

		return termVectorsReader.get(docNumber, field);
	}

	@Override
	public void getTermFreqVector(int docNumber, String field,
			TermVectorMapper mapper) throws IOException {
		ensureOpen();
		FieldInfo fi = core.fieldInfos.fieldInfo(field);
		if (fi == null || !fi.storeTermVector)
			return;

		TermVectorsReader termVectorsReader = getTermVectorsReader();
		if (termVectorsReader == null) {
			return;
		}

		termVectorsReader.get(docNumber, field, mapper);
	}

	@Override
	public void getTermFreqVector(int docNumber, TermVectorMapper mapper)
			throws IOException {
		ensureOpen();

		TermVectorsReader termVectorsReader = getTermVectorsReader();
		if (termVectorsReader == null)
			return;

		termVectorsReader.get(docNumber, mapper);
	}

	@Override
	public TermFreqVector[] getTermFreqVectors(int docNumber)
			throws IOException {
		ensureOpen();

		TermVectorsReader termVectorsReader = getTermVectorsReader();
		if (termVectorsReader == null)
			return null;

		return termVectorsReader.get(docNumber);
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		if (hasChanges) {
			buffer.append('*');
		}
		buffer.append(si.toString(core.dir, pendingDeleteCount));
		return buffer.toString();
	}

	public String getSegmentName() {
		return core.segment;
	}

	SegmentInfo getSegmentInfo() {
		return si;
	}

	void setSegmentInfo(SegmentInfo info) {
		si = info;
	}

	void startCommit() {
		rollbackSegmentInfo = (SegmentInfo) si.clone();
		rollbackHasChanges = hasChanges;
		rollbackDeletedDocsDirty = deletedDocsDirty;
		rollbackNormsDirty = normsDirty;
		rollbackPendingDeleteCount = pendingDeleteCount;
		for (SegmentNorms norm : norms.values()) {
			norm.rollbackDirty = norm.dirty;
		}
	}

	void rollbackCommit() {
		si.reset(rollbackSegmentInfo);
		hasChanges = rollbackHasChanges;
		deletedDocsDirty = rollbackDeletedDocsDirty;
		normsDirty = rollbackNormsDirty;
		pendingDeleteCount = rollbackPendingDeleteCount;
		for (SegmentNorms norm : norms.values()) {
			norm.dirty = norm.rollbackDirty;
		}
	}

	@Override
	public Directory directory() {

		return core.dir;
	}

	@Override
	public final Object getCoreCacheKey() {
		return core.freqStream;
	}

	@Override
	public Object getDeletesCacheKey() {
		return deletedDocs;
	}

	@Override
	public long getUniqueTermCount() {
		return core.getTermsReader().size();
	}

	@Deprecated
	static SegmentReader getOnlySegmentReader(Directory dir) throws IOException {
		return getOnlySegmentReader(IndexReader.open(dir, false));
	}

	static SegmentReader getOnlySegmentReader(IndexReader reader) {
		if (reader instanceof SegmentReader)
			return (SegmentReader) reader;

		if (reader instanceof DirectoryReader) {
			IndexReader[] subReaders = reader.getSequentialSubReaders();
			if (subReaders.length != 1)
				throw new IllegalArgumentException(reader + " has "
						+ subReaders.length
						+ " segments instead of exactly one");

			return (SegmentReader) subReaders[0];
		}

		throw new IllegalArgumentException(reader
				+ " is not a SegmentReader or a single-segment DirectoryReader");
	}

	@Override
	public int getTermInfosIndexDivisor() {
		return core.termsIndexDivisor;
	}

	@Override
	protected void readerFinished() {

	}
}
