/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.data.search;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

import com.shatam.util.U;

public class CheckAddressWithUSPS {

	public static String main(String args[]) throws IOException {
		try {

			String linkUsps = "https://tools.usps.com/go/ZipLookupResultsAction!input.action?resultMode=0&companyName=&address1="
					+ args[0]
					+ "&address2=&city="
					+ args[1]
					+ "&state="
					+ args[2] + "&urbanCode=&postalCode=&zip=" + args[3] + "";

			U.log(linkUsps);
			String path = linkUsps.replaceAll(" ", "%20");
			String html = getHTML(linkUsps);

			String secOriginal = HttpURLConnectionExample.getSectionValue(
					html + "", "You entered:</h3>", "id=\"results-wrapper\">")
					.trim();

			String streetOriName = HttpURLConnectionExample.getSectionValue(
					secOriginal, "class=\"address2\">", "</span>").trim();

			String cityOriName = HttpURLConnectionExample.getSectionValue(
					secOriginal, "class=\"city\">", "</span>").trim();

			String stateOriName = HttpURLConnectionExample.getSectionValue(
					secOriginal, "class=\"state\">", "</span>").trim();

			String secTemp = HttpURLConnectionExample.getSectionValue(
					secOriginal, "<span class=\"state\">", "</p>").trim();

			String zipOriName = HttpURLConnectionExample.getSectionValue(
					secTemp, "</span>", "</span>").trim();

			U.log("original address: " + streetOriName + "," + cityOriName
					+ "," + stateOriName + " " + zipOriName);

			String secUSPSAdds = HttpURLConnectionExample.getSectionValue(html
					+ "", "<div class=\"data\"", "class=\"zip4\">");

			String streetUSPSName = HttpURLConnectionExample.getSectionValue(
					secUSPSAdds, "\"address1 range\">", "</span>").trim();

			String cityUSPSName = HttpURLConnectionExample.getSectionValue(
					secUSPSAdds, "class=\"city range\">", "</span>").trim();

			String stateUSPSName = HttpURLConnectionExample.getSectionValue(
					secUSPSAdds, "class=\"state range\">", "</span>").trim();

			String zipUSPSOriName = HttpURLConnectionExample.getSectionValue(
					secUSPSAdds, "class=\"zip\" style=\"\">", "</span>").trim();

			U.log("USPS address: " + streetUSPSName + "," + cityUSPSName + ","
					+ stateUSPSName + " " + zipUSPSOriName);
			return (streetUSPSName + " " + cityUSPSName + " " + stateUSPSName
					+ " " + zipUSPSOriName).trim();
		} catch (NullPointerException e) {
			return (args[0] + " " + args[1] + " " + args[2] + " " + args[3])
					.trim();
		}

	}

	public static String getHTML(String path) throws IOException {

		path = path.replaceAll(" ", "%20");
		String fileName = getCache(path);
		File cacheFile = new File(fileName);
		if (cacheFile.exists())
			return FileUtil.readAllText(fileName);

		URL url = new URL(path);

		final URLConnection urlConnection = url.openConnection();

		urlConnection
				.addRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
		urlConnection.addRequestProperty("Accept", "text/css,*/*;q=0.1");
		urlConnection.addRequestProperty("Accept-Language", "en-us,en;q=0.5");
		urlConnection.addRequestProperty("Cache-Control", "max-age=0");
		urlConnection.addRequestProperty("Connection", "keep-alive");

		final InputStream inputStream = urlConnection.getInputStream();

		final String html = IOUtils.toString(inputStream);

		inputStream.close();

		if (!cacheFile.exists())
			FileUtil.writeAllText(fileName, html);

		return html;

	}

	public static String getCache(String path) throws MalformedURLException {

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

	public static String getCacheFileName(String url) {

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

	public static String[] getValues(String code, String From, String To) {

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
