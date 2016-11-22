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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.NoSuchDirectoryException;
import com.shatam.shatamindex.util.CollectionUtil;

final class IndexFileDeleter {

	private List<String> deletable;

	private Map<String, RefCount> refCounts = new HashMap<String, RefCount>();

	private List<CommitPoint> commits = new ArrayList<CommitPoint>();

	private List<Collection<String>> lastFiles = new ArrayList<Collection<String>>();

	private List<CommitPoint> commitsToDelete = new ArrayList<CommitPoint>();

	private PrintStream infoStream;
	private Directory directory;
	private IndexDeletionPolicy policy;

	final boolean startingCommitDeleted;
	private SegmentInfos lastSegmentInfos;

	public static boolean VERBOSE_REF_COUNTS = false;

	private final IndexWriter writer;

	void setInfoStream(PrintStream infoStream) {
		this.infoStream = infoStream;
		if (infoStream != null) {
			message("setInfoStream deletionPolicy=" + policy);
		}
	}

	private void message(String message) {
		infoStream.println("IFD [" + new Date() + "; "
				+ Thread.currentThread().getName() + "]: " + message);
	}

	private boolean locked() {
		return writer == null || Thread.holdsLock(writer);
	}

	public IndexFileDeleter(Directory directory, IndexDeletionPolicy policy,
			SegmentInfos segmentInfos, PrintStream infoStream,
			IndexWriter writer) throws CorruptIndexException, IOException {

		this.infoStream = infoStream;
		this.writer = writer;

		final String currentSegmentsFile = segmentInfos
				.getCurrentSegmentFileName();

		if (infoStream != null) {
			message("init: current segments file is \"" + currentSegmentsFile
					+ "\"; deletionPolicy=" + policy);
		}

		this.policy = policy;
		this.directory = directory;

		long currentGen = segmentInfos.getGeneration();
		IndexFileNameFilter filter = IndexFileNameFilter.getFilter();

		CommitPoint currentCommitPoint = null;
		String[] files = null;
		try {
			files = directory.listAll();
		} catch (NoSuchDirectoryException e) {

			files = new String[0];
		}

		for (String fileName : files) {

			if (filter.accept(null, fileName)
					&& !fileName.equals(IndexFileNames.SEGMENTS_GEN)) {

				getRefCount(fileName);

				if (fileName.startsWith(IndexFileNames.SEGMENTS)) {

					if (infoStream != null) {
						message("init: load commit \"" + fileName + "\"");
					}
					SegmentInfos sis = new SegmentInfos();
					try {
						sis.read(directory, fileName);
					} catch (FileNotFoundException e) {

						if (infoStream != null) {
							message("init: hit FileNotFoundException when loading commit \""
									+ fileName
									+ "\"; skipping this commit point");
						}
						sis = null;
					} catch (IOException e) {
						if (SegmentInfos
								.generationFromSegmentsFileName(fileName) <= currentGen) {
							throw e;
						} else {

							sis = null;
						}
					}
					if (sis != null) {
						CommitPoint commitPoint = new CommitPoint(
								commitsToDelete, directory, sis);
						if (sis.getGeneration() == segmentInfos.getGeneration()) {
							currentCommitPoint = commitPoint;
						}
						commits.add(commitPoint);
						incRef(sis, true);

						if (lastSegmentInfos == null
								|| sis.getGeneration() > lastSegmentInfos
										.getGeneration()) {
							lastSegmentInfos = sis;
						}
					}
				}
			}
		}

		if (currentCommitPoint == null && currentSegmentsFile != null) {

			SegmentInfos sis = new SegmentInfos();
			try {
				sis.read(directory, currentSegmentsFile);
			} catch (IOException e) {
				throw new CorruptIndexException(
						"failed to locate current segments_N file");
			}
			if (infoStream != null) {
				message("forced open of current segments file "
						+ segmentInfos.getCurrentSegmentFileName());
			}
			currentCommitPoint = new CommitPoint(commitsToDelete, directory,
					sis);
			commits.add(currentCommitPoint);
			incRef(sis, true);
		}

		CollectionUtil.mergeSort(commits);

		for (Map.Entry<String, RefCount> entry : refCounts.entrySet()) {
			RefCount rc = entry.getValue();
			final String fileName = entry.getKey();
			if (0 == rc.count) {
				if (infoStream != null) {
					message("init: removing unreferenced file \"" + fileName
							+ "\"");
				}
				deleteFile(fileName);
			}
		}

		if (currentSegmentsFile != null) {
			policy.onInit(commits);
		}

		checkpoint(segmentInfos, false);

		startingCommitDeleted = currentCommitPoint == null ? false
				: currentCommitPoint.isDeleted();

		deleteCommits();
	}

