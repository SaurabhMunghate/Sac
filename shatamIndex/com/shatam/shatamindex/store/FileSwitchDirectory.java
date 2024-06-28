/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class FileSwitchDirectory extends Directory {
	private final Directory secondaryDir;
	private final Directory primaryDir;
	private final Set<String> primaryExtensions;
	private boolean doClose;

	public FileSwitchDirectory(Set<String> primaryExtensions,
			Directory primaryDir, Directory secondaryDir, boolean doClose) {
		this.primaryExtensions = primaryExtensions;
		this.primaryDir = primaryDir;
		this.secondaryDir = secondaryDir;
		this.doClose = doClose;
		this.lockFactory = primaryDir.getLockFactory();
	}

	public Directory getPrimaryDir() {
		return primaryDir;
	}

	public Directory getSecondaryDir() {
		return secondaryDir;
	}

	@Override
	public void close() throws IOException {
		if (doClose) {
			try {
				secondaryDir.close();
			} finally {
				primaryDir.close();
			}
			doClose = false;
		}
	}

	@Override
	public String[] listAll() throws IOException {
		Set<String> files = new HashSet<String>();

		NoSuchDirectoryException exc = null;
		try {
			for (String f : primaryDir.listAll()) {
				files.add(f);
			}
		} catch (NoSuchDirectoryException e) {
			exc = e;
		}
		try {
			for (String f : secondaryDir.listAll()) {
				files.add(f);
			}
		} catch (NoSuchDirectoryException e) {

			if (exc != null) {
				throw exc;
			}

			if (files.isEmpty()) {
				throw e;
			}
		}

		if (exc != null && files.isEmpty()) {
			throw exc;
		}
		return files.toArray(new String[files.size()]);
	}

	public static String getExtension(String name) {
		int i = name.lastIndexOf('.');
		if (i == -1) {
			return "";
		}
		return name.substring(i + 1, name.length());
	}

	private Directory getDirectory(String name) {
		String ext = getExtension(name);
		if (primaryExtensions.contains(ext)) {
			return primaryDir;
		} else {
			return secondaryDir;
		}
	}

	@Override
	public boolean fileExists(String name) throws IOException {
		return getDirectory(name).fileExists(name);
	}

	@Override
	public long fileModified(String name) throws IOException {
		return getDirectory(name).fileModified(name);
	}

	@Deprecated
	@Override
	public void touchFile(String name) throws IOException {
		getDirectory(name).touchFile(name);
	}

	@Override
	public void deleteFile(String name) throws IOException {
		getDirectory(name).deleteFile(name);
	}

	@Override
	public long fileLength(String name) throws IOException {
		return getDirectory(name).fileLength(name);
	}

	@Override
	public IndexOutput createOutput(String name) throws IOException {
		return getDirectory(name).createOutput(name);
	}

	@Deprecated
	@Override
	public void sync(String name) throws IOException {
		sync(Collections.singleton(name));
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		List<String> primaryNames = new ArrayList<String>();
		List<String> secondaryNames = new ArrayList<String>();

		for (String name : names)
			if (primaryExtensions.contains(getExtension(name)))
				primaryNames.add(name);
			else
				secondaryNames.add(name);

		primaryDir.sync(primaryNames);
		secondaryDir.sync(secondaryNames);
	}

	@Override
	public IndexInput openInput(String name) throws IOException {
		return getDirectory(name).openInput(name);
	}
}
