package com.shatam.zip.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.shatam.util.U;

public class ZipSearchUtil {

	public static void main(String args[]) throws IOException,
			URISyntaxException {
		// countAllZip();
		//
		zipCityStateMap = _loadAndGetMap1();
		int reapetedState = 0;
		for (Entry<String, List<Set<String>>> obj : zipCityStateMap.entrySet()) {

			String zip = obj.getKey();
			List<Set<String>> list = obj.getValue();
			Set<String> citySet = list.get(0);
			Set<String> stateSet = list.get(1);
			if (stateSet.size() > 1) {
				// System.out.println(zip + "\t" + citySet + "\t" + stateSet);
				reapetedState += 1;
			}
		}

		System.out.println(reapetedState);
		System.out.println(getCityState("58845"));
		System.out.println(getCityState("92612"));
		System.out.println(getCityState("54540"));
	}

	static Map<String, List<Set<String>>> zipCityStateMap = null;

	/**
	 * Get City-State from zip code.
	 * 
	 * @param zip
	 * @return List > at index 0 has city set and at index 1 has state set
	 * @throws IOException
	 */

	public static List<Set<String>> getCityState(String zip) throws IOException {

		if (zipCityStateMap == null) {
			try {
				zipCityStateMap = _loadAndGetMap1();
				//U.log("ZIP Data Loaded");
			} catch (URISyntaxException e) {
				e.printStackTrace();
				U.log("Error In loading City-State Map File");
			}
		}
		return zipCityStateMap.get(zip);
	}

	/**
	 * Load corrected zip-state map. Ex:56164 [pipestone, verdi, hatfield,
	 * ihlen] [SD, MN] > Here one zip points to multiple state, So it load
	 * correctd zip-state from csv file.
	 * 
	 * @return Map
	 * @throws IOException
	 */

	private static Map<String, String> loadZipCodeAppearedInMultipleState()
			throws IOException {

		InputStream in = ZipSearchUtil.class.getResourceAsStream("/"
				+ "ZipStateCorrectMapping.csv");
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		Map<String, String> map = new HashMap<>();
		String line = null;
		while ((line = d.readLine()) != null) {
			String data[] = line.split("\t");
			String key = data[0];
			String value = data[3];
			map.put(key, value);
		}
		// System.out.println(map);
		return map;

	}

	private static Map<String, List<Set<String>>> _loadAndGetMap1()
			throws IOException, URISyntaxException {
		int count = 0;

		// If the zip belongs the multiple state, Load here correct CSV data
		// file.
		Map<String, String> correctZipStateMap = loadZipCodeAppearedInMultipleState();

		Map<String, List<Set<String>>> zipCityStateMap = new HashMap<>();
		Set<String> s = new HashSet<>();
		try {
			for (String stateFdr : U.STATE_MAP.keySet()) {

				// Read data from class path
				InputStream in = ZipSearchUtil.class.getResourceAsStream("/"
						+ stateFdr + ".tab");
				BufferedReader d = new BufferedReader(new InputStreamReader(in));
				String line = null;
				while ((line = d.readLine()) != null) {
					String row = line;
					String zip = row.split("\t")[0];
					String city = row.split("\t")[1];
					List<Set<String>> value = zipCityStateMap.get(zip);
					if (value == null) {

						value = new ArrayList<>();
						Set<String> citySet = new HashSet<>();
						Set<String> stateSet = new HashSet<>();

						String correctState = correctZipStateMap.get(zip);
						if (correctState != null) {
							stateFdr = correctState;
						}
						citySet.add(city);
						stateSet.add(stateFdr);
						value.add(citySet);
						value.add(stateSet);

					} else {

						Set<String> citySet = value.get(0);
						Set<String> stateSet = value.get(1);
						citySet.add(city);

						String correctState = correctZipStateMap.get(zip);
						if (correctState != null) {
							stateFdr = correctState;
						}

						stateSet.add(stateFdr);

					}
					zipCityStateMap.put(zip, value);
					count++;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//System.out.println("Total Zip_City:\t" + count);
		//System.out.println("zipCityStateMap Size:\t" + zipCityStateMap.size());
		return zipCityStateMap;
	}

	private static void countAllZip() throws IOException {

		String dirPath = "D:/USPSZIPCITY/ZipCity/";
		File dir = new File(dirPath);
		String list[] = dir.list();
		int count = 0;
		Set<String> zipCity = new HashSet<String>();

		Map<String, List<Set<String>>> zipCityStateMap = new HashMap<>();

		for (String stateFdr : list) {

			String path = dirPath + "/" + stateFdr + "/" + stateFdr + ".tab";
			List<String> dataList = Files.readAllLines(Paths.get(path),
					StandardCharsets.UTF_8);
			count += dataList.size();
			Iterator<String> d = dataList.iterator();
			while (d.hasNext()) {
				String row = d.next();
				String zip = row.split("\t")[0];
				String city = row.split("\t")[1];
				List<Set<String>> value = zipCityStateMap.get(zip);
				if (value == null) {
					value = new ArrayList<>();
					Set<String> citySet = new HashSet<>();
					Set<String> stateSet = new HashSet<>();
					citySet.add(city);
					stateSet.add(stateFdr);
					value.add(citySet);
					value.add(stateSet);
				} else {

					Set<String> citySet = value.get(0);
					Set<String> stateSet = value.get(1);
					citySet.add(city);
					stateSet.add(stateFdr);
				}
				zipCityStateMap.put(zip, value);
			}

		}

		System.out.println("Total Zip_City:\t" + count);
		System.out.println("Total Unique Zip:\t" + zipCity.size());
		System.out.println("zipCityStateMap Size:\t" + zipCityStateMap.size());
	}
}
