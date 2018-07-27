/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import com.shatam.model.AddressStruct;

public class U {

	public static final boolean DO_GEOCODE = false;
	public static final int MAX_SEARCH_RESULTS_ALLOWED = 3;

	public static final String ZIP_ENHANCE = "^4";
//	public static String ZIP_ENHANCE = "^4";
	public static String CITY_ENHANCE = "^4";
	
	public static String STREET_ENHANCE = "^5";

	public static final String USPS = "USPS";
	public static final String TIGER = "TIGER";
	public static String ROOT = System.getProperty("user.dir");
	public static String PATH = new File(ROOT).getParent() + "/LOG/";

	
	public static void disp(AddressStruct addStruct) throws Exception {

	}

	public static void disp(String address1, String address2, String city,
			String zip, String state, AddressStruct addStruct) throws Exception {

	}

	public static void redirect(InputStream is, OutputStream os)
			throws IOException {
		final int BUFFER = 2048;

		int count;
		byte data[] = new byte[BUFFER];
		while ((count = is.read(data, 0, BUFFER)) != -1) {
			os.write(data, 0, count);
		}

		os.flush();
		os.close();
		is.close();
	}

	public static void log(Object o) {
		System.out.println(o);
	}

	public static String getSearchableAddress(String pAddress1, String pAddress2)
			throws Exception {
		if (StrUtil.isEmpty(pAddress1) && StrUtil.isEmpty(pAddress2))
			return null;

		pAddress1 = pAddress1.replaceFirst("([^\\w #]+|^)", "");

		pAddress2 = pAddress2.replaceFirst("([^\\w #]+|^)", "");

		if (Util.match(pAddress1, "#\\s*\\d+") == null)
			pAddress1 = pAddress1.replaceAll("#{2,}", "");

		String address1 = StrUtil.removeUnitFromAddress(pAddress1);

		String address2 = StrUtil.removeUnitFromAddress(pAddress2);

		if (StrUtil.isNum(address2)) {
			address2 = "";
		}

		address1 = correctNumbersDisguisedAsWords(address1);

		address2 = correctNumbersDisguisedAsWords(address2);

		if (!StrUtil.isEmpty(address1)
				&& StrUtil.containsOnlyAlphaAndSpaces(address1)
				&& address2.matches("\\d+ .*")
				&& StrUtil.containsString(address2)) {

			{

				System.err.println(address1
						+ " probably is an establishment NAME");
				address1 = "";
			}
		}

		String address = address1 + " " + address2;

		address = address.replaceAll(StrUtil.WORD_DELIMETER, " ").trim();

		if (address.length() == 0) {
			String[] unitArr = StrUtil.extractApartment(pAddress1, pAddress2);

			if (!StrUtil.isEmpty(unitArr[0])) {

				return unitArr[0];
			} else {
				return address;
			}
		}

		address = StrUtil.fixNumericStreetSuffixes(address);

		{
			String twochar = StrUtil.extractPattern(address,
					" ([\\w\\d] [\\w\\d]) ");

			if (!StrUtil.isEmpty(twochar)) {

			}

		}

		address = address.toLowerCase().replace("&amp;", "and");

		{
			String pattern = ".*([ewsn])(\\d+)(th|nd|st).*";

			if (address.matches(pattern)) {
				String dir = StrUtil.extractPattern(address, pattern, 1);
				String num = StrUtil.extractPattern(address, pattern, 2);
				address = address.replace(dir + num, dir + " " + num);
			}
		}

		{
			String DIR_REG = "(w|n|s|ne|nw|se|sw|east|west|north|south|northeast|northwest|southeast|southwest)";

			String hn = StrUtil.extractPattern(address.replace("&amp;", "and"),
					"(\\d+\\s*[and & -]*\\s*\\d+)" + " ", 1);

			if (hn == null) {
				hn = StrUtil.extractPattern(address, "(\\d+)" + DIR_REG + " ",
						1);
			}

			if (hn == null) {

				hn = StrUtil.extractPattern(address, "(\\d+)[a-z]{3,}", 1);
			}

			if (hn == null) {

				hn = StrUtil.extractPattern(address, DIR_REG + "(\\d+)", 2);
			}

			if (hn != null) {
				address = address.replaceFirst(hn, " " + hn + " ").replaceAll(
						"\\s+", " ");

			}
		}

		{
			String rrPattern = "^(rt|rr|rd|rfd|rural route|rural rt|r route|r rt|rural|route )";
			String rrCurrName = StrUtil.extractPattern(address, rrPattern, 1);

			if (!StrUtil.isEmpty(rrCurrName)) {
				address = address.replace(rrCurrName, "RR ").replaceAll("\\s+",
						" ");

			}

		}

		{
			address = address.toLowerCase();
			String pattern = "\\d+ ([a-z]) (st|ave|dr|pl)";

			if (address.matches(pattern)) {
				String oneLetterStreet = StrUtil.extractPattern(address,
						pattern, 1);

				if (oneLetterStreet.length() != 1) {
					throw new Exception("oneLetterStreet.len!=1: "
							+ oneLetterStreet);
				} else {
					String sixLetterStreet = oneLetterStreet + oneLetterStreet
							+ oneLetterStreet + oneLetterStreet
							+ oneLetterStreet + oneLetterStreet;

					address = address.replaceFirst(" " + oneLetterStreet + " ",
							" " + sixLetterStreet + " ");
				}
			}
		}

		address = address.replaceAll("null|Null|NULL", "");

		return address.trim().toLowerCase();

	}

