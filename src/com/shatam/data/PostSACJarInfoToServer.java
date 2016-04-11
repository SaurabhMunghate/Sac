package com.shatam.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;

public class PostSACJarInfoToServer {

	/**
	 * 
	 *
	 * @author Rakesh Chaudhari
	 *
	 */
	int addressCount = 0;
	private final static String USER_AGENT = "Mozilla/5.0";

	public PostSACJarInfoToServer(int addressCount) {
		this.addressCount = addressCount;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PostSACJarInfoToServer datapost = new PostSACJarInfoToServer(3);

		datapost.post();

	}

	public void post() {
		// Posting the data to server
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				posting();

			}
		});

		t.start();
	}

	private void posting() {
		URL url;

		try {

			url = new URL("http://www.fixaddress.com/DataHandler.ashx");

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("POST");

			conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			conn.setRequestProperty("User-Agent", USER_AGENT);

			conn.setDoOutput(true);

			OutputStreamWriter wr = new OutputStreamWriter(
					conn.getOutputStream());

			// String stringBoxContainHere = "Contain Posting There";
			// String form =
			// "<form action=\"http://192.168.1.4:8080/DataHandler.ashx\" method=\"post\" enctype=\"multipart/form-data\">  "
			// +
			// "<input type ='text' name= \"dataBox\" value=" +
			// stringBoxContainHere +" />"+
			// "<input type='submit'/> </form>";

			InetAddress ip = java.net.Inet4Address.getLocalHost();

			int count = this.addressCount;
			String urlParameter = "AddressCount =" + count + "&" + "Date ="
					+ new Date().toString() + "&" + "IP=" + ip;

			wr.write(urlParameter.toString());

			wr.flush();
			String str = conn.getContent().toString();
			str = null;

		} catch (IOException e) {

			// Ignore it.
		}
	}

}
