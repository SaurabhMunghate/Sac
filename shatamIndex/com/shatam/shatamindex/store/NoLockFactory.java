/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;

public class NoLockFactory extends LockFactory {

	private static NoLock singletonLock = new NoLock();
	private static NoLockFactory singleton = new NoLockFactory();

	@Deprecated
	public NoLockFactory() {
	}

	public static NoLockFactory getNoLockFactory() {
		return singleton;
	}

	@Override
	public Lock makeLock(String lockName) {
		return singletonLock;
	}

	@Override
	public void clearLock(String lockName) {
	}
}

class NoLock extends Lock {
	@Override
	public boolean obtain() throws IOException {
		return true;
	}

	@Override
	public void release() {
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	@Override
	public String toString() {
		return "NoLock";
	}
}