	public static String _getNumSuf(int i) {

		String suf = "";
		if (i % 100 >= 11 && i % 100 <= 20)
			suf = "th";
		else if (i % 10 == 1)
			suf = "st";
		else if (i % 10 == 2)
			suf = "nd";
		else if (i % 10 == 3)
			suf = "rd";
		else
			suf = "th";

		return suf;
	}

	public static String _toStr(Object o) {
		if (o == null || o.toString().equals("null"))
			return "";

		if (o instanceof Double || o instanceof Float) {
			double d = (Double) o;
			d = d / 10000;
			o = ("" + d).replace(".", "");
		}
		return o.toString().trim().toLowerCase();
	}

	private static String correctNumbersDisguisedAsWords(String address) {
		if (StrUtil.isEmpty(address))
			return address;

		{
			String[] num2Words = { "zero", "one", "two", "three", "four",
					"five", "six", "seven", "eight", "nine", "ten" };
			for (int i = 0; i < num2Words.length; i++) {

				if (address.startsWith(num2Words[i] + " ")) {
					address = address.replaceFirst(num2Words[i] + " ", i + " ");
					break;
				}
			}
		}

		{
			String[] stNum2Words = { "zeroeth", "first", "second", "third",
					"fourth", "fifth", "sixth", "seventh", "eighth", "nineth",
					"tenth", "eleventh", "twelth", "thirteenth", "fourteenth",
					"fifteenth", "sixteenth", "seventeenth", "eighteenth",
					"SEVENTEENTH" };
			for (int i = 0; i < stNum2Words.length; i++) {
				if (address.contains(stNum2Words[i])) {

					if (i < 10)
						address = address.replaceFirst(stNum2Words[i], i
								+ _getNumSuf(i) + " " + stNum2Words[i] + " ");

					else

						address = address.replaceFirst(stNum2Words[i], i
								+ _getNumSuf(i));

					break;
				}

			}

		}

		address = address.replaceAll("twp|miles|t-\\d+", "");

		return address;

	}

	public static String getStateCode(String abbr) {
		return STATE_MAP.get(abbr.toUpperCase());
	}

