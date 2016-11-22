/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.Lock;
import com.shatam.shatamindex.store.LockObtainFailedException;
import com.shatam.shatamindex.util.MapBackedSet;

class DirectoryReader extends IndexReader implements Cloneable {
	protected Directory directory;
	protected boolean readOnly;

	IndexWriter writer;

	private IndexDeletionPolicy deletionPolicy;
	private Lock writeLock;
	private final SegmentInfos segmentInfos;
	private boolean stale;
	private final int termInfosIndexDivisor;

	private boolean rollbackHasChanges;

	private SegmentReader[] subReaders;
	private int[] starts;
	private Map<String, byte[]> normsCache = new HashMap<String, byte[]>();
	private int maxDoc = 0;
	private int numDocs = -1;
	private boolean hasDeletions = false;

	private long maxIndexVersion;

	private final boolean applyAllDeletes;

	static IndexReader open(final Directory directory,
			final IndexDeletionPolicy deletionPolicy, final IndexCommit commit,
			final boolean readOnly, final int termInfosIndexDivisor)
			throws CorruptIndexException, IOException {
		return (IndexReader) new SegmentInfos.FindSegmentsFile(directory) {
			@Override
			protected Object doBody(String segmentFileName)
					throws CorruptIndexException, IOException {
				SegmentInfos infos = new SegmentInfos();
				infos.read(directory, segmentFileName);
				if (readOnly)
					return new ReadOnlyDirectoryReader(directory, infos,
							deletionPolicy, termInfosIndexDivisor, null);
				else
					return new DirectoryReader(directory, infos,
							deletionPolicy, false, termInfosIndexDivisor, null);
			}
		}.run(commit);
	}

	DirectoryReader(Directory directory, SegmentInfos sis,
			IndexDeletionPolicy deletionPolicy, boolean readOnly,
			int termInfosIndexDivisor,
			Collection<ReaderFinishedListener> readerFinishedListeners)
			throws IOException {
		this.directory = directory;
		this.readOnly = readOnly;
		this.segmentInfos = sis;
		this.deletionPolicy = deletionPolicy;
		this.termInfosIndexDivisor = termInfosIndexDivisor;

		if (readerFinishedListeners == null) {
			this.readerFinishedListeners = new MapBackedSet<ReaderFinishedListener>(
					new ConcurrentHashMap<ReaderFinishedListener, Boolean>());
		} else {
			this.readerFinishedListeners = readerFinishedListeners;
		}
		applyAllDeletes = false;

		SegmentReader[] readers = new SegmentReader[sis.size()];
		for (int i = sis.size() - 1; i >= 0; i--) {
			boolean success = false;
			try {
				readers[i] = SegmentReader.get(readOnly, sis.info(i),
						termInfosIndexDivisor);
				readers[i].readerFinishedListeners = this.readerFinishedListeners;
				success = true;
			} finally {
				if (!success) {

					for (i++; i < sis.size(); i++) {
						try {
							readers[i].close();
						} catch (Throwable ignore) {

						}
					}
				}
			}
		}

		initialize(readers);
	}

	DirectoryReader(IndexWriter writer, SegmentInfos infos,
			int termInfosIndexDivisor, boolean applyAllDeletes)
			throws IOException {
		this.directory = writer.getDirectory();
		this.readOnly = true;
		this.applyAllDeletes = applyAllDeletes;

		this.termInfosIndexDivisor = termInfosIndexDivisor;
		readerFinishedListeners = writer.getReaderFinishedListeners();

		final int numSegments = infos.size();

		List<SegmentReader> readers = new ArrayList<SegmentReader>();
		final Directory dir = writer.getDirectory();

		segmentInfos = (SegmentInfos) infos.clone();
		int infosUpto = 0;
		for (int i = 0; i < numSegments; i++) {
			boolean success = false;
			try {
				final SegmentInfo info = infos.info(i);
				assert info.dir == dir;
				final SegmentReader reader = writer.readerPool
						.getReadOnlyClone(info, true, termInfosIndexDivisor);
				if (reader.numDocs() > 0
						|| writer.getKeepFullyDeletedSegments()) {
					reader.readerFinishedListeners = readerFinishedListeners;
					readers.add(reader);
					infosUpto++;
				} else {
					reader.close();
					segmentInfos.remove(infosUpto);
				}
				success = true;
			} finally {
				if (!success) {

					for (SegmentReader reader : readers) {
						try {
							reader.close();
						} catch (Throwable ignore) {

						}
					}
				}
			}
		}

		this.writer = writer;

		initialize(readers.toArray(new SegmentReader[readers.size()]));
	}

