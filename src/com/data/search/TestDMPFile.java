/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.data.search;

import java.io.FileReader;
import java.io.PrintWriter;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.AbbrReplacement;
import com.shatam.util.AddressCorrector;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

class ProcessState {
	public double startTime = System.currentTimeMillis();
	public int totalProcessed = 0;
	public int cassMatch = 0;
	public int iAmBetterMatch = 0;
	public int iAmSoWrongMatch = 0;
	public int iAmExtactMatch = 0;
}

class _CountStatusStruct {
	public int exactCAMatchCount = 0;
	public int shatamIsBetterMatchCount = 0;
	public int caIsBetterMatchCount = 0;
	public int inconclusiveMatchCount;
	public String title;
}

public class TestDMPFile {

	private final static String TEST_ONLY_FOR_STATE = "AZ";
	private static final String ONLY_TEST_ID = null;

	public static void main(String[] args) throws Exception {

		String addPath = "D://DMPSampleAddresses_10K.csv";

		_CountStatusStruct[] matchCount = new _CountStatusStruct[18];
		for (int i = 0; i < matchCount.length; i++) {
			matchCount[i] = new _CountStatusStruct();
		}

		StringBuffer fullhtml = new StringBuffer();

		ProcessState processState = new ProcessState();
		CsvListReader csvReader = new CsvListReader(new FileReader(addPath),
				CsvPreference.STANDARD_PREFERENCE);

		List<String> caRow = null;
		int i1 = 0;
		int i2 = 0;
		int i3 = 0;
		while ((caRow = csvReader.read()) != null) {

			String ipState = caRow.get(3).trim();

			if (TEST_ONLY_FOR_STATE != null
					&& !TEST_ONLY_FOR_STATE.contains(ipState))
				continue;

			processState.totalProcessed++;

			String ipId = caRow.get(0).trim();

			if (ONLY_TEST_ID != null && !ipId.equals(ONLY_TEST_ID))
				continue;

			String ipAddress = getCsvVal(caRow, 1).replaceAll("\\s+|/", " ")
					.toUpperCase();
			String ipCity = getCsvVal(caRow, 2).toUpperCase();
			String ipZip = getCsvVal(caRow, 4).toUpperCase();

			String caStreetNumber = getCsvVal(caRow, 10);
			String caPreDirectional = getCsvVal(caRow, 11);
			String caStreetName = getCsvVal(caRow, 12);
			String caStreetSuffix = getCsvVal(caRow, 13);
			String caPostDirectional = getCsvVal(caRow, 14);
			String caSecondaryDesignation = getCsvVal(caRow, 15);
			String caSecondaryNumber = getCsvVal(caRow, 16);
			String caCity = getCsvVal(caRow, 17);
			String caState = getCsvVal(caRow, 18);

			String caZip = getCsvVal(caRow, 20);
			String caAddon = getCsvVal(caRow, 21);

			if (StrUtil.isEmpty(caState) || caState.length() != 2
					|| ipState.toUpperCase().equals("TA")) {
				continue;
			}

			ipAddress = ipAddress.replace("/", "");
			AddressStruct addStruct = AddressCorrector
					.corrUsingAppropriateIndex(ipAddress, "", ipCity, ipState,
							ipZip);

			String fullCAOutput = appendString(caRow, 10, 18);
			String fullSHOutput = addStruct.toFullAddressString();

			if (!fullCAOutput.equals(fullSHOutput)) {
				U.log(" Input Address     \t:" + ipAddress + ", " + ipCity
						+ ", " + ipZip + ", " + ipState);
				U.log("fullCAOutput:" + fullCAOutput + "---");
				U.log("fullSHOutput:" + fullSHOutput + "---");
			}

			StringBuffer rowhtml = new StringBuffer();

			rowhtml.append("<tr>");

			rowhtml.append("<td>" + ipId + "(" + addStruct.hitScore + ")</td>");
			rowhtml.append("<td>" + ipAddress + "</td>");
			rowhtml.append("<td>" + ipCity + "</td>");
			rowhtml.append("<td>" + ipZip + "</td>");
			rowhtml.append("<td>" + ipState + "</td>");

			int matchIndex = 5;

			createCell(rowhtml, addStruct, fullCAOutput, fullSHOutput, ipState,
					matchCount[matchIndex++], null);

			createCell(rowhtml, addStruct, caStreetNumber,
					addStruct.getHouseNumber(), ipState,
					matchCount[matchIndex++], null);
			createCell(rowhtml, addStruct, caPreDirectional,
					AddColumns.PREDIRABRV, ipState, matchCount[matchIndex++]);
			createCell(rowhtml, addStruct, caStreetName, AddColumns.NAME,
					ipState, matchCount[matchIndex++]);
			createCell(rowhtml, addStruct, caStreetSuffix,
					AddColumns.SUFTYPABRV, ipState, matchCount[matchIndex++]);
			createCell(rowhtml, addStruct, caPostDirectional,
					AddColumns.SUFDIRABRV, ipState, matchCount[matchIndex++]);
			createCell(rowhtml, addStruct, caSecondaryDesignation,
					addStruct.getUnitType(), ipState, matchCount[matchIndex++],
					null);
			createCell(rowhtml, addStruct, caSecondaryNumber,
					addStruct.unitNumber, ipState, matchCount[matchIndex++],
					null);
			createCell(rowhtml, addStruct, caCity, AddColumns.CITY, ipState,
					matchCount[matchIndex++]);

			createCell(rowhtml, addStruct, caZip, AddColumns.ZIP, ipState,
					matchCount[matchIndex++]);
			createCell(rowhtml, addStruct, caAddon, addStruct.getZip4(),
					ipState, matchCount[matchIndex++], null);

			rowhtml.append("</tr>");

			if (!fullCAOutput.equals(fullSHOutput)) {
				fullhtml.append(rowhtml);
			}

			U.disp(addStruct);

		}

		csvReader.close();

		U.log("***********************************************");
		U.log(" cassMatch:" + processState.cassMatch);
		U.log(" iAmExtactMatch:" + processState.iAmExtactMatch);
		U.log(" iAmBetterMatch:" + processState.iAmBetterMatch);
		U.log(" iAmSoWrongMatch:" + processState.iAmSoWrongMatch);
		U.log(" Total Processed:" + processState.totalProcessed);
		U.log(" CASS compatibility:"
				+ (processState.cassMatch * 100 / processState.totalProcessed)
				+ "%");
		U.log(" iAmBetterMatch compatibility:"
				+ (processState.iAmBetterMatch * 100 / processState.totalProcessed)
				+ "%");
		U.log(" iAmSoWrongMatch compatibility:"
				+ (processState.iAmSoWrongMatch * 100 / processState.totalProcessed)
				+ "%");
		U.log(" iAmExtactMatch compatibility:"
				+ (processState.iAmExtactMatch * 100 / processState.totalProcessed)
				+ "%");

		double endTime = System.currentTimeMillis();
		U.log(" Time Per Address:"
				+ ((endTime - processState.startTime) / processState.totalProcessed));

		StringBuffer headerHtml = new StringBuffer();
		headerHtml.append("<tr>");

		for (_CountStatusStruct countStruct : matchCount) {
			headerHtml.append("<td>");

			{
				if (countStruct.title != null)
					headerHtml.append("<br/><br/>*** " + countStruct.title);
				headerHtml.append(createHeaderCountVal("Exact Match",
						countStruct.exactCAMatchCount,
						processState.totalProcessed));
				headerHtml.append(createHeaderCountVal("CA is Better",
						countStruct.caIsBetterMatchCount,
						processState.totalProcessed));
				headerHtml.append(createHeaderCountVal("Shatam is Better",
						countStruct.shatamIsBetterMatchCount,
						processState.totalProcessed));
				headerHtml.append(createHeaderCountVal("Inconclusive",
						countStruct.inconclusiveMatchCount,
						processState.totalProcessed));

			}
			headerHtml.append("</td>");

		}

		headerHtml.append("</tr>");

		fullhtml.insert(0, headerHtml);
		fullhtml.insert(0, "<table border=1 callpadding=0 cellspacing=0>");
		fullhtml.append("</table>");

		PrintWriter out = new PrintWriter("C:\\disposable\\delete\\op.html");
		out.println(fullhtml.toString());
		out.close();

	}

