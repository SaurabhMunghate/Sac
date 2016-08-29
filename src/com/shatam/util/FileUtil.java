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
		U.log("DIR::::"+dir);
		InputStream urlInputStream = U.getInputStream(url);
		U.log(urlInputStream);
		java.io.FileOutputStream fos = new java.io.FileOutputStream(fileZip);

		U.redirect(urlInputStream, fos);
		/*
		 * int MAX_BYTES = 100; java.io.BufferedInputStream in = new
		 * java.io.BufferedInputStream(urlInputStream);
		 * java.io.BufferedOutputStream bout = new BufferedOutputStream(fos,
		 * 1024); byte data[] = new byte[MAX_BYTES]; while (in.read(data, 0,
		 * MAX_BYTES) >= 0) { bout.write(data); } bout.close(); in.close();
		 */
		/*
		 * URL google = new URL(url);
		 * 
		 * ReadableByteChannel rbc = Channels.newChannel(google.openStream());
		 * FileOutputStream fos = new FileOutputStream(fileZip);
		 * fos.getChannel().transferFrom(rbc, 0, 1 << 24);
		 * fos.getChannel().force(true); fos.getChannel().close();
		 */

		return fileZip;
	}

	public static String readTextFromTile(String path) throws IOException {

		StringBuffer list = new StringBuffer();
		// Open the file that is the first
		// command line parameter
		FileInputStream fstream = new FileInputStream(path);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			strLine = strLine.trim().toLowerCase();
			if (strLine.length() == 0)
				continue;

			// Print the content on the console
			list.append(strLine).append("\n");
		}
		// Close the input stream
		in.close();

		return list.toString();
	}

	public static ArrayList<String> readLines(String path) throws IOException {

		ArrayList<String> list = new ArrayList<String>();
		// Open the file that is the first
		// command line parameter
		FileInputStream fstream = new FileInputStream(path);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			strLine = strLine.trim();
			if (strLine.length() == 0)
				continue;

			// Print the content on the console
			list.add(strLine);
		}
		// Close the input stream
		in.close();

		return list;
	}

	public static ArrayList<String> readLinesNoTrim(String path)
			throws IOException {

		ArrayList<String> list = new ArrayList<String>();
		// Open the file that is the first
		// command line parameter
		FileInputStream fstream = new FileInputStream(path);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			if (strLine.length() == 0)
				continue;

			// Print the content on the console
			list.add(strLine);
		}
		// Close the input stream
		in.close();

		return list;
	}

	public static void readLines(String path, Observer callback)
			throws IOException {

		FileInputStream fstream = new FileInputStream(path);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			if (strLine.trim().length() == 0)
				continue;

			// Print the content on the console
			callback.update(null, strLine);
		}
		// Close the input stream
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
		// http://www2.census.gov/geo/tiger/TIGER2010/EDGES/tl_2010_53033_edges.zip
		// http://www2.census.gov/geo/pvs/tiger2010st/53_Washington/53033/tl_2010_53033_edges.zip
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

		// Extract DBF
		ArrayList<File> dbfFiles = UnZip.extract(fileZip, "dbf");

		U.log("Done extract " + dbfFiles.get(0));

		return dbfFiles.get(0);

	}// saveZipAndGetDbf()

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

		// Extract DBF
		ArrayList<File> dbfFiles = UnZip.extract(fileZip, null);

		U.log("Done extract " + dbfFiles.get(0));

		return dbfFiles;

	}// saveZipAndGetDbf()

	public static File downloadZipFromCensus(String fipsCode, String type)
			throws Exception {

		String URL_PREFIX = "http://www2.census.gov/geo/tiger/TIGER2010/";

		String stateNumber = fipsCode.substring(0, 2);
		String DATA_DIR = Paths.combine(Paths.DATA_ROOT, type.toLowerCase(),
				stateNumber);

		String zipName = "tl_2010_" + fipsCode + "_" + type.toLowerCase()
				+ ".zip";
		String google = (URL_PREFIX + type.toUpperCase() + "/" + zipName);

		// File fileZip = FileUtil.downloadFileUsingBuffer(google, DATA_DIR,
		// zipName);
		File fileZip = FileUtil.downloadFile(google, DATA_DIR, zipName);
		U.log("Done download " + fileZip);

		return fileZip;
	}// downloadZipFromCensus()

}
