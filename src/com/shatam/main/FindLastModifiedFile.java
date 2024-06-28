/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

public class FindLastModifiedFile {
	public static void main(String[] args) throws UnknownHostException {
		File dir = new File("C:\\SAC\\LOG\\");
		File[] files = dir.listFiles();
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		System.out.println("**************" + files[0]);
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			System.out.printf("File %s - %2$tm %2$te,%2$tY%n= ",
					file.getName(), file.lastModified());
		}
	}
}
