package com.shatam.zip.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import com.shatam.main.InputJsonSchema;

public class ZipSearch {

	public static void main(String args[]) throws JSONException, IOException {

		ZipSearch zS = new ZipSearch();
		String str = "[\"fake_id_value\",\"18831 von karman\",\"address2\",\"\",\"\",\"92612\"]";
		JSONArray arr = new JSONArray(str);

		System.out.println(zS.getJSONArray(arr));

	}

	public InputJsonSchema getInputJsonSchema(InputJsonSchema obj) {

		if (obj.zip.length() > 0) {
			try {
				List<Set<String>> lset = ZipSearchUtil.getCityState(obj.zip);
				Set<String> city = lset.get(0);
				Set<String> state = lset.get(1);
				for (String c : city) {
					obj.city = c;
					obj.state = state.toString().replaceAll("\\[|\\]", "");
					break;
				}
			} catch (IOException e) {
				System.out.println("Error in loading zip-city cache: " + e);
			}

		}
		return obj;
	}

	public List<Object> getJSONArray(JSONArray arr) throws JSONException,
			IOException {

		String state = (String) arr.get(4);
		String city = (String) arr.get(3);
		String zip = (String) arr.get(5);
		List<Object> rs = null;

		if (state.length() == 0 && city.length() == 0 && zip.length() > 0) {
			List<Set<String>> lset = ZipSearchUtil.getCityState(zip);
			if (lset != null) {
				Set<String> citySet = lset.get(0);
				Set<String> stateSet = lset.get(1);
				rs = new ArrayList<>();
				rs.add(true);
				for (String c : citySet) {
					arr.put(3, c);
					arr.put(4, stateSet.toString().replaceAll("\\[|\\]", ""));
					break;
				}
				rs.add(arr);
			}
		}
		return rs;
	}

	public List<Object> getJSONArrays(JSONArray arr) throws JSONException,
			IOException {

		String state = (String) arr.get(4);
		String city = (String) arr.get(3);
		String zip = (String) arr.get(5);
		List<Object> rs = null;
		List<JSONArray> lJsonArr = null;
		if (state.length() == 0 && city.length() == 0 && zip.length() > 0) {
			List<Set<String>> lset = ZipSearchUtil.getCityState(zip);
			if (lset != null) {
				Set<String> citySet = lset.get(0);
				Set<String> stateSet = lset.get(1);
				rs = new ArrayList<>();
				lJsonArr = new ArrayList<>();
				rs.add(true);				
				for (String c : citySet) {
					JSONArray arr1 = new JSONArray(arr.toString());					
					arr1.put(3, c);
					arr1.put(4, stateSet.toString().replaceAll("\\[|\\]", ""));
					lJsonArr.add(arr1);
				}
				rs.add(lJsonArr);
			}else
			{
				rs = new ArrayList<>();
				rs.add(true);
				return rs;
			}
		}
		return rs;
	}

}