	private static String appendString(List<String> caRow, int st, int end) {

		StringBuffer buf = new StringBuffer();
		for (int i = st; i <= end; i++) {
			String s = caRow.get(i).trim();
			if (s.length() > 0) {
				buf.append(s).append(" ");
			}
		}
		return buf.toString().trim();
	}

	private static StringBuffer createHeaderCountVal(String h,
			int exactCAMatchCount, int totalProcessed) {
		StringBuffer buf = new StringBuffer();
		if (exactCAMatchCount > 0) {
			String per = Math.abs(exactCAMatchCount * 100 / totalProcessed)
					+ "%";
			buf.append("<br/><nobr>" + h + ":<b>" + exactCAMatchCount + " ("
					+ per + ")</b></nobr>");

		}
		return buf;
	}

	private static void createCell(StringBuffer html, AddressStruct addStruct,
			String v1, AddColumns col, String state,
			_CountStatusStruct matchCount) {
		String v2 = val(addStruct, col);

		if (!v1.equals(v2)) {
			if (col.name().equalsIgnoreCase("PRETYPABRV")) {

			} else if (col.name().startsWith("PRE")) {
				v2 = AbbrReplacement.getAbbr(v2, AbbrReplacement.PREFIX, state);
			} else if (col.name().startsWith("SUF")) {
				v2 = AbbrReplacement.getAbbr(v2, AbbrReplacement.SUFFIX, state);

			}
			if (!StrUtil.isEmpty(v2))
				v2 = v2.toUpperCase();
		}

		createCell(html, addStruct, v1, v2, state, matchCount, col);

	}

