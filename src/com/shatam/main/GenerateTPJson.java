/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

class Schema {
	public String address;
	public String address2;
	public String city;
	public String state;

	public String zip;

	public String key;

}

public class GenerateTPJson {

	/**
	 * @param args
	 */

	static String dirpath = "C:\\SAC_cach\\Ashish_Cache\\";

	public static void main(String[] args) throws Exception {

		File directory = new File(dirpath);

		File[] fList = directory.listFiles();
		for (File file : fList) {
			String sCurrentLine;
			ArrayList<Schema> SchemaArrayList = new ArrayList<>();
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((sCurrentLine = br.readLine()) != null) {

				if (sCurrentLine.trim().length() > 0) {
					System.out.println(sCurrentLine);
					Schema obj = GetJsonObject(sCurrentLine);
					SchemaArrayList.add(obj);
				}
			}

			WriteObjectToJson(file.getName(), SchemaArrayList);
		}

	}

	private static void WriteObjectToJson(String name,
			ArrayList<Schema> schemaArrayList) throws Exception {

		String targetDir = "C:\\SAC_cach\\Ashish_Cache_Output\\";

		org.json.JSONArray completArray = new org.json.JSONArray();
		for (Schema obj : schemaArrayList) {
			org.json.JSONArray jsonColl = new org.json.JSONArray();
			jsonColl.put(obj.address);
			jsonColl.put(obj.address2);
			jsonColl.put(obj.city);
			jsonColl.put(obj.state);
			jsonColl.put(obj.zip);
			jsonColl.put(obj.key);
			completArray.put(jsonColl);
		}

		System.out.println(completArray.toString());
		File file = new File(targetDir + name);
		FileWriter fileWritter = new FileWriter(file);
		BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		bufferWritter.newLine();
		bufferWritter.write(completArray.toString());
		bufferWritter.close();

	}

	private static Schema GetJsonObject(String sCurrentLine) {

		String array[] = sCurrentLine.split(",");
		Schema obj = new Schema();
		obj.address = array[0];
		obj.city = array[1];
		obj.state = array[2];
		obj.zip = array[3];
		obj.address2 = "";
		obj.key = "";
		return obj;

	}

}