	DirectoryReader(Directory directory, SegmentInfos infos,
			SegmentReader[] oldReaders, int[] oldStarts,
			Map<String, byte[]> oldNormsCache, boolean readOnly,
			boolean doClone, int termInfosIndexDivisor,
			Collection<ReaderFinishedListener> readerFinishedListeners)
			throws IOException {
		this.directory = directory;
		this.readOnly = readOnly;
		this.segmentInfos = infos;
		this.termInfosIndexDivisor = termInfosIndexDivisor;
		assert readerFinishedListeners != null;
		this.readerFinishedListeners = readerFinishedListeners;
		applyAllDeletes = false;

		Map<String, Integer> segmentReaders = new HashMap<String, Integer>();

		if (oldReaders != null) {

			for (int i = 0; i < oldReaders.length; i++) {
				segmentReaders.put(oldReaders[i].getSegmentName(),
						Integer.valueOf(i));
			}
		}

		SegmentReader[] newReaders = new SegmentReader[infos.size()];

		boolean[] readerShared = new boolean[infos.size()];

		for (int i = infos.size() - 1; i >= 0; i--) {

			Integer oldReaderIndex = segmentReaders.get(infos.info(i).name);
			if (oldReaderIndex == null) {

				newReaders[i] = null;
			} else {

				newReaders[i] = oldReaders[oldReaderIndex.intValue()];
			}

			boolean success = false;
			try {
				SegmentReader newReader;
				if (newReaders[i] == null
						|| infos.info(i).getUseCompoundFile() != newReaders[i]
								.getSegmentInfo().getUseCompoundFile()) {

					assert !doClone;

					newReader = SegmentReader.get(readOnly, infos.info(i),
							termInfosIndexDivisor);
					newReader.readerFinishedListeners = readerFinishedListeners;
					readerShared[i] = false;
					newReaders[i] = newReader;
				} else {
					newReader = newReaders[i].reopenSegment(infos.info(i),
							doClone, readOnly);
					if (newReader == null) {

						readerShared[i] = true;
						newReaders[i].incRef();
					} else {
						assert newReader.readerFinishedListeners == readerFinishedListeners;
						readerShared[i] = false;

						newReaders[i] = newReader;
					}
				}
				success = true;
			} finally {
				if (!success) {
					for (i++; i < infos.size(); i++) {
						if (newReaders[i] != null) {
							try {
								if (!readerShared[i]) {

									newReaders[i].close();
								} else {

									newReaders[i].decRef();
								}
							} catch (IOException ignore) {

							}
						}
					}
				}
			}
		}

		initialize(newReaders);

		if (oldNormsCache != null) {
			for (Map.Entry<String, byte[]> entry : oldNormsCache.entrySet()) {
				String field = entry.getKey();
				if (!hasNorms(field)) {
					continue;
				}

				byte[] oldBytes = entry.getValue();

				byte[] bytes = new byte[maxDoc()];

				for (int i = 0; i < subReaders.length; i++) {
					Integer oldReaderIndex = segmentReaders.get(subReaders[i]
							.getSegmentName());

					if (oldReaderIndex != null
							&& (oldReaders[oldReaderIndex.intValue()] == subReaders[i] || oldReaders[oldReaderIndex
									.intValue()].norms.get(field) == subReaders[i].norms
									.get(field))) {

						System.arraycopy(oldBytes,
								oldStarts[oldReaderIndex.intValue()], bytes,
								starts[i], starts[i + 1] - starts[i]);
					} else {
						subReaders[i].norms(field, bytes, starts[i]);
					}
				}

				normsCache.put(field, bytes);
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		if (hasChanges) {
			buffer.append("*");
		}
		buffer.append(getClass().getSimpleName());
		buffer.append('(');
		final String segmentsFile = segmentInfos.getCurrentSegmentFileName();
		if (segmentsFile != null) {
			buffer.append(segmentsFile);
		}
		if (writer != null) {
			buffer.append(":nrt");
		}
		for (int i = 0; i < subReaders.length; i++) {
			buffer.append(' ');
			buffer.append(subReaders[i]);
		}
		buffer.append(')');
		return buffer.toString();
	}

	private void initialize(SegmentReader[] subReaders) throws IOException {
		this.subReaders = subReaders;
		starts = new int[subReaders.length + 1];
		for (int i = 0; i < subReaders.length; i++) {
			starts[i] = maxDoc;
			maxDoc += subReaders[i].maxDoc();

			if (subReaders[i].hasDeletions())
				hasDeletions = true;
		}
		starts[subReaders.length] = maxDoc;

		if (!readOnly) {
			maxIndexVersion = SegmentInfos.readCurrentVersion(directory);
		}
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

		DirectoryReader newReader = doOpenIfChanged(
				(SegmentInfos) segmentInfos.clone(), true, openReadOnly);

		if (this != newReader) {
			newReader.deletionPolicy = deletionPolicy;
		}
		newReader.writer = writer;

		if (!openReadOnly && writeLock != null) {

			assert writer == null;
			newReader.writeLock = writeLock;
			newReader.hasChanges = hasChanges;
			newReader.hasDeletions = hasDeletions;
			writeLock = null;
			hasChanges = false;
		}
		assert newReader.readerFinishedListeners != null;

		return newReader;
	}

	@Override
	protected final IndexReader doOpenIfChanged() throws CorruptIndexException,
			IOException {

		return doOpenIfChanged(readOnly, null);
	}

	@Override
	protected final IndexReader doOpenIfChanged(boolean openReadOnly)
			throws CorruptIndexException, IOException {
		return doOpenIfChanged(openReadOnly, null);
	}

	@Override
	protected final IndexReader doOpenIfChanged(final IndexCommit commit)
			throws CorruptIndexException, IOException {
		return doOpenIfChanged(true, commit);
	}

	@Override
	protected final IndexReader doOpenIfChanged(IndexWriter writer,
			boolean applyAllDeletes) throws CorruptIndexException, IOException {
		if (writer == this.writer && applyAllDeletes == this.applyAllDeletes) {
			return doOpenIfChanged();
		} else {
			return super.doOpenIfChanged(writer, applyAllDeletes);
		}
	}

	private final IndexReader doOpenFromWriter(boolean openReadOnly,
			IndexCommit commit) throws CorruptIndexException, IOException {
		assert readOnly;

		if (!openReadOnly) {
			throw new IllegalArgumentException(
					"a reader obtained from IndexWriter.getReader() can only be reopened with openReadOnly=true (got false)");
		}

		if (commit != null) {
			throw new IllegalArgumentException(
					"a reader obtained from IndexWriter.getReader() cannot currently accept a commit");
		}

		if (writer.nrtIsCurrent(segmentInfos)) {
			return null;
		}

		IndexReader reader = writer.getReader(applyAllDeletes);

		if (reader.getVersion() == segmentInfos.getVersion()) {
			reader.decRef();
			return null;
		}

		reader.readerFinishedListeners = readerFinishedListeners;
		return reader;
	}

	private IndexReader doOpenIfChanged(final boolean openReadOnly,
			IndexCommit commit) throws CorruptIndexException, IOException {
		ensureOpen();

		assert commit == null || openReadOnly;

		if (writer != null) {
			return doOpenFromWriter(openReadOnly, commit);
		} else {
			return doOpenNoWriter(openReadOnly, commit);
		}
	}

	private synchronized IndexReader doOpenNoWriter(final boolean openReadOnly,
			IndexCommit commit) throws CorruptIndexException, IOException {

		if (commit == null) {
			if (hasChanges) {

				assert readOnly == false;

				assert writeLock != null;

				assert isCurrent();

				if (openReadOnly) {
					return clone(openReadOnly);
				} else {
					return null;
				}
			} else if (isCurrent()) {
				if (openReadOnly != readOnly) {

					return clone(openReadOnly);
				} else {
					return null;
				}
			}
		} else {
			if (directory != commit.getDirectory()) {
				throw new IOException(
						"the specified commit does not match the specified Directory");
			}
			if (segmentInfos != null
					&& commit.getSegmentsFileName().equals(
							segmentInfos.getCurrentSegmentFileName())) {
				if (readOnly != openReadOnly) {

					return clone(openReadOnly);
				} else {
					return null;
				}
			}
		}

		return (IndexReader) new SegmentInfos.FindSegmentsFile(directory) {
			@Override
			protected Object doBody(String segmentFileName)
					throws CorruptIndexException, IOException {
				SegmentInfos infos = new SegmentInfos();
				infos.read(directory, segmentFileName);
				return doOpenIfChanged(infos, false, openReadOnly);
			}
		}.run(commit);
	}

	private synchronized DirectoryReader doOpenIfChanged(SegmentInfos infos,
			boolean doClone, boolean openReadOnly)
			throws CorruptIndexException, IOException {
		DirectoryReader reader;
		if (openReadOnly) {
			reader = new ReadOnlyDirectoryReader(directory, infos, subReaders,
					starts, normsCache, doClone, termInfosIndexDivisor,
					readerFinishedListeners);
		} else {
			reader = new DirectoryReader(directory, infos, subReaders, starts,
					normsCache, false, doClone, termInfosIndexDivisor,
					readerFinishedListeners);
		}
		return reader;
	}

	@Override
	public long getVersion() {
		ensureOpen();
		return segmentInfos.getVersion();
	}

	@Override
	public TermFreqVector[] getTermFreqVectors(int n) throws IOException {
		ensureOpen();
		int i = readerIndex(n);
		return subReaders[i].getTermFreqVectors(n - starts[i]);
	}

	@Override
	public TermFreqVector getTermFreqVector(int n, String field)
			throws IOException {
		ensureOpen();
		int i = readerIndex(n);
		return subReaders[i].getTermFreqVector(n - starts[i], field);
	}

	@Override
	public void getTermFreqVector(int docNumber, String field,
			TermVectorMapper mapper) throws IOException {
		ensureOpen();
		int i = readerIndex(docNumber);
		subReaders[i].getTermFreqVector(docNumber - starts[i], field, mapper);
	}

	@Override
	public void getTermFreqVector(int docNumber, TermVectorMapper mapper)
			throws IOException {
		ensureOpen();
		int i = readerIndex(docNumber);
		subReaders[i].getTermFreqVector(docNumber - starts[i], mapper);
	}

	@Deprecated
	@Override
	public boolean isOptimized() {
		ensureOpen();
		return segmentInfos.size() == 1 && !hasDeletions();
	}

	@Override
	public int numDocs() {

		if (numDocs == -1) {
			int n = 0;
			for (int i = 0; i < subReaders.length; i++)
				n += subReaders[i].numDocs();
			numDocs = n;
		}
		return numDocs;
	}

	@Override
	public int maxDoc() {

		return maxDoc;
	}

	@Override
	public Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		ensureOpen();
		int i = readerIndex(n);
		return subReaders[i].document(n - starts[i], fieldSelector);
	}

	@Override
	public boolean isDeleted(int n) {

		final int i = readerIndex(n);
		return subReaders[i].isDeleted(n - starts[i]);
	}

	@Override
	public boolean hasDeletions() {
		ensureOpen();
		return hasDeletions;
	}

	@Override
	protected void doDelete(int n) throws CorruptIndexException, IOException {
		numDocs = -1;
		int i = readerIndex(n);
		subReaders[i].deleteDocument(n - starts[i]);
		hasDeletions = true;
	}

	@Override
	protected void doUndeleteAll() throws CorruptIndexException, IOException {
		for (int i = 0; i < subReaders.length; i++)
			subReaders[i].undeleteAll();

		hasDeletions = false;
		numDocs = -1;
	}

	private int readerIndex(int n) {
		return readerIndex(n, this.starts, this.subReaders.length);
	}

	final static int readerIndex(int n, int[] starts, int numSubReaders) {
		int lo = 0;
		int hi = numSubReaders - 1;

		while (hi >= lo) {
			int mid = (lo + hi) >>> 1;
			int midValue = starts[mid];
			if (n < midValue)
				hi = mid - 1;
			else if (n > midValue)
				lo = mid + 1;
			else {
				while (mid + 1 < numSubReaders && starts[mid + 1] == midValue) {
					mid++;
				}
				return mid;
			}
		}
		return hi;
	}

	@Override
	public boolean hasNorms(String field) throws IOException {
		ensureOpen();
		for (int i = 0; i < subReaders.length; i++) {
			if (subReaders[i].hasNorms(field))
				return true;
		}
		return false;
	}

	@Override
	public synchronized byte[] norms(String field) throws IOException {
		ensureOpen();
		byte[] bytes = normsCache.get(field);
		if (bytes != null)
			return bytes;
		if (!hasNorms(field))
			return null;

		bytes = new byte[maxDoc()];
		for (int i = 0; i < subReaders.length; i++)
			subReaders[i].norms(field, bytes, starts[i]);
		normsCache.put(field, bytes);
		return bytes;
	}

	@Override
	public synchronized void norms(String field, byte[] result, int offset)
			throws IOException {
		ensureOpen();
		byte[] bytes = normsCache.get(field);
		if (bytes == null && !hasNorms(field)) {
			Arrays.fill(result, offset, result.length, Similarity.getDefault()
					.encodeNormValue(1.0f));
		} else if (bytes != null) {
			System.arraycopy(bytes, 0, result, offset, maxDoc());
		} else {
			for (int i = 0; i < subReaders.length; i++) {
				subReaders[i].norms(field, result, offset + starts[i]);
			}
		}
	}

	@Override
	protected void doSetNorm(int n, String field, byte value)
			throws CorruptIndexException, IOException {
		synchronized (normsCache) {
			normsCache.remove(field);
		}
		int i = readerIndex(n);
		subReaders[i].setNorm(n - starts[i], field, value);
	}

	@Override
	public TermEnum terms() throws IOException {
		ensureOpen();
		if (subReaders.length == 1) {

			return subReaders[0].terms();
		} else {
			return new MultiTermEnum(this, subReaders, starts, null);
		}
	}

	@Override
	public TermEnum terms(Term term) throws IOException {
		ensureOpen();
		if (subReaders.length == 1) {

			return subReaders[0].terms(term);
		} else {
			return new MultiTermEnum(this, subReaders, starts, term);
		}
	}

	@Override
	public int docFreq(Term t) throws IOException {
		ensureOpen();
		int total = 0;
		for (int i = 0; i < subReaders.length; i++)
			total += subReaders[i].docFreq(t);
		return total;
	}

	@Override
	public TermDocs termDocs() throws IOException {
		ensureOpen();
		if (subReaders.length == 1) {

			return subReaders[0].termDocs();
		} else {
			return new MultiTermDocs(this, subReaders, starts);
		}
	}

	@Override
	public TermDocs termDocs(Term term) throws IOException {
		ensureOpen();
		if (subReaders.length == 1) {

			return subReaders[0].termDocs(term);
		} else {
			return super.termDocs(term);
		}
	}

	@Override
	public TermPositions termPositions() throws IOException {
		ensureOpen();
		if (subReaders.length == 1) {

			return subReaders[0].termPositions();
		} else {
			return new MultiTermPositions(this, subReaders, starts);
		}
	}

	@Override
	protected void acquireWriteLock() throws StaleReaderException,
			CorruptIndexException, LockObtainFailedException, IOException {

		if (readOnly) {

			ReadOnlySegmentReader.noWrite();
		}

		if (segmentInfos != null) {
			ensureOpen();
			if (stale)
				throw new StaleReaderException(
						"IndexReader out of date and no longer valid for delete, undelete, or setNorm operations");

			if (writeLock == null) {
				Lock writeLock = directory
						.makeLock(IndexWriter.WRITE_LOCK_NAME);
				if (!writeLock.obtain(IndexWriterConfig.WRITE_LOCK_TIMEOUT))
					throw new LockObtainFailedException(
							"Index locked for write: " + writeLock);
				this.writeLock = writeLock;

				if (SegmentInfos.readCurrentVersion(directory) > maxIndexVersion) {
					stale = true;
					this.writeLock.release();
					this.writeLock = null;
					throw new StaleReaderException(
							"IndexReader out of date and no longer valid for delete, undelete, or setNorm operations");
				}
			}
		}
	}

	@Override
	protected void doCommit(Map<String, String> commitUserData)
			throws IOException {
		if (hasChanges) {
			segmentInfos.setUserData(commitUserData);

			IndexFileDeleter deleter = new IndexFileDeleter(
					directory,
					deletionPolicy == null ? new KeepOnlyLastCommitDeletionPolicy()
							: deletionPolicy, segmentInfos, null, null);
			segmentInfos.updateGeneration(deleter.getLastSegmentInfos());
			segmentInfos.changed();

			startCommit();

			final List<SegmentInfo> rollbackSegments = segmentInfos
					.createBackupSegmentInfos(false);

			boolean success = false;
			try {
				for (int i = 0; i < subReaders.length; i++)
					subReaders[i].commit();

				segmentInfos.pruneDeletedSegments();

				directory.sync(segmentInfos.files(directory, false));
				segmentInfos.commit(directory);
				success = true;
			} finally {

				if (!success) {

					rollbackCommit();

					deleter.refresh();

					segmentInfos.rollbackSegmentInfos(rollbackSegments);
				}
			}

			deleter.checkpoint(segmentInfos, true);
			deleter.close();

			maxIndexVersion = segmentInfos.getVersion();

			if (writeLock != null) {
				writeLock.release();
				writeLock = null;
			}
		}
		hasChanges = false;
	}

	void startCommit() {
		rollbackHasChanges = hasChanges;
		for (int i = 0; i < subReaders.length; i++) {
			subReaders[i].startCommit();
		}
	}

	void rollbackCommit() {
		hasChanges = rollbackHasChanges;
		for (int i = 0; i < subReaders.length; i++) {
			subReaders[i].rollbackCommit();
		}
	}

	@Override
	public Map<String, String> getCommitUserData() {
		ensureOpen();
		return segmentInfos.getUserData();
	}

	@Override
	public boolean isCurrent() throws CorruptIndexException, IOException {
		ensureOpen();
		if (writer == null || writer.isClosed()) {

			return SegmentInfos.readCurrentVersion(directory) == segmentInfos
					.getVersion();
		} else {
			return writer.nrtIsCurrent(segmentInfos);
		}
	}

	@Override
	protected synchronized void doClose() throws IOException {
		IOException ioe = null;
		normsCache = null;
		for (int i = 0; i < subReaders.length; i++) {

			try {
				subReaders[i].decRef();
			} catch (IOException e) {
				if (ioe == null)
					ioe = e;
			}
		}

		if (writer != null) {

			writer.deletePendingFiles();
		}

		if (ioe != null)
			throw ioe;
	}

	@Override
	public Collection<String> getFieldNames(IndexReader.FieldOption fieldNames) {
		ensureOpen();
		return getFieldNames(fieldNames, this.subReaders);
	}

	static Collection<String> getFieldNames(IndexReader.FieldOption fieldNames,
			IndexReader[] subReaders) {

		Set<String> fieldSet = new HashSet<String>();
		for (IndexReader reader : subReaders) {
			Collection<String> names = reader.getFieldNames(fieldNames);
			fieldSet.addAll(names);
		}
		return fieldSet;
	}

	@Override
	public IndexReader[] getSequentialSubReaders() {
		return subReaders;
	}

	@Override
	public Directory directory() {

		return directory;
	}

	@Override
	public int getTermInfosIndexDivisor() {
		ensureOpen();
		return termInfosIndexDivisor;
	}

	@Override
	public IndexCommit getIndexCommit() throws IOException {
		ensureOpen();
		return new ReaderCommit(segmentInfos, directory);
	}

	public static Collection<IndexCommit> listCommits(Directory dir)
			throws IOException {
		final String[] files = dir.listAll();

		List<IndexCommit> commits = new ArrayList<IndexCommit>();

		SegmentInfos latest = new SegmentInfos();
		latest.read(dir);
		final long currentGen = latest.getGeneration();

		commits.add(new ReaderCommit(latest, dir));

		for (int i = 0; i < files.length; i++) {

			final String fileName = files[i];

			if (fileName.startsWith(IndexFileNames.SEGMENTS)
					&& !fileName.equals(IndexFileNames.SEGMENTS_GEN)
					&& SegmentInfos.generationFromSegmentsFileName(fileName) < currentGen) {

				SegmentInfos sis = new SegmentInfos();
				try {

					sis.read(dir, fileName);
				} catch (FileNotFoundException fnfe) {

					sis = null;
				}

				if (sis != null)
					commits.add(new ReaderCommit(sis, dir));
			}
		}

		Collections.sort(commits);

		return commits;
	}

	private static final class ReaderCommit extends IndexCommit {
		private String segmentsFileName;
		Collection<String> files;
		Directory dir;
		long generation;
		long version;
		final Map<String, String> userData;
		private final int segmentCount;

		ReaderCommit(SegmentInfos infos, Directory dir) throws IOException {
			segmentsFileName = infos.getCurrentSegmentFileName();
			this.dir = dir;
			userData = infos.getUserData();
			files = Collections.unmodifiableCollection(infos.files(dir, true));
			version = infos.getVersion();
			generation = infos.getGeneration();
			segmentCount = infos.size();
		}

		@Override
		public String toString() {
			return "DirectoryReader.ReaderCommit(" + segmentsFileName + ")";
		}

		@Override
		public int getSegmentCount() {
			return segmentCount;
		}

		@Override
		public String getSegmentsFileName() {
			return segmentsFileName;
		}

		@Override
		public Collection<String> getFileNames() {
			return files;
		}

		@Override
		public Directory getDirectory() {
			return dir;
		}

		@Override
		public long getVersion() {
			return version;
		}

		@Override
		public long getGeneration() {
			return generation;
		}

		@Override
		public boolean isDeleted() {
			return false;
		}

		@Override
		public Map<String, String> getUserData() {
			return userData;
		}

		@Override
		public void delete() {
			throw new UnsupportedOperationException(
					"This IndexCommit does not support deletions");
		}
	}

	static class MultiTermEnum extends TermEnum {
		IndexReader topReader;
		private SegmentMergeQueue queue;

		private Term term;
		private int docFreq;
		final SegmentMergeInfo[] matchingSegments;

		public MultiTermEnum(IndexReader topReader, IndexReader[] readers,
				int[] starts, Term t) throws IOException {
			this.topReader = topReader;
			queue = new SegmentMergeQueue(readers.length);
			matchingSegments = new SegmentMergeInfo[readers.length + 1];
			for (int i = 0; i < readers.length; i++) {
				IndexReader reader = readers[i];
				TermEnum termEnum;

				if (t != null) {
					termEnum = reader.terms(t);
				} else
					termEnum = reader.terms();

				SegmentMergeInfo smi = new SegmentMergeInfo(starts[i],
						termEnum, reader);
				smi.ord = i;
				if (t == null ? smi.next() : termEnum.term() != null)
					queue.add(smi);
				else
					smi.close();
			}

			if (t != null && queue.size() > 0) {
				next();
			}
		}

		@Override
		public boolean next() throws IOException {
			for (int i = 0; i < matchingSegments.length; i++) {
				SegmentMergeInfo smi = matchingSegments[i];
				if (smi == null)
					break;
				if (smi.next())
					queue.add(smi);
				else
					smi.close();
			}

			int numMatchingSegments = 0;
			matchingSegments[0] = null;

			SegmentMergeInfo top = queue.top();

			if (top == null) {
				term = null;
				return false;
			}

			term = top.term;
			docFreq = 0;

			while (top != null && term.compareTo(top.term) == 0) {
				matchingSegments[numMatchingSegments++] = top;
				queue.pop();
				docFreq += top.termEnum.docFreq();
				top = queue.top();
			}

			matchingSegments[numMatchingSegments] = null;
			return true;
		}

		@Override
		public Term term() {
			return term;
		}

		@Override
		public int docFreq() {
			return docFreq;
		}

		@Override
		public void close() throws IOException {
			queue.close();
		}
	}

	static class MultiTermDocs implements TermDocs {
		IndexReader topReader;
		protected IndexReader[] readers;
		protected int[] starts;
		protected Term term;

		protected int base = 0;
		protected int pointer = 0;

		private TermDocs[] readerTermDocs;
		protected TermDocs current;

		private MultiTermEnum tenum;
		int matchingSegmentPos;
		SegmentMergeInfo smi;

		public MultiTermDocs(IndexReader topReader, IndexReader[] r, int[] s) {
			this.topReader = topReader;
			readers = r;
			starts = s;

			readerTermDocs = new TermDocs[r.length];
		}

		public int doc() {
			return base + current.doc();
		}

		public int freq() {
			return current.freq();
		}

		public void seek(Term term) {
			this.term = term;
			this.base = 0;
			this.pointer = 0;
			this.current = null;
			this.tenum = null;
			this.smi = null;
			this.matchingSegmentPos = 0;
		}

		public void seek(TermEnum termEnum) throws IOException {
			seek(termEnum.term());
			if (termEnum instanceof MultiTermEnum) {
				tenum = (MultiTermEnum) termEnum;
				if (topReader != tenum.topReader)
					tenum = null;
			}
		}

		public boolean next() throws IOException {
			for (;;) {
				if (current != null && current.next()) {
					return true;
				} else if (pointer < readers.length) {
					if (tenum != null) {
						smi = tenum.matchingSegments[matchingSegmentPos++];
						if (smi == null) {
							pointer = readers.length;
							return false;
						}
						pointer = smi.ord;
					}
					base = starts[pointer];
					current = termDocs(pointer++);
				} else {
					return false;
				}
			}
		}

		public int read(final int[] docs, final int[] freqs) throws IOException {
			while (true) {
				while (current == null) {
					if (pointer < readers.length) {
						if (tenum != null) {
							smi = tenum.matchingSegments[matchingSegmentPos++];
							if (smi == null) {
								pointer = readers.length;
								return 0;
							}
							pointer = smi.ord;
						}
						base = starts[pointer];
						current = termDocs(pointer++);
					} else {
						return 0;
					}
				}
				int end = current.read(docs, freqs);
				if (end == 0) {
					current = null;
				} else {
					final int b = base;
					for (int i = 0; i < end; i++)
						docs[i] += b;
					return end;
				}
			}
		}

		public boolean skipTo(int target) throws IOException {
			for (;;) {
				if (current != null && current.skipTo(target - base)) {
					return true;
				} else if (pointer < readers.length) {
					if (tenum != null) {
						SegmentMergeInfo smi = tenum.matchingSegments[matchingSegmentPos++];
						if (smi == null) {
							pointer = readers.length;
							return false;
						}
						pointer = smi.ord;
					}
					base = starts[pointer];
					current = termDocs(pointer++);
				} else
					return false;
			}
		}

		private TermDocs termDocs(int i) throws IOException {
			TermDocs result = readerTermDocs[i];
			if (result == null)
				result = readerTermDocs[i] = termDocs(readers[i]);
			if (smi != null) {
				assert (smi.ord == i);
				assert (smi.termEnum.term().equals(term));
				result.seek(smi.termEnum);
			} else {
				result.seek(term);
			}
			return result;
		}

		protected TermDocs termDocs(IndexReader reader) throws IOException {
			return term == null ? reader.termDocs(null) : reader.termDocs();
		}

		public void close() throws IOException {
			for (int i = 0; i < readerTermDocs.length; i++) {
				if (readerTermDocs[i] != null)
					readerTermDocs[i].close();
			}
		}
	}

	static class MultiTermPositions extends MultiTermDocs implements
			TermPositions {
		public MultiTermPositions(IndexReader topReader, IndexReader[] r,
				int[] s) {
			super(topReader, r, s);
		}

		@Override
		protected TermDocs termDocs(IndexReader reader) throws IOException {
			return reader.termPositions();
		}

		public int nextPosition() throws IOException {
			return ((TermPositions) current).nextPosition();
		}

		public int getPayloadLength() {
			return ((TermPositions) current).getPayloadLength();
		}

		public byte[] getPayload(byte[] data, int offset) throws IOException {
			return ((TermPositions) current).getPayload(data, offset);
		}

		public boolean isPayloadAvailable() {
			return ((TermPositions) current).isPayloadAvailable();
		}
	}
}
