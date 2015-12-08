package com.data.search;

import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.AddressCorrector;
import com.shatam.util.U;

public class Search {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		//1309	NULL	NULL	BERLIN	ST  	NULL	TRLR	4	CASTROVILLE	TX	78009
		//625 ORE STREET			
		String address1 = " 625 ORE STREET        ";
        String address2 = "";
        String city = "BOWMANSTOWN";
        String state = "PA";
        String zip = "18030";// correct 85537
        long start= System.currentTimeMillis();
        AddressStruct addStruct = AddressCorrector.corrUsingAppropriateIndex(address1,"",city,state,zip);
        U.log("FOUND ADDRESS:::"+addStruct.toFullAddressString());
        long end=System.currentTimeMillis();
        U.log(start-end);
        U.log("FOUND:"+ addStruct.toOnlyStreet());
        U.log(" addStruct:" + AbstractIndexType.NORMAL.buildQuery(addStruct));
        U.log(" hitScore:" + addStruct.hitScore);
      
	}

}
