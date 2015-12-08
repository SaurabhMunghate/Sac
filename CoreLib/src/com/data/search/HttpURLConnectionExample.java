package com.data.search;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;



import com.shatam.util.AbbrReplacement;
import com.shatam.util.U;


public class HttpURLConnectionExample {

	private final String USER_AGENT = "Mozilla/5.0";
	 
	public static String[] main(String[] args) throws Exception {
 
		HttpURLConnectionExample http = new HttpURLConnectionExample();
 
		System.out.println("Testing 1 - Send Http GET request");
		String matchedAdddress[]= http.sendGet(args);
 
		return matchedAdddress;
		/*System.out.println("\nTesting 2 - Send Http POST request");
		http.sendPost();*/
 
	}
 
	// HTTP GET request
	private String[] sendGet(String []add) throws Exception {
 
		String url = "http://localhost:8081/"+add[2]+"/"+add[1]+"/"+add[3]+"/"+ add[0].replaceAll(" ", "%20")+"/";
		U.log(url);
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 	
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		String section =getSectionValue(response.toString(), "<address>", "<_hnDistance>");
		U.log(response.toString());
		String matchedStreet= getSectionValue(section, "<address>", "</address>").toUpperCase();
		String matchedCity= getSectionValue(section, "<city>", "</city>").toUpperCase();
		String matchedState= getSectionValue(section, "<state>", "</state>").toUpperCase();
		String matchedZip= getSectionValue(section, "<zip>", "</zip>").toUpperCase();
		String preAddress= AbbrReplacement.getAbbr(getSectionValue(section, "<prefix_direction>", "</prefix_direction>"), matchedState).toUpperCase();
		String postAddress= AbbrReplacement.getAbbr(getSectionValue(section, "<suffix_type>", "</suffix_type>"), matchedState).toUpperCase();
		String streetAddress= AbbrReplacement.getAbbr(getSectionValue(section, "<street_name>", "</street_name>"), matchedState).toUpperCase();
		String matchedAddress= matchedStreet+" "+matchedCity+" "+matchedState+" "+matchedZip;
		//print result
		String arr[]=new String[]{preAddress,streetAddress,postAddress,matchedCity,matchedState,matchedZip,matchedAddress};
		return arr;
 
	}
	
	public static String getSectionValue(String code, String From, String To) {

		String section = null;
		int start, end;
		start = code.indexOf(From);
		if (start != -1) {
			end = code.indexOf(To, start + From.length());
			if (start < end)
				section = code.substring(start + From.length(), end);
		}
		return section;
	}
 
	// HTTP POST request
	private void sendPost() throws Exception {
 
		String url = "http://localhost:8081/AZ/TUCSON/85746/6943 s placita del perone";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
 
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
		String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
		System.out.println(response.toString());
 
	}

	
}
