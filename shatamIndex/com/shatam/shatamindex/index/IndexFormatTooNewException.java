/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.DataInput;

public class IndexFormatTooNewException extends CorruptIndexException {

	public IndexFormatTooNewException(String resourceDesc, int version,
			int minVersion, int maxVersion) {
		super("Format version is not supported (resource: " + resourceDesc
				+ "): " + version + " (needs to be between " + minVersion
				+ " and " + maxVersion + ")");
		assert resourceDesc != null;
	}

	public IndexFormatTooNewException(DataInput in, int version,
			int minVersion, int maxVersion) {
		this(in.toString(), version, minVersion, maxVersion);
	}

}