	private static boolean createCell(StringBuffer html,
			AddressStruct addStruct, String caVal, String shatamVal,
			String state, _CountStatusStruct matchCount, AddColumns col) {

		if (col != null)
			matchCount.title = col.name();

		final String SHATAM_IS_WRONG_COLOR = "red";
		final String SHATAM_IS_BETTER_COLOR = "green";
		final String BOTH_ARE_WRONG_COLOR = "red";
		final String BOTH_ARE_RIGHT_COLOR = "white";
		final String INCONCLUSIVE_COLOR = "gray";

		if (StrUtil.isEmpty(caVal))
			caVal = "&nbsp;";

		if (StrUtil.isEmpty(shatamVal))
			shatamVal = "&nbsp;";
		else
			shatamVal = shatamVal.toUpperCase();

		boolean exactMatch = caVal.equals(shatamVal);

		String bgColor;

		if (exactMatch) {
			bgColor = BOTH_ARE_RIGHT_COLOR;
			matchCount.exactCAMatchCount++;
		} else if (addStruct.getHouseNumber().contains("&")) {
			matchCount.shatamIsBetterMatchCount++;
			bgColor = SHATAM_IS_BETTER_COLOR;
		} else {
			String ip = null;

			if (col == AddColumns.NAME) {
				ip = " " + addStruct.inputAddress.toUpperCase() + " ";
			} else if (col == AddColumns.CITY) {
				if (addStruct.getQueryStruct() != null)
					ip = " " + addStruct.getQueryCity() + " ";
			} else if (col == AddColumns.ZIP) {
				if (addStruct.getQueryStruct() != null)
					ip = " " + addStruct.getQueryZip() + " ";
			}

			bgColor = SHATAM_IS_WRONG_COLOR;

			if (ip != null) {

				String shatamFoundVal = addStruct.get(col).toUpperCase();

				U.log("XXXXXXXXXXXXXX   ip:" + ip + "  shatamFoundVal:"
						+ shatamFoundVal);

				if (addStruct.get(AddColumns.NAME).toUpperCase()
						.startsWith("RR")) {

					matchCount.shatamIsBetterMatchCount++;
					bgColor = SHATAM_IS_BETTER_COLOR;
				} else if (ip.contains(shatamFoundVal)
						&& StrUtil.isEmpty(caVal)) {
					matchCount.shatamIsBetterMatchCount++;
					bgColor = SHATAM_IS_BETTER_COLOR;
				} else if (ip.contains(shatamFoundVal)
						&& !caVal.contains(shatamFoundVal)) {

					matchCount.shatamIsBetterMatchCount++;
					bgColor = SHATAM_IS_BETTER_COLOR;
				} else if (ip.contains(caVal) && !ip.contains(shatamVal)) {
					bgColor = SHATAM_IS_WRONG_COLOR;
					matchCount.caIsBetterMatchCount++;
				} else {
					U.log("@@@@@@@@@ INCONCLUSIVE caVal:" + caVal
							+ "  shatamVal:" + shatamVal
							+ " addStruct.inputAddress:"
							+ addStruct.inputAddress);
					bgColor = INCONCLUSIVE_COLOR;
					matchCount.inconclusiveMatchCount++;
				}
			}
		}

		String dispVal = BOTH_ARE_RIGHT_COLOR.equals(bgColor) ? caVal : (caVal
				+ " / " + shatamVal);

		html.append("<td style='background-color:" + bgColor + ";'>" + dispVal
				+ "</td>");

		return exactMatch;

	}

	private static String val(AddressStruct addStruct, AddColumns name) {
		if (name == AddColumns.NAME
				&& addStruct.contains(AddColumns.PRETYPABRV)) {

			return addStruct.get(AddColumns.PRETYPABRV) + " "
					+ addStruct.get(AddColumns.NAME);
		}

		return addStruct.get(name).toUpperCase();

	}

	private static String getCsvVal(List<String> caRow, int i) throws Exception {

		return caRow.get(i).trim().toUpperCase().replaceAll("\\s+", " ");

	}

}
