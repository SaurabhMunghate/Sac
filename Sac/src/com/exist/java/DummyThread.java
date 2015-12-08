package com.exist.java;

import org.json.JSONArray;

import com.shatam.util.U;

public class DummyThread extends Thread {

	public static String text = "";
	public static String hitscore = "";
	public static String count = "";
	org.json.JSONArray outputArry = new org.json.JSONArray();

	public JSONArray runThread(String textStr, String htscore, String cnt)
			throws Exception {

		text = textStr;
		hitscore = htscore;
		count = cnt;
		// U.log(text);
		for (int i = 0; i < 500; i++) {
			Thread thread = new DummyThread();
			thread.start();
			while (thread.isAlive()) {
				/*thread.sleep(1);*/
			}
		}
		 return outputArry;

	}

	public void run() {

		/*try {
			outputArry = CustomAddressCorrector.passSortedAddressToSAC(text,
					hitscore, count);
			U.log(outputArry);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
