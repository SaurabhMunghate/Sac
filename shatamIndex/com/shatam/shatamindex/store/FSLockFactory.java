/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.File;

public abstract class FSLockFactory extends LockFactory {

	protected File lockDir = null;

	protected final void setLockDir(File lockDir) {
		if (this.lockDir != null)
			throw new IllegalStateException(
					"You can set the lock directory for this factory only once.");
		this.lockDir = lockDir;
	}

	public File getLockDir() {
		return lockDir;
	}

}
