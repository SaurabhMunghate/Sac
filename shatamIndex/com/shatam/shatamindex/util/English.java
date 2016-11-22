/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public final class English {

	private English() {
	}

	public static String longToEnglish(long i) {
		StringBuilder result = new StringBuilder();
		longToEnglish(i, result);
		return result.toString();
	}

	public static void longToEnglish(long i, StringBuilder result) {
		if (i == 0) {
			result.append("zero");
			return;
		}
		if (i < 0) {
			result.append("minus ");
			i = -i;
		}
		if (i >= 1000000000000000000l) {
			longToEnglish(i / 1000000000000000000l, result);
			result.append("quintillion, ");
			i = i % 1000000000000000000l;
		}
		if (i >= 1000000000000000l) {
			longToEnglish(i / 1000000000000000l, result);
			result.append("quadrillion, ");
			i = i % 1000000000000000l;
		}
		if (i >= 1000000000000l) {
			longToEnglish(i / 1000000000000l, result);
			result.append("trillion, ");
			i = i % 1000000000000l;
		}
		if (i >= 1000000000) {
			longToEnglish(i / 1000000000, result);
			result.append("billion, ");
			i = i % 1000000000;
		}
		if (i >= 1000000) {
			longToEnglish(i / 1000000, result);
			result.append("million, ");
			i = i % 1000000;
		}
		if (i >= 1000) {
			longToEnglish(i / 1000, result);
			result.append("thousand, ");
			i = i % 1000;
		}
		if (i >= 100) {
			longToEnglish(i / 100, result);
			result.append("hundred ");
			i = i % 100;
		}

		if (i >= 20) {
			switch (((int) i) / 10) {
			case 9:
				result.append("ninety");
				break;
			case 8:
				result.append("eighty");
				break;
			case 7:
				result.append("seventy");
				break;
			case 6:
				result.append("sixty");
				break;
			case 5:
				result.append("fifty");
				break;
			case 4:
				result.append("forty");
				break;
			case 3:
				result.append("thirty");
				break;
			case 2:
				result.append("twenty");
				break;
			}
			i = i % 10;
			if (i == 0)
				result.append(" ");
			else
				result.append("-");
		}
		switch ((int) i) {
		case 19:
			result.append("nineteen ");
			break;
		case 18:
			result.append("eighteen ");
			break;
		case 17:
			result.append("seventeen ");
			break;
		case 16:
			result.append("sixteen ");
			break;
		case 15:
			result.append("fifteen ");
			break;
		case 14:
			result.append("fourteen ");
			break;
		case 13:
			result.append("thirteen ");
			break;
		case 12:
			result.append("twelve ");
			break;
		case 11:
			result.append("eleven ");
			break;
		case 10:
			result.append("ten ");
			break;
		case 9:
			result.append("nine ");
			break;
		case 8:
			result.append("eight ");
			break;
		case 7:
			result.append("seven ");
			break;
		case 6:
			result.append("six ");
			break;
		case 5:
			result.append("five ");
			break;
		case 4:
			result.append("four ");
			break;
		case 3:
			result.append("three ");
			break;
		case 2:
			result.append("two ");
			break;
		case 1:
			result.append("one ");
			break;
		case 0:
			result.append("");
			break;
		}
	}

	public static String intToEnglish(int i) {
		StringBuilder result = new StringBuilder();
		longToEnglish(i, result);
		return result.toString();
	}

	public static void intToEnglish(int i, StringBuilder result) {
		longToEnglish(i, result);
	}

	public static void main(String[] args) {
		System.out.println(longToEnglish(Long.parseLong(args[0])));
	}

}
