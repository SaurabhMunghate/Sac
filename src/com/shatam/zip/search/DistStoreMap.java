package com.shatam.zip.search;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class DistStoreMap {
	public static void main(String[] args) throws UnknownHostException,
			IOException {
//		for (int i = 0; i < 100; i++) {
//			put("put", i + "", "Rakesh" + i);
//		}
		
		put("put", 1+"", "Rakesh" + 1);
		
	//	System.out.println(get("3"));
		//System.out.println(get("99"));
	}

	public static void put(String methodType, String key, String value)
			throws IOException {

		Socket s = new Socket("localhost", 3308);
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		out.writeUTF(methodType);
		out.writeUTF(key);
		out.writeUTF(value);
		out.flush();
		s.close();
	}

	public static String get(String key) throws IOException {

		Socket s = new Socket("localhost", 3308);
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		out.writeUTF("get");
		out.writeUTF(key);
		out.writeUTF("");
		out.flush();
		DataInputStream in = new DataInputStream(s.getInputStream());
		String value1 = in.readUTF();
		in.close();
		out.flush();
		s.close();
		return value1;

	}

	public static void put(Socket s, String methodType, String key, String value)
			throws IOException {

		// ObjectOutputStream dout = new ObjectOutputStream(
		// s.getOutputStream());
		// List<String> f = new ArrayList<>();
		// f.add("Edd");
		// dout.writeObject(f);
		// dout.flush();
		// dout.close();
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		out.writeUTF(methodType);
		out.writeUTF(key);
		out.writeUTF(value);
		out.flush();

	}

	public static String get(Socket s, String key) throws IOException {

		// ObjectOutputStream dout = new ObjectOutputStream(
		// s.getOutputStream());
		// List<String> f = new ArrayList<>();
		// f.add("Edd");
		// dout.writeObject(f);
		// dout.flush();
		// dout.close();
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		out.writeUTF("get");
		out.flush();
		DataInputStream in = new DataInputStream(s.getInputStream());
		String value1 = in.readUTF();
		System.out.println(value1 + "ddd");
		out.flush();
		return value1;
	}

	public static void batchAdd() throws UnknownHostException, IOException {

		Socket s = new Socket("localhost", 3308);
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		for (int i = 0; i < 10; i++) {
			put(s, "put", i + "", "Rakesh" + i);
		}
		put(s, "stop", "stop", "Rakesh");
		System.out.println("Close All");
		out.close();
	}
}