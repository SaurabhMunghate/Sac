package com.shatam.zip.search;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.shatam.util.Util;

public class CorrectZipAndCityState {

	public static void main(String args[]) throws IOException {

		List<String> list = Files.readAllLines(
				Paths.get("D:/USPSZipToCities/data.txt"),
				StandardCharsets.UTF_8);
		FileWriter writer = new FileWriter(
				"D:/USPSZipToCities/data_Corrected1.csv");
		int count = 0;
		for (String line : list) {
			String d = getHtml(line.split("\t")[0]);
			String stateZip = Util.match(d,
					"\"formatted_address\" : \"(.*?),\\s*(.*?)\\s*,\\s*USA\",",
					2);
			if (stateZip == null) {
				System.out.println(line);
				count++;
			} else {
				writer.write(line + "\t" + stateZip.split(" ")[0] + "\n");
			}
		}
		writer.close();
		System.out.println(count);
	}

	public static String getHtml(String zip) throws IOException {
		String path = "https://maps.googleapis.com/maps/api/geocode/json?&address={ZIP}&key=AIzaSyDiPvWYw9-ZGDnCfQ5kLJ8UUNlntD8doWk";
		String newPath = path.replace("{ZIP}", zip);
		
		String value = DistStoreMap.get(newPath);
		if (!value.equalsIgnoreCase("null")) {
			// System.out.println("From cache");
			return value;
		}
		URL url = new URL(newPath);
		URLConnection conn = url.openConnection();
		InputStream in = conn.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuffer buff = new StringBuffer();
		String line = null;
		while ((line = reader.readLine()) != null) {
			buff.append(line);
		}
		DistStoreMap.put("put", newPath, buff.toString());
		reader.close();
		return buff.toString();
	}
}
