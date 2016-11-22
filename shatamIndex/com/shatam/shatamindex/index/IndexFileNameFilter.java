/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;

public class IndexFileNameFilter implements FilenameFilter {

	private static IndexFileNameFilter singleton = new IndexFileNameFilter();
	private HashSet<String> extensions;
	private HashSet<String> extensionsInCFS;

	private IndexFileNameFilter() {
		extensions = new HashSet<String>();
		for (String ext : IndexFileNames.INDEX_EXTENSIONS) {
			extensions.add(ext);
		}
		extensionsInCFS = new HashSet<String>();
		for (String ext : IndexFileNames.INDEX_EXTENSIONS_IN_COMPOUND_FILE) {
			extensionsInCFS.add(ext);
		}
	}

	public boolean accept(File dir, String name) {
		int i = name.lastIndexOf('.');
		if (i != -1) {
			String extension = name.substring(1 + i);
			if (extensions.contains(extension)) {
				return true;
			} else if (extension.startsWith("f") && extension.matches("f\\d+")) {
				return true;
			} else if (extension.startsWith("s") && extension.matches("s\\d+")) {
				return true;
			}
		} else {
			if (name.equals(IndexFileNames.DELETABLE))
				return true;
			else if (name.startsWith(IndexFileNames.SEGMENTS))
				return true;
		}
		return false;
	}

	public boolean isCFSFile(String name) {
		int i = name.lastIndexOf('.');
		if (i != -1) {
			String extension = name.substring(1 + i);
			if (extensionsInCFS.contains(extension)) {
				return true;
			}
			if (extension.startsWith("f") && extension.matches("f\\d+")) {
				return true;
			}
		}
		return false;
	}

	public static IndexFileNameFilter getFilter() {
		return singleton;
	}
}
