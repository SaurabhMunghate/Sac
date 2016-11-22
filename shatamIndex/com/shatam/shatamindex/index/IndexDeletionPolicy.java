/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.List;
import java.io.IOException;

public interface IndexDeletionPolicy {

	public void onInit(List<? extends IndexCommit> commits) throws IOException;

	public void onCommit(List<? extends IndexCommit> commits)
			throws IOException;
}
