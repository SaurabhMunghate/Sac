package com.shatam.main;

import java.util.ArrayList;

import com.shatam.model.AddressStruct;
import com.shatam.util.U;

public class ImplementMultiThread extends Thread{

	/*public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public void run (){
		org.json.JSONArray arr = new org.json.JSONArray(textEntered);

		org.json.JSONArray ouputArry = new org.json.JSONArray();

		
        
		U.log("Length of arr.."+arr.length());
		
		
		for (int i = 0; i < arr.length(); i++) {
			org.json.JSONArray innerArr = arr.getJSONArray(i);
			String address1 = innerArr.getString(0);
			String address2 = innerArr.getString(1);
			String city = innerArr.getString(2);
			String state = innerArr.getString(3);
			String zip = innerArr.getString(4);
			
             U.log("Address:"+address1); 
             U.log("Zip" +zip);
             
			ArrayList<AddressStruct> addStruct = CustomAddressCorrector
					.corrUsingAppropriateIndex(address1, address2, city, state,
							zip);

			org.json.JSONArray outputJson = addToJson(addStruct, 3,
					3);
			
           
			
			ouputArry.put(outputJson);

		}
		
		//outputObj.put(ouputArry);

		//return ouputArry;
		// return null;
*/	//}

	
}
