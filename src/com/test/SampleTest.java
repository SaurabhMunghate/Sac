package com.test;

import com.shatam.util.AbbrReplacement;
import com.shatam.util.DistanceMatchForResult;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

public class SampleTest {

	public static void main(String[] args) throws Exception 
	{

//		int distanceCriteria = 96;
//		float threshold = 0f;
//		if(distanceCriteria >= 70 && distanceCriteria <= 100){
//			threshold = distanceCriteria/100f;
//			U.log("Num is valid"+threshold);
//			
//		}else{
//			threshold = 90f/100;
//			U.log("NUm is not valid"+threshold);
//		}
		
//		JaroWinkler algorithm = new JaroWinkler();
//		System.out.println(algorithm.getSimilarity("AVENIDA BRISA","AVENIDA ALONDRA"));
//		//System.out.println(algorithm.getSimilarityExplained("AVENIDA BRISA","AVENIDA ALONDRA Asa s"));
//		System.out.println(algorithm.getSimilarity("VIA DE SUENO", "VIA DE SANTA FE"));
//		System.out.println(algorithm.getSimilarity("234", "235"));
//		System.out.println(algorithm.getSimilarity("ST CHRISTOPHER LANE", "SAINT CHRISTOPHER LN"));
//		System.out.println(algorithm.getSimilarity("st christopher ln", "saint christopher ln"));
//		System.out.println(DistanceMatchForResult.standrdForm("Rio", "CA"));
		
		System.out.println(AbbrReplacement.getFullAddress("North via", "CA"));
		
	}

}
