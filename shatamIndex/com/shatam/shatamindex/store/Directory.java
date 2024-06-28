/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Closeable;
import java.util.Collection;

import com.shatam.shatamindex.index.IndexFileNameFilter;
import com.shatam.shatamindex.util.IOUtils;

public abstract class Directory implements Closeable {

	volatile protected boolean isOpen = true;

	protected LockFactory lockFactory;

	public abstract String[] listAll() throws IOException;

	public abstract boolean fileExists(String name) throws IOException;

	public abstract long fileModified(String name) throws IOException;

	@Deprecated
	public abstract void touchFile(String name) throws IOException;

	public abstract void deleteFile(String name) throws IOException;

	public abstract long fileLength(String name) throws IOException;

	public abstract IndexOutput createOutput(String name) throws IOException;

	@Deprecated
	public void sync(String name) throws IOException {
	}

	public void sync(Collection<String> names) throws IOException {
		for (String name : names)
			sync(name);
	}

	public abstract IndexInput openInput(String name) throws IOException;

	public IndexInput openInput(String name, int bufferSize) throws IOException {
		return openInput(name);
	}

	public Lock makeLock(String name) {
		return lockFactory.makeLock(name);
	}

	public void clearLock(String name) throws IOException {
		if (lockFactory != null) {
			lockFactory.clearLock(name);
		}
	}

	public abstract void close() throws IOException;

	public void setLockFactory(LockFactory lockFactory) throws IOException {
		assert lockFactory != null;
		this.lockFactory = lockFactory;
		lockFactory.setLockPrefix(this.getLockID());
	}

	public LockFactory getLockFactory() {
		return this.lockFactory;
	}

	public String getLockID() {
		return this.toString();
	}

	@Override
	public String toString() {
		return super.toString() + " lockFactory=" + getLockFactory();
	}

	public void copy(Directory to, String src, String dest) throws IOException {
		IndexOutput os = null;
		IndexInput is = null;
		IOException priorException = null;
		try {
			os = to.createOutput(dest);
			is = openInput(src);
			is.copyBytes(os, is.length());
		} catch (IOException ioe) {
			priorException = ioe;
		} finally {
			IOUtils.closeWhileHandlingException(priorException, os, is);
		}
	}

	@Deprecated
	public static void copy(Directory src, Directory dest, boolean closeDirSrc)
			throws IOException {
		IndexFileNameFilter filter = IndexFileNameFilter.getFilter();
		for (String file : src.listAll()) {
			if (filter.accept(null, file)) {
				src.copy(dest, file, file);
			}
		}
		if (closeDirSrc) {
			src.close();
		}
	}

	protected final void ensureOpen() throws AlreadyClosedException {
		if (!isOpen)
			throw new AlreadyClosedException("this Directory is closed");
	}
}