	public SegmentInfos getLastSegmentInfos() {
		return lastSegmentInfos;
	}

	private void deleteCommits() throws IOException {

		int size = commitsToDelete.size();

		if (size > 0) {

			for (int i = 0; i < size; i++) {
				CommitPoint commit = commitsToDelete.get(i);
				if (infoStream != null) {
					message("deleteCommits: now decRef commit \""
							+ commit.getSegmentsFileName() + "\"");
				}
				for (final String file : commit.files) {
					decRef(file);
				}
			}
			commitsToDelete.clear();

			size = commits.size();
			int readFrom = 0;
			int writeTo = 0;
			while (readFrom < size) {
				CommitPoint commit = commits.get(readFrom);
				if (!commit.deleted) {
					if (writeTo != readFrom) {
						commits.set(writeTo, commits.get(readFrom));
					}
					writeTo++;
				}
				readFrom++;
			}

			while (size > writeTo) {
				commits.remove(size - 1);
				size--;
			}
		}
	}

	public void refresh(String segmentName) throws IOException {
		assert locked();

		String[] files = directory.listAll();
		IndexFileNameFilter filter = IndexFileNameFilter.getFilter();
		String segmentPrefix1;
		String segmentPrefix2;
		if (segmentName != null) {
			segmentPrefix1 = segmentName + ".";
			segmentPrefix2 = segmentName + "_";
		} else {
			segmentPrefix1 = null;
			segmentPrefix2 = null;
		}

		for (int i = 0; i < files.length; i++) {
			String fileName = files[i];
			if (filter.accept(null, fileName)
					&& (segmentName == null
							|| fileName.startsWith(segmentPrefix1) || fileName
								.startsWith(segmentPrefix2))
					&& !refCounts.containsKey(fileName)
					&& !fileName.equals(IndexFileNames.SEGMENTS_GEN)) {

				if (infoStream != null) {
					message("refresh [prefix=" + segmentName
							+ "]: removing newly created unreferenced file \""
							+ fileName + "\"");
				}
				deleteFile(fileName);
			}
		}
	}

	public void refresh() throws IOException {

		assert locked();
		deletable = null;
		refresh(null);
	}

