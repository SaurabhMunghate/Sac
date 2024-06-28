/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.model;

import com.shatam.util.StrUtil;

class _USPSSecondaryStruct {

	public boolean isUnitInRange(String unitNumber) {
		if (StrUtil.isEmpty(unitNumber)) {
			return (StrUtil.isEmpty(addrSecondaryLowNo) || StrUtil
					.isEmpty(addrSecondaryHighNo));
		}

		if (addrSecondaryHighNo == null && addrSecondaryLowNo == null) {

			return this.isHouseNumInRange(unitNumber);

		} else {

			String paddedUnitNum = padIfNeeded(unitNumber);

			boolean lessThanHigh = (addrSecondaryHighNo == null) ? true
					: paddedUnitNum.compareToIgnoreCase(addrSecondaryHighNo) <= 0;
			boolean greaterThanLow = (addrSecondaryLowNo == null) ? true
					: paddedUnitNum.compareToIgnoreCase(addrSecondaryLowNo) >= 0;

			return (lessThanHigh && greaterThanLow);

		}
	}

	public boolean isOnSameSideOfRoad(String houseNumber) {
		if (StrUtil.isEmpty(houseNumber))
			return true;
		if (StrUtil.isEmpty(addrPrimaryHighNo))
			return false;

		if (this.addrPrimaryOddEvenCode.equalsIgnoreCase("B"))
			return true;

		int givenHn = Integer.parseInt(StrUtil.extractPattern(houseNumber,
				"(\\d+)", 1));

		if (givenHn % 2 == 0
				&& this.addrPrimaryOddEvenCode.equalsIgnoreCase("E"))
			return true;
		if (givenHn % 2 == 1
				&& this.addrPrimaryOddEvenCode.equalsIgnoreCase("O"))
			return true;

		return false;

	}

	public boolean isHouseNumInRange(String houseNumber) {
		if (StrUtil.isEmpty(houseNumber))
			return true;

		String paddedHouseNumber = padIfNeeded(houseNumber);

		boolean lessThanHigh = addrPrimaryHighNo == null ? true
				: paddedHouseNumber.compareToIgnoreCase(addrPrimaryHighNo) <= 0;
		boolean greaterThanLow = addrPrimaryLowNo == null ? true
				: paddedHouseNumber.compareToIgnoreCase(addrPrimaryLowNo) >= 0;

		return (lessThanHigh && greaterThanLow);

	}

	public boolean isSameHouseNumAsStartAndEnd(String houseNumber) {
		if (StrUtil.isEmpty(houseNumber))
			return true;
		String paddedHouseNumber = padIfNeeded(houseNumber);
		return paddedHouseNumber.equalsIgnoreCase(addrPrimaryLowNo)
				&& paddedHouseNumber.equalsIgnoreCase(addrPrimaryHighNo);

	}

	private static String padIfNeeded(String s) {
		if (StrUtil.isEmpty(s))
			return null;

		String word = StrUtil.extractPattern(s, "([a-z]+)");

		if (word != null)
			s = s.replaceAll(word, " " + word + " ");

		String num = StrUtil.extractPattern(s, "(\\d+)");

		if (num != null)
			s = s.replaceAll(num, " " + num + " ");

		StringBuffer newUnit = new StringBuffer();
		for (String sub : s.split(StrUtil.WORD_DELIMETER)) {
			while (sub.length() < 6) {
				sub = "0" + sub;
			}
			newUnit.append(sub);

		}

		return newUnit.toString();
	}

	String addrPrimaryOddEvenCode = null;
	String buildingOrFirmName = null;
	String addrSecondaryAbbr = null;
	String addrPrimaryLowNo;
	String addrPrimaryHighNo;
	String addrSecondaryLowNo;
	String addrSecondaryHighNo;
	String county = null;
	String lowNoZipPlus4 = null;
	String highNoZipPlus4 = null;
	String line = null;

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(", addrPrimaryOddEvenCode:" + addrPrimaryOddEvenCode);
		buf.append(", addrSecondaryAbbr:" + addrSecondaryAbbr);
		buf.append(", addrPrimaryLowNo:" + addrPrimaryLowNo);
		buf.append(", addrPrimaryHighNo:" + addrPrimaryHighNo);
		buf.append(", addrSecondaryLowNo:" + addrSecondaryLowNo);
		buf.append(", addrSecondaryHighNo:" + addrSecondaryHighNo);
		buf.append(", county:" + county);
		buf.append(", lowNoZipPlus4:" + lowNoZipPlus4);
		buf.append(", highNoZipPlus4:" + highNoZipPlus4);
		buf.append(", buildingOrFirmName:" + buildingOrFirmName);

		return buf.toString();
	}

	public _USPSSecondaryStruct(String line) {
		this.line = line;

		String[] arr = line.split(",");
		int i = 0;

		addrPrimaryOddEvenCode = arr[i++].toUpperCase().trim();
		buildingOrFirmName = arr[i++];
		addrSecondaryAbbr = arr[i++].toUpperCase().trim();
		addrPrimaryLowNo = padIfNeeded(arr[i++]);
		addrPrimaryHighNo = padIfNeeded(arr[i++]);
		addrSecondaryLowNo = padIfNeeded(arr[i++]);
		addrSecondaryHighNo = padIfNeeded(arr[i++]);
		county = arr[i++];
		lowNoZipPlus4 = arr[i++];
		highNoZipPlus4 = arr[i++];

		if (!StrUtil.isEmpty(lowNoZipPlus4))
			lowNoZipPlus4 = ""
					+ Integer.parseInt(StrUtil.extractPattern(lowNoZipPlus4,
							"(\\d+)", 1));
	}

	public int deltaInHouseNum() {

		try {
			int i1 = StrUtil.isEmpty(addrPrimaryLowNo) ? 0 : Integer
					.parseInt(addrPrimaryLowNo);
			int i2 = StrUtil.isEmpty(addrPrimaryHighNo) ? 0 : Integer
					.parseInt(addrPrimaryHighNo);

			return Math.abs(i1 - i2);
		} catch (Exception ex) {
			return 0;
		}
	}
}