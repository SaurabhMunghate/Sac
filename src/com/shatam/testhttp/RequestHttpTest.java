/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.testhttp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import com.shatam.util.U;

public class RequestHttpTest {

	public static void main(String[] args) throws IOException {

		String path = args[0];
		int n;
		File f = new File(path);
		if (!f.exists()) {
			U.log("File is missing");
		}
		BufferedReader r = new BufferedReader(new FileReader(f));
		String data = r.readLine();
		String address = data;
		while (data != null) {
			address = data;
			data = r.readLine();

		}
		StringBuffer buf = new StringBuffer();
		if (args.length == 1) {
			n = Integer.parseInt(args[1]);
			String[] splitadd = address.split("],");

			for (int i = 0; i < n; i++) {
				String s = splitadd[i];
				if (i == (n - 1))
					s = s + "]";
				else
					s = s + "],";
				buf.append(s);

			}
			buf.append("]");
			address = buf.toString();
		}
		r.close();
		if (args.length == 3) {
			int iteration = Integer.parseInt(args[2]);
			String[] splitadd = address.split("],");

			int start = 0;
			int end = Integer.parseInt(args[1]);

			for (int i = 0; i < iteration; i++) {
				if (i > 0) {
					buf = new StringBuffer();
					buf.append("[");

				}

				for (int j = start; j < end; j++) {
					String s = splitadd[j];

					if (j == (end - 1)) {
						s = s + "]";

					} else
						s = s + "],";
					buf.append(s);

				}
				start = end;
				end = end + start;
				buf.append("]");
				address = buf.toString();
				U.log("\n=Input address=\n" + address);
				U.log("========Output Address====");
				httprequestToSac(address);
				U.log("************************************************************************************");
			}
		} else {
			httprequestToSac(address);
		}

	}

	public static void httprequestToSac(String addresses) {
		String data = addresses;
		HttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setConnectionTimeout(client.getParams(), 1000);
		HttpResponse response;
		JSONObject json = new JSONObject();

		try {
			HttpPost post = new HttpPost("http://localhost:3309/postData/");
			json.put("address", data.toString());
			json.put("count", "1");
			json.put("jobs", "23");
			json.put("data", "USPS");

			StringEntity se = new StringEntity(json.toString());
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
					"application/json"));
			post.setEntity(se);
			response = client.execute(post);

			if (response != null) {
				InputStream in = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in));
				StringBuilder out = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					out.append(line);
				}
				System.out.println("OUTPUT=" + out.toString());
				reader.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

	}
}