	public void close() throws IOException {

		assert locked();
		int size = lastFiles.size();
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				decRef(lastFiles.get(i));
			}
			lastFiles.clear();
		}

		deletePendingFiles();
	}

	void revisitPolicy() throws IOException {
		assert locked();
		if (infoStream != null) {
			message("now revisitPolicy");
		}

		if (commits.size() > 0) {
			policy.onCommit(commits);
			deleteCommits();
		}
	}

	public void deletePendingFiles() throws IOException {
		assert locked();
		if (deletable != null) {
			List<String> oldDeletable = deletable;
			deletable = null;
			int size = oldDeletable.size();
			for (int i = 0; i < size; i++) {
				if (infoStream != null) {
					message("delete pending file " + oldDeletable.get(i));
				}
				deleteFile(oldDeletable.get(i));
			}
		}
	}

	public void checkpoint(SegmentInfos segmentInfos, boolean isCommit)
			throws IOException {
		assert locked();

		if (infoStream != null) {
			message("now checkpoint \""
					+ segmentInfos.getCurrentSegmentFileName() + "\" ["
					+ segmentInfos.size() + " segments " + "; isCommit = "
					+ isCommit + "]");
		}

		deletePendingFiles();

		incRef(segmentInfos, isCommit);

		if (isCommit) {

			commits.add(new CommitPoint(commitsToDelete, directory,
					segmentInfos));

			policy.onCommit(commits);

			deleteCommits();
		} else {

			for (Collection<String> lastFile : lastFiles) {
				decRef(lastFile);
			}
			lastFiles.clear();

			lastFiles.add(segmentInfos.files(directory, false));
		}
	}

	void incRef(SegmentInfos segmentInfos, boolean isCommit) throws IOException {
		assert locked();

		for (final String fileName : segmentInfos.files(directory, isCommit)) {
			incRef(fileName);
		}
	}

	void incRef(Collection<String> files) throws IOException {
		assert locked();
		for (final String file : files) {
			incRef(file);
		}
	}

	void incRef(String fileName) throws IOException {
		assert locked();
		RefCount rc = getRefCount(fileName);
		if (infoStream != null && VERBOSE_REF_COUNTS) {
			message("  IncRef \"" + fileName + "\": pre-incr count is "
					+ rc.count);
		}
		rc.IncRef();
	}

	void decRef(Collection<String> files) throws IOException {
		assert locked();
		for (final String file : files) {
			decRef(file);
		}
	}

	void decRef(String fileName) throws IOException {
		assert locked();
		RefCount rc = getRefCount(fileName);
		if (infoStream != null && VERBOSE_REF_COUNTS) {
			message("  DecRef \"" + fileName + "\": pre-decr count is "
					+ rc.count);
		}
		if (0 == rc.DecRef()) {

			deleteFile(fileName);
			refCounts.remove(fileName);
		}
	}

	void decRef(SegmentInfos segmentInfos) throws IOException {
		assert locked();
		for (final String file : segmentInfos.files(directory, false)) {
			decRef(file);
		}
	}

	public boolean exists(String fileName) {
		assert locked();
		if (!refCounts.containsKey(fileName)) {
			return false;
		} else {
			return getRefCount(fileName).count > 0;
		}
	}

	private RefCount getRefCount(String fileName) {
		assert locked();
		RefCount rc;
		if (!refCounts.containsKey(fileName)) {
			rc = new RefCount(fileName);
			refCounts.put(fileName, rc);
		} else {
			rc = refCounts.get(fileName);
		}
		return rc;
	}

	void deleteFiles(List<String> files) throws IOException {
		assert locked();
		for (final String file : files) {
			deleteFile(file);
		}
	}

	void deleteNewFiles(Collection<String> files) throws IOException {
		assert locked();
		for (final String fileName : files) {
			if (!refCounts.containsKey(fileName)) {
				if (infoStream != null) {
					message("delete new file \"" + fileName + "\"");
				}
				deleteFile(fileName);
			}
		}
	}

	void deleteFile(String fileName) throws IOException {
		assert locked();
		try {
			if (infoStream != null) {
				message("delete \"" + fileName + "\"");
			}
			directory.deleteFile(fileName);
		} catch (IOException e) {
			if (directory.fileExists(fileName)) {

				if (infoStream != null) {
					message("unable to remove file \"" + fileName + "\": "
							+ e.toString() + "; Will re-try later.");
				}
				if (deletable == null) {
					deletable = new ArrayList<String>();
				}
				deletable.add(fileName);
			}
		}
	}

	final private static class RefCount {

		final String fileName;
		boolean initDone;

		RefCount(String fileName) {
			this.fileName = fileName;
		}

		int count;

		public int IncRef() {
			if (!initDone) {
				initDone = true;
			} else {
				assert count > 0 : Thread.currentThread().getName()
						+ ": RefCount is 0 pre-increment for file \""
						+ fileName + "\"";
			}
			return ++count;
		}

		public int DecRef() {
			assert count > 0 : Thread.currentThread().getName()
					+ ": RefCount is 0 pre-decrement for file \"" + fileName
					+ "\"";
			return --count;
		}
	}

	final private static class CommitPoint extends IndexCommit {

		Collection<String> files;
		String segmentsFileName;
		boolean deleted;
		Directory directory;
		Collection<CommitPoint> commitsToDelete;
		long version;
		long generation;
		final Map<String, String> userData;
		private final int segmentCount;

		public CommitPoint(Collection<CommitPoint> commitsToDelete,
				Directory directory, SegmentInfos segmentInfos)
				throws IOException {
			this.directory = directory;
			this.commitsToDelete = commitsToDelete;
			userData = segmentInfos.getUserData();
			segmentsFileName = segmentInfos.getCurrentSegmentFileName();
			version = segmentInfos.getVersion();
			generation = segmentInfos.getGeneration();
			files = Collections.unmodifiableCollection(segmentInfos.files(
					directory, true));
			segmentCount = segmentInfos.size();
		}

		@Override
		public String toString() {
			return "IndexFileDeleter.CommitPoint(" + segmentsFileName + ")";
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
		public Collection<String> getFileNames() throws IOException {
			return files;
		}

		@Override
		public Directory getDirectory() {
			return directory;
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
		public Map<String, String> getUserData() {
			return userData;
		}

		@Override
		public void delete() {
			if (!deleted) {
				deleted = true;
				commitsToDelete.add(this);
			}
		}

		@Override
		public boolean isDeleted() {
			return deleted;
		}
	}
}
