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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.analysis.LimitTokenCountAnalyzer;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.index.IndexWriterConfig.OpenMode;
import com.shatam.shatamindex.index.PayloadProcessorProvider.DirPayloadProcessor;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.store.AlreadyClosedException;
import com.shatam.shatamindex.store.BufferedIndexInput;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.Lock;
import com.shatam.shatamindex.store.LockObtainFailedException;
import com.shatam.shatamindex.util.Constants;
import com.shatam.shatamindex.util.MapBackedSet;
import com.shatam.shatamindex.util.StringHelper;
import com.shatam.shatamindex.util.ThreadInterruptedException;
import com.shatam.shatamindex.util.TwoPhaseCommit;
import com.shatam.shatamindex.util.Version;

public class IndexWriter implements Closeable, TwoPhaseCommit {

	@Deprecated
	public static long WRITE_LOCK_TIMEOUT = IndexWriterConfig.WRITE_LOCK_TIMEOUT;

	private long writeLockTimeout;

	public static final String WRITE_LOCK_NAME = "write.lock";

	@Deprecated
	public final static int DISABLE_AUTO_FLUSH = IndexWriterConfig.DISABLE_AUTO_FLUSH;

	@Deprecated
	public final static int DEFAULT_MAX_BUFFERED_DOCS = IndexWriterConfig.DEFAULT_MAX_BUFFERED_DOCS;

	@Deprecated
	public final static double DEFAULT_RAM_BUFFER_SIZE_MB = IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB;

	@Deprecated
	public final static int DEFAULT_MAX_BUFFERED_DELETE_TERMS = IndexWriterConfig.DEFAULT_MAX_BUFFERED_DELETE_TERMS;

	@Deprecated
	public final static int DEFAULT_MAX_FIELD_LENGTH = MaxFieldLength.UNLIMITED
			.getLimit();

	@Deprecated
	public final static int DEFAULT_TERM_INDEX_INTERVAL = IndexWriterConfig.DEFAULT_TERM_INDEX_INTERVAL;

	public final static int MAX_TERM_LENGTH = DocumentsWriter.MAX_TERM_LENGTH;

	private final static int MERGE_READ_BUFFER_SIZE = 4096;

	private static final AtomicInteger MESSAGE_ID = new AtomicInteger();
	private int messageID = MESSAGE_ID.getAndIncrement();
	volatile private boolean hitOOM;

	private final Directory directory;
	private final Analyzer analyzer;

	private/* final */Similarity similarity = Similarity.getDefault();

	private volatile long changeCount;

	private long lastCommitChangeCount;

	private List<SegmentInfo> rollbackSegments;

	volatile SegmentInfos pendingCommit;

	volatile long pendingCommitChangeCount;

	final SegmentInfos segmentInfos = new SegmentInfos();

	private DocumentsWriter docWriter;
	private IndexFileDeleter deleter;

	private Map<SegmentInfo, Boolean> segmentsToMerge = new HashMap<SegmentInfo, Boolean>();
	private int mergeMaxNumSegments;

	private Lock writeLock;

	private volatile boolean closed;
	private volatile boolean closing;

	private HashSet<SegmentInfo> mergingSegments = new HashSet<SegmentInfo>();

	private MergePolicy mergePolicy;

	private/* final */MergeScheduler mergeScheduler;
	private LinkedList<MergePolicy.OneMerge> pendingMerges = new LinkedList<MergePolicy.OneMerge>();
	private Set<MergePolicy.OneMerge> runningMerges = new HashSet<MergePolicy.OneMerge>();
	private List<MergePolicy.OneMerge> mergeExceptions = new ArrayList<MergePolicy.OneMerge>();
	private long mergeGen;
	private boolean stopMerges;

	private final AtomicInteger flushCount = new AtomicInteger();
	private final AtomicInteger flushDeletesCount = new AtomicInteger();

	final ReaderPool readerPool = new ReaderPool();
	final BufferedDeletesStream bufferedDeletesStream;

	private volatile boolean poolReaders;

	private final IndexWriterConfig config;

	private PayloadProcessorProvider payloadProcessorProvider;

	boolean anyNonBulkMerges;

	@Deprecated
	public IndexReader getReader() throws IOException {
		return getReader(config.getReaderTermsIndexDivisor(), true);
	}

	IndexReader getReader(boolean applyAllDeletes) throws IOException {
		return getReader(config.getReaderTermsIndexDivisor(), applyAllDeletes);
	}

	@Deprecated
	public IndexReader getReader(int termInfosIndexDivisor) throws IOException {
		return getReader(termInfosIndexDivisor, true);
	}

	IndexReader getReader(int termInfosIndexDivisor, boolean applyAllDeletes)
			throws IOException {
		ensureOpen();

		final long tStart = System.currentTimeMillis();

		if (infoStream != null) {
			message("flush at getReader");
		}

		poolReaders = true;

		IndexReader r;
		synchronized (this) {
			flush(false, applyAllDeletes);
			r = new ReadOnlyDirectoryReader(this, segmentInfos,
					termInfosIndexDivisor, applyAllDeletes);
			if (infoStream != null) {
				message("return reader version=" + r.getVersion() + " reader="
						+ r);
			}
		}

		maybeMerge();

		if (infoStream != null) {
			message("getReader took " + (System.currentTimeMillis() - tStart)
					+ " msec");
		}
		return r;
	}

	private final Collection<IndexReader.ReaderFinishedListener> readerFinishedListeners = new MapBackedSet<IndexReader.ReaderFinishedListener>(
			new ConcurrentHashMap<IndexReader.ReaderFinishedListener, Boolean>());

	Collection<IndexReader.ReaderFinishedListener> getReaderFinishedListeners()
			throws IOException {
		return readerFinishedListeners;
	}

	class ReaderPool {

		private final Map<SegmentInfo, SegmentReader> readerMap = new HashMap<SegmentInfo, SegmentReader>();

		synchronized void clear(List<SegmentInfo> infos) throws IOException {
			if (infos == null) {
				for (Map.Entry<SegmentInfo, SegmentReader> ent : readerMap
						.entrySet()) {
					ent.getValue().hasChanges = false;
				}
			} else {
				for (final SegmentInfo info : infos) {
					final SegmentReader r = readerMap.get(info);
					if (r != null) {
						r.hasChanges = false;
					}
				}
			}
		}

		public synchronized boolean infoIsLive(SegmentInfo info) {
			int idx = segmentInfos.indexOf(info);
			assert idx != -1 : "info=" + info + " isn't in pool";
			assert segmentInfos.info(idx) == info : "info=" + info
					+ " doesn't match live info in segmentInfos";
			return true;
		}

		public synchronized SegmentInfo mapToLive(SegmentInfo info) {
			int idx = segmentInfos.indexOf(info);
			if (idx != -1) {
				info = segmentInfos.info(idx);
			}
			return info;
		}

		public synchronized boolean release(SegmentReader sr)
				throws IOException {
			return release(sr, false);
		}

		public synchronized boolean release(SegmentReader sr, boolean drop)
				throws IOException {

			final boolean pooled = readerMap.containsKey(sr.getSegmentInfo());

			assert !pooled || readerMap.get(sr.getSegmentInfo()) == sr;

			sr.decRef();

			if (pooled && (drop || (!poolReaders && sr.getRefCount() == 1))) {

				assert !sr.hasChanges || Thread.holdsLock(IndexWriter.this);

				sr.hasChanges &= !drop;

				final boolean hasChanges = sr.hasChanges;

				sr.close();

				readerMap.remove(sr.getSegmentInfo());

				return hasChanges;
			}

			return false;
		}

		public synchronized void drop(List<SegmentInfo> infos)
				throws IOException {
			for (SegmentInfo info : infos) {
				drop(info);
			}
		}

		public synchronized void drop(SegmentInfo info) throws IOException {
			final SegmentReader sr = readerMap.get(info);
			if (sr != null) {
				sr.hasChanges = false;
				readerMap.remove(info);
				sr.close();
			}
		}

		public synchronized void dropAll() throws IOException {
			for (SegmentReader reader : readerMap.values()) {
				reader.hasChanges = false;

				reader.decRef();
			}
			readerMap.clear();
		}

		synchronized void close() throws IOException {

			assert Thread.holdsLock(IndexWriter.this);

			for (Map.Entry<SegmentInfo, SegmentReader> ent : readerMap
					.entrySet()) {

				SegmentReader sr = ent.getValue();
				if (sr.hasChanges) {
					assert infoIsLive(sr.getSegmentInfo());
					sr.doCommit(null);

					deleter.checkpoint(segmentInfos, false);
				}

				sr.decRef();
			}

			readerMap.clear();
		}

		synchronized void commit(SegmentInfos infos) throws IOException {

			assert Thread.holdsLock(IndexWriter.this);

			for (SegmentInfo info : infos) {

				final SegmentReader sr = readerMap.get(info);
				if (sr != null && sr.hasChanges) {
					assert infoIsLive(info);
					sr.doCommit(null);

					deleter.checkpoint(segmentInfos, false);
				}
			}
		}

		public synchronized SegmentReader getReadOnlyClone(SegmentInfo info,
				boolean doOpenStores, int termInfosIndexDivisor)
				throws IOException {
			SegmentReader sr = get(info, doOpenStores,
					BufferedIndexInput.BUFFER_SIZE, termInfosIndexDivisor);
			try {
				return (SegmentReader) sr.clone(true);
			} finally {
				sr.decRef();
			}
		}

		public synchronized SegmentReader get(SegmentInfo info,
				boolean doOpenStores) throws IOException {
			return get(info, doOpenStores, BufferedIndexInput.BUFFER_SIZE,
					config.getReaderTermsIndexDivisor());
		}

		public synchronized SegmentReader get(SegmentInfo info,
				boolean doOpenStores, int readBufferSize, int termsIndexDivisor)
				throws IOException {

			if (poolReaders) {
				readBufferSize = BufferedIndexInput.BUFFER_SIZE;
			}

			SegmentReader sr = readerMap.get(info);
			if (sr == null) {

				sr = SegmentReader.get(false, info.dir, info, readBufferSize,
						doOpenStores, termsIndexDivisor);
				sr.readerFinishedListeners = readerFinishedListeners;

				if (info.dir == directory) {

					readerMap.put(info, sr);
				}
			} else {
				if (doOpenStores) {
					sr.openDocStores();
				}
				if (termsIndexDivisor != -1 && !sr.termsIndexLoaded()) {

					sr.loadTermsIndex(termsIndexDivisor);
				}
			}

			if (info.dir == directory) {

				sr.incRef();
			}
			return sr;
		}

