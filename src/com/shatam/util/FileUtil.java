/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Observer;

public class FileUtil {

	public static File downloadFile(String url, String dir, String file)
			throws Exception {
		if (!new File(dir).exists()) {
			new File(dir).mkdirs();
		}

		File fileZip = new File(dir, file);

		if (fileZip.exists()) {
			return fileZip;
		}
		U.log("DIR::::" + dir);
		InputStream urlInputStream = U.getInputStream(url);
		U.log(urlInputStream);
		java.io.FileOutputStream fos = new java.io.FileOutputStream(fileZip);

		U.redirect(urlInputStream, fos);

		return fileZip;
	}

	public static String readTextFromTile(String path) throws IOException {

		StringBuffer list = new StringBuffer();

		FileInputStream fstream = new FileInputStream(path);

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			strLine = strLine.trim().toLowerCase();
			if (strLine.length() == 0)
				continue;

			list.append(strLine).append("\n");
		}

		in.close();

		return list.toString();
	}

	public static ArrayList<String> readLines(String path) throws IOException {

		ArrayList<String> list = new ArrayList<String>();

		FileInputStream fstream = new FileInputStream(path);

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			strLine = strLine.trim();
			if (strLine.length() == 0)
				continue;

			list.add(strLine);
		}

		in.close();

		return list;
	}

	public static ArrayList<String> readLinesNoTrim(String path)
			throws IOException {

		ArrayList<String> list = new ArrayList<String>();

		FileInputStream fstream = new FileInputStream(path);

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			if (strLine.length() == 0)
				continue;

			list.add(strLine);
		}

		in.close();

		return list;
	}

	public static void readLines(String path, Observer callback)
			throws IOException {

		FileInputStream fstream = new FileInputStream(path);

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			if (strLine.trim().length() == 0)
				continue;

			callback.update(null, strLine);
		}

		in.close();

	}

	public static boolean deleteDir(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDir(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	public static File saveZipAndGetDbf(String fipsCode, String type)
			throws Exception {

		String URL_PREFIX = "http://www2.census.gov/geo/tiger/TIGER2010/";

		String stateNumber = fipsCode.substring(0, 2);
		String DATA_DIR = Paths.combine(Paths.DATA_ROOT, type.toLowerCase(),
				stateNumber);

		String zipName = "tl_2010_" + fipsCode + "_" + type.toLowerCase()
				+ ".zip";
		String google = (URL_PREFIX + type.toUpperCase() + "/" + zipName);

		U.log("Downloading '" + google + "' in " + DATA_DIR);
		File fileZip = FileUtil.downloadFile(google, DATA_DIR, zipName);
		U.log("Done download " + fileZip);

		ArrayList<File> dbfFiles = UnZip.extract(fileZip, "dbf");

		U.log("Done extract " + dbfFiles.get(0));

		return dbfFiles.get(0);

	}

	public static ArrayList<File> saveZipAndExtractAll(String fipsCode,
			String type) throws Exception {
		U.log("*************-----saveZipAndExtractAll-----*************");
		String URL_PREFIX = "http://www2.census.gov/geo/tiger/TIGER2010/";

		String stateNumber = fipsCode.substring(0, 2);
		String DATA_DIR = Paths.combine(Paths.DATA_ROOT, type.toLowerCase(),
				stateNumber);

		String zipName = "tl_2010_" + fipsCode + "_" + type.toLowerCase()
				+ ".zip";
		String google = (URL_PREFIX + type.toUpperCase() + "/" + zipName);

		File fileZip = FileUtil.downloadFile(google, DATA_DIR, zipName);
		U.log("Done download " + fileZip);

		ArrayList<File> dbfFiles = UnZip.extract(fileZip, null);

		U.log("Done extract " + dbfFiles.get(0));

		return dbfFiles;

	}

	public static File downloadZipFromCensus(String fipsCode, String type)
			throws Exception {

		String URL_PREFIX = "http://www2.census.gov/geo/tiger/TIGER2010/";

		String stateNumber = fipsCode.substring(0, 2);
		String DATA_DIR = Paths.combine(Paths.DATA_ROOT, type.toLowerCase(),
				stateNumber);

		String zipName = "tl_2010_" + fipsCode + "_" + type.toLowerCase()
				+ ".zip";
		String google = (URL_PREFIX + type.toUpperCase() + "/" + zipName);

		File fileZip = FileUtil.downloadFile(google, DATA_DIR, zipName);
		U.log("Done download " + fileZip);

		return fileZip;
	}

}
