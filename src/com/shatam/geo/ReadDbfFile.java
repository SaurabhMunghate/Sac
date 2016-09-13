/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.geo;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Observer;

import org.geotools.data.shapefile.dbf.DbaseFileReader;

public class ReadDbfFile {

	public static void iterate(File file, Observer observer) throws Exception {

		FileChannel in = new FileInputStream(file).getChannel();
		DbaseFileReader r = new DbaseFileReader(in, false,
				Charset.defaultCharset());
		while (r.hasNext()) {
			Object[] objRow = r.readEntry();
			observer.update(null, objRow);
		}

		r.close();

	}
}
