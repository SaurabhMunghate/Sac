/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.data;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.shatam.geo.ReadShapeFile;
import com.shatam.util.Paths;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class ZipCodes {

	private static ReadShapeFile shapeFile = null;

	public static ArrayList<String> getZips(Object edge) throws Exception {

		if (shapeFile == null) {
			U.log("Reading ZIP shape file");
			shapeFile = new ReadShapeFile(new File(Paths.ZIP_SHAPE_PATH),
					"ZCTA5CE10");
			U.log("Done ZIP shape file");
		}

		return shapeFile.query(edge);
	}

	public static HashSet<String> getCity(String zip) throws Exception {
		if (StrUtil.isEmpty(zip))
			throw new IOException("BAD Zip:" + zip);

		if (zipToCity.size() == 0) {
			readFile(Paths.combine(Paths.SETTINGS_PATH, "ZIP_CODES_2.txt"));
			readUSPSCityStateFile();
		}
		HashSet<String> city = zipToCity.get(zip);

		int i = 1;
		while (city == null || city.size() == 0) {

			int numZip = Integer.parseInt(zip);

			numZip += i;

			i *= -1;
			if (i > 0)
				i++;
			else
				i--;

			zip = numToStringZip(numZip);

			city = zipToCity.get(zip);

		}
		return city;

	}

	private static HashSet<String> getZoneSplitZips1(String zipCode)
			throws Exception {
		if (zoneSplitZipMap.size() == 0) {
			readZoneSplittingRecords();
		}
		HashSet<String> arr = zoneSplitZipMap.get(zipCode);
		if (arr == null)
			arr = new HashSet<String>();
		return arr;
	}

	static HashMap<String, HashSet<String>> zoneSplitZipMap = new HashMap<String, HashSet<String>>();

	private static void readZoneSplittingRecords() throws Exception {
		USPSUtil.readFile(USPSUtil.CITY_STATE_FILE_NAME, 'Z', 129,
				new USPSUtil._USPSFileCallback() {

					@Override
					public void callback(byte[] data) throws Exception {
						String oldZip = new String(data, 1, 5).trim()
								.toUpperCase();
						String newZip = new String(data, 10, 5).trim()
								.toUpperCase();
						if (!zoneSplitZipMap.containsKey(oldZip)) {
							zoneSplitZipMap.put(oldZip, new HashSet<String>());
						}
						zoneSplitZipMap.get(oldZip).add(newZip);

						if (!zoneSplitZipMap.containsKey(newZip)) {
							zoneSplitZipMap.put(newZip, new HashSet<String>());
						}
						zoneSplitZipMap.get(newZip).add(oldZip);

					}
				});
	}

	private static void readUSPSCityStateFile() throws Exception {
		String cityStateFile = USPSUtil.USPS_ZIP4_DIR
				+ "ctystate\\ctystate.txt";
		FileInputStream fstream = new FileInputStream(cityStateFile);

		byte[] data = new byte[129];

		while (fstream.read(data) > 0) {
			if (data[0] == 'S') {

				String labelZip = new String(data, 1, 5).trim().toUpperCase();
				String combinedZip = new String(data, 6, 5).trim()
						.toUpperCase();
				if (labelZip.startsWith("724") || combinedZip.startsWith("724")) {
					U.log("*********** labelZip:" + labelZip + " combinedZip:"
							+ combinedZip);
				}
			}
			if (data[0] != 68)
				continue;
			USPSCityModel rec = new USPSCityModel(data);

			HashSet<String> cityArr = zipToCity.get(rec.zip);
			if (cityArr == null)
				cityArr = new HashSet<String>();

			cityArr.add(rec.cityStateName.trim().toUpperCase());
			zipToCity.put(rec.zip, cityArr);

		}
		fstream.close();

	}

	private static HashMap<String, HashSet<String>> zipToCity = new HashMap<String, HashSet<String>>();

	private static void readFile(String file) throws IOException {

		FileInputStream fstream = new FileInputStream(file);

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			String[] arr = strLine.split(",");

			String zip = numToStringZip(Integer.parseInt(arr[0]));
			String city = arr[1].trim().toUpperCase();

			HashSet<String> cityArr = zipToCity.get(zip);
			if (cityArr == null) {
				cityArr = new HashSet<String>();
			}
			cityArr.add(city);
			zipToCity.put(zip, cityArr);

		}

		in.close();

	}

	private static String numToStringZip(int z) {
		String zip = String.format("%05d", z);
		return zip;
	}
}
