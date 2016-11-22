/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.ChecksumIndexInput;
import com.shatam.shatamindex.store.ChecksumIndexOutput;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.store.NoSuchDirectoryException;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.ThreadInterruptedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SegmentInfos implements Cloneable, Iterable<SegmentInfo> {

	public static final int FORMAT = -1;

	public static final int FORMAT_LOCKLESS = -2;

	public static final int FORMAT_SINGLE_NORM_FILE = -3;

	public static final int FORMAT_SHARED_DOC_STORE = -4;

	public static final int FORMAT_CHECKSUM = -5;

	public static final int FORMAT_DEL_COUNT = -6;

	public static final int FORMAT_HAS_PROX = -7;

	public static final int FORMAT_USER_DATA = -8;

	public static final int FORMAT_DIAGNOSTICS = -9;

	public static final int FORMAT_HAS_VECTORS = -10;

	public static final int FORMAT_3_1 = -11;

	public static final int CURRENT_FORMAT = FORMAT_3_1;

	public static final int FORMAT_MINIMUM = FORMAT;
	public static final int FORMAT_MAXIMUM = CURRENT_FORMAT;

	public int counter = 0;

	long version = System.currentTimeMillis();

	private long generation = 0;
	private long lastGeneration = 0;

	private Map<String, String> userData = Collections
			.<String, String> emptyMap();

	private int format;

	private List<SegmentInfo> segments = new ArrayList<SegmentInfo>();
	private Set<SegmentInfo> segmentSet = new HashSet<SegmentInfo>();
	private transient List<SegmentInfo> cachedUnmodifiableList;
	private transient Set<SegmentInfo> cachedUnmodifiableSet;

	private static PrintStream infoStream = null;

	public void setFormat(int format) {
		this.format = format;
	}

	public int getFormat() {
		return format;
	}

	public SegmentInfo info(int i) {
		return segments.get(i);
	}

	public static long getCurrentSegmentGeneration(String[] files) {
		if (files == null) {
			return -1;
		}
		long max = -1;
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			if (file.startsWith(IndexFileNames.SEGMENTS)
					&& !file.equals(IndexFileNames.SEGMENTS_GEN)) {
				long gen = generationFromSegmentsFileName(file);
				if (gen > max) {
					max = gen;
				}
			}
		}
		return max;
	}

	public static long getCurrentSegmentGeneration(Directory directory)
			throws IOException {
		try {
			return getCurrentSegmentGeneration(directory.listAll());
		} catch (NoSuchDirectoryException nsde) {
			return -1;
		}
	}

	public static String getCurrentSegmentFileName(String[] files)
			throws IOException {
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS,
				"", getCurrentSegmentGeneration(files));
	}

	public static String getCurrentSegmentFileName(Directory directory)
			throws IOException {
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS,
				"", getCurrentSegmentGeneration(directory));
	}

	public String getCurrentSegmentFileName() {
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS,
				"", lastGeneration);
	}

	public static long generationFromSegmentsFileName(String fileName) {
		if (fileName.equals(IndexFileNames.SEGMENTS)) {
			return 0;
		} else if (fileName.startsWith(IndexFileNames.SEGMENTS)) {
			return Long.parseLong(
					fileName.substring(1 + IndexFileNames.SEGMENTS.length()),
					Character.MAX_RADIX);
		} else {
			throw new IllegalArgumentException("fileName \"" + fileName
					+ "\" is not a segments file");
		}
	}

	public String getNextSegmentFileName() {
		long nextGeneration;

		if (generation == -1) {
			nextGeneration = 1;
		} else {
			nextGeneration = generation + 1;
		}
		return IndexFileNames.fileNameFromGeneration(IndexFileNames.SEGMENTS,
				"", nextGeneration);
	}

	public final void read(Directory directory, String segmentFileName)
			throws CorruptIndexException, IOException {
		boolean success = false;

		this.clear();

		ChecksumIndexInput input = new ChecksumIndexInput(
				directory.openInput(segmentFileName));

		generation = generationFromSegmentsFileName(segmentFileName);

		lastGeneration = generation;

		try {
			int format = input.readInt();

			if (format > FORMAT_MINIMUM) {
				throw new IndexFormatTooOldException(input, format,
						FORMAT_MINIMUM, FORMAT_MAXIMUM);
			}
			if (format < FORMAT_MAXIMUM) {
				throw new IndexFormatTooNewException(input, format,
						FORMAT_MINIMUM, FORMAT_MAXIMUM);
			}
			version = input.readLong();
			counter = input.readInt();

			for (int i = input.readInt(); i > 0; i--) {
				SegmentInfo si = new SegmentInfo(directory, format, input);
				if (si.getVersion() == null) {

					Directory dir = directory;
					if (si.getDocStoreOffset() != -1) {
						if (si.getDocStoreIsCompoundFile()) {
							dir = new CompoundFileReader(
									dir,
									IndexFileNames.segmentFileName(
											si.getDocStoreSegment(),
											IndexFileNames.COMPOUND_FILE_STORE_EXTENSION),
									1024);
						}
					} else if (si.getUseCompoundFile()) {
						dir = new CompoundFileReader(
								dir,
								IndexFileNames.segmentFileName(si.name,
										IndexFileNames.COMPOUND_FILE_EXTENSION),
								1024);
					}

					try {
						String store = si.getDocStoreOffset() != -1 ? si
								.getDocStoreSegment() : si.name;
						si.setVersion(FieldsReader
								.detectCodeVersion(dir, store));
					} finally {

						if (dir != directory)
							dir.close();
					}
				}
				add(si);
			}

			if (format >= 0) {
				if (input.getFilePointer() >= input.length())
					version = System.currentTimeMillis();
				else
					version = input.readLong();
			}

			if (format <= FORMAT_USER_DATA) {
				if (format <= FORMAT_DIAGNOSTICS) {
					userData = input.readStringStringMap();
				} else if (0 != input.readByte()) {
					userData = Collections.singletonMap("userData",
							input.readString());
				} else {
					userData = Collections.<String, String> emptyMap();
				}
			} else {
				userData = Collections.<String, String> emptyMap();
			}

			if (format <= FORMAT_CHECKSUM) {
				final long checksumNow = input.getChecksum();
				final long checksumThen = input.readLong();
				if (checksumNow != checksumThen)
					throw new CorruptIndexException(
							"checksum mismatch in segments file (resource: "
									+ input + ")");
			}
			success = true;
		} finally {
			input.close();
			if (!success) {

				this.clear();
			}
		}
	}

	public final void read(Directory directory) throws CorruptIndexException,
			IOException {

		generation = lastGeneration = -1;

		new FindSegmentsFile(directory) {

			@Override
			protected Object doBody(String segmentFileName)
					throws CorruptIndexException, IOException {
				read(directory, segmentFileName);
				return null;
			}
		}.run();
	}

	ChecksumIndexOutput pendingSegnOutput;

	private final void write(Directory directory) throws IOException {

		String segmentFileName = getNextSegmentFileName();

		if (generation == -1) {
			generation = 1;
		} else {
			generation++;
		}

		ChecksumIndexOutput segnOutput = new ChecksumIndexOutput(
				directory.createOutput(segmentFileName));

		boolean success = false;

		try {
			segnOutput.writeInt(CURRENT_FORMAT);
			segnOutput.writeLong(version);
			segnOutput.writeInt(counter);
			segnOutput.writeInt(size());
			for (SegmentInfo si : this) {
				si.write(segnOutput);
			}
			segnOutput.writeStringStringMap(userData);
			segnOutput.prepareCommit();
			pendingSegnOutput = segnOutput;
			success = true;
		} finally {
			if (!success) {

				IOUtils.closeWhileHandlingException(segnOutput);
				try {

					directory.deleteFile(segmentFileName);
				} catch (Throwable t) {

				}
			}
		}
	}

	public void pruneDeletedSegments() throws IOException {
		for (final Iterator<SegmentInfo> it = segments.iterator(); it.hasNext();) {
			final SegmentInfo info = it.next();
			if (info.getDelCount() == info.docCount) {
				it.remove();
				segmentSet.remove(info);
			}
		}
		assert segmentSet.size() == segments.size();
	}

	@Override
	public Object clone() {
		try {
			final SegmentInfos sis = (SegmentInfos) super.clone();

			sis.segments = new ArrayList<SegmentInfo>(size());
			sis.segmentSet = new HashSet<SegmentInfo>(size());
			sis.cachedUnmodifiableList = null;
			sis.cachedUnmodifiableSet = null;
			for (final SegmentInfo info : this) {

				sis.add((SegmentInfo) info.clone());
			}
			sis.userData = new HashMap<String, String>(userData);
			return sis;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("should not happen", e);
		}
	}

	public long getVersion() {
		return version;
	}

	public long getGeneration() {
		return generation;
	}

	public long getLastGeneration() {
		return lastGeneration;
	}

	public static long readCurrentVersion(Directory directory)
			throws CorruptIndexException, IOException {

		SegmentInfos sis = new SegmentInfos();
		sis.read(directory);
		return sis.version;
	}

	public static Map<String, String> readCurrentUserData(Directory directory)
			throws CorruptIndexException, IOException {
		SegmentInfos sis = new SegmentInfos();
		sis.read(directory);
		return sis.getUserData();
	}

	public static void setInfoStream(PrintStream infoStream) {
		SegmentInfos.infoStream = infoStream;
	}

	private static int defaultGenFileRetryCount = 10;
	private static int defaultGenFileRetryPauseMsec = 50;
	private static int defaultGenLookaheadCount = 10;

	public static void setDefaultGenFileRetryCount(int count) {
		defaultGenFileRetryCount = count;
	}

	public static int getDefaultGenFileRetryCount() {
		return defaultGenFileRetryCount;
	}

	public static void setDefaultGenFileRetryPauseMsec(int msec) {
		defaultGenFileRetryPauseMsec = msec;
	}

	public static int getDefaultGenFileRetryPauseMsec() {
		return defaultGenFileRetryPauseMsec;
	}

	public static void setDefaultGenLookaheadCount(int count) {
		defaultGenLookaheadCount = count;
	}

	public static int getDefaultGenLookahedCount() {
		return defaultGenLookaheadCount;
	}

	public static PrintStream getInfoStream() {
		return infoStream;
	}

	private static void message(String message) {
		infoStream.println("SIS [" + Thread.currentThread().getName() + "]: "
				+ message);
	}

	public abstract static class FindSegmentsFile {

		final Directory directory;

		public FindSegmentsFile(Directory directory) {
			this.directory = directory;
		}

		public Object run() throws CorruptIndexException, IOException {
			return run(null);
		}

		public Object run(IndexCommit commit) throws CorruptIndexException,
				IOException {
			if (commit != null) {
				if (directory != commit.getDirectory())
					throw new IOException(
							"the specified commit does not match the specified Directory");
				return doBody(commit.getSegmentsFileName());
			}

			String segmentFileName = null;
			long lastGen = -1;
			long gen = 0;
			int genLookaheadCount = 0;
			IOException exc = null;
			int retryCount = 0;

			boolean useFirstMethod = true;

			while (true) {

				if (useFirstMethod) {

					String[] files = null;

					long genA = -1;

					files = directory.listAll();

					if (files != null) {
						genA = getCurrentSegmentGeneration(files);
					}

					if (infoStream != null) {
						message("directory listing genA=" + genA);
					}

					long genB = -1;
					for (int i = 0; i < defaultGenFileRetryCount; i++) {
						IndexInput genInput = null;
						try {
							genInput = directory
									.openInput(IndexFileNames.SEGMENTS_GEN);
						} catch (FileNotFoundException e) {
							if (infoStream != null) {
								message("segments.gen open: FileNotFoundException "
										+ e);
							}
							break;
						} catch (IOException e) {
							if (infoStream != null) {
								message("segments.gen open: IOException " + e);
							}
						}

						if (genInput != null) {
							try {
								int version = genInput.readInt();
								if (version == FORMAT_LOCKLESS) {
									long gen0 = genInput.readLong();
									long gen1 = genInput.readLong();
									if (infoStream != null) {
										message("fallback check: " + gen0
												+ "; " + gen1);
									}
									if (gen0 == gen1) {

										genB = gen0;
										break;
									}
								}
							} catch (IOException err2) {

							} finally {
								genInput.close();
							}
						}
						try {
							Thread.sleep(defaultGenFileRetryPauseMsec);
						} catch (InterruptedException ie) {
							throw new ThreadInterruptedException(ie);
						}
					}

					if (infoStream != null) {
						message(IndexFileNames.SEGMENTS_GEN + " check: genB="
								+ genB);
					}

					if (genA > genB)
						gen = genA;
					else
						gen = genB;

					if (gen == -1) {

						throw new IndexNotFoundException(
								"no segments* file found in " + directory
										+ ": files: " + Arrays.toString(files));
					}
				}

				if (useFirstMethod && lastGen == gen && retryCount >= 2) {

					useFirstMethod = false;
				}

				if (!useFirstMethod) {
					if (genLookaheadCount < defaultGenLookaheadCount) {
						gen++;
						genLookaheadCount++;
						if (infoStream != null) {
							message("look ahead increment gen to " + gen);
						}
					} else {

						throw exc;
					}
				} else if (lastGen == gen) {

					retryCount++;
				} else {

					retryCount = 0;
				}

				lastGen = gen;

				segmentFileName = IndexFileNames.fileNameFromGeneration(
						IndexFileNames.SEGMENTS, "", gen);

				try {
					Object v = doBody(segmentFileName);
					if (infoStream != null) {
						message("success on " + segmentFileName);
					}
					return v;
				} catch (IOException err) {

					if (exc == null) {
						exc = err;
					}

					if (infoStream != null) {
						message("primary Exception on '" + segmentFileName
								+ "': " + err + "'; will retry: retryCount="
								+ retryCount + "; gen = " + gen);
					}

					if (gen > 1 && useFirstMethod && retryCount == 1) {

						String prevSegmentFileName = IndexFileNames
								.fileNameFromGeneration(
										IndexFileNames.SEGMENTS, "", gen - 1);

						final boolean prevExists;
						prevExists = directory.fileExists(prevSegmentFileName);

						if (prevExists) {
							if (infoStream != null) {
								message("fallback to prior segment file '"
										+ prevSegmentFileName + "'");
							}
							try {
								Object v = doBody(prevSegmentFileName);
								if (infoStream != null) {
									message("success on fallback "
											+ prevSegmentFileName);
								}
								return v;
							} catch (IOException err2) {
								if (infoStream != null) {
									message("secondary Exception on '"
											+ prevSegmentFileName + "': "
											+ err2 + "'; will retry");
								}
							}
						}
					}
				}
			}
		}

		protected abstract Object doBody(String segmentFileName)
				throws CorruptIndexException, IOException;
	}

	@Deprecated
	public SegmentInfos range(int first, int last) {
		SegmentInfos infos = new SegmentInfos();
		infos.addAll(segments.subList(first, last));
		return infos;
	}

	void updateGeneration(SegmentInfos other) {
		lastGeneration = other.lastGeneration;
		generation = other.generation;
	}

	final void rollbackCommit(Directory dir) throws IOException {
		if (pendingSegnOutput != null) {
			try {
				pendingSegnOutput.close();
			} catch (Throwable t) {

			}

			try {
				final String segmentFileName = IndexFileNames
						.fileNameFromGeneration(IndexFileNames.SEGMENTS, "",
								generation);
				dir.deleteFile(segmentFileName);
			} catch (Throwable t) {

			}
			pendingSegnOutput = null;
		}
	}

	final void prepareCommit(Directory dir) throws IOException {
		if (pendingSegnOutput != null)
			throw new IllegalStateException("prepareCommit was already called");
		write(dir);
	}

	public Collection<String> files(Directory dir, boolean includeSegmentsFile)
			throws IOException {
		HashSet<String> files = new HashSet<String>();
		if (includeSegmentsFile) {
			files.add(getCurrentSegmentFileName());
		}
		final int size = size();
		for (int i = 0; i < size; i++) {
			final SegmentInfo info = info(i);
			if (info.dir == dir) {
				files.addAll(info(i).files());
			}
		}
		return files;
	}

	final void finishCommit(Directory dir) throws IOException {
		if (pendingSegnOutput == null)
			throw new IllegalStateException("prepareCommit was not called");
		boolean success = false;
		try {
			pendingSegnOutput.finishCommit();
			pendingSegnOutput.close();
			pendingSegnOutput = null;
			success = true;
		} finally {
			if (!success)
				rollbackCommit(dir);
		}

		final String fileName = IndexFileNames.fileNameFromGeneration(
				IndexFileNames.SEGMENTS, "", generation);
		success = false;
		try {
			dir.sync(Collections.singleton(fileName));
			success = true;
		} finally {
			if (!success) {
				try {
					dir.deleteFile(fileName);
				} catch (Throwable t) {

				}
			}
		}

		lastGeneration = generation;

		try {
			IndexOutput genOutput = dir
					.createOutput(IndexFileNames.SEGMENTS_GEN);
			try {
				genOutput.writeInt(FORMAT_LOCKLESS);
				genOutput.writeLong(generation);
				genOutput.writeLong(generation);
			} finally {
				genOutput.close();
			}
		} catch (ThreadInterruptedException t) {
			throw t;
		} catch (Throwable t) {

		}
	}

	final void commit(Directory dir) throws IOException {
		prepareCommit(dir);
		finishCommit(dir);
	}

	public String toString(Directory directory) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(getCurrentSegmentFileName()).append(": ");
		final int count = size();
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				buffer.append(' ');
			}
			final SegmentInfo info = info(i);
			buffer.append(info.toString(directory, 0));
		}
		return buffer.toString();
	}

	public Map<String, String> getUserData() {
		return userData;
	}

	void setUserData(Map<String, String> data) {
		if (data == null) {
			userData = Collections.<String, String> emptyMap();
		} else {
			userData = data;
		}
	}

	void replace(SegmentInfos other) {
		rollbackSegmentInfos(other.asList());
		lastGeneration = other.lastGeneration;
	}

	public int totalDocCount() {
		int count = 0;
		for (SegmentInfo info : this) {
			count += info.docCount;
		}
		return count;
	}

	public void changed() {
		version++;
	}

	void applyMergeChanges(MergePolicy.OneMerge merge, boolean dropSegment) {
		final Set<SegmentInfo> mergedAway = new HashSet<SegmentInfo>(
				merge.segments);
		boolean inserted = false;
		int newSegIdx = 0;
		for (int segIdx = 0, cnt = segments.size(); segIdx < cnt; segIdx++) {
			assert segIdx >= newSegIdx;
			final SegmentInfo info = segments.get(segIdx);
			if (mergedAway.contains(info)) {
				if (!inserted && !dropSegment) {
					segments.set(segIdx, merge.info);
					inserted = true;
					newSegIdx++;
				}
			} else {
				segments.set(newSegIdx, info);
				newSegIdx++;
			}
		}

		if (!inserted && !dropSegment) {
			segments.add(0, merge.info);
		}

		segments.subList(newSegIdx, segments.size()).clear();

		if (!dropSegment) {
			segmentSet.add(merge.info);
		}
		segmentSet.removeAll(mergedAway);

		assert segmentSet.size() == segments.size();
	}

	List<SegmentInfo> createBackupSegmentInfos(boolean cloneChildren) {
		if (cloneChildren) {
			final List<SegmentInfo> list = new ArrayList<SegmentInfo>(size());
			for (final SegmentInfo info : this) {
				list.add((SegmentInfo) info.clone());
			}
			return list;
		} else {
			return new ArrayList<SegmentInfo>(segments);
		}
	}

	void rollbackSegmentInfos(List<SegmentInfo> infos) {
		this.clear();
		this.addAll(infos);
	}

	public Iterator<SegmentInfo> iterator() {
		return asList().iterator();
	}

	public List<SegmentInfo> asList() {
		if (cachedUnmodifiableList == null) {
			cachedUnmodifiableList = Collections.unmodifiableList(segments);
		}
		return cachedUnmodifiableList;
	}

	public Set<SegmentInfo> asSet() {
		if (cachedUnmodifiableSet == null) {
			cachedUnmodifiableSet = Collections.unmodifiableSet(segmentSet);
		}
		return cachedUnmodifiableSet;
	}

	public int size() {
		return segments.size();
	}

	public void add(SegmentInfo si) {
		if (segmentSet.contains(si)) {
			throw new IllegalStateException(
					"Cannot add the same segment two times to this SegmentInfos instance");
		}
		segments.add(si);
		segmentSet.add(si);
		assert segmentSet.size() == segments.size();
	}

	public void addAll(Iterable<SegmentInfo> sis) {
		for (final SegmentInfo si : sis) {
			this.add(si);
		}
	}

	public void clear() {
		segments.clear();
		segmentSet.clear();
	}

	public void remove(SegmentInfo si) {
		final int index = this.indexOf(si);
		if (index >= 0) {
			this.remove(index);
		}
	}

	public void remove(int index) {
		segmentSet.remove(segments.remove(index));
		assert segmentSet.size() == segments.size();
	}

	public boolean contains(SegmentInfo si) {
		return segmentSet.contains(si);
	}

	public int indexOf(SegmentInfo si) {
		if (segmentSet.contains(si)) {
			return segments.indexOf(si);
		} else {
			return -1;
		}
	}

}
