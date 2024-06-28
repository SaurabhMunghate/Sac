/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.search.FieldCache;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.store.*;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.ReaderUtil;
import com.shatam.shatamindex.util.VirtualMethod;

public abstract class IndexReader implements Cloneable, Closeable {

	public static interface ReaderFinishedListener {
		public void finished(IndexReader reader);
	}

	protected volatile Collection<ReaderFinishedListener> readerFinishedListeners;

	public void addReaderFinishedListener(ReaderFinishedListener listener) {
		ensureOpen();
		readerFinishedListeners.add(listener);
	}

	public void removeReaderFinishedListener(ReaderFinishedListener listener) {
		ensureOpen();
		readerFinishedListeners.remove(listener);
	}

	protected void notifyReaderFinishedListeners() {

		if (readerFinishedListeners != null) {
			for (ReaderFinishedListener listener : readerFinishedListeners) {
				listener.finished(this);
			}
		}
	}

	protected void readerFinished() {
		notifyReaderFinishedListeners();
	}

	public static enum FieldOption {

		ALL,

		INDEXED,

		STORES_PAYLOADS,

		OMIT_TERM_FREQ_AND_POSITIONS,

		OMIT_POSITIONS,

		UNINDEXED,

		INDEXED_WITH_TERMVECTOR,

		INDEXED_NO_TERMVECTOR,

		TERMVECTOR,

		TERMVECTOR_WITH_POSITION,

		TERMVECTOR_WITH_OFFSET,

		TERMVECTOR_WITH_POSITION_OFFSET,
	}

	private volatile boolean closed;
	protected boolean hasChanges;

	private final AtomicInteger refCount = new AtomicInteger();

	static int DEFAULT_TERMS_INDEX_DIVISOR = 1;

	public int getRefCount() {
		return refCount.get();
	}

	public void incRef() {
		ensureOpen();
		refCount.incrementAndGet();
	}

