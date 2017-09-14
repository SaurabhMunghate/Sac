/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import com.shatam.util.U;

public class Seeding {

	public static void seed() throws Exception {

		File f = new File("ALL.txt");
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		StringBuffer buf = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			buf.append(line);
		}
		String textEntered = buf.toString();
		SortJsonArray sorter = new SortJsonArray();
		ArrayList<InputJsonSchema> addresses = sorter
				.sortInputAddress(textEntered);
		ThreadedSAC threadedSAC = new ThreadedSAC();
		U.log("seed");
		threadedSAC.processByParts(addresses, "", "1", "10", "USPS and TIGER",
				false);
		
	}

}