		public synchronized SegmentReader getIfExists(SegmentInfo info)
				throws IOException {
			SegmentReader sr = readerMap.get(info);
			if (sr != null) {
				sr.incRef();
			}
			return sr;
		}
	}

	public int numDeletedDocs(SegmentInfo info) throws IOException {
		ensureOpen(false);
		SegmentReader reader = readerPool.getIfExists(info);
		try {
			if (reader != null) {
				return reader.numDeletedDocs();
			} else {
				return info.getDelCount();
			}
		} finally {
			if (reader != null) {
				readerPool.release(reader);
			}
		}
	}

	protected final void ensureOpen(boolean includePendingClose)
			throws AlreadyClosedException {
		if (closed || (includePendingClose && closing)) {
			throw new AlreadyClosedException("this IndexWriter is closed");
		}
	}

	protected final void ensureOpen() throws AlreadyClosedException {
		ensureOpen(true);
	}

	public void message(String message) {
		if (infoStream != null)
			infoStream.println("IW " + messageID + " [" + new Date() + "; "
					+ Thread.currentThread().getName() + "]: " + message);
	}

	private LogMergePolicy getLogMergePolicy() {
		if (mergePolicy instanceof LogMergePolicy)
			return (LogMergePolicy) mergePolicy;
		else
			throw new IllegalArgumentException(
					"this method can only be called when the merge policy is the default LogMergePolicy");
	}

	@Deprecated
	public boolean getUseCompoundFile() {
		return getLogMergePolicy().getUseCompoundFile();
	}

	@Deprecated
	public void setUseCompoundFile(boolean value) {
		getLogMergePolicy().setUseCompoundFile(value);
	}

	@Deprecated
	public void setSimilarity(Similarity similarity) {
		ensureOpen();
		this.similarity = similarity;
		docWriter.setSimilarity(similarity);

		config.setSimilarity(similarity);
	}

	@Deprecated
	public Similarity getSimilarity() {
		ensureOpen();
		return similarity;
	}

	@Deprecated
	public void setTermIndexInterval(int interval) {
		ensureOpen();
		config.setTermIndexInterval(interval);
	}

	@Deprecated
	public int getTermIndexInterval() {

		ensureOpen(false);
		return config.getTermIndexInterval();
	}

