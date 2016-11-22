/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.shatam.shatamindex.index.ConcurrentMergeScheduler;
import com.shatam.shatamindex.index.IndexFileNames;
import com.shatam.shatamindex.index.IndexWriter;
import com.shatam.shatamindex.index.MergePolicy;
import com.shatam.shatamindex.index.MergeScheduler;
import com.shatam.shatamindex.store.RAMDirectory;
import com.shatam.shatamindex.util.IOUtils;

public class NRTCachingDirectory extends Directory {

	private final RAMDirectory cache = new RAMDirectory();

	private final Directory delegate;

	private final long maxMergeSizeBytes;
	private final long maxCachedBytes;

	private static final boolean VERBOSE = false;

	public NRTCachingDirectory(Directory delegate, double maxMergeSizeMB,
			double maxCachedMB) {
		this.delegate = delegate;
		maxMergeSizeBytes = (long) (maxMergeSizeMB * 1024 * 1024);
		maxCachedBytes = (long) (maxCachedMB * 1024 * 1024);
	}

	@Override
	public LockFactory getLockFactory() {
		return delegate.getLockFactory();
	}

	@Override
	public void setLockFactory(LockFactory lf) throws IOException {
		delegate.setLockFactory(lf);
	}

	@Override
	public String getLockID() {
		return delegate.getLockID();
	}

	@Override
	public Lock makeLock(String name) {
		return delegate.makeLock(name);
	}

	@Override
	public void clearLock(String name) throws IOException {
		delegate.clearLock(name);
	}

	@Override
	public String toString() {
		return "NRTCachingDirectory(" + delegate + "; maxCacheMB="
				+ (maxCachedBytes / 1024 / 1024.) + " maxMergeSizeMB="
				+ (maxMergeSizeBytes / 1024 / 1024.) + ")";
	}

	@Override
	public synchronized String[] listAll() throws IOException {
		final Set<String> files = new HashSet<String>();
		for (String f : cache.listAll()) {
			files.add(f);
		}

		try {
			for (String f : delegate.listAll()) {

				files.add(f);
			}
		} catch (NoSuchDirectoryException ex) {

			if (files.isEmpty()) {
				throw ex;
			}
		}
		return files.toArray(new String[files.size()]);
	}

	public long sizeInBytes() {
		return cache.sizeInBytes();
	}

	@Override
	public synchronized boolean fileExists(String name) throws IOException {
		return cache.fileExists(name) || delegate.fileExists(name);
	}

	@Override
	public synchronized long fileModified(String name) throws IOException {
		if (cache.fileExists(name)) {
			return cache.fileModified(name);
		} else {
			return delegate.fileModified(name);
		}
	}

	@Override
	@Deprecated
	public synchronized void touchFile(String name) throws IOException {
		if (cache.fileExists(name)) {
			cache.touchFile(name);
		} else {
			delegate.touchFile(name);
		}
	}

	@Override
	public synchronized void deleteFile(String name) throws IOException {
		if (VERBOSE) {
			System.out.println("nrtdir.deleteFile name=" + name);
		}
		if (cache.fileExists(name)) {
			assert !delegate.fileExists(name);
			cache.deleteFile(name);
		} else {
			delegate.deleteFile(name);
		}
	}

	@Override
	public synchronized long fileLength(String name) throws IOException {
		if (cache.fileExists(name)) {
			return cache.fileLength(name);
		} else {
			return delegate.fileLength(name);
		}
	}

	public String[] listCachedFiles() {
		return cache.listAll();
	}

	@Override
	public IndexOutput createOutput(String name) throws IOException {
		if (VERBOSE) {
			System.out.println("nrtdir.createOutput name=" + name);
		}
		if (doCacheWrite(name)) {
			if (VERBOSE) {
				System.out.println("  to cache");
			}
			return cache.createOutput(name);
		} else {
			return delegate.createOutput(name);
		}
	}

	@Override
	public void sync(Collection<String> fileNames) throws IOException {
		if (VERBOSE) {
			System.out.println("nrtdir.sync files=" + fileNames);
		}
		for (String fileName : fileNames) {
			unCache(fileName);
		}
		delegate.sync(fileNames);
	}

	@Override
	public synchronized IndexInput openInput(String name) throws IOException {
		if (VERBOSE) {
			System.out.println("nrtdir.openInput name=" + name);
		}
		if (cache.fileExists(name)) {
			if (VERBOSE) {
				System.out.println("  from cache");
			}
			return cache.openInput(name);
		} else {
			return delegate.openInput(name);
		}
	}

	@Override
	public synchronized IndexInput openInput(String name, int bufferSize)
			throws IOException {
		if (cache.fileExists(name)) {
			return cache.openInput(name, bufferSize);
		} else {
			return delegate.openInput(name, bufferSize);
		}
	}

	@Override
	public void close() throws IOException {
		for (String fileName : cache.listAll()) {
			unCache(fileName);
		}
		cache.close();
		delegate.close();
	}

	private final ConcurrentHashMap<Thread, MergePolicy.OneMerge> merges = new ConcurrentHashMap<Thread, MergePolicy.OneMerge>();

	public MergeScheduler getMergeScheduler() {
		return new ConcurrentMergeScheduler() {
			@Override
			protected void doMerge(MergePolicy.OneMerge merge)
					throws IOException {
				try {
					merges.put(Thread.currentThread(), merge);
					super.doMerge(merge);
				} finally {
					merges.remove(Thread.currentThread());
				}
			}
		};
	}

	protected boolean doCacheWrite(String name) {
		final MergePolicy.OneMerge merge = merges.get(Thread.currentThread());

		return !name.equals(IndexFileNames.SEGMENTS_GEN)
				&& (merge == null || merge.estimatedMergeBytes <= maxMergeSizeBytes)
				&& cache.sizeInBytes() <= maxCachedBytes;
	}

	private void unCache(String fileName) throws IOException {
		final IndexOutput out;
		synchronized (this) {
			if (!delegate.fileExists(fileName)) {
				assert cache.fileExists(fileName);
				out = delegate.createOutput(fileName);
			} else {
				out = null;
			}
		}

		if (out != null) {
			IndexInput in = null;
			try {
				in = cache.openInput(fileName);
				in.copyBytes(out, in.length());
			} finally {
				IOUtils.close(in, out);
			}
			synchronized (this) {
				cache.deleteFile(fileName);
			}
		}
	}
}
