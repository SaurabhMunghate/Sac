package com.shatam.test;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Caverphone1;
import org.apache.commons.codec.language.ColognePhonetic;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.RefinedSoundex;
//import org.apache.commons.codec.language.MatchRatingApproachEncoder;
import org.apache.commons.codec.language.Metaphone;
//import org.apache.commons.codec.language.Nysiis;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.bm.NameType;
import org.apache.commons.codec.language.bm.PhoneticEngine;
import org.apache.commons.codec.language.bm.RuleType;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.shatam.util.DistanceMatchForResult;
import com.shatam.util.U;
//import org.apache.commons.codec.language.

public class TestCodecNew implements StringEncoder {

public static void main(String args[]){
		
		/*String sampleString = "wood";
		
		sampleString = "POMERENi";
		
		sampleString = "ANDERSoN";
		sampleString = "NEEDLeS";
		//LEIGHFIELD VALLUY
		sampleString = "LEIGHFIELD VALLUY";
		//25856 RAWLEY SPRuNGS dr
		sampleString = "SPRuNGS";
		
		sampleString = "KYNNRIVER";
		
		
		
		sampleString = "POUNT";
		
		sampleString = "RIDGu";
		sampleString = "ROYCHEIK";
		sampleString = "to";
		sampleString = "GALLIEFORD";
		//ALUM RIDGE
		sampleString = "LEEJACKSIN";
		
		sampleString = "korieville";
		
		sampleString = "storm bord";
		sampleString = "red feathar"; */
				
		/*U.log("KirtiMisal");
		ColognePhonetic obj3 = new ColognePhonetic();
		 U.log(obj3.encode("kirti"));
		Metaphone obj2 = new Metaphone();
		 U.log(obj2.encode("kirti"));
		RefinedSoundex obj1 = new RefinedSoundex();
		 U.log(obj1.encode("kirti"));
		Caverphone1 obj4 = new Caverphone1();
		 U.log(obj4.encode("kirti"));
		DoubleMetaphone  obj5 = new DoubleMetaphone();
		 U.log(obj5.encode("kirti"));
		// Daitch-Mokotoff Soundex
		*/
		
		//String
		
		//PhoneticEngine obj = new PhoneticEngine(NameType.GENERIC, RuleType.EXACT, true);
		// System.arraycopy(this.value, paramInt + 1, this.value, paramInt, this.count - paramInt - 1);
		/*String str = "rakesh";
		
		
		U.log("Address Varification: " + str);
		
        StringBuilder build = new StringBuilder("me");
        build.deleteCharAt(1);
		U.log(build);*/
		
		//Nysiis obj = new Nysiis();
		
		 //POMERENi
		/*Soundex obj = new Soundex();
		//MatchRatingApproachEncoder obj =  new MatchRatingApproachEncoder(); 
		//Metaphone obj = new Metaphone();
		//Caverphone1 obj = new Caverphone1();
		//DoubleMetaphone  obj = new DoubleMetaphone();
		 U.log(obj.encode(sampleString));
		 
		 
		 //sampleString = "woods";
		 
		 sampleString = "POMERENe";
		 sampleString = "ANDERSuN";
		 sampleString = "NEEDLiS";
		 sampleString = "LEIGHFIELD VALLeY";
		 sampleString = "SPRiNGS";
		 sampleString = "lYNNRIVER";
		
		
		 
		 sampleString = "POiNT";
		
		 sampleString = "RIDGe";
		 sampleString = "ROYCHEeK";
		 sampleString = "too";
		 sampleString = "HALLIEFORD";
		 sampleString = "PINETREE";
		 sampleString = "bHAKESH";
		 
		 sampleString = "LEEJACKSON";
		 
		 sampleString = "lorieville";
		 
		 sampleString = "storm bird";
		 
		 sampleString = "red feather";*/
		 
		// U.log(obj.encode(sampleString));
	int t=(int) System.currentTimeMillis();
		 JaroWinkler algorithm = new JaroWinkler();
		   U.log(algorithm.getSimilarity("RR 1".toLowerCase(),"rr".toLowerCase()));
		   if(algorithm.getSimilarity("96782","96782")==1.0){
			   U.log("KIRTI ");
		   }
		   int p=(int) System.currentTimeMillis();
		   
		   int finalTime=p-t;
		   U.log(finalTime);
	}

@Override
public Object encode(Object arg0) throws EncoderException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public String encode(String arg0) throws EncoderException {
	// TODO Auto-generated method stub
	return null;
}
	
}
