package com.shatam.zip.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MultiMap;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.shatam.main.ThreadedSAC;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.AbbrReplacement;
import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.U;

public class RecordSelector {

	public static HashMap<String, String> abbrv = null;

	static {
		abbrv = ThreadedSAC.abbrv;
	}

	public static void main(String args[]) {
		String str = "[\"fake_id_value\",\"18831 von karman\",\"address2\",\"\",\"state\",\"92612\"]";
	}

	public void sortAndgetHighScoreRecords(MultiMap m,
			Map<Integer, List<String>> groupMap) throws Exception {		
		for (Entry<Integer, List<String>> rec : groupMap.entrySet()) {

			ArrayList<AddressStruct> addStruct = null;
			// Group Id			
			List<String> range = rec.getValue();
			// If the zip belongs to one city only
			if (range.size() == 1) {
				U.log("Zip belongs to one city only");
				continue;
			}

			// Range Loop if zip points to multiple cities
			// U.log("range\t" + range);
			float maxScore = 0;
			int id = -1;
			for (int i = 0; i < range.size(); i++) {

				List list = (List) m.get(range.get(i));
				addStruct = (ArrayList<AddressStruct>) list.get(0);

				if (addStruct == null || addStruct.size() == 0) {
					continue;
				}

				float jaroPer = 0;
				for (AddressStruct current : addStruct) {
					jaroPer += calculateScoring(current) * 100;
				}

				// If user select o/p count more than one ( outputCount > 1)
				// Then take avg of each address struct.
				if (addStruct.size() > 1) {
					jaroPer = jaroPer / addStruct.size();
				}

				if (maxScore < jaroPer) {
					maxScore = jaroPer;
					id = Integer.parseInt(range.get(i));
				}
			}
			if (id != -1) {

				for (int i = 0; i < range.size(); i++) {
					if (id != Integer.parseInt(range.get(i))) {						
						m.remove((Object) range.get(i));
					}
				}

			}
			// System.out.println("MaxScore:\t" + maxScore + "\tID:\t" + id);
		}
	}

	private float calculateScoring(AddressStruct addstruct) throws Exception {
		float per = 0;
		int persentage;

		if (addstruct.get(AddColumns.NAME).length() == 0) {
			return 0;
		}
		String foundaddress = DistanceMatchForResult
				.getCompleteStreet(addstruct).toLowerCase()
				.replace(addstruct.unitNumber, "");

		String inputAddress = addstruct.inputAddress.replace(
				addstruct.getHouseNumber(), "");
		if (com.shatam.util.Util
				.match(inputAddress.toLowerCase(),
						"(zeroeth|first|second|third|fourth|fifth|sixth|seventh|eighth|nineth)") != null
				&& com.shatam.util.Util.match(inputAddress.toLowerCase(),
						"\\d(th|st|nd|rd)") != null)
			inputAddress = inputAddress.toLowerCase().replaceAll(
					"\\d(th|st|nd|rd)", "");

		String foundcity = addstruct.get(AddColumns.CITY).toLowerCase();
		String inputCity = addstruct.getQueryCity();
		String state = addstruct.getState();
		String foundZip = addstruct.get(AddColumns.ZIP);
		String inputZip = addstruct.getQueryZip();
		StringBuffer buf = new StringBuffer();
		for (String s : inputAddress.split(" ")) {
			String result = standrdForm(s, addstruct.getState());
			buf.append(result);
			buf.append(" ");
		}
		String inputStreetAbrv = buf.toString().replace("  ", " ")
				.toLowerCase();
		String completeInputAddress = inputStreetAbrv.trim() + " "
				+ inputCity.trim() + " " + state.trim() + " " + inputZip.trim();
		String completeOutputAddress = foundaddress.trim() + " "
				+ foundcity.trim() + " " + state.trim() + " " + foundZip.trim();
		JaroWinkler algorithm = new JaroWinkler();
		float Matchingper = algorithm.getSimilarity(completeInputAddress
				.toLowerCase().replace("  ", " "), completeOutputAddress
				.toLowerCase().replace("  ", " "));

		return Matchingper;
	}

	private static String standrdForm(String s, String state) throws Exception {

		String val;
		if (s.trim().length() > 0) {
			val = abbrv.get(s.toUpperCase());

		} else
			val = AbbrReplacement.getFullAddress(s, state);

		if (val == null)
			val = AbbrReplacement.getFullAddress(s, state);

		return val;
	}
}
