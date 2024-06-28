/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import au.com.bytecode.opencsv.CSVWriter;

public class Fipsdemo {

	public static HashMap FIPS = new HashMap();

	public static void main(String[] args) throws IOException,
			InterruptedException {
		createFIPSFiles();
	}

	public static void createFIPSFiles() throws IOException {
		CsvListReader csvReader = new CsvListReader(new FileReader(
				"D:/CMD Line Programe/FIPS.csv"),
				CsvPreference.STANDARD_PREFERENCE);
		File file = new File("D:/CMD Line Programe/FIPS1.csv");
		FileWriter writer = new FileWriter(file);
		List<String> caRow = null;
		StringWriter sw = new StringWriter();
		CSVWriter cwriter = new CSVWriter(sw, ',');
		while ((caRow = csvReader.read()) != null) {
			System.out.println(caRow.get(0).trim() + "::ROW2::" + caRow.size());
			String fips = caRow.get(5);
			if (caRow.get(5).length() == 4) {
				fips = "0" + fips;
			}
			String str[] = { caRow.get(0), caRow.get(1), caRow.get(2),
					caRow.get(3), caRow.get(4), fips };
			cwriter.writeNext(str);
		}
		writer.write(sw.toString());
		cwriter.close();
		writer.close();
	}

	public static HashMap getfips() throws IOException, InterruptedException {
		HashMap FIPS = new HashMap();
		CsvListReader csvReader = new CsvListReader(new FileReader(
				System.getProperty("user.dir") + "\\FIPS.csv"),
				CsvPreference.STANDARD_PREFERENCE);
		List<String> caRow = null;
		while ((caRow = csvReader.read()) != null) {
			System.out.println(caRow.get(0).trim() + "::ROW2::" + caRow.size());
			FIPS.put(caRow.get(0).trim(), caRow.get(5).trim());
		}
		return FIPS;
	}
}
