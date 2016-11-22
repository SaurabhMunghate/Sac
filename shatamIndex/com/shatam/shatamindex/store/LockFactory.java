/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;

public abstract class LockFactory {

	protected String lockPrefix = null;

	public void setLockPrefix(String lockPrefix) {
		this.lockPrefix = lockPrefix;
	}

	public String getLockPrefix() {
		return this.lockPrefix;
	}

	public abstract Lock makeLock(String lockName);

	abstract public void clearLock(String lockName) throws IOException;
}
