package com.test;

import com.shatam.util.U;

public class SampleTest {

	public static void main(String[] args) {

		int distanceCriteria = 96;
		float threshold = 0f;
		if(distanceCriteria >= 70 && distanceCriteria <= 100){
			threshold = distanceCriteria/100f;
			U.log("Num is valid"+threshold);
			
		}else{
			threshold = 90f/100;
			U.log("NUm is not valid"+threshold);
		}
		
	}

}