	public boolean tryIncRef() {
		int count;
		while ((count = refCount.get()) > 0) {
			if (refCount.compareAndSet(count, count + 1)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		if (hasChanges) {
			buffer.append('*');
		}
		buffer.append(getClass().getSimpleName());
		buffer.append('(');
		final IndexReader[] subReaders = getSequentialSubReaders();
		if ((subReaders != null) && (subReaders.length > 0)) {
			buffer.append(subReaders[0]);
			for (int i = 1; i < subReaders.length; ++i) {
				buffer.append(" ").append(subReaders[i]);
			}
		}
		buffer.append(')');
		return buffer.toString();
	}

	public void decRef() throws IOException {
		ensureOpen();
		final int rc = refCount.getAndDecrement();
		if (rc == 1) {
			boolean success = false;
			try {
				commit();
				doClose();
				success = true;
			} finally {
				if (!success) {

					refCount.incrementAndGet();
				}
			}
			readerFinished();
		} else if (rc <= 0) {
			throw new IllegalStateException(
					"too many decRef calls: refCount was " + rc
							+ " before decrement");
		}
	}

	protected IndexReader() {
		refCount.set(1);
	}

	protected final void ensureOpen() throws AlreadyClosedException {
		if (refCount.get() <= 0) {
			throw new AlreadyClosedException("this IndexReader is closed");
		}
	}

	public static IndexReader open(final Directory directory)
			throws CorruptIndexException, IOException {
		return open(directory, null, null, true, DEFAULT_TERMS_INDEX_DIVISOR);
	}

	public static IndexReader open(final Directory directory, boolean readOnly)
			throws CorruptIndexException, IOException {
		return open(directory, null, null, readOnly,
				DEFAULT_TERMS_INDEX_DIVISOR);
	}

	public static IndexReader open(final IndexWriter writer,
			boolean applyAllDeletes) throws CorruptIndexException, IOException {
		return writer.getReader(applyAllDeletes);
	}

	public static IndexReader open(final IndexCommit commit, boolean readOnly)
			throws CorruptIndexException, IOException {
		return open(commit.getDirectory(), null, commit, readOnly,
				DEFAULT_TERMS_INDEX_DIVISOR);
	}

	public static IndexReader open(final Directory directory,
			IndexDeletionPolicy deletionPolicy, boolean readOnly)
			throws CorruptIndexException, IOException {
		return open(directory, deletionPolicy, null, readOnly,
				DEFAULT_TERMS_INDEX_DIVISOR);
	}

	public static IndexReader open(final Directory directory,
			IndexDeletionPolicy deletionPolicy, boolean readOnly,
			int termInfosIndexDivisor) throws CorruptIndexException,
			IOException {
		return open(directory, deletionPolicy, null, readOnly,
				termInfosIndexDivisor);
	}

	public static IndexReader open(final IndexCommit commit,
			IndexDeletionPolicy deletionPolicy, boolean readOnly)
			throws CorruptIndexException, IOException {
		return open(commit.getDirectory(), deletionPolicy, commit, readOnly,
				DEFAULT_TERMS_INDEX_DIVISOR);
	}

	public static IndexReader open(final IndexCommit commit,
			IndexDeletionPolicy deletionPolicy, boolean readOnly,
			int termInfosIndexDivisor) throws CorruptIndexException,
			IOException {
		return open(commit.getDirectory(), deletionPolicy, commit, readOnly,
				termInfosIndexDivisor);
	}

	private static IndexReader open(final Directory directory,
			final IndexDeletionPolicy deletionPolicy, final IndexCommit commit,
			final boolean readOnly, int termInfosIndexDivisor)
			throws CorruptIndexException, IOException {
		return DirectoryReader.open(directory, deletionPolicy, commit,
				readOnly, termInfosIndexDivisor);
	}

	public static IndexReader openIfChanged(IndexReader oldReader)
			throws IOException {
		if (oldReader.hasNewReopenAPI1) {
			final IndexReader newReader = oldReader.doOpenIfChanged();
			assert newReader != oldReader;
			return newReader;
		} else {
			final IndexReader newReader = oldReader.reopen();
			if (newReader == oldReader) {
				return null;
			} else {
				return newReader;
			}
		}
	}

	public static IndexReader openIfChanged(IndexReader oldReader,
			boolean readOnly) throws IOException {
		if (oldReader.hasNewReopenAPI2) {
			final IndexReader newReader = oldReader.doOpenIfChanged(readOnly);
			assert newReader != oldReader;
			return newReader;
		} else {
			final IndexReader newReader = oldReader.reopen(readOnly);
			if (newReader == oldReader) {
				return null;
			} else {
				return newReader;
			}
		}
	}

	public static IndexReader openIfChanged(IndexReader oldReader,
			IndexCommit commit) throws IOException {
		if (oldReader.hasNewReopenAPI3) {
			final IndexReader newReader = oldReader.doOpenIfChanged(commit);
			assert newReader != oldReader;
			return newReader;
		} else {
			final IndexReader newReader = oldReader.reopen(commit);
			if (newReader == oldReader) {
				return null;
			} else {
				return newReader;
			}
		}
	}

	public static IndexReader openIfChanged(IndexReader oldReader,
			IndexWriter writer, boolean applyAllDeletes) throws IOException {
		if (oldReader.hasNewReopenAPI4) {
			final IndexReader newReader = oldReader.doOpenIfChanged(writer,
					applyAllDeletes);
			assert newReader != oldReader;
			return newReader;
		} else {
			final IndexReader newReader = oldReader.reopen(writer,
					applyAllDeletes);
			if (newReader == oldReader) {
				return null;
			} else {
				return newReader;
			}
		}
	}

	@Deprecated
	public IndexReader reopen() throws CorruptIndexException, IOException {
		final IndexReader newReader = IndexReader.openIfChanged(this);
		if (newReader == null) {
			return this;
		} else {
			return newReader;
		}
	}

	@Deprecated
	public IndexReader reopen(boolean openReadOnly)
			throws CorruptIndexException, IOException {
		final IndexReader newReader = IndexReader.openIfChanged(this,
				openReadOnly);
		if (newReader == null) {
			return this;
		} else {
			return newReader;
		}
	}

	@Deprecated
	public IndexReader reopen(IndexCommit commit) throws CorruptIndexException,
			IOException {
		final IndexReader newReader = IndexReader.openIfChanged(this, commit);
		if (newReader == null) {
			return this;
		} else {
			return newReader;
		}
	}

	@Deprecated
	public IndexReader reopen(IndexWriter writer, boolean applyAllDeletes)
			throws CorruptIndexException, IOException {
		final IndexReader newReader = IndexReader.openIfChanged(this, writer,
				applyAllDeletes);
		if (newReader == null) {
			return this;
		} else {
			return newReader;
		}
	}

	protected IndexReader doOpenIfChanged() throws CorruptIndexException,
			IOException {
		throw new UnsupportedOperationException(
				"This reader does not support reopen().");
	}

	protected IndexReader doOpenIfChanged(boolean openReadOnly)
			throws CorruptIndexException, IOException {
		throw new UnsupportedOperationException(
				"This reader does not support reopen().");
	}

	protected IndexReader doOpenIfChanged(final IndexCommit commit)
			throws CorruptIndexException, IOException {
		throw new UnsupportedOperationException(
				"This reader does not support reopen(IndexCommit).");
	}

	protected IndexReader doOpenIfChanged(IndexWriter writer,
			boolean applyAllDeletes) throws CorruptIndexException, IOException {
		return writer.getReader(applyAllDeletes);
	}

	@Override
	public synchronized Object clone() {
		throw new UnsupportedOperationException(
				"This reader does not implement clone()");
	}

	public synchronized IndexReader clone(boolean openReadOnly)
			throws CorruptIndexException, IOException {
		throw new UnsupportedOperationException(
				"This reader does not implement clone()");
	}

	public Directory directory() {
		ensureOpen();
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}

	public static long lastModified(final Directory directory2)
			throws CorruptIndexException, IOException {
		return ((Long) new SegmentInfos.FindSegmentsFile(directory2) {
			@Override
			public Object doBody(String segmentFileName) throws IOException {
				return Long.valueOf(directory2.fileModified(segmentFileName));
			}
		}.run()).longValue();
	}

	public static long getCurrentVersion(Directory directory)
			throws CorruptIndexException, IOException {
		return SegmentInfos.readCurrentVersion(directory);
	}

	public static Map<String, String> getCommitUserData(Directory directory)
			throws CorruptIndexException, IOException {
		return SegmentInfos.readCurrentUserData(directory);
	}

	public long getVersion() {
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}

	public Map<String, String> getCommitUserData() {
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}

	public boolean isCurrent() throws CorruptIndexException, IOException {
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}

	@Deprecated
	public boolean isOptimized() {
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}

	abstract public TermFreqVector[] getTermFreqVectors(int docNumber)
			throws IOException;

	abstract public TermFreqVector getTermFreqVector(int docNumber, String field)
			throws IOException;

	abstract public void getTermFreqVector(int docNumber, String field,
			TermVectorMapper mapper) throws IOException;

	abstract public void getTermFreqVector(int docNumber,
			TermVectorMapper mapper) throws IOException;

	public static boolean indexExists(Directory directory) throws IOException {
		try {
			new SegmentInfos().read(directory);
			return true;
		} catch (IOException ioe) {
			return false;
		}
	}

	public abstract int numDocs();

	public abstract int maxDoc();

	public int numDeletedDocs() {
		return maxDoc() - numDocs();
	}

	public Document document(int n) throws CorruptIndexException, IOException {
		ensureOpen();
		if (n < 0 || n >= maxDoc()) {
			throw new IllegalArgumentException(
					"docID must be >= 0 and < maxDoc=" + maxDoc()
							+ " (got docID=" + n + ")");
		}
		return document(n, null);
	}

	public abstract Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException;

	public abstract boolean isDeleted(int n);

	public abstract boolean hasDeletions();

	public boolean hasNorms(String field) throws IOException {

		ensureOpen();
		return norms(field) != null;
	}

	public abstract byte[] norms(String field) throws IOException;

	public abstract void norms(String field, byte[] bytes, int offset)
			throws IOException;

	public synchronized void setNorm(int doc, String field, byte value)
			throws StaleReaderException, CorruptIndexException,
			LockObtainFailedException, IOException {
		ensureOpen();
		acquireWriteLock();
		hasChanges = true;
		doSetNorm(doc, field, value);
	}

	protected abstract void doSetNorm(int doc, String field, byte value)
			throws CorruptIndexException, IOException;

	@Deprecated
	public void setNorm(int doc, String field, float value)
			throws StaleReaderException, CorruptIndexException,
			LockObtainFailedException, IOException {
		ensureOpen();
		setNorm(doc, field, Similarity.getDefault().encodeNormValue(value));
	}

	public abstract TermEnum terms() throws IOException;

	public abstract TermEnum terms(Term t) throws IOException;

	public abstract int docFreq(Term t) throws IOException;

	public TermDocs termDocs(Term term) throws IOException {
		ensureOpen();
		TermDocs termDocs = termDocs();
		termDocs.seek(term);
		return termDocs;
	}

	public abstract TermDocs termDocs() throws IOException;

	public TermPositions termPositions(Term term) throws IOException {
		ensureOpen();
		TermPositions termPositions = termPositions();
		termPositions.seek(term);
		return termPositions;
	}

	public abstract TermPositions termPositions() throws IOException;

	public synchronized void deleteDocument(int docNum)
			throws StaleReaderException, CorruptIndexException,
			LockObtainFailedException, IOException {
		ensureOpen();
		acquireWriteLock();
		hasChanges = true;
		doDelete(docNum);
	}

	protected abstract void doDelete(int docNum) throws CorruptIndexException,
			IOException;

	public int deleteDocuments(Term term) throws StaleReaderException,
			CorruptIndexException, LockObtainFailedException, IOException {
		ensureOpen();
		TermDocs docs = termDocs(term);
		if (docs == null)
			return 0;
		int n = 0;
		try {
			while (docs.next()) {
				deleteDocument(docs.doc());
				n++;
			}
		} finally {
			docs.close();
		}
		return n;
	}

	public synchronized void undeleteAll() throws StaleReaderException,
			CorruptIndexException, LockObtainFailedException, IOException {
		ensureOpen();
		acquireWriteLock();
		hasChanges = true;
		doUndeleteAll();
	}

	protected abstract void doUndeleteAll() throws CorruptIndexException,
			IOException;

	protected synchronized void acquireWriteLock() throws IOException {

	}

	public final synchronized void flush() throws IOException {
		ensureOpen();
		commit();
	}

	public final synchronized void flush(Map<String, String> commitUserData)
			throws IOException {
		ensureOpen();
		commit(commitUserData);
	}

	protected final synchronized void commit() throws IOException {
		commit(null);
	}

	public final synchronized void commit(Map<String, String> commitUserData)
			throws IOException {

		doCommit(commitUserData);
		hasChanges = false;
	}

	protected abstract void doCommit(Map<String, String> commitUserData)
			throws IOException;

	public final synchronized void close() throws IOException {
		if (!closed) {
			decRef();
			closed = true;
		}
	}

	protected abstract void doClose() throws IOException;

	public abstract Collection<String> getFieldNames(FieldOption fldOption);

	public IndexCommit getIndexCommit() throws IOException {
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}

	public static void main(String[] args) {
		String filename = null;
		boolean extract = false;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-extract")) {
				extract = true;
			} else if (filename == null) {
				filename = args[i];
			}
		}

		if (filename == null) {
			System.out
					.println("Usage: org.shatam.shatam.index.IndexReader [-extract] <cfsfile>");
			return;
		}

		Directory dir = null;
		CompoundFileReader cfr = null;

		try {
			File file = new File(filename);
			String dirname = file.getAbsoluteFile().getParent();
			filename = file.getName();
			dir = FSDirectory.open(new File(dirname));
			cfr = new CompoundFileReader(dir, filename);

			String[] files = cfr.listAll();
			ArrayUtil.mergeSort(files);

			for (int i = 0; i < files.length; ++i) {
				long len = cfr.fileLength(files[i]);

				if (extract) {
					System.out.println("extract " + files[i] + " with " + len
							+ " bytes to local directory...");
					IndexInput ii = cfr.openInput(files[i]);

					FileOutputStream f = new FileOutputStream(files[i]);

					byte[] buffer = new byte[1024];
					int chunk = buffer.length;
					while (len > 0) {
						final int bufLen = (int) Math.min(chunk, len);
						ii.readBytes(buffer, 0, bufLen);
						f.write(buffer, 0, bufLen);
						len -= bufLen;
					}

					f.close();
					ii.close();
				} else
					System.out.println(files[i] + ": " + len + " bytes");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				if (dir != null)
					dir.close();
				if (cfr != null)
					cfr.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public static Collection<IndexCommit> listCommits(Directory dir)
			throws IOException {
		return DirectoryReader.listCommits(dir);
	}

	public IndexReader[] getSequentialSubReaders() {
		ensureOpen();
		return null;
	}

	public Object getCoreCacheKey() {

		return this;
	}

	public Object getDeletesCacheKey() {
		return this;
	}

	public long getUniqueTermCount() throws IOException {
		throw new UnsupportedOperationException(
				"this reader does not implement getUniqueTermCount()");
	}

	@Deprecated
	private static final VirtualMethod<IndexReader> reopenMethod1 = new VirtualMethod<IndexReader>(
			IndexReader.class, "reopen");
	@Deprecated
	private static final VirtualMethod<IndexReader> doOpenIfChangedMethod1 = new VirtualMethod<IndexReader>(
			IndexReader.class, "doOpenIfChanged");
	@Deprecated
	private final boolean hasNewReopenAPI1 = VirtualMethod
			.compareImplementationDistance(getClass(), doOpenIfChangedMethod1,
					reopenMethod1) >= 0;

	@Deprecated
	private static final VirtualMethod<IndexReader> reopenMethod2 = new VirtualMethod<IndexReader>(
			IndexReader.class, "reopen", boolean.class);
	@Deprecated
	private static final VirtualMethod<IndexReader> doOpenIfChangedMethod2 = new VirtualMethod<IndexReader>(
			IndexReader.class, "doOpenIfChanged", boolean.class);
	@Deprecated
	private final boolean hasNewReopenAPI2 = VirtualMethod
			.compareImplementationDistance(getClass(), doOpenIfChangedMethod2,
					reopenMethod2) >= 0;

	@Deprecated
	private static final VirtualMethod<IndexReader> reopenMethod3 = new VirtualMethod<IndexReader>(
			IndexReader.class, "reopen", IndexCommit.class);
	@Deprecated
	private static final VirtualMethod<IndexReader> doOpenIfChangedMethod3 = new VirtualMethod<IndexReader>(
			IndexReader.class, "doOpenIfChanged", IndexCommit.class);
	@Deprecated
	private final boolean hasNewReopenAPI3 = VirtualMethod
			.compareImplementationDistance(getClass(), doOpenIfChangedMethod3,
					reopenMethod3) >= 0;

	@Deprecated
	private static final VirtualMethod<IndexReader> reopenMethod4 = new VirtualMethod<IndexReader>(
			IndexReader.class, "reopen", IndexWriter.class, boolean.class);
	@Deprecated
	private static final VirtualMethod<IndexReader> doOpenIfChangedMethod4 = new VirtualMethod<IndexReader>(
			IndexReader.class, "doOpenIfChanged", IndexWriter.class,
			boolean.class);
	@Deprecated
	private final boolean hasNewReopenAPI4 = VirtualMethod
			.compareImplementationDistance(getClass(), doOpenIfChangedMethod4,
					reopenMethod4) >= 0;

	public int getTermInfosIndexDivisor() {
		throw new UnsupportedOperationException(
				"This reader does not support this method.");
	}
}
