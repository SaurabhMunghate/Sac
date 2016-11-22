/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;
import java.util.Map;

public interface TwoPhaseCommit {

	public void prepareCommit() throws IOException;

	public void prepareCommit(Map<String, String> commitData)
			throws IOException;

	public void commit() throws IOException;

	public void commit(Map<String, String> commitData) throws IOException;

	public void rollback() throws IOException;

}
