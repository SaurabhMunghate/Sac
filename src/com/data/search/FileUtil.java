/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.data.search;

import java.io.*;

public class FileUtil {

	public static void makeDir(String path) {
		File folder = new File(path);
		if (!folder.exists())
			folder.mkdirs();
	}

	public static String readAllText(String path) throws IOException {

		File aFile = new File(path);

		StringBuilder contents = new StringBuilder();

		BufferedReader input = new BufferedReader(new FileReader(aFile));
		try {
			String line = null;

			while ((line = input.readLine()) != null) {
				contents.append(line);
				contents.append(System.getProperty("line.separator"));
			}
		} finally {
			input.close();
		}

		return contents.toString();

	}

	public static void writeAllText(String path, String aContents)
			throws FileNotFoundException, IOException {

		File aFile = new File(path);

		Writer output = new BufferedWriter(new FileWriter(aFile));
		try {

			output.write(aContents);
		} finally {
			output.close();
		}
	}

}
