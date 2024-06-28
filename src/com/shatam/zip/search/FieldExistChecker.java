package com.shatam.zip.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldExistChecker {

	static Pattern citystatepat = null;
	final static String STATECITYMISSINGCHECKER = "\\[\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*,\\s*\"(.+?)\"\\s*,\\s*\"(.+?)\"\\s*,\\s*\"(.+?)\"\\]";

	public static void main(String args[]) {

		String str = "[\"fake_id_value\",\"18831 von karman\",\"address2\",\"\",\"state\",\"92612\"]";
		long s = System.currentTimeMillis();	
		hasCityState(str);
		long e = System.currentTimeMillis();
		System.out.println((e - s));
		
	}

	public static boolean hasCityState(String str) {

		String city = match(str, STATECITYMISSINGCHECKER, 4);
		String state = match(str, STATECITYMISSINGCHECKER, 5);
		if (city == null && state == null) {
			return false;
		}
		return true;
	}

	public static String match(String str, String rgx) {
		return match(str, rgx, 0);
	}

	private static String match(String str, String rgx, int group) {

		if (citystatepat == null) {
			citystatepat = Pattern.compile(STATECITYMISSINGCHECKER);
		}
		Matcher mat = citystatepat.matcher(str);
		try {
			if (mat.find()) {
				return mat.group(group);
			}
		} catch (Exception e) {

		} finally {
			mat = null;
		}
		return null;
	}

}