	public static HashMap<String, String> STATE_MAP = new HashMap<String, String>();
	static {

		STATE_MAP.put("AK", "02");
		STATE_MAP.put("AL", "01");
		STATE_MAP.put("AR", "05");
		STATE_MAP.put("AS", "60");
		STATE_MAP.put("AZ", "04");
		STATE_MAP.put("CA", "06");
		STATE_MAP.put("CO", "08");
		STATE_MAP.put("CT", "09");
		STATE_MAP.put("DC", "11");
		STATE_MAP.put("DE", "10");
		STATE_MAP.put("FL", "12");
		STATE_MAP.put("FM", "64");
		STATE_MAP.put("GA", "13");
		STATE_MAP.put("GU", "66");
		STATE_MAP.put("HI", "15");
		STATE_MAP.put("IA", "19");
		STATE_MAP.put("ID", "16");
		STATE_MAP.put("IL", "17");
		STATE_MAP.put("IN", "18");
		STATE_MAP.put("KS", "20");
		STATE_MAP.put("KY", "21");
		STATE_MAP.put("LA", "22");
		STATE_MAP.put("MA", "25");
		STATE_MAP.put("MD", "24");
		STATE_MAP.put("ME", "23");
		STATE_MAP.put("MH", "68");
		STATE_MAP.put("MI", "26");
		STATE_MAP.put("MN", "27");
		STATE_MAP.put("MO", "29");
		STATE_MAP.put("MP", "69");
		STATE_MAP.put("MS", "28");
		STATE_MAP.put("MT", "30");
		STATE_MAP.put("NC", "37");
		STATE_MAP.put("ND", "38");
		STATE_MAP.put("NE", "31");
		STATE_MAP.put("NH", "33");
		STATE_MAP.put("NJ", "34");
		STATE_MAP.put("NM", "35");
		STATE_MAP.put("NV", "32");
		STATE_MAP.put("NY", "36");
		STATE_MAP.put("OH", "39");
		STATE_MAP.put("OK", "40");
		STATE_MAP.put("OR", "41");
		STATE_MAP.put("PA", "42");
		STATE_MAP.put("PR", "72");
		STATE_MAP.put("PW", "70");
		STATE_MAP.put("RI", "44");
		STATE_MAP.put("SC", "45");
		STATE_MAP.put("SD", "46");
		STATE_MAP.put("TN", "47");
		STATE_MAP.put("TX", "48");
		STATE_MAP.put("UT", "49");
		STATE_MAP.put("VA", "51");
		STATE_MAP.put("VI", "78");
		STATE_MAP.put("VT", "50");
		STATE_MAP.put("WA", "53");
		STATE_MAP.put("WI", "55");
		STATE_MAP.put("WV", "54");
		STATE_MAP.put("WY", "56");
	}

	public static InputStream getInputStream(String url) throws Exception {
		URL yahoo = new URL(url);

		URLConnection hc = yahoo.openConnection();

		hc.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		hc.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
		hc.setRequestProperty("Cache-Control", "max-age=0");
		hc.setRequestProperty("Connection", "keep-alive");
		hc.setRequestProperty("Host", "craigslist.org");
		hc.setRequestProperty("User-Agent",
				"Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:13.0) Gecko/20100101 Firefox/13.0.1");

		return hc.getInputStream();

	}

	public static String readFromUrl(String path) throws Exception {
		if (!path.startsWith("http"))
			throw new Exception("Bad URL " + path);

		BufferedReader br;

		br = new BufferedReader(new InputStreamReader(getInputStream(path)));

		StringBuffer buf = new StringBuffer();
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.trim().length() == 0)
				continue;
			buf.append(line).append("\n");

		}

		br.close();

		String returnStringData = buf.toString();
		return returnStringData;

	}

	public static void writeFile(String text) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
				"SAC-Latency.txt", true)));
		out.println("\n" + text);
		out.close();

	}

	public static String lastUpdatedFile() throws UnknownHostException {
		File dir = new File(PATH);
		File[] files = dir.listFiles();
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		File file = files[0];
		return file.toString();

	}

	public static void ftpUploadFile() {
		String server = "***********";
		int port = 21;
		String user = "*****************";
		String pass = "***********";

		FTPClient ftpClient = new FTPClient();
		try {

			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			String filePath = lastUpdatedFile();
			File secondLocalFile = new File(filePath);
			String secondRemoteFile = "SAC LOG FILE/"
					+ secondLocalFile.getName();
			FileInputStream inputStream = new FileInputStream(secondLocalFile);
			OutputStream outputStream = ftpClient
					.storeFileStream(secondRemoteFile);
			byte[] bytesIn = new byte[4096];
			int read = 0;

			while ((read = inputStream.read(bytesIn)) != -1) {
				outputStream.write(bytesIn, 0, read);
			}
			inputStream.close();
			outputStream.close();

			boolean completed = ftpClient.completePendingCommand();
			if (completed) {

			}

		} catch (IOException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}
}
