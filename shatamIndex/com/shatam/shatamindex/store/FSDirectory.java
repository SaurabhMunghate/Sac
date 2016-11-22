/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Collection;
import static java.util.Collections.synchronizedSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import com.shatam.shatamindex.util.Constants;
import com.shatam.shatamindex.util.ThreadInterruptedException;

public abstract class FSDirectory extends Directory {

	public static final int DEFAULT_READ_CHUNK_SIZE = Constants.JRE_IS_64BIT ? Integer.MAX_VALUE
			: 100 * 1024 * 1024;

	protected final File directory;
	protected final Set<String> staleFiles = synchronizedSet(new HashSet<String>());
	private int chunkSize = DEFAULT_READ_CHUNK_SIZE;

	private static File getCanonicalPath(File file) throws IOException {
		return new File(file.getCanonicalPath());
	}

	protected FSDirectory(File path, LockFactory lockFactory)
			throws IOException {

		if (lockFactory == null) {
			lockFactory = new NativeFSLockFactory();
		}
		directory = getCanonicalPath(path);

		if (directory.exists() && !directory.isDirectory())
			throw new NoSuchDirectoryException("file '" + directory
					+ "' exists but is not a directory");

		setLockFactory(lockFactory);
	}

	public static FSDirectory open(File path) throws IOException {
		return open(path, null);
	}

	public static FSDirectory open(File path, LockFactory lockFactory)
			throws IOException {
		if ((Constants.WINDOWS || Constants.SUN_OS || Constants.LINUX)
				&& Constants.JRE_IS_64BIT && MMapDirectory.UNMAP_SUPPORTED) {
			return new MMapDirectory(path, lockFactory);
		} else if (Constants.WINDOWS) {
			return new SimpleFSDirectory(path, lockFactory);
		} else {
			return new NIOFSDirectory(path, lockFactory);
		}
	}

	@Override
	public void setLockFactory(LockFactory lockFactory) throws IOException {
		super.setLockFactory(lockFactory);

		if (lockFactory instanceof FSLockFactory) {
			final FSLockFactory lf = (FSLockFactory) lockFactory;
			final File dir = lf.getLockDir();

			if (dir == null) {
				lf.setLockDir(directory);
				lf.setLockPrefix(null);
			} else if (dir.getCanonicalPath().equals(
					directory.getCanonicalPath())) {
				lf.setLockPrefix(null);
			}
		}

	}

