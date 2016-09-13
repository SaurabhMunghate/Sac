/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.data;

import java.io.FileInputStream;

public class USPSUtil {
	public interface _USPSFileCallback {
		void callback(byte[] data) throws Exception;
	}

	public static final String USPS_ZIP4_DIR = "D:/USPS_20160524/";
	public static final String CITY_STATE_FILE_NAME = USPS_ZIP4_DIR
			+ "ctystate\\ctystate.txt";

	public static void readFile(String f, char recordType, int recSize,
			_USPSFileCallback callback) throws Exception {

		FileInputStream fstream = new FileInputStream(f);

		byte[] data = new byte[recSize];

		while (fstream.read(data) > 0) {
			if (data[0] != recordType)
				continue;

			callback.callback(data);

		}
		fstream.close();

	}
}
