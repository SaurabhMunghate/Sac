/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.List;

public final class KeepOnlyLastCommitDeletionPolicy implements
		IndexDeletionPolicy {

	public void onInit(List<? extends IndexCommit> commits) {

		onCommit(commits);
	}

	public void onCommit(List<? extends IndexCommit> commits) {

		int size = commits.size();
		for (int i = 0; i < size - 1; i++) {
			commits.get(i).delete();
		}
	}
}
