/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.Collection;
import java.util.Map;
import java.io.IOException;

import com.shatam.shatamindex.store.Directory;

public abstract class IndexCommit implements Comparable<IndexCommit> {

	public abstract String getSegmentsFileName();

	public abstract Collection<String> getFileNames() throws IOException;

	public abstract Directory getDirectory();

	public abstract void delete();

	public abstract boolean isDeleted();

	public abstract int getSegmentCount();

	@Override
	public boolean equals(Object other) {
		if (other instanceof IndexCommit) {
			IndexCommit otherCommit = (IndexCommit) other;
			return otherCommit.getDirectory().equals(getDirectory())
					&& otherCommit.getVersion() == getVersion();
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return (int) (getDirectory().hashCode() + getVersion());
	}

	public abstract long getVersion();

	public abstract long getGeneration();

	public long getTimestamp() throws IOException {
		return getDirectory().fileModified(getSegmentsFileName());
	}

	public abstract Map<String, String> getUserData() throws IOException;

	public int compareTo(IndexCommit commit) {
		long gen = getGeneration();
		long comgen = commit.getGeneration();
		if (gen < comgen) {
			return -1;
		} else if (gen > comgen) {
			return 1;
		} else {
			return 0;
		}
	}

}