	public static String[] listAll(File dir) throws IOException {
		if (!dir.exists())
			throw new NoSuchDirectoryException("directory '" + dir
					+ "' does not exist");
		else if (!dir.isDirectory())
			throw new NoSuchDirectoryException("file '" + dir
					+ "' exists but is not a directory");

		String[] result = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String file) {
				return !new File(dir, file).isDirectory();
			}
		});

		if (result == null)
			throw new IOException(
					"directory '"
							+ dir
							+ "' exists and is a directory, but cannot be listed: list() returned null");

		return result;
	}

	@Override
	public String[] listAll() throws IOException {
		ensureOpen();
		return listAll(directory);
	}

	@Override
	public boolean fileExists(String name) {
		ensureOpen();
		File file = new File(directory, name);
		return file.exists();
	}

	@Override
	public long fileModified(String name) {
		ensureOpen();
		File file = new File(directory, name);
		return file.lastModified();
	}

	public static long fileModified(File directory, String name) {
		File file = new File(directory, name);
		return file.lastModified();
	}

	@Override
	@Deprecated
	public void touchFile(String name) {
		ensureOpen();
		File file = new File(directory, name);
		file.setLastModified(System.currentTimeMillis());
	}

	@Override
	public long fileLength(String name) throws IOException {
		ensureOpen();
		File file = new File(directory, name);
		final long len = file.length();
		if (len == 0 && !file.exists()) {
			throw new FileNotFoundException(name);
		} else {
			return len;
		}
	}

	@Override
	public void deleteFile(String name) throws IOException {
		ensureOpen();
		File file = new File(directory, name);
		if (!file.delete())
			throw new IOException("Cannot delete " + file);
		staleFiles.remove(name);
	}

	@Override
	public IndexOutput createOutput(String name) throws IOException {
		ensureOpen();

		ensureCanWrite(name);
		return new FSIndexOutput(this, name);
	}

	protected void ensureCanWrite(String name) throws IOException {
		if (!directory.exists())
			if (!directory.mkdirs())
				throw new IOException("Cannot create directory: " + directory);

		File file = new File(directory, name);
		if (file.exists() && !file.delete())
			throw new IOException("Cannot overwrite: " + file);
	}

	protected void onIndexOutputClosed(FSIndexOutput io) {
		staleFiles.add(io.name);
	}

	@Deprecated
	@Override
	public void sync(String name) throws IOException {
		sync(Collections.singleton(name));
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		ensureOpen();
		Set<String> toSync = new HashSet<String>(names);
		toSync.retainAll(staleFiles);

		for (String name : toSync)
			fsync(name);

		staleFiles.removeAll(toSync);
	}

	@Override
	public IndexInput openInput(String name) throws IOException {
		ensureOpen();
		return openInput(name, BufferedIndexInput.BUFFER_SIZE);
	}

	@Override
	public String getLockID() {
		ensureOpen();
		String dirName;
		try {
			dirName = directory.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}

		int digest = 0;
		for (int charIDX = 0; charIDX < dirName.length(); charIDX++) {
			final char ch = dirName.charAt(charIDX);
			digest = 31 * digest + ch;
		}
		return "shatamindex-" + Integer.toHexString(digest);
	}

	@Override
	public synchronized void close() {
		isOpen = false;
	}

	@Deprecated
	public File getFile() {
		return getDirectory();
	}

	public File getDirectory() {
		ensureOpen();
		return directory;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "@" + directory + " lockFactory="
				+ getLockFactory();
	}

	public final void setReadChunkSize(int chunkSize) {

		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize must be positive");
		}
		if (!Constants.JRE_IS_64BIT) {
			this.chunkSize = chunkSize;
		}
	}

	public final int getReadChunkSize() {

		return chunkSize;
	}

	protected static class FSIndexOutput extends BufferedIndexOutput {
		private final FSDirectory parent;
		private final String name;
		private final RandomAccessFile file;
		private volatile boolean isOpen;

		public FSIndexOutput(FSDirectory parent, String name)
				throws IOException {
			this.parent = parent;
			this.name = name;
			file = new RandomAccessFile(new File(parent.directory, name), "rw");
			isOpen = true;
		}

		@Override
		public void flushBuffer(byte[] b, int offset, int size)
				throws IOException {
			file.write(b, offset, size);
		}

		@Override
		public void close() throws IOException {
			parent.onIndexOutputClosed(this);

			if (isOpen) {
				boolean success = false;
				try {
					super.close();
					success = true;
				} finally {
					isOpen = false;
					if (!success) {
						try {
							file.close();
						} catch (Throwable t) {

						}
					} else {
						file.close();
					}
				}
			}
		}

		@Override
		public void seek(long pos) throws IOException {
			super.seek(pos);
			file.seek(pos);
		}

		@Override
		public long length() throws IOException {
			return file.length();
		}

		@Override
		public void setLength(long length) throws IOException {
			file.setLength(length);
		}
	}

	protected void fsync(String name) throws IOException {
		File fullFile = new File(directory, name);
		boolean success = false;
		int retryCount = 0;
		IOException exc = null;
		while (!success && retryCount < 5) {
			retryCount++;
			RandomAccessFile file = null;
			try {
				try {
					file = new RandomAccessFile(fullFile, "rw");
					file.getFD().sync();
					success = true;
				} finally {
					if (file != null)
						file.close();
				}
			} catch (IOException ioe) {
				if (exc == null)
					exc = ioe;
				try {

					Thread.sleep(5);
				} catch (InterruptedException ie) {
					throw new ThreadInterruptedException(ie);
				}
			}
		}
		if (!success)

			throw exc;
	}
}
