package com.data.search;

import java.util.Iterator;
import java.util.Map;

import com.shatam.util.U;

public class CallCheckDMPFILE {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		int matched =0, cantTell=0;
		Iterator  iter=U.STATE_MAP.entrySet().iterator();
		/*while(iter.hasNext()){
			Map.Entry mapEntry = (Map.Entry) iter.next();
			System.out.println("The key is: " + mapEntry.getKey()
				+ ",value is :" + mapEntry.getValue());
			
			CheckDMPFile obj= new CheckDMPFile(mapEntry.getKey().toString());
			int arr[]=obj.main();
			matched=matched+arr[0];
			cantTell=cantTell+arr[1];
			U.log(arr[0]+"::::::::::::::::::::::::"+arr[1]);
		
			obj=null;
			System.gc();
		}*/
		U.log(matched+":::::::::Matched +++::::::::::Unmatched +++:::::"+cantTell);
		
		//CheckDMPFile obj= new CheckDMPFile(mapEntry.getKey().toString());
		CheckDMPFile obj= new CheckDMPFile();
		int arr[]=obj.main();
	
		U.log("Both Correct:-"+arr[0]+":Can't Tell:"+arr[1]+"::CASS BETTER::"+arr[2]+"::::SHATAM BETTER:::::"+arr[3]+":::APPROX MATCHED::::"+arr[4]);
	
		obj=null;
		System.gc();
	}

}
