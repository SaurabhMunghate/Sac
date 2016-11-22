/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.File;
import java.io.IOException;

public class SimpleFSLockFactory extends FSLockFactory {

	public SimpleFSLockFactory() throws IOException {
		this((File) null);
	}

	public SimpleFSLockFactory(File lockDir) throws IOException {
		setLockDir(lockDir);
	}

	public SimpleFSLockFactory(String lockDirName) throws IOException {
		setLockDir(new File(lockDirName));
	}

	@Override
	public Lock makeLock(String lockName) {
		if (lockPrefix != null) {
			lockName = lockPrefix + "-" + lockName;
		}
		return new SimpleFSLock(lockDir, lockName);
	}

	@Override
	public void clearLock(String lockName) throws IOException {
		if (lockDir.exists()) {
			if (lockPrefix != null) {
				lockName = lockPrefix + "-" + lockName;
			}
			File lockFile = new File(lockDir, lockName);
			if (lockFile.exists() && !lockFile.delete()) {
				throw new IOException("Cannot delete " + lockFile);
			}
		}
	}
}

class SimpleFSLock extends Lock {

	File lockFile;
	File lockDir;

	public SimpleFSLock(File lockDir, String lockFileName) {
		this.lockDir = lockDir;
		lockFile = new File(lockDir, lockFileName);
	}

	@Override
	public boolean obtain() throws IOException {

		if (!lockDir.exists()) {
			if (!lockDir.mkdirs())
				throw new IOException("Cannot create directory: "
						+ lockDir.getAbsolutePath());
		} else if (!lockDir.isDirectory()) {
			throw new IOException(
					"Found regular file where directory expected: "
							+ lockDir.getAbsolutePath());
		}
		return lockFile.createNewFile();
	}

	@Override
	public void release() throws LockReleaseFailedException {
		if (lockFile.exists() && !lockFile.delete())
			throw new LockReleaseFailedException("failed to delete " + lockFile);
	}

	@Override
	public boolean isLocked() {
		return lockFile.exists();
	}

	@Override
	public String toString() {
		return "SimpleFSLock@" + lockFile;
	}
}
