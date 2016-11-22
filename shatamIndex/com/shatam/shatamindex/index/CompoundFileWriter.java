/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.IOUtils;

import java.util.LinkedList;
import java.util.HashSet;

import java.io.IOException;

public final class CompoundFileWriter {

	private static final class FileEntry {

		String file;

		long directoryOffset;

		long dataOffset;

		Directory dir;
	}

	static final int FORMAT_PRE_VERSION = 0;

	static final int FORMAT_NO_SEGMENT_PREFIX = -1;

	static final int FORMAT_CURRENT = FORMAT_NO_SEGMENT_PREFIX;

	private Directory directory;
	private String fileName;
	private HashSet<String> ids;
	private LinkedList<FileEntry> entries;
	private boolean merged = false;
	private SegmentMerger.CheckAbort checkAbort;

	public CompoundFileWriter(Directory dir, String name) {
		this(dir, name, null);
	}

	CompoundFileWriter(Directory dir, String name,
			SegmentMerger.CheckAbort checkAbort) {
		if (dir == null)
			throw new NullPointerException("directory cannot be null");
		if (name == null)
			throw new NullPointerException("name cannot be null");
		this.checkAbort = checkAbort;
		directory = dir;
		fileName = name;
		ids = new HashSet<String>();
		entries = new LinkedList<FileEntry>();
	}

	public Directory getDirectory() {
		return directory;
	}

	public String getName() {
		return fileName;
	}

	public void addFile(String file) {
		addFile(file, directory);
	}

	public void addFile(String file, Directory dir) {
		if (merged)
			throw new IllegalStateException(
					"Can't add extensions after merge has been called");

		if (file == null)
			throw new NullPointerException("file cannot be null");

		if (!ids.add(file))
			throw new IllegalArgumentException("File " + file
					+ " already added");

		FileEntry entry = new FileEntry();
		entry.file = file;
		entry.dir = dir;
		entries.add(entry);
	}

	public void close() throws IOException {
		if (merged)
			throw new IllegalStateException("Merge already performed");

		if (entries.isEmpty())
			throw new IllegalStateException(
					"No entries to merge have been defined");

		merged = true;

		IndexOutput os = directory.createOutput(fileName);
		IOException priorException = null;
		try {

			os.writeVInt(FORMAT_CURRENT);

			os.writeVInt(entries.size());

			long totalSize = 0;
			for (FileEntry fe : entries) {
				fe.directoryOffset = os.getFilePointer();
				os.writeLong(0);
				os.writeString(IndexFileNames.stripSegmentName(fe.file));
				totalSize += fe.dir.fileLength(fe.file);
			}

			final long finalLength = totalSize + os.getFilePointer();
			os.setLength(finalLength);

			for (FileEntry fe : entries) {
				fe.dataOffset = os.getFilePointer();
				copyFile(fe, os);
			}

			for (FileEntry fe : entries) {
				os.seek(fe.directoryOffset);
				os.writeLong(fe.dataOffset);
			}

			assert finalLength == os.length();

			IndexOutput tmp = os;
			os = null;
			tmp.close();
		} catch (IOException e) {
			priorException = e;
		} finally {
			IOUtils.closeWhileHandlingException(priorException, os);
		}
	}

	private void copyFile(FileEntry source, IndexOutput os) throws IOException {
		IndexInput is = source.dir.openInput(source.file);
		try {
			long startPtr = os.getFilePointer();
			long length = is.length();
			os.copyBytes(is, length);

			if (checkAbort != null) {
				checkAbort.work(length);
			}

			long endPtr = os.getFilePointer();
			long diff = endPtr - startPtr;
			if (diff != length)
				throw new IOException("Difference in the output file offsets "
						+ diff + " does not match the original file length "
						+ length);

		} finally {
			is.close();
		}
	}
}
