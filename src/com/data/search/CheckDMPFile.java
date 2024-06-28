/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.data.search;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import au.com.bytecode.opencsv.CSVWriter;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.AbbrReplacement;
import com.shatam.util.AddressCorrector;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class CheckDMPFile {
	private String TEST_ONLY_FOR_STATE = "AZ";
	private final String ONLY_TEST_ID = null;
	public ArrayList<String> allData = new ArrayList<String>();
	public ArrayList<String> matchedAddr = new ArrayList<String>();
	public int shatamCorrect = 0;
	public int BothCorrect = 0;
	public int CACorrect = 0;
	public int cantTell = 0;
	public int totalCount = 6;
	public int allCount = 0;
	public int approxMatched = 0;

	public int[] main() throws Exception {

		String addPath = "D://DMPSampleAddresses_10K.csv";

		_CountStatusStruct[] matchCount = new _CountStatusStruct[18];
		for (int i = 0; i < matchCount.length; i++) {
			matchCount[i] = new _CountStatusStruct();
		}

		ProcessState processState = new ProcessState();
		CsvListReader csvReader = new CsvListReader(new FileReader(addPath),
				CsvPreference.STANDARD_PREFERENCE);

		List<String> caRow = null;
		int i1 = 0;
		int i2 = 0;
		int i3 = 0;
		while ((caRow = csvReader.read()) != null) {

			String ipState = caRow.get(3).trim();

			processState.totalProcessed++;

			String ipId = caRow.get(0).trim();
			U.log("--------------------------" + processState.totalProcessed);

			if (ONLY_TEST_ID != null && !ipId.equals(ONLY_TEST_ID))
				continue;

			String ipAddress = getCsvVal(caRow, 1).replaceAll("\\s+|/", " ")
					.toUpperCase();
			String ipCity = getCsvVal(caRow, 2).toUpperCase();
			String ipZip = getCsvVal(caRow, 4).toUpperCase();
			if (ipAddress.contains("625 ORE STREET"))
				continue;

			String caState = getCsvVal(caRow, 18);
			String caStreet = getCsvVal(caRow, 12);
			String caStreetPre = getCsvVal(caRow, 11);
			String caStreetPost = getCsvVal(caRow, 13);
			String caCity = getCsvVal(caRow, 17);

			String caZip = getCsvVal(caRow, 20);
			String caAddon = getCsvVal(caRow, 21);

			if (StrUtil.isEmpty(caState) || caState.length() != 2
					|| ipState.toUpperCase().equals("TA")) {
				continue;
			}

			ipAddress = ipAddress.replace("/", "");
			ipAddress = ipAddress.replace("342D", "342");
			U.log(ipAddress + "," + ipCity);
			AddressStruct addStruct = AddressCorrector
					.corrUsingAppropriateIndex(ipAddress, "", ipCity, ipState,
							ipZip);

			String fullCAOutput = appendString(caRow, 10, 18);
			fullCAOutput = getFullFormAddress(fullCAOutput);
			String fullSHOutput = addStruct.toFullAddressString();
			fullSHOutput = getFullFormAddress(fullSHOutput);
			U.log("fullCAOutput:" + fullCAOutput + "---");
			U.log("fullSHOutput:" + fullSHOutput + "---");

			if (!fullCAOutput.equalsIgnoreCase(fullSHOutput)) {
				U.log(" Input Address     \t:" + ipAddress + ", " + ipCity
						+ ", " + ipZip + ", " + ipState);
				U.log("fullCAOutput:" + fullCAOutput + "---");
				U.log("fullSHOutput:" + fullSHOutput + "---");
				String fullSHOutput2 = getFullFormAddress(addStruct
						.toFullAddressString2());
				String fullSHOutput3 = getFullFormAddress(
						addStruct.toFullAddressString3()).trim();
				String fullCAOutput2 = fullCAOutput + " " + caZip;
				String oriAdd = ipAddress.trim().replaceAll("####|###", " ")
						+ "," + ipCity + "," + ipState + " " + ipZip;
				String googleAdd[] = getGooAddress(oriAdd);
				String gadd = getFullFormAddress(googleAdd[googleAdd.length - 2]);
				String gadd2 = getFullFormAddress(
						googleAdd[googleAdd.length - 1]).replace("usa", "")
						.trim();
				String fullSHOutput4 = getFullFormAddress(
						addStruct.toFullAddressString4()).trim();
				if (gadd.equalsIgnoreCase(fullSHOutput2)
						|| gadd2.equalsIgnoreCase(fullSHOutput2)
						|| gadd2.equalsIgnoreCase(fullSHOutput3)
						|| gadd2.equalsIgnoreCase(fullSHOutput4))
					shatamCorrect++;
				else if (gadd.equalsIgnoreCase(fullCAOutput2)
						|| gadd2.equalsIgnoreCase(fullCAOutput2))
					CACorrect++;
				else {
					String matchedAddress[] = addStruct.toSplitAddress();
					int count = 1;
					if (caStreetPre.equals(matchedAddress[0]))
						count++;
					if (caStreetPost.equals(matchedAddress[2]))
						count++;
					if (caStreet.equals(matchedAddress[1]))
						count++;
					if (caCity.equals(matchedAddress[3]))
						count++;
					if (caState.equals(matchedAddress[4]))
						count++;
					if (caZip.equals(matchedAddress[5]))
						count++;

					double percent = 100 * ((count + 0.0) / totalCount);
					U.log("percent:::" + percent);
					if (percent > 80)
						BothCorrect++;
					else {

						String address = CheckAddressWithUSPS
								.main(new String[] {
										getCsvVal(caRow, 1).toUpperCase(),
										getCsvVal(caRow, 2).toUpperCase(),
										getCsvVal(caRow, 3).toUpperCase(),
										getCsvVal(caRow, 4).toUpperCase() });
						if (address.equalsIgnoreCase(fullSHOutput)) {
							shatamCorrect++;
						} else if (address.equalsIgnoreCase(fullCAOutput)) {
							CACorrect++;
						} else if ((addStruct.get(AddColumns.ZIP)
								.contains(caZip))
								&& (addStruct.get(AddColumns.NAME)
										.equalsIgnoreCase(caStreet))
								&& (addStruct.get(AddColumns.NAME)
										.equalsIgnoreCase(caStreet))) {

							approxMatched++;
						} else {

							cantTell++;

							allData.add(oriAdd + "::" + fullCAOutput + "::"
									+ fullSHOutput + "::" + gadd + "::" + gadd2
									+ "::" + fullSHOutput3 + "::"
									+ fullSHOutput4);

						}

					}
				}

			} else {
				BothCorrect++;
			}

		}

		csvReader.close();

		WriteToCsvFile(allData, "UnMatched_AZ_13_June_Addresses5.csv");
		return new int[] { BothCorrect, cantTell, CACorrect, shatamCorrect,
				approxMatched };

	}

	public static String getFullFormAddress(String add) throws Exception {

		StringBuffer buf = new StringBuffer();
		String[] arr = add.split("[^\\d\\w\\-/]");

		for (String part : arr) {
			if (StrUtil.isEmpty(part))
				continue;

			String v = AbbrReplacement.getFullName(part, "AZ");
			if (v.matches("[\\-_]")) {
				continue;
			}

			if (StrUtil.isEmpty(v)) {
				v = part;
			}

			buf.append(" ").append(v);
		}

		return buf.toString();

	}

	private void WriteCountToFile() throws Exception {

		File outputFile = new File("D://FilesD/18_Jan_Count_All.csv");
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		StringWriter sw = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(sw, ',');
		String[] writeEntries = new String[] { "Match Type", "Count", "Percent" };
		csvWriter.writeNext(writeEntries);

		double totalPercent = 100 * (Double.parseDouble(BothCorrect + "") / Double
				.parseDouble(allCount + ""));
		double shatamCorrctPercent = 100 * (Double.parseDouble(shatamCorrect
				+ "") / Double.parseDouble(allCount + ""));
		double CACorrectPercent = 100 * (Double.parseDouble(CACorrect + "") / Double
				.parseDouble(allCount + ""));
		double CantTellPercent = 100 * (Double.parseDouble(cantTell + "") / Double
				.parseDouble(allCount + ""));

		writeEntries = new String[] { "Exact_match", BothCorrect + "",
				totalPercent + "" };
		csvWriter.writeNext(writeEntries);

		writeEntries = new String[] { "Shatam_Better ", shatamCorrect + "",
				shatamCorrctPercent + "" };
		csvWriter.writeNext(writeEntries);

		writeEntries = new String[] { "CA_Better", CACorrect + "",
				CACorrectPercent + "" };
		csvWriter.writeNext(writeEntries);

		writeEntries = new String[] { "Cant_Tell", cantTell + "",
				CantTellPercent + "" };
		csvWriter.writeNext(writeEntries);

		writer.write(sw.toString());
		csvWriter.close();
		sw.close();
		csvWriter = null;
		sw = null;

		writer.close();
	}

	private void WriteToCsvFile(ArrayList<String> allData2, String fileName)
			throws Exception {

		File outputFile = new File("D://FilesD//" + fileName);
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		StringWriter sw = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(sw, ',');
		String[] writeEntries = new String[] { "ORIGINAL ADDRESS",
				"CASS ADDRESS", "SHATAM ADDRESS", "GOOGLE ADDRESSS",
				"GOOGLE ADDRESS 2", "SHATAM OUTPUT 3", "SHATAM OUTPUT 4" };
		csvWriter.writeNext(writeEntries);
		for (String item : allData2) {

			String[] entries = item.split("::");

			csvWriter.writeNext(entries);
		}
		writer.write(sw.toString());
		csvWriter.close();
		sw.close();
		csvWriter = null;
		sw = null;

		writer.close();
	}

	private String appendString(List<String> caRow, int st, int end) {

		StringBuffer buf = new StringBuffer();
		for (int i = st; i <= end; i++) {
			String s = caRow.get(i).trim();
			if (s.length() > 0) {
				buf.append(s).append(" ");
			}
		}
		return buf.toString().trim();
	}

	private String getCsvVal(List<String> caRow, int i) throws Exception {

		return caRow.get(i).trim().toUpperCase().replaceAll("\\s+", " ");

	}

	public String[] getGooAddress(String add) throws Exception {

		String addr = add;
		String data = null;
		String latLong = null;
		StringBuffer input = new StringBuffer();
		addr = addr.replace("AVENIDA", "AVE");
		String link = "http://maps-api-ssl.google.com/maps/api/geocode/xml?address="
				+ URLEncoder.encode(addr, "UTF-8") + "&sensor=false";

		U.log(link);

		String path = link.replaceAll(" ", "%20");
		String fileName = getCache(path);
		File cacheFile = new File(fileName);
		if (cacheFile.exists())
			input = new StringBuffer(FileUtil.readAllText(fileName));
		else {
			URL url = new URL(link);

			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			while ((data = br.readLine()) != null) {
				input.append(data);
			}

			if (!cacheFile.exists())
				FileUtil.writeAllText(fileName, input.toString());
		}

		String path2 = "D:\\google.txt";
		File file = new File(path2);
		FileWriter wr = new FileWriter(file);
		U.log(input.toString());

		String values[] = getValues(input.toString(), "<short_name>",
				"</short_name>");

		String formattedAd = HttpURLConnectionExample.getSectionValue(input
				+ "", "<formatted_address>", "</formatted_address>");
		String ad = null;
		String[] adress = null;
		if (values.length == 8) {
			ad = values[0] + " " + values[1] + " " + values[3] + " "
					+ values[5] + " " + values[7];
			adress = new String[] { values[0], values[1], values[3], values[5],
					values[7], ad, formattedAd };
		}

		if (values.length == 6) {
			ad = values[0] + " " + values[1] + " " + values[2] + " "
					+ values[3] + " " + values[5];
			adress = new String[] { values[0], values[1], values[2], values[3],
					values[5], ad, formattedAd };
		}

		if (values.length == 7) {
			ad = values[0] + " " + values[1] + " " + values[2] + " "
					+ values[4] + " " + values[6];
			adress = new String[] { values[0], values[1], values[2], values[4],
					values[6], ad, formattedAd };
		}
		if (values.length > 12) {
			ad = values[0] + " " + values[1] + " " + values[2] + " "
					+ values[4] + " " + values[6];
			adress = new String[] { values[0], values[1], values[2], values[4],
					values[6], ad, formattedAd };
		}
		if (values.length == 12) {
			ad = values[0] + " " + values[1] + " " + values[3] + " "
					+ values[5];
			adress = new String[] { values[0], values[1], values[3], values[5],
					ad, formattedAd };
		}
		if (values.length == 9) {
			ad = values[2] + " " + values[3] + " " + values[4] + " "
					+ values[6] + " " + values[8];
			adress = new String[] { values[2], values[3], values[4], values[6],
					values[8], ad, formattedAd };
		}
		if (values.length <= 5) {
			ad = "";
			adress = new String[] { ad, formattedAd };
		}
		U.log(ad);
		if (ad == null) {

			adress = new String[] { ad = "", formattedAd };
		}
		if (formattedAd == null) {
			adress = new String[] { ad = "", formattedAd = "" };
		}
		formattedAd = formattedAd.replaceAll("usa|USA", "");
		formattedAd = formattedAd.replace(",", "");
		U.log("::::::" + ad);
		U.log(formattedAd);
		return adress;
	}

	public String getCache(String path) throws MalformedURLException {

		String Dname = null;
		String host = new URL(path).getHost();
		host = host.replace("www.", "");
		int dot = host.indexOf("/");
		Dname = (dot != -1) ? host.substring(0, dot) : host;

		File folder = new File("c:\\cache\\" + Dname);
		if (!folder.exists())
			folder.mkdirs();
		String fileName = getCacheFileName(path);
		fileName = "c:\\cache\\" + Dname + "\\" + fileName;
		return fileName;
	}

	public String getCacheFileName(String url) {

		String str = url.replaceAll("http://", "");
		str = str.replaceAll("www.", "");
		str = str.replaceAll("[^\\w]", "");
		if (str.length() > 200) {
			str = str.substring(0, 100) + str.substring(170, 190)
					+ str.length() + "-" + str.hashCode();
		}

		try {
			str = URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str + ".txt";
	}

	public String[] getValues(String code, String From, String To) {

		ArrayList<String> al = new ArrayList<String>();
		int n = 0;
		String value = null;
		while (n != -1) {
			int start = code.indexOf(From, n);

			if (start != -1) {
				int end = code.indexOf(To, start + From.length());

				try {
					if (end != -1 && start < end && end < code.length())
						value = code.substring(start + From.length(), end);
				} catch (StringIndexOutOfBoundsException ex) {
					n = end;
					continue;
				}

				al.add(value);
				n = end;
			} else
				break;
		}

		Object ia[] = al.toArray();
		String[] values = new String[ia.length];

		for (int i = 0; i < values.length; i++)
			values[i] = ia[i].toString();

		return values;

	}

}