	@Deprecated
	public IndexWriter(Directory d, Analyzer a, boolean create,
			MaxFieldLength mfl) throws CorruptIndexException,
			LockObtainFailedException, IOException {
		this(d, new IndexWriterConfig(Version.SHATAM_31, a)
				.setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND));
		setMaxFieldLength(mfl.getLimit());
	}

	@Deprecated
	public IndexWriter(Directory d, Analyzer a, MaxFieldLength mfl)
			throws CorruptIndexException, LockObtainFailedException,
			IOException {
		this(d, new IndexWriterConfig(Version.SHATAM_31, a));
		setMaxFieldLength(mfl.getLimit());
	}

	@Deprecated
	public IndexWriter(Directory d, Analyzer a,
			IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl)
			throws CorruptIndexException, LockObtainFailedException,
			IOException {
		this(d, new IndexWriterConfig(Version.SHATAM_31, a)
				.setIndexDeletionPolicy(deletionPolicy));
		setMaxFieldLength(mfl.getLimit());
	}

	@Deprecated
	public IndexWriter(Directory d, Analyzer a, boolean create,
			IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl)
			throws CorruptIndexException, LockObtainFailedException,
			IOException {
		this(d, new IndexWriterConfig(Version.SHATAM_31, a).setOpenMode(
				create ? OpenMode.CREATE : OpenMode.APPEND)
				.setIndexDeletionPolicy(deletionPolicy));
		setMaxFieldLength(mfl.getLimit());
	}

	@Deprecated
	public IndexWriter(Directory d, Analyzer a,
			IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl,
			IndexCommit commit) throws CorruptIndexException,
			LockObtainFailedException, IOException {
		this(d, new IndexWriterConfig(Version.SHATAM_31, a)
				.setOpenMode(OpenMode.APPEND)
				.setIndexDeletionPolicy(deletionPolicy).setIndexCommit(commit));
		setMaxFieldLength(mfl.getLimit());
	}

	public IndexWriter(Directory d, IndexWriterConfig conf)
			throws CorruptIndexException, LockObtainFailedException,
			IOException {
		config = (IndexWriterConfig) conf.clone();
		directory = d;
		analyzer = conf.getAnalyzer();
		infoStream = defaultInfoStream;
		writeLockTimeout = conf.getWriteLockTimeout();
		similarity = conf.getSimilarity();
		mergePolicy = conf.getMergePolicy();
		mergePolicy.setIndexWriter(this);
		mergeScheduler = conf.getMergeScheduler();
		bufferedDeletesStream = new BufferedDeletesStream(messageID);
		bufferedDeletesStream.setInfoStream(infoStream);
		poolReaders = conf.getReaderPooling();

		writeLock = directory.makeLock(WRITE_LOCK_NAME);

		if (!writeLock.obtain(writeLockTimeout))
			throw new LockObtainFailedException("Index locked for write: "
					+ writeLock);

		OpenMode mode = conf.getOpenMode();
		boolean create;
		if (mode == OpenMode.CREATE) {
			create = true;
		} else if (mode == OpenMode.APPEND) {
			create = false;
		} else {

			create = !IndexReader.indexExists(directory);
		}

		boolean success = false;

		try {
			if (create) {

				try {
					segmentInfos.read(directory);
					segmentInfos.clear();
				} catch (IOException e) {

				}

				changeCount++;
				segmentInfos.changed();
			} else {
				segmentInfos.read(directory);

				IndexCommit commit = conf.getIndexCommit();
				if (commit != null) {

					if (commit.getDirectory() != directory)
						throw new IllegalArgumentException(
								"IndexCommit's directory doesn't match my directory");
					SegmentInfos oldInfos = new SegmentInfos();
					oldInfos.read(directory, commit.getSegmentsFileName());
					segmentInfos.replace(oldInfos);
					changeCount++;
					segmentInfos.changed();
					if (infoStream != null)
						message("init: loaded commit \""
								+ commit.getSegmentsFileName() + "\"");
				}
			}

			rollbackSegments = segmentInfos.createBackupSegmentInfos(true);

			docWriter = new DocumentsWriter(config, directory, this,
					getCurrentFieldInfos(), bufferedDeletesStream);
			docWriter.setInfoStream(infoStream);
			docWriter.setMaxFieldLength(maxFieldLength);

			synchronized (this) {
				deleter = new IndexFileDeleter(directory,
						conf.getIndexDeletionPolicy(), segmentInfos,
						infoStream, this);
			}

			if (deleter.startingCommitDeleted) {

				changeCount++;
				segmentInfos.changed();
			}

			if (infoStream != null) {
				messageState();
			}

			success = true;

		} finally {
			if (!success) {
				if (infoStream != null) {
					message("init: hit exception on init; releasing write lock");
				}
				try {
					writeLock.release();
				} catch (Throwable t) {

				}
				writeLock = null;
			}
		}
	}

	private FieldInfos getFieldInfos(SegmentInfo info) throws IOException {
		Directory cfsDir = null;
		try {
			if (info.getUseCompoundFile()) {
				cfsDir = new CompoundFileReader(directory,
						IndexFileNames.segmentFileName(info.name,
								IndexFileNames.COMPOUND_FILE_EXTENSION));
			} else {
				cfsDir = directory;
			}
			return new FieldInfos(cfsDir, IndexFileNames.segmentFileName(
					info.name, IndexFileNames.FIELD_INFOS_EXTENSION));
		} finally {
			if (info.getUseCompoundFile() && cfsDir != null) {
				cfsDir.close();
			}
		}
	}

	private FieldInfos getCurrentFieldInfos() throws IOException {
		final FieldInfos fieldInfos;
		if (segmentInfos.size() > 0) {
			if (segmentInfos.getFormat() > SegmentInfos.FORMAT_DIAGNOSTICS) {

				fieldInfos = new FieldInfos();
				for (SegmentInfo info : segmentInfos) {
					final FieldInfos segFieldInfos = getFieldInfos(info);
					final int fieldCount = segFieldInfos.size();
					for (int fieldNumber = 0; fieldNumber < fieldCount; fieldNumber++) {
						fieldInfos.add(segFieldInfos.fieldInfo(fieldNumber));
					}
				}
			} else {

				fieldInfos = getFieldInfos(segmentInfos.info(segmentInfos
						.size() - 1));
			}
		} else {
			fieldInfos = new FieldInfos();
		}
		return fieldInfos;
	}

	public IndexWriterConfig getConfig() {
		ensureOpen(false);
		return config;
	}

	@Deprecated
	public void setMergePolicy(MergePolicy mp) {
		ensureOpen();
		if (mp == null)
			throw new NullPointerException("MergePolicy must be non-null");

		if (mergePolicy != mp)
			mergePolicy.close();
		mergePolicy = mp;
		mergePolicy.setIndexWriter(this);
		pushMaxBufferedDocs();
		if (infoStream != null)
			message("setMergePolicy " + mp);

		config.setMergePolicy(mp);
	}

	@Deprecated
	public MergePolicy getMergePolicy() {
		ensureOpen();
		return mergePolicy;
	}

	@Deprecated
	synchronized public void setMergeScheduler(MergeScheduler mergeScheduler)
			throws CorruptIndexException, IOException {
		ensureOpen();
		if (mergeScheduler == null)
			throw new NullPointerException("MergeScheduler must be non-null");

		if (this.mergeScheduler != mergeScheduler) {
			finishMerges(true);
			this.mergeScheduler.close();
		}
		this.mergeScheduler = mergeScheduler;
		if (infoStream != null)
			message("setMergeScheduler " + mergeScheduler);

		config.setMergeScheduler(mergeScheduler);
	}

	@Deprecated
	public MergeScheduler getMergeScheduler() {
		ensureOpen();
		return mergeScheduler;
	}

	@Deprecated
	public void setMaxMergeDocs(int maxMergeDocs) {
		getLogMergePolicy().setMaxMergeDocs(maxMergeDocs);
	}

	@Deprecated
	public int getMaxMergeDocs() {
		return getLogMergePolicy().getMaxMergeDocs();
	}

	@Deprecated
	public void setMaxFieldLength(int maxFieldLength) {
		ensureOpen();
		this.maxFieldLength = maxFieldLength;
		docWriter.setMaxFieldLength(maxFieldLength);
		if (infoStream != null)
			message("setMaxFieldLength " + maxFieldLength);
	}

	@Deprecated
	public int getMaxFieldLength() {
		ensureOpen();
		return maxFieldLength;
	}

	@Deprecated
	public void setReaderTermsIndexDivisor(int divisor) {
		ensureOpen();
		config.setReaderTermsIndexDivisor(divisor);
		if (infoStream != null) {
			message("setReaderTermsIndexDivisor " + divisor);
		}
	}

	@Deprecated
	public int getReaderTermsIndexDivisor() {
		ensureOpen();
		return config.getReaderTermsIndexDivisor();
	}

	@Deprecated
	public void setMaxBufferedDocs(int maxBufferedDocs) {
		ensureOpen();
		pushMaxBufferedDocs();
		if (infoStream != null) {
			message("setMaxBufferedDocs " + maxBufferedDocs);
		}

		config.setMaxBufferedDocs(maxBufferedDocs);
	}

	private void pushMaxBufferedDocs() {
		if (config.getMaxBufferedDocs() != DISABLE_AUTO_FLUSH) {
			final MergePolicy mp = mergePolicy;
			if (mp instanceof LogDocMergePolicy) {
				LogDocMergePolicy lmp = (LogDocMergePolicy) mp;
				final int maxBufferedDocs = config.getMaxBufferedDocs();
				if (lmp.getMinMergeDocs() != maxBufferedDocs) {
					if (infoStream != null)
						message("now push maxBufferedDocs " + maxBufferedDocs
								+ " to LogDocMergePolicy");
					lmp.setMinMergeDocs(maxBufferedDocs);
				}
			}
		}
	}

	@Deprecated
	public int getMaxBufferedDocs() {
		ensureOpen();
		return config.getMaxBufferedDocs();
	}

	@Deprecated
	public void setRAMBufferSizeMB(double mb) {
		if (infoStream != null) {
			message("setRAMBufferSizeMB " + mb);
		}

		config.setRAMBufferSizeMB(mb);
	}

	@Deprecated
	public double getRAMBufferSizeMB() {
		return config.getRAMBufferSizeMB();
	}

	@Deprecated
	public void setMaxBufferedDeleteTerms(int maxBufferedDeleteTerms) {
		ensureOpen();
		if (infoStream != null)
			message("setMaxBufferedDeleteTerms " + maxBufferedDeleteTerms);

		config.setMaxBufferedDeleteTerms(maxBufferedDeleteTerms);
	}

	@Deprecated
	public int getMaxBufferedDeleteTerms() {
		ensureOpen();
		return config.getMaxBufferedDeleteTerms();
	}

	@Deprecated
	public void setMergeFactor(int mergeFactor) {
		getLogMergePolicy().setMergeFactor(mergeFactor);
	}

	@Deprecated
	public int getMergeFactor() {
		return getLogMergePolicy().getMergeFactor();
	}

	public static void setDefaultInfoStream(PrintStream infoStream) {
		IndexWriter.defaultInfoStream = infoStream;
	}

	public static PrintStream getDefaultInfoStream() {
		return IndexWriter.defaultInfoStream;
	}

	public void setInfoStream(PrintStream infoStream) throws IOException {
		ensureOpen();
		this.infoStream = infoStream;
		docWriter.setInfoStream(infoStream);
		deleter.setInfoStream(infoStream);
		bufferedDeletesStream.setInfoStream(infoStream);
		if (infoStream != null)
			messageState();
	}

	private void messageState() throws IOException {
		message("\ndir=" + directory + "\n" + "index=" + segString() + "\n"
				+ "version=" + Constants.SHATAM_VERSION + "\n"
				+ config.toString());
	}

	public PrintStream getInfoStream() {
		ensureOpen();
		return infoStream;
	}

	public boolean verbose() {
		return infoStream != null;
	}

	@Deprecated
	public void setWriteLockTimeout(long writeLockTimeout) {
		ensureOpen();
		this.writeLockTimeout = writeLockTimeout;

		config.setWriteLockTimeout(writeLockTimeout);
	}

	@Deprecated
	public long getWriteLockTimeout() {
		ensureOpen();
		return writeLockTimeout;
	}

	@Deprecated
	public static void setDefaultWriteLockTimeout(long writeLockTimeout) {
		IndexWriterConfig.setDefaultWriteLockTimeout(writeLockTimeout);
	}

	@Deprecated
	public static long getDefaultWriteLockTimeout() {
		return IndexWriterConfig.getDefaultWriteLockTimeout();
	}

	public void close() throws CorruptIndexException, IOException {
		close(true);
	}

	public void close(boolean waitForMerges) throws CorruptIndexException,
			IOException {

		if (shouldClose()) {

			if (hitOOM)
				rollbackInternal();
			else
				closeInternal(waitForMerges);
		}
	}

	synchronized private boolean shouldClose() {
		while (true) {
			if (!closed) {
				if (!closing) {
					closing = true;
					return true;
				} else {

					doWait();
				}
			} else
				return false;
		}
	}

	private void closeInternal(boolean waitForMerges)
			throws CorruptIndexException, IOException {

		try {
			if (infoStream != null) {
				message("now flush at close waitForMerges=" + waitForMerges);
			}

			docWriter.close();

			if (!hitOOM) {
				flush(waitForMerges, true);
			}

			if (waitForMerges)

				mergeScheduler.merge(this);

			mergePolicy.close();

			synchronized (this) {
				finishMerges(waitForMerges);
				stopMerges = true;
			}

			mergeScheduler.close();

			if (infoStream != null)
				message("now call final commit()");

			if (!hitOOM) {
				commitInternal(null);
			}

			if (infoStream != null)
				message("at close: " + segString());

			synchronized (this) {
				readerPool.close();
				docWriter = null;
				deleter.close();
			}

			if (writeLock != null) {
				writeLock.release();
				writeLock = null;
			}
			synchronized (this) {
				closed = true;
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "closeInternal");
		} finally {
			synchronized (this) {
				closing = false;
				notifyAll();
				if (!closed) {
					if (infoStream != null)
						message("hit exception while closing");
				}
			}
		}
	}

	public Directory getDirectory() {

		ensureOpen(false);
		return directory;
	}

	public Analyzer getAnalyzer() {
		ensureOpen();
		return analyzer;
	}

	public synchronized int maxDoc() {
		ensureOpen();
		int count;
		if (docWriter != null)
			count = docWriter.getNumDocs();
		else
			count = 0;

		count += segmentInfos.totalDocCount();
		return count;
	}

	public synchronized int numDocs() throws IOException {
		ensureOpen();
		int count;
		if (docWriter != null)
			count = docWriter.getNumDocs();
		else
			count = 0;

		for (final SegmentInfo info : segmentInfos) {
			count += info.docCount - numDeletedDocs(info);
		}
		return count;
	}

	public synchronized boolean hasDeletions() throws IOException {
		ensureOpen();
		if (bufferedDeletesStream.any()) {
			return true;
		}
		if (docWriter.anyDeletions()) {
			return true;
		}
		for (final SegmentInfo info : segmentInfos) {
			if (info.hasDeletions()) {
				return true;
			}
		}
		return false;
	}

	@Deprecated
	private int maxFieldLength = DEFAULT_MAX_FIELD_LENGTH;

	public void addDocument(Document doc) throws CorruptIndexException,
			IOException {
		addDocument(doc, analyzer);
	}

	public void addDocument(Document doc, Analyzer analyzer)
			throws CorruptIndexException, IOException {
		ensureOpen();
		boolean doFlush = false;
		boolean success = false;
		try {
			try {
				doFlush = docWriter.updateDocument(doc, analyzer, null);
				success = true;
			} finally {
				if (!success && infoStream != null)
					message("hit exception adding document");
			}
			if (doFlush)
				flush(true, false);
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "addDocument");
		}
	}

	public void addDocuments(Collection<Document> docs)
			throws CorruptIndexException, IOException {

		addDocuments(docs, analyzer);
	}

	public void addDocuments(Collection<Document> docs, Analyzer analyzer)
			throws CorruptIndexException, IOException {

		updateDocuments(null, docs, analyzer);
	}

	public void updateDocuments(Term delTerm, Collection<Document> docs)
			throws CorruptIndexException, IOException {

		updateDocuments(delTerm, docs, analyzer);
	}

	public void updateDocuments(Term delTerm, Collection<Document> docs,
			Analyzer analyzer) throws CorruptIndexException, IOException {

		ensureOpen();
		try {
			boolean success = false;
			boolean doFlush = false;
			try {
				doFlush = docWriter.updateDocuments(docs, analyzer, delTerm);
				success = true;
			} finally {
				if (!success && infoStream != null) {
					message("hit exception updating document");
				}
			}
			if (doFlush) {
				flush(true, false);
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "updateDocuments");
		}
	}

	public void deleteDocuments(Term term) throws CorruptIndexException,
			IOException {
		ensureOpen();
		try {
			if (docWriter.deleteTerm(term, false)) {
				flush(true, false);
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Term)");
		}
	}

	public void deleteDocuments(Term... terms) throws CorruptIndexException,
			IOException {
		ensureOpen();
		try {
			if (docWriter.deleteTerms(terms)) {
				flush(true, false);
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Term..)");
		}
	}

	public void deleteDocuments(Query query) throws CorruptIndexException,
			IOException {
		ensureOpen();
		try {
			if (docWriter.deleteQuery(query)) {
				flush(true, false);
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Query)");
		}
	}

	public void deleteDocuments(Query... queries) throws CorruptIndexException,
			IOException {
		ensureOpen();
		try {
			if (docWriter.deleteQueries(queries)) {
				flush(true, false);
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteDocuments(Query..)");
		}
	}

	public void updateDocument(Term term, Document doc)
			throws CorruptIndexException, IOException {
		ensureOpen();
		updateDocument(term, doc, getAnalyzer());
	}

	public void updateDocument(Term term, Document doc, Analyzer analyzer)
			throws CorruptIndexException, IOException {
		ensureOpen();
		try {
			boolean doFlush = false;
			boolean success = false;
			try {
				doFlush = docWriter.updateDocument(doc, analyzer, term);
				success = true;
			} finally {
				if (!success && infoStream != null)
					message("hit exception updating document");
			}
			if (doFlush) {
				flush(true, false);
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "updateDocument");
		}
	}

	final synchronized int getSegmentCount() {
		return segmentInfos.size();
	}

	final synchronized int getNumBufferedDocuments() {
		return docWriter.getNumDocs();
	}

	final synchronized int getDocCount(int i) {
		if (i >= 0 && i < segmentInfos.size()) {
			return segmentInfos.info(i).docCount;
		} else {
			return -1;
		}
	}

	final int getFlushCount() {
		return flushCount.get();
	}

	final int getFlushDeletesCount() {
		return flushDeletesCount.get();
	}

	final String newSegmentName() {

		synchronized (segmentInfos) {

			changeCount++;
			segmentInfos.changed();
			return "_"
					+ Integer.toString(segmentInfos.counter++,
							Character.MAX_RADIX);
		}
	}

	private PrintStream infoStream;
	private static PrintStream defaultInfoStream;

	@Deprecated
	public void optimize() throws CorruptIndexException, IOException {
		forceMerge(1, true);
	}

	@Deprecated
	public void optimize(int maxNumSegments) throws CorruptIndexException,
			IOException {
		forceMerge(maxNumSegments, true);
	}

	@Deprecated
	public void optimize(boolean doWait) throws CorruptIndexException,
			IOException {
		forceMerge(1, doWait);
	}

	public void forceMerge(int maxNumSegments) throws CorruptIndexException,
			IOException {
		forceMerge(maxNumSegments, true);
	}

	public void forceMerge(int maxNumSegments, boolean doWait)
			throws CorruptIndexException, IOException {
		ensureOpen();

		if (maxNumSegments < 1)
			throw new IllegalArgumentException(
					"maxNumSegments must be >= 1; got " + maxNumSegments);

		if (infoStream != null) {
			message("forceMerge: index now " + segString());
			message("now flush at forceMerge");
		}

		flush(true, true);

		synchronized (this) {
			resetMergeExceptions();
			segmentsToMerge.clear();
			for (SegmentInfo info : segmentInfos) {
				segmentsToMerge.put(info, Boolean.TRUE);
			}
			mergeMaxNumSegments = maxNumSegments;

			for (final MergePolicy.OneMerge merge : pendingMerges) {
				merge.maxNumSegments = maxNumSegments;
				segmentsToMerge.put(merge.info, Boolean.TRUE);
			}

			for (final MergePolicy.OneMerge merge : runningMerges) {
				merge.maxNumSegments = maxNumSegments;
				segmentsToMerge.put(merge.info, Boolean.TRUE);
			}
		}

		maybeMerge(maxNumSegments);

		if (doWait) {
			synchronized (this) {
				while (true) {

					if (hitOOM) {
						throw new IllegalStateException(
								"this writer hit an OutOfMemoryError; cannot complete forceMerge");
					}

					if (mergeExceptions.size() > 0) {

						final int size = mergeExceptions.size();
						for (int i = 0; i < size; i++) {
							final MergePolicy.OneMerge merge = mergeExceptions
									.get(i);
							if (merge.maxNumSegments != -1) {
								IOException err = new IOException(
										"background merge hit exception: "
												+ merge.segString(directory));
								final Throwable t = merge.getException();
								if (t != null)
									err.initCause(t);
								throw err;
							}
						}
					}

					if (maxNumSegmentsMergesPending())
						doWait();
					else
						break;
				}
			}

			ensureOpen();
		}

	}

	private synchronized boolean maxNumSegmentsMergesPending() {
		for (final MergePolicy.OneMerge merge : pendingMerges) {
			if (merge.maxNumSegments != -1)
				return true;
		}

		for (final MergePolicy.OneMerge merge : runningMerges) {
			if (merge.maxNumSegments != -1)
				return true;
		}

		return false;
	}

	@Deprecated
	public void expungeDeletes(boolean doWait) throws CorruptIndexException,
			IOException {
		forceMergeDeletes(doWait);
	}

	public void forceMergeDeletes(boolean doWait) throws CorruptIndexException,
			IOException {
		ensureOpen();

		flush(true, true);

		if (infoStream != null)
			message("forceMergeDeletes: index now " + segString());

		MergePolicy.MergeSpecification spec;

		synchronized (this) {
			spec = mergePolicy.findForcedDeletesMerges(segmentInfos);
			if (spec != null) {
				final int numMerges = spec.merges.size();
				for (int i = 0; i < numMerges; i++)
					registerMerge(spec.merges.get(i));
			}
		}

		mergeScheduler.merge(this);

		if (spec != null && doWait) {
			final int numMerges = spec.merges.size();
			synchronized (this) {
				boolean running = true;
				while (running) {

					if (hitOOM) {
						throw new IllegalStateException(
								"this writer hit an OutOfMemoryError; cannot complete forceMergeDeletes");
					}

					running = false;
					for (int i = 0; i < numMerges; i++) {
						final MergePolicy.OneMerge merge = spec.merges.get(i);
						if (pendingMerges.contains(merge)
								|| runningMerges.contains(merge))
							running = true;
						Throwable t = merge.getException();
						if (t != null) {
							IOException ioe = new IOException(
									"background merge hit exception: "
											+ merge.segString(directory));
							ioe.initCause(t);
							throw ioe;
						}
					}

					if (running)
						doWait();
				}
			}
		}

	}

	@Deprecated
	public void expungeDeletes() throws CorruptIndexException, IOException {
		forceMergeDeletes();
	}

	public void forceMergeDeletes() throws CorruptIndexException, IOException {
		forceMergeDeletes(true);
	}

	public final void maybeMerge() throws CorruptIndexException, IOException {
		maybeMerge(-1);
	}

	private final void maybeMerge(int maxNumSegments)
			throws CorruptIndexException, IOException {
		ensureOpen(false);
		updatePendingMerges(maxNumSegments);
		mergeScheduler.merge(this);
	}

	private synchronized void updatePendingMerges(int maxNumSegments)
			throws CorruptIndexException, IOException {
		assert maxNumSegments == -1 || maxNumSegments > 0;

		if (stopMerges) {
			return;
		}

		if (hitOOM) {
			return;
		}

		final MergePolicy.MergeSpecification spec;
		if (maxNumSegments != -1) {
			spec = mergePolicy.findForcedMerges(segmentInfos, maxNumSegments,
					Collections.unmodifiableMap(segmentsToMerge));
			if (spec != null) {
				final int numMerges = spec.merges.size();
				for (int i = 0; i < numMerges; i++) {
					final MergePolicy.OneMerge merge = spec.merges.get(i);
					merge.maxNumSegments = maxNumSegments;
				}
			}

		} else {
			spec = mergePolicy.findMerges(segmentInfos);
		}

		if (spec != null) {
			final int numMerges = spec.merges.size();
			for (int i = 0; i < numMerges; i++) {
				registerMerge(spec.merges.get(i));
			}
		}
	}

	public synchronized Collection<SegmentInfo> getMergingSegments() {
		return mergingSegments;
	}

	public synchronized MergePolicy.OneMerge getNextMerge() {
		if (pendingMerges.size() == 0)
			return null;
		else {

			MergePolicy.OneMerge merge = pendingMerges.removeFirst();
			runningMerges.add(merge);
			return merge;
		}
	}

	public void rollback() throws IOException {
		ensureOpen();

		if (shouldClose())
			rollbackInternal();
	}

	private void rollbackInternal() throws IOException {

		boolean success = false;

		if (infoStream != null) {
			message("rollback");
		}

		try {
			synchronized (this) {
				finishMerges(false);
				stopMerges = true;
			}

			if (infoStream != null) {
				message("rollback: done finish merges");
			}

			mergePolicy.close();
			mergeScheduler.close();

			bufferedDeletesStream.clear();

			synchronized (this) {

				if (pendingCommit != null) {
					pendingCommit.rollbackCommit(directory);
					deleter.decRef(pendingCommit);
					pendingCommit = null;
					notifyAll();
				}

				segmentInfos.rollbackSegmentInfos(rollbackSegments);
				if (infoStream != null) {
					message("rollback: infos=" + segString(segmentInfos));
				}

				docWriter.abort();

				assert testPoint("rollback before checkpoint");

				deleter.checkpoint(segmentInfos, false);
				deleter.refresh();
			}

			readerPool.clear(null);

			lastCommitChangeCount = changeCount;

			success = true;
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "rollbackInternal");
		} finally {
			synchronized (this) {
				if (!success) {
					closing = false;
					notifyAll();
					if (infoStream != null)
						message("hit exception during rollback");
				}
			}
		}

		closeInternal(false);
	}

	public synchronized void deleteAll() throws IOException {
		ensureOpen();
		try {

			finishMerges(false);

			docWriter.abort();

			segmentInfos.clear();

			deleter.checkpoint(segmentInfos, false);
			deleter.refresh();

			readerPool.dropAll();

			++changeCount;
			segmentInfos.changed();
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "deleteAll");
		} finally {
			if (infoStream != null) {
				message("hit exception during deleteAll");
			}
		}
	}

	private synchronized void finishMerges(boolean waitForMerges)
			throws IOException {
		if (!waitForMerges) {

			stopMerges = true;

			for (final MergePolicy.OneMerge merge : pendingMerges) {
				if (infoStream != null)
					message("now abort pending merge "
							+ merge.segString(directory));
				merge.abort();
				mergeFinish(merge);
			}
			pendingMerges.clear();

			for (final MergePolicy.OneMerge merge : runningMerges) {
				if (infoStream != null)
					message("now abort running merge "
							+ merge.segString(directory));
				merge.abort();
			}

			while (runningMerges.size() > 0) {
				if (infoStream != null)
					message("now wait for " + runningMerges.size()
							+ " running merge to abort");
				doWait();
			}

			stopMerges = false;
			notifyAll();

			assert 0 == mergingSegments.size();

			if (infoStream != null)
				message("all running merges have aborted");

		} else {

			waitForMerges();
		}
	}

	public synchronized void waitForMerges() {
		ensureOpen(false);
		if (infoStream != null) {
			message("waitForMerges");
		}
		while (pendingMerges.size() > 0 || runningMerges.size() > 0) {
			doWait();
		}

		assert 0 == mergingSegments.size();

		if (infoStream != null) {
			message("waitForMerges done");
		}
	}

	synchronized void checkpoint() throws IOException {
		changeCount++;
		segmentInfos.changed();
		deleter.checkpoint(segmentInfos, false);
	}

	private synchronized void resetMergeExceptions() {
		mergeExceptions = new ArrayList<MergePolicy.OneMerge>();
		mergeGen++;
	}

	private void noDupDirs(Directory... dirs) {
		HashSet<Directory> dups = new HashSet<Directory>();
		for (Directory dir : dirs) {
			if (dups.contains(dir))
				throw new IllegalArgumentException("Directory " + dir
						+ " appears more than once");
			if (dir == directory)
				throw new IllegalArgumentException(
						"Cannot add directory to itself");
			dups.add(dir);
		}
	}

	@Deprecated
	public void addIndexesNoOptimize(Directory... dirs)
			throws CorruptIndexException, IOException {
		addIndexes(dirs);
	}

	public void addIndexes(Directory... dirs) throws CorruptIndexException,
			IOException {
		ensureOpen();

		noDupDirs(dirs);

		try {
			if (infoStream != null)
				message("flush at addIndexes(Directory...)");
			flush(false, true);

			int docCount = 0;
			List<SegmentInfo> infos = new ArrayList<SegmentInfo>();
			Comparator<String> versionComparator = StringHelper
					.getVersionComparator();
			for (Directory dir : dirs) {
				if (infoStream != null) {
					message("addIndexes: process directory " + dir);
				}
				SegmentInfos sis = new SegmentInfos();
				sis.read(dir);
				final Set<String> dsFilesCopied = new HashSet<String>();
				final Map<String, String> dsNames = new HashMap<String, String>();
				for (SegmentInfo info : sis) {
					assert !infos.contains(info) : "dup info dir=" + info.dir
							+ " name=" + info.name;

					docCount += info.docCount;
					String newSegName = newSegmentName();
					String dsName = info.getDocStoreSegment();

					if (infoStream != null) {
						message("addIndexes: process segment origName="
								+ info.name + " newName=" + newSegName
								+ " dsName=" + dsName + " info=" + info);
					}

					boolean createCFS;
					synchronized (this) {
						createCFS = !info.getUseCompoundFile()
								&& mergePolicy.useCompoundFile(segmentInfos,
										info)

								&& versionComparator.compare(info.getVersion(),
										"3.1") >= 0;
					}

					if (createCFS) {
						copySegmentIntoCFS(info, newSegName);
					} else {
						copySegmentAsIs(info, newSegName, dsNames,
								dsFilesCopied);
					}
					infos.add(info);
				}
			}

			synchronized (this) {
				ensureOpen();
				segmentInfos.addAll(infos);
				checkpoint();
			}

		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "addIndexes(Directory...)");
		}
	}

	public void addIndexes(IndexReader... readers)
			throws CorruptIndexException, IOException {

		ensureOpen();

		try {
			if (infoStream != null)
				message("flush at addIndexes(IndexReader...)");
			flush(false, true);

			String mergedName = newSegmentName();

			SegmentMerger merger = new SegmentMerger(directory,
					config.getTermIndexInterval(), mergedName, null,
					payloadProcessorProvider, ((FieldInfos) docWriter
							.getFieldInfos().clone()));

			for (IndexReader reader : readers)

				merger.add(reader);

			int docCount = merger.merge();

			SegmentInfo info = new SegmentInfo(mergedName, docCount, directory,
					false, true, merger.fieldInfos().hasProx(), merger
							.fieldInfos().hasVectors());
			setDiagnostics(info, "addIndexes(IndexReader...)");

			boolean useCompoundFile;
			synchronized (this) {
				if (stopMerges) {
					deleter.deleteNewFiles(info.files());
					return;
				}
				ensureOpen();
				useCompoundFile = mergePolicy.useCompoundFile(segmentInfos,
						info);
			}

			if (useCompoundFile) {
				merger.createCompoundFile(mergedName + ".cfs", info);

				synchronized (this) {
					deleter.deleteNewFiles(info.files());
				}
				info.setUseCompoundFile(true);
			}

			synchronized (this) {
				if (stopMerges) {
					deleter.deleteNewFiles(info.files());
					return;
				}
				ensureOpen();
				segmentInfos.add(info);
				checkpoint();
			}

		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "addIndexes(IndexReader...)");
		}
	}

	private void copySegmentIntoCFS(SegmentInfo info, String segName)
			throws IOException {
		String segFileName = IndexFileNames.segmentFileName(segName,
				IndexFileNames.COMPOUND_FILE_EXTENSION);
		Collection<String> files = info.files();
		CompoundFileWriter cfsWriter = new CompoundFileWriter(directory,
				segFileName);
		for (String file : files) {
			String newFileName = segName
					+ IndexFileNames.stripSegmentName(file);
			if (!IndexFileNames.matchesExtension(file,
					IndexFileNames.DELETES_EXTENSION)
					&& !IndexFileNames.isSeparateNormsFile(file)) {
				cfsWriter.addFile(file, info.dir);
			} else {
				assert !directory.fileExists(newFileName) : "file \""
						+ newFileName + "\" already exists";
				info.dir.copy(directory, file, newFileName);
			}
		}

		cfsWriter.close();

		info.dir = directory;
		info.name = segName;
		info.setUseCompoundFile(true);
	}

	private void copySegmentAsIs(SegmentInfo info, String segName,
			Map<String, String> dsNames, Set<String> dsFilesCopied)
			throws IOException {

		String dsName = info.getDocStoreSegment();
		final String newDsName;
		if (dsName != null) {
			if (dsNames.containsKey(dsName)) {
				newDsName = dsNames.get(dsName);
			} else {
				dsNames.put(dsName, segName);
				newDsName = segName;
			}
		} else {
			newDsName = segName;
		}

		for (String file : info.files()) {
			final String newFileName;
			if (IndexFileNames.isDocStoreFile(file)) {
				newFileName = newDsName + IndexFileNames.stripSegmentName(file);
				if (dsFilesCopied.contains(newFileName)) {
					continue;
				}
				dsFilesCopied.add(newFileName);
			} else {
				newFileName = segName + IndexFileNames.stripSegmentName(file);
			}

			assert !directory.fileExists(newFileName) : "file \"" + newFileName
					+ "\" already exists";
			info.dir.copy(directory, file, newFileName);
		}

		info.setDocStore(info.getDocStoreOffset(), newDsName,
				info.getDocStoreIsCompoundFile());
		info.dir = directory;
		info.name = segName;
	}

	protected void doAfterFlush() throws IOException {
	}

	protected void doBeforeFlush() throws IOException {
	}

	public final void prepareCommit() throws CorruptIndexException, IOException {
		ensureOpen();
		prepareCommit(null);
	}

	public final void prepareCommit(Map<String, String> commitUserData)
			throws CorruptIndexException, IOException {
		ensureOpen(false);

		if (hitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot commit");
		}

		if (pendingCommit != null)
			throw new IllegalStateException(
					"prepareCommit was already called with no corresponding call to commit");

		if (infoStream != null)
			message("prepareCommit: flush");

		ensureOpen(false);
		boolean anySegmentsFlushed = false;
		SegmentInfos toCommit = null;
		boolean success = false;
		try {
			try {
				synchronized (this) {
					anySegmentsFlushed = doFlush(true);
					readerPool.commit(segmentInfos);
					toCommit = (SegmentInfos) segmentInfos.clone();
					pendingCommitChangeCount = changeCount;

					deleter.incRef(toCommit, false);
				}
				success = true;
			} finally {
				if (!success && infoStream != null) {
					message("hit exception during prepareCommit");
				}
				doAfterFlush();
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "prepareCommit");
		}

		success = false;
		try {
			if (anySegmentsFlushed) {
				maybeMerge();
			}
			success = true;
		} finally {
			if (!success) {
				synchronized (this) {
					deleter.decRef(toCommit);
				}
			}
		}

		startCommit(toCommit, commitUserData);
	}

	private final Object commitLock = new Object();

	public final void commit() throws CorruptIndexException, IOException {
		commit(null);
	}

	public final void commit(Map<String, String> commitUserData)
			throws CorruptIndexException, IOException {

		ensureOpen();

		commitInternal(commitUserData);
	}

	private final void commitInternal(Map<String, String> commitUserData)
			throws CorruptIndexException, IOException {

		if (infoStream != null) {
			message("commit: start");
		}

		synchronized (commitLock) {
			if (infoStream != null) {
				message("commit: enter lock");
			}

			if (pendingCommit == null) {
				if (infoStream != null) {
					message("commit: now prepare");
				}
				prepareCommit(commitUserData);
			} else if (infoStream != null) {
				message("commit: already prepared");
			}

			finishCommit();
		}
	}

	private synchronized final void finishCommit()
			throws CorruptIndexException, IOException {

		if (pendingCommit != null) {
			try {
				if (infoStream != null)
					message("commit: pendingCommit != null");
				pendingCommit.finishCommit(directory);
				if (infoStream != null)
					message("commit: wrote segments file \""
							+ pendingCommit.getCurrentSegmentFileName() + "\"");
				lastCommitChangeCount = pendingCommitChangeCount;
				segmentInfos.updateGeneration(pendingCommit);
				segmentInfos.setUserData(pendingCommit.getUserData());
				rollbackSegments = pendingCommit.createBackupSegmentInfos(true);
				deleter.checkpoint(pendingCommit, true);
			} finally {

				deleter.decRef(pendingCommit);
				pendingCommit = null;
				notifyAll();
			}

		} else if (infoStream != null) {
			message("commit: pendingCommit == null; skip");
		}

		if (infoStream != null) {
			message("commit: done");
		}
	}

	protected final void flush(boolean triggerMerge, boolean flushDocStores,
			boolean flushDeletes) throws CorruptIndexException, IOException {
		flush(triggerMerge, flushDeletes);
	}

	protected final void flush(boolean triggerMerge, boolean applyAllDeletes)
			throws CorruptIndexException, IOException {

		ensureOpen(false);
		if (doFlush(applyAllDeletes) && triggerMerge) {
			maybeMerge();
		}
	}

	private synchronized boolean doFlush(boolean applyAllDeletes)
			throws CorruptIndexException, IOException {

		if (hitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot flush");
		}

		doBeforeFlush();

		assert testPoint("startDoFlush");

		flushControl.setFlushPendingNoWait("explicit flush");

		boolean success = false;

		try {

			if (infoStream != null) {
				message("  start flush: applyAllDeletes=" + applyAllDeletes);
				message("  index before flush " + segString());
			}

			final SegmentInfo newSegment = docWriter.flush(this, deleter,
					mergePolicy, segmentInfos);
			if (newSegment != null) {
				setDiagnostics(newSegment, "flush");
				segmentInfos.add(newSegment);
				checkpoint();
			}

			if (!applyAllDeletes) {

				if (flushControl.getFlushDeletes()
						|| (config.getRAMBufferSizeMB() != IndexWriterConfig.DISABLE_AUTO_FLUSH && bufferedDeletesStream
								.bytesUsed() > (1024 * 1024 * config
								.getRAMBufferSizeMB() / 2))) {
					applyAllDeletes = true;
					if (infoStream != null) {
						message("force apply deletes bytesUsed="
								+ bufferedDeletesStream.bytesUsed()
								+ " vs ramBuffer="
								+ (1024 * 1024 * config.getRAMBufferSizeMB()));
					}
				}
			}

			if (applyAllDeletes) {
				if (infoStream != null) {
					message("apply all deletes during flush");
				}

				flushDeletesCount.incrementAndGet();
				final BufferedDeletesStream.ApplyDeletesResult result = bufferedDeletesStream
						.applyDeletes(readerPool, segmentInfos.asList());
				if (result.anyDeletes) {
					checkpoint();
				}
				if (!keepFullyDeletedSegments && result.allDeleted != null) {
					if (infoStream != null) {
						message("drop 100% deleted segments: "
								+ result.allDeleted);
					}
					for (SegmentInfo info : result.allDeleted) {

						if (!mergingSegments.contains(info)) {
							segmentInfos.remove(info);
							if (readerPool != null) {
								readerPool.drop(info);
							}
						}
					}
					checkpoint();
				}
				bufferedDeletesStream.prune(segmentInfos);

				assert !bufferedDeletesStream.any();
				flushControl.clearDeletes();
			} else if (infoStream != null) {
				message("don't apply deletes now delTermCount="
						+ bufferedDeletesStream.numTerms() + " bytesUsed="
						+ bufferedDeletesStream.bytesUsed());
			}

			doAfterFlush();
			flushCount.incrementAndGet();

			success = true;

			return newSegment != null;

		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "doFlush");

			return false;
		} finally {
			flushControl.clearFlushPending();
			if (!success && infoStream != null)
				message("hit exception during flush");
		}
	}

	public final long ramSizeInBytes() {
		ensureOpen();
		return docWriter.bytesUsed() + bufferedDeletesStream.bytesUsed();
	}

	public final synchronized int numRamDocs() {
		ensureOpen();
		return docWriter.getNumDocs();
	}

	private void ensureValidMerge(MergePolicy.OneMerge merge)
			throws IOException {
		for (SegmentInfo info : merge.segments) {
			if (!segmentInfos.contains(info)) {
				throw new MergePolicy.MergeException(
						"MergePolicy selected a segment (" + info.name
								+ ") that is not in the current index "
								+ segString(), directory);
			}
		}
	}

	synchronized private void commitMergedDeletes(MergePolicy.OneMerge merge,
			SegmentReader mergedReader) throws IOException {

		assert testPoint("startCommitMergeDeletes");

		final List<SegmentInfo> sourceSegments = merge.segments;

		if (infoStream != null)
			message("commitMergeDeletes " + merge.segString(directory));

		int docUpto = 0;
		int delCount = 0;
		long minGen = Long.MAX_VALUE;

		for (int i = 0; i < sourceSegments.size(); i++) {
			SegmentInfo info = sourceSegments.get(i);
			minGen = Math.min(info.getBufferedDeletesGen(), minGen);
			int docCount = info.docCount;
			final SegmentReader previousReader = merge.readerClones.get(i);
			if (previousReader == null) {

				continue;
			}
			final SegmentReader currentReader = merge.readers.get(i);
			if (previousReader.hasDeletions()) {

				if (currentReader.numDeletedDocs() > previousReader
						.numDeletedDocs()) {

					for (int j = 0; j < docCount; j++) {
						if (previousReader.isDeleted(j))
							assert currentReader.isDeleted(j);
						else {
							if (currentReader.isDeleted(j)) {
								mergedReader.doDelete(docUpto);
								delCount++;
							}
							docUpto++;
						}
					}
				} else {
					docUpto += docCount - previousReader.numDeletedDocs();
				}
			} else if (currentReader.hasDeletions()) {

				for (int j = 0; j < docCount; j++) {
					if (currentReader.isDeleted(j)) {
						mergedReader.doDelete(docUpto);
						delCount++;
					}
					docUpto++;
				}
			} else

				docUpto += info.docCount;
		}

		assert mergedReader.numDeletedDocs() == delCount;

		mergedReader.hasChanges = delCount > 0;

		assert !mergedReader.hasChanges
				|| minGen > mergedReader.getSegmentInfo()
						.getBufferedDeletesGen();

		mergedReader.getSegmentInfo().setBufferedDeletesGen(minGen);
	}

	synchronized private boolean commitMerge(MergePolicy.OneMerge merge,
			SegmentReader mergedReader) throws IOException {

		assert testPoint("startCommitMerge");

		if (hitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot complete merge");
		}

		if (infoStream != null)
			message("commitMerge: " + merge.segString(directory) + " index="
					+ segString());

		assert merge.registerDone;

		if (merge.isAborted()) {
			if (infoStream != null)
				message("commitMerge: skipping merge "
						+ merge.segString(directory) + ": it was aborted");
			return false;
		}

		commitMergedDeletes(merge, mergedReader);

		assert !segmentInfos.contains(merge.info);

		final boolean allDeleted = mergedReader.numDocs() == 0;

		if (infoStream != null && allDeleted) {
			message("merged segment " + merge.info + " is 100% deleted"
					+ (keepFullyDeletedSegments ? "" : "; skipping insert"));
		}

		final boolean dropSegment = allDeleted && !keepFullyDeletedSegments;
		segmentInfos.applyMergeChanges(merge, dropSegment);

		if (dropSegment) {
			readerPool.drop(merge.info);
		}

		if (infoStream != null) {
			message("after commit: " + segString());
		}

		closeMergeReaders(merge, false);

		checkpoint();

		readerPool.clear(merge.segments);

		if (merge.maxNumSegments != -1) {

			if (!segmentsToMerge.containsKey(merge.info)) {
				segmentsToMerge.put(merge.info, Boolean.FALSE);
			}
		}

		return true;
	}

	final private void handleMergeException(Throwable t,
			MergePolicy.OneMerge merge) throws IOException {

		if (infoStream != null) {
			message("handleMergeException: merge=" + merge.segString(directory)
					+ " exc=" + t);
		}

		merge.setException(t);
		addMergeException(merge);

		if (t instanceof MergePolicy.MergeAbortedException) {

			if (merge.isExternal)
				throw (MergePolicy.MergeAbortedException) t;
		} else if (t instanceof IOException)
			throw (IOException) t;
		else if (t instanceof RuntimeException)
			throw (RuntimeException) t;
		else if (t instanceof Error)
			throw (Error) t;
		else

			throw new RuntimeException(t);
	}

	public void merge(MergePolicy.OneMerge merge) throws CorruptIndexException,
			IOException {

		boolean success = false;

		final long t0 = System.currentTimeMillis();

		try {
			try {
				try {
					mergeInit(merge);

					if (infoStream != null)
						message("now merge\n  merge="
								+ merge.segString(directory) + "\n  merge="
								+ merge + "\n  index=" + segString());

					mergeMiddle(merge);
					mergeSuccess(merge);
					success = true;
				} catch (Throwable t) {
					handleMergeException(t, merge);
				}
			} finally {
				synchronized (this) {
					mergeFinish(merge);

					if (!success) {
						if (infoStream != null)
							message("hit exception during merge");
						if (merge.info != null
								&& !segmentInfos.contains(merge.info))
							deleter.refresh(merge.info.name);
					}

					if (success
							&& !merge.isAborted()
							&& (merge.maxNumSegments != -1 || (!closed && !closing))) {
						updatePendingMerges(merge.maxNumSegments);
					}
				}
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "merge");
		}
		if (infoStream != null && merge.info != null) {
			message("merge time " + (System.currentTimeMillis() - t0)
					+ " msec for " + merge.info.docCount + " docs");
		}

	}

	void mergeSuccess(MergePolicy.OneMerge merge) {
	}

	final synchronized boolean registerMerge(MergePolicy.OneMerge merge)
			throws MergePolicy.MergeAbortedException, IOException {

		if (merge.registerDone)
			return true;

		if (stopMerges) {
			merge.abort();
			throw new MergePolicy.MergeAbortedException("merge is aborted: "
					+ merge.segString(directory));
		}

		boolean isExternal = false;
		for (SegmentInfo info : merge.segments) {
			if (mergingSegments.contains(info)) {
				return false;
			}
			if (!segmentInfos.contains(info)) {
				return false;
			}
			if (info.dir != directory) {
				isExternal = true;
			}
			if (segmentsToMerge.containsKey(info)) {
				merge.maxNumSegments = mergeMaxNumSegments;
			}
		}

		ensureValidMerge(merge);

		pendingMerges.add(merge);

		if (infoStream != null)
			message("add merge to pendingMerges: " + merge.segString(directory)
					+ " [total " + pendingMerges.size() + " pending]");

		merge.mergeGen = mergeGen;
		merge.isExternal = isExternal;

		message("registerMerge merging=" + mergingSegments);
		for (SegmentInfo info : merge.segments) {
			message("registerMerge info=" + info);
			mergingSegments.add(info);
		}

		merge.registerDone = true;
		return true;
	}

	final synchronized void mergeInit(MergePolicy.OneMerge merge)
			throws IOException {
		boolean success = false;
		try {
			_mergeInit(merge);
			success = true;
		} finally {
			if (!success) {
				if (infoStream != null) {
					message("hit exception in mergeInit");
				}
				mergeFinish(merge);
			}
		}
	}

	synchronized private void _mergeInit(MergePolicy.OneMerge merge)
			throws IOException {

		assert testPoint("startMergeInit");

		assert merge.registerDone;
		assert merge.maxNumSegments == -1 || merge.maxNumSegments > 0;

		if (hitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot merge");
		}

		if (merge.info != null)

			return;

		if (merge.isAborted())
			return;

		boolean hasVectors = false;
		for (SegmentInfo sourceSegment : merge.segments) {
			if (sourceSegment.getHasVectors()) {
				hasVectors = true;
			}
		}

		merge.info = new SegmentInfo(newSegmentName(), 0, directory, false,
				true, false, hasVectors);

		final BufferedDeletesStream.ApplyDeletesResult result = bufferedDeletesStream
				.applyDeletes(readerPool, merge.segments);

		if (result.anyDeletes) {
			checkpoint();
		}

		if (!keepFullyDeletedSegments && result.allDeleted != null) {
			if (infoStream != null) {
				message("drop 100% deleted segments: " + result.allDeleted);
			}
			for (SegmentInfo info : result.allDeleted) {
				segmentInfos.remove(info);
				if (merge.segments.contains(info)) {
					mergingSegments.remove(info);
					merge.segments.remove(info);
				}
			}
			if (readerPool != null) {
				readerPool.drop(result.allDeleted);
			}
			checkpoint();
		}

		merge.info.setBufferedDeletesGen(result.gen);

		bufferedDeletesStream.prune(segmentInfos);

		Map<String, String> details = new HashMap<String, String>();
		details.put("mergeMaxNumSegments", "" + merge.maxNumSegments);
		details.put("mergeFactor", Integer.toString(merge.segments.size()));
		setDiagnostics(merge.info, "merge", details);

		if (infoStream != null) {
			message("merge seg=" + merge.info.name);
		}

		assert merge.estimatedMergeBytes == 0;
		for (SegmentInfo info : merge.segments) {
			if (info.docCount > 0) {
				final int delCount = numDeletedDocs(info);
				assert delCount <= info.docCount;
				final double delRatio = ((double) delCount) / info.docCount;
				merge.estimatedMergeBytes += info.sizeInBytes(true)
						* (1.0 - delRatio);
			}
		}

		mergingSegments.add(merge.info);
	}

	private void setDiagnostics(SegmentInfo info, String source) {
		setDiagnostics(info, source, null);
	}

	private void setDiagnostics(SegmentInfo info, String source,
			Map<String, String> details) {
		Map<String, String> diagnostics = new HashMap<String, String>();
		diagnostics.put("source", source);
		diagnostics.put("shatamIndex.version", Constants.SHATAM_VERSION);
		diagnostics.put("os", Constants.OS_NAME);
		diagnostics.put("os.arch", Constants.OS_ARCH);
		diagnostics.put("os.version", Constants.OS_VERSION);
		diagnostics.put("java.version", Constants.JAVA_VERSION);
		diagnostics.put("java.vendor", Constants.JAVA_VENDOR);
		if (details != null) {
			diagnostics.putAll(details);
		}
		info.setDiagnostics(diagnostics);
	}

	final synchronized void mergeFinish(MergePolicy.OneMerge merge)
			throws IOException {

		notifyAll();

		if (merge.registerDone) {
			final List<SegmentInfo> sourceSegments = merge.segments;
			for (SegmentInfo info : sourceSegments) {
				mergingSegments.remove(info);
			}

			mergingSegments.remove(merge.info);
			merge.registerDone = false;
		}

		runningMerges.remove(merge);
	}

	private final synchronized void closeMergeReaders(
			MergePolicy.OneMerge merge, boolean suppressExceptions)
			throws IOException {
		final int numSegments = merge.readers.size();
		Throwable th = null;

		boolean anyChanges = false;
		boolean drop = !suppressExceptions;
		for (int i = 0; i < numSegments; i++) {
			if (merge.readers.get(i) != null) {
				try {
					anyChanges |= readerPool
							.release(merge.readers.get(i), drop);
				} catch (Throwable t) {
					if (th == null) {
						th = t;
					}
				}
				merge.readers.set(i, null);
			}

			if (i < merge.readerClones.size()
					&& merge.readerClones.get(i) != null) {
				try {
					merge.readerClones.get(i).close();
				} catch (Throwable t) {
					if (th == null) {
						th = t;
					}
				}

				assert merge.readerClones.get(i).getRefCount() == 0 : "refCount should be 0 but is "
						+ merge.readerClones.get(i).getRefCount();
				merge.readerClones.set(i, null);
			}
		}

		if (suppressExceptions && anyChanges) {
			checkpoint();
		}

		if (!suppressExceptions && th != null) {
			if (th instanceof IOException)
				throw (IOException) th;
			if (th instanceof RuntimeException)
				throw (RuntimeException) th;
			if (th instanceof Error)
				throw (Error) th;
			throw new RuntimeException(th);
		}
	}

	final private int mergeMiddle(MergePolicy.OneMerge merge)
			throws CorruptIndexException, IOException {

		merge.checkAborted(directory);

		final String mergedName = merge.info.name;

		int mergedDocCount = 0;

		List<SegmentInfo> sourceSegments = merge.segments;

		SegmentMerger merger = new SegmentMerger(directory,
				config.getTermIndexInterval(), mergedName, merge,
				payloadProcessorProvider, ((FieldInfos) docWriter
						.getFieldInfos().clone()));

		if (infoStream != null) {
			message("merging " + merge.segString(directory) + " mergeVectors="
					+ merge.info.getHasVectors());
		}

		merge.readers = new ArrayList<SegmentReader>();
		merge.readerClones = new ArrayList<SegmentReader>();

		boolean success = false;
		try {
			int totDocCount = 0;
			int segUpto = 0;
			while (segUpto < sourceSegments.size()) {

				final SegmentInfo info = sourceSegments.get(segUpto);

				final SegmentReader reader = readerPool.get(info, true,
						MERGE_READ_BUFFER_SIZE, -1);
				merge.readers.add(reader);

				final SegmentReader clone = (SegmentReader) reader.clone(true);
				merge.readerClones.add(clone);

				if (clone.numDocs() > 0) {
					merger.add(clone);
					totDocCount += clone.numDocs();
				}
				segUpto++;
			}

			if (infoStream != null) {
				message("merge: total " + totDocCount + " docs");
			}

			merge.checkAborted(directory);

			mergedDocCount = merge.info.docCount = merger.merge();

			merge.info.setHasVectors(merger.fieldInfos().hasVectors());

			assert mergedDocCount == totDocCount;

			if (infoStream != null) {
				message("merge store matchedCount="
						+ merger.getMatchedSubReaderCount() + " vs "
						+ merge.readers.size());
			}

			anyNonBulkMerges |= merger.getAnyNonBulkMerges();

			assert mergedDocCount == totDocCount : "mergedDocCount="
					+ mergedDocCount + " vs " + totDocCount;

			merge.info.setHasProx(merger.fieldInfos().hasProx());

			boolean useCompoundFile;
			synchronized (this) {
				useCompoundFile = mergePolicy.useCompoundFile(segmentInfos,
						merge.info);
			}

			if (useCompoundFile) {

				success = false;
				final String compoundFileName = IndexFileNames.segmentFileName(
						mergedName, IndexFileNames.COMPOUND_FILE_EXTENSION);

				try {
					if (infoStream != null) {
						message("create compound file " + compoundFileName);
					}
					merger.createCompoundFile(compoundFileName, merge.info);
					success = true;
				} catch (IOException ioe) {
					synchronized (this) {
						if (merge.isAborted()) {

						} else {
							handleMergeException(ioe, merge);
						}
					}
				} catch (Throwable t) {
					handleMergeException(t, merge);
				} finally {
					if (!success) {
						if (infoStream != null) {
							message("hit exception creating compound file during merge");
						}

						synchronized (this) {
							deleter.deleteFile(compoundFileName);
							deleter.deleteNewFiles(merge.info.files());
						}
					}
				}

				success = false;

				synchronized (this) {

					deleter.deleteNewFiles(merge.info.files());

					if (merge.isAborted()) {
						if (infoStream != null) {
							message("abort merge after building CFS");
						}
						deleter.deleteFile(compoundFileName);
						return 0;
					}
				}

				merge.info.setUseCompoundFile(true);
			}

			if (infoStream != null) {
				message(String.format(
						"merged segment size=%.3f MB vs estimate=%.3f MB",
						merge.info.sizeInBytes(true) / 1024. / 1024.,
						merge.estimatedMergeBytes / 1024 / 1024.));
			}

			final IndexReaderWarmer mergedSegmentWarmer = config
					.getMergedSegmentWarmer();

			final int termsIndexDivisor;
			final boolean loadDocStores;

			if (mergedSegmentWarmer != null) {

				termsIndexDivisor = config.getReaderTermsIndexDivisor();
				loadDocStores = true;
			} else {
				termsIndexDivisor = -1;
				loadDocStores = false;
			}

			final SegmentReader mergedReader = readerPool.get(merge.info,
					loadDocStores, BufferedIndexInput.BUFFER_SIZE,
					termsIndexDivisor);
			try {
				if (poolReaders && mergedSegmentWarmer != null) {
					mergedSegmentWarmer.warm(mergedReader);
				}

				if (!commitMerge(merge, mergedReader)) {

					return 0;
				}
			} finally {
				synchronized (this) {
					if (readerPool.release(mergedReader)) {

						checkpoint();
					}
				}
			}

			success = true;

		} finally {

			if (!success) {
				closeMergeReaders(merge, true);
			}
		}

		return mergedDocCount;
	}

	synchronized void addMergeException(MergePolicy.OneMerge merge) {
		assert merge.getException() != null;
		if (!mergeExceptions.contains(merge) && mergeGen == merge.mergeGen)
			mergeExceptions.add(merge);
	}

	final int getBufferedDeleteTermsSize() {
		return docWriter.getPendingDeletes().terms.size();
	}

	final int getNumBufferedDeleteTerms() {
		return docWriter.getPendingDeletes().numTermDeletes.get();
	}

	synchronized SegmentInfo newestSegment() {
		return segmentInfos.size() > 0 ? segmentInfos
				.info(segmentInfos.size() - 1) : null;
	}

	public synchronized String segString() throws IOException {
		return segString(segmentInfos);
	}

	public synchronized String segString(Iterable<SegmentInfo> infos)
			throws IOException {
		final StringBuilder buffer = new StringBuilder();
		for (final SegmentInfo s : infos) {
			if (buffer.length() > 0) {
				buffer.append(' ');
			}
			buffer.append(segString(s));
		}
		return buffer.toString();
	}

	public synchronized String segString(SegmentInfo info) throws IOException {
		StringBuilder buffer = new StringBuilder();
		SegmentReader reader = readerPool.getIfExists(info);
		try {
			if (reader != null) {
				buffer.append(reader.toString());
			} else {
				buffer.append(info.toString(directory, 0));
				if (info.dir != directory) {
					buffer.append("**");
				}
			}
		} finally {
			if (reader != null) {
				readerPool.release(reader);
			}
		}
		return buffer.toString();
	}

	private synchronized void doWait() {

		try {
			wait(1000);
		} catch (InterruptedException ie) {
			throw new ThreadInterruptedException(ie);
		}
	}

	private boolean keepFullyDeletedSegments;

	void keepFullyDeletedSegments() {
		keepFullyDeletedSegments = true;
	}

	boolean getKeepFullyDeletedSegments() {
		return keepFullyDeletedSegments;
	}

	private boolean filesExist(SegmentInfos toSync) throws IOException {
		Collection<String> files = toSync.files(directory, false);
		for (final String fileName : files) {
			assert directory.fileExists(fileName) : "file " + fileName
					+ " does not exist";

			assert deleter.exists(fileName) : "IndexFileDeleter doesn't know about file "
					+ fileName;
		}
		return true;
	}

	private void startCommit(SegmentInfos toSync,
			Map<String, String> commitUserData) throws IOException {

		assert testPoint("startStartCommit");
		assert pendingCommit == null;

		if (hitOOM) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot commit");
		}

		try {

			if (infoStream != null)
				message("startCommit(): start");

			synchronized (this) {

				assert lastCommitChangeCount <= changeCount;

				if (pendingCommitChangeCount == lastCommitChangeCount) {
					if (infoStream != null) {
						message("  skip startCommit(): no changes pending");
					}
					deleter.decRef(toSync);
					return;
				}

				if (infoStream != null)
					message("startCommit index=" + segString(toSync)
							+ " changeCount=" + changeCount);

				assert filesExist(toSync);

				if (commitUserData != null) {
					toSync.setUserData(commitUserData);
				}
			}

			assert testPoint("midStartCommit");

			boolean pendingCommitSet = false;

			try {

				directory.sync(toSync.files(directory, false));

				assert testPoint("midStartCommit2");

				synchronized (this) {

					assert pendingCommit == null;

					assert segmentInfos.getGeneration() == toSync
							.getGeneration();

					toSync.prepareCommit(directory);
					pendingCommitSet = true;
					pendingCommit = toSync;
				}

				if (infoStream != null) {
					message("done all syncs");
				}

				assert testPoint("midStartCommitSuccess");

			} finally {
				synchronized (this) {

					segmentInfos.updateGeneration(toSync);

					if (!pendingCommitSet) {
						if (infoStream != null) {
							message("hit exception committing segments file");
						}

						deleter.decRef(toSync);
					}
				}
			}
		} catch (OutOfMemoryError oom) {
			handleOOM(oom, "startCommit");
		}
		assert testPoint("finishStartCommit");
	}

	public static boolean isLocked(Directory directory) throws IOException {
		return directory.makeLock(WRITE_LOCK_NAME).isLocked();
	}

	public static void unlock(Directory directory) throws IOException {
		directory.makeLock(IndexWriter.WRITE_LOCK_NAME).release();
	}

	@Deprecated
	public static final class MaxFieldLength {

		private int limit;
		private String name;

		private MaxFieldLength(String name, int limit) {
			this.name = name;
			this.limit = limit;
		}

		public MaxFieldLength(int limit) {
			this("User-specified", limit);
		}

		public int getLimit() {
			return limit;
		}

		@Override
		public String toString() {
			return name + ":" + limit;
		}

		public static final MaxFieldLength UNLIMITED = new MaxFieldLength(
				"UNLIMITED", Integer.MAX_VALUE);

		public static final MaxFieldLength LIMITED = new MaxFieldLength(
				"LIMITED", 10000);
	}

	public static abstract class IndexReaderWarmer {
		public abstract void warm(IndexReader reader) throws IOException;
	}

	@Deprecated
	public void setMergedSegmentWarmer(IndexReaderWarmer warmer) {
		config.setMergedSegmentWarmer(warmer);
	}

	@Deprecated
	public IndexReaderWarmer getMergedSegmentWarmer() {
		return config.getMergedSegmentWarmer();
	}

	private void handleOOM(OutOfMemoryError oom, String location) {
		if (infoStream != null) {
			message("hit OutOfMemoryError inside " + location);
		}
		hitOOM = true;
		throw oom;
	}

	boolean testPoint(String name) {
		return true;
	}

	synchronized boolean nrtIsCurrent(SegmentInfos infos) {

		ensureOpen();
		return infos.version == segmentInfos.version && !docWriter.anyChanges()
				&& !bufferedDeletesStream.any();
	}

	synchronized boolean isClosed() {
		return closed;
	}

	public synchronized void deleteUnusedFiles() throws IOException {
		ensureOpen(false);
		deleter.deletePendingFiles();
		deleter.revisitPolicy();
	}

	synchronized void deletePendingFiles() throws IOException {
		deleter.deletePendingFiles();
	}

	public void setPayloadProcessorProvider(PayloadProcessorProvider pcp) {
		ensureOpen();
		payloadProcessorProvider = pcp;
	}

	public PayloadProcessorProvider getPayloadProcessorProvider() {
		ensureOpen();
		return payloadProcessorProvider;
	}

	final class FlushControl {

		private boolean flushPending;
		private boolean flushDeletes;
		private int delCount;
		private int docCount;
		private boolean flushing;

		private synchronized boolean setFlushPending(String reason,
				boolean doWait) {
			if (flushPending || flushing) {
				if (doWait) {
					while (flushPending || flushing) {
						try {
							wait();
						} catch (InterruptedException ie) {
							throw new ThreadInterruptedException(ie);
						}
					}
				}
				return false;
			} else {
				if (infoStream != null) {
					message("now trigger flush reason=" + reason);
				}
				flushPending = true;
				return flushPending;
			}
		}

		public synchronized void setFlushPendingNoWait(String reason) {
			setFlushPending(reason, false);
		}

		public synchronized boolean getFlushPending() {
			return flushPending;
		}

		public synchronized boolean getFlushDeletes() {
			return flushDeletes;
		}

		public synchronized void clearFlushPending() {
			if (infoStream != null) {
				message("clearFlushPending");
			}
			flushPending = false;
			flushDeletes = false;
			docCount = 0;
			notifyAll();
		}

		public synchronized void clearDeletes() {
			delCount = 0;
		}

		public synchronized boolean waitUpdate(int docInc, int delInc) {
			return waitUpdate(docInc, delInc, false);
		}

		public synchronized boolean waitUpdate(int docInc, int delInc,
				boolean skipWait) {
			while (flushPending) {
				try {
					wait();
				} catch (InterruptedException ie) {
					throw new ThreadInterruptedException(ie);
				}
			}

			docCount += docInc;
			delCount += delInc;

			if (skipWait) {
				return false;
			}

			final int maxBufferedDocs = config.getMaxBufferedDocs();
			if (maxBufferedDocs != IndexWriterConfig.DISABLE_AUTO_FLUSH
					&& docCount >= maxBufferedDocs) {
				return setFlushPending("maxBufferedDocs", true);
			}

			final int maxBufferedDeleteTerms = config
					.getMaxBufferedDeleteTerms();
			if (maxBufferedDeleteTerms != IndexWriterConfig.DISABLE_AUTO_FLUSH
					&& delCount >= maxBufferedDeleteTerms) {
				flushDeletes = true;
				return setFlushPending("maxBufferedDeleteTerms", true);
			}

			return flushByRAMUsage("add delete/doc");
		}

		public synchronized boolean flushByRAMUsage(String reason) {
			final double ramBufferSizeMB = config.getRAMBufferSizeMB();
			if (ramBufferSizeMB != IndexWriterConfig.DISABLE_AUTO_FLUSH) {
				final long limit = (long) (ramBufferSizeMB * 1024 * 1024);
				long used = bufferedDeletesStream.bytesUsed()
						+ docWriter.bytesUsed();
				if (used >= limit) {

					docWriter.balanceRAM();

					used = bufferedDeletesStream.bytesUsed()
							+ docWriter.bytesUsed();
					if (used >= limit) {
						return setFlushPending("ram full: " + reason, false);
					}
				}
			}
			return false;
		}
	}

	final FlushControl flushControl = new FlushControl();
}
