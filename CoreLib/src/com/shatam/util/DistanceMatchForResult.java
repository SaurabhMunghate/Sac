package com.shatam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;

//import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;

public class DistanceMatchForResult
{
    //Commented by me
	
   /* private static boolean isMatchGoodEnough(final String name1, final AddressStruct a, final AbstractIndexType it)
            throws Exception
    {

       if (name1.length() < 2)
            return false;

        final String q1 = a.getshatamIndexQueryString();
        final String q2 = q1.replace(U.STREET_ENHANCE, "").replace(U.CITY_ENHANCE, "").replace("_", "");
        final String name2 = it.encode(name1);
      
       if (name2.length() < 2)
            return false;

       U.log("is matchfound==="+q1+"::"+name1);
       
       
        if (q1.toLowerCase().contains(name1.toLowerCase()) || q1.toLowerCase().contains(name2.toLowerCase()))
            return true;
        if (q2.toLowerCase().contains(name1) || q2.toLowerCase().contains(name2))
            return true;

        final String name3 = findLargestString(name1.split(" "));
        final String name4 = findLargestString(name2.split(" "));

        if (q1.toLowerCase().contains(name3) || q1.toLowerCase().contains(name4))
            return true;
        if (q2.toLowerCase().contains(name3) || q2.toLowerCase().contains(name4))
            return true;

    
        
        return false;
    }*///isMatchGoodEnough()    
	//******************************************************************************************************************************
	
	private static boolean isMatchGoodEnough( String name1,
			final AddressStruct a, final AbstractIndexType it, String value)
			throws Exception {

		// if(name1.toLowerCase().contains("canyon"))Thread.sleep(7000);

		U.log("name1:- " + name1);
		
		name1=name1.replaceAll("\\d+", "");

		if (name1.length() < 1)
			return false;

		
		
		String q1 = a.getLuceneQueryString();

		// @@@@@@@@@@@@@@@@@
		/*
		 * if (name1.contains("PO BOX") && value.contains("street") &&
		 * a.get(AddColumns.CITY).toUpperCase().contains("BAKERSFIELD") &&
		 * q1.contains("BAKERSFIELD")) { return true; }
		 */

		// U.log("Distance match for res___Q1: " + q1);

		final String q2 = q1.replace(U.STREET_ENHANCE, "")
				.replace(U.CITY_ENHANCE, "").replace("_", "");

		// U.log("Q2: " + q2);
		name1=name1.replaceAll("\\d+", "");

		final String name2 = it.encode(name1);
		 U.log("Name2=="+name2);
		if (name2.length() < 2)
			return false;

		if (q1.contains(name1) || q1.contains(name2))
			return true;

		
		
		if (q2.contains(name1) || q2.contains(name2))
			return true;
		
		

		 String name3 = findLargestString(name1.split(" "));
		String name4 = findLargestString(name2.split(" "));

		
		
		U.log("name3" + name3);
		U.log("Name4==" + name4);
		
		name3=name3.replaceAll("\\d+", "-null-");
		name4=name4.replaceAll("\\d+", "-null-");
		
	
		

		// name4=name3.substring(1);

		U.log("Q1" + q1);
		U.log("Q2:" + q2);
		
		

		if (q1.contains(name3) || q1.contains(name4))
			return true;

		if (q2.contains(name3) || q2.contains(name4))
			return true;

		String s="";
		
		int groupNum=1;

		        Matcher m = Pattern.compile("([A-Z]+)[\\~]?\\^5", Pattern.CASE_INSENSITIVE).matcher(" "+q1);

		        if (groupNum <= 0) {
		            if (m.find()) {
		                s= m.group();
		            }
		            else {
		               s= null;
		            }
		        }

		        if (m.find(groupNum)) {
		            s= m.group(groupNum).trim();
		        }
		        else {
		            s= null;
		        }
		   
		U.log("MATCHER=="+s);
		int jaroTime=(int) System.currentTimeMillis();
	/*	if(s!=null){
		 JaroWinkler algorithm = new JaroWinkler();
		   U.log(algorithm.getSimilarity(s,name3)); 
		   if(algorithm.getSimilarity(s,name3)>0.85)return true;
		   int jaroLastTime=(int) System.currentTimeMillis();
			U.log("jaroalgo time=="+(jaroLastTime-jaroTime));
		}*/
		
		// Create HEre another matcher usesing Double Metaphone..

		// Here I writtena another logic of matching the addresses...

		// U.log("Query String:  " + q1);
		// U.log("After Enchase it Query String:  " + q2);
//		@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
/*		Pattern pat = null;
		StringBuffer buf = new StringBuffer();

		if (value.equalsIgnoreCase("street")) {
			// pat = Pattern.compile("[A-Z]+\\^5");
			// pat = Pattern.compile("[A-Z]+~1\\^5");
			// U.log("Q1::: " + q1);

			pat = Pattern.compile("\\d+[A-Z]+~1|[A-Z]+~1");

			Matcher mat = pat.matcher(q1);

			while (mat.find()) {
				buf.append(mat.group());

			}
			if (buf.toString().isEmpty()) {
				q1 = q1.replace(a.get(AddColumns.ZIP), "");
				String cty = a.get(AddColumns.CITY).replaceAll("\\s+", "");
				q1 = q1.replace(cty.toUpperCase(), "");

				q1 = q1.replace(U.STREET_ENHANCE, "")
						.replace(U.CITY_ENHANCE, "").replace("_", "");
				q1 = q1.replaceAll("CITY|ZIP", "");

				String street = removePrefix_Suf(q1, "AZ").toUpperCase();
				street = street.replace("?", "");
				if (street.length() > 1) {
					if (street.contains(name1) || name1.contains(street))
						return true;
				}
				U.log("Street Found ::" + street);
				

			}

		}

		if (value.equalsIgnoreCase("city")) {
			pat = Pattern.compile("[A-Z]+_CITY~\\^4");

			// Alveys try to use the Interface because its faster than other
			// class.....

			Matcher mat = pat.matcher(q1);
			// StringBuffer buf = new StringBuffer();
			while (mat.find()) {

				buf.append(mat.group());

			}
		}

//		U.log(it.getFieldName().contains("k1"));
//		U.log(!buf.toString().isEmpty());
		if (it.getFieldName().contains("k1") && !buf.toString().isEmpty()) {

			// System.out.println("Name Printing Here.." + buf.toString());
			// System.out.println("Found Name: " + name1);
			String q3 = new Soundex().encode(buf.toString());

			String name5 = new Soundex().encode(name1);

			// q3=q3.substring(1);
			// U.log("name5:  " + name5);
			// Kirti made changes
			if (name5.trim().length() != 0) {
				name5 = name5.substring(1);
			}
			// U.log("Q3::::  " + q3);

			// U.log("name5:  " + name5);

			// U.log("Q3: " + q3);
			//
			// U.log("name5: " + name5);

			String numStreet = extractPattern(q1, "\\d+[A-Z]+~1");
			// U.log("NumStreet: " + numStreet);
			if (numStreet.length() == 0) {

				if (q3.contains(name5) || q3.contains(name5)) {

					return true;
				}
			}

			if (value.equalsIgnoreCase("street")) {

				String str = buf.toString().replace("~1", "").trim();
				int dist = StringUtils.getLevenshteinDistance(name1, str);

				System.out.println("Dist: " + dist);

				if (dist == 1)
					return checkDiffChar(name1, str);
				// return true; // Original Addresses...

			}

		}

		// buf=null;
		
		 * U.log("isMatchGoodEnough q1:" + q1); U.log("isMatchGoodEnough q2:" +
		 * q2); U.log("isMatchGoodEnough name1:" + name1);
		 * U.log("isMatchGoodEnough name2:" + name2);
		 
*/
		//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
		return false;
	}// isMatchGoodEnough()
	private static String removePrefix_Suf(String streetName, String state) {
		// = add.split("\\s");

		// add = "5723  44th ln";
		streetName = streetName.toLowerCase();
		String splitAdd[] = streetName.split("[^\\d\\w\\-/]");
		splitAdd = streetName.split("\\s");
		List<String> abbrArr = new ArrayList<String>();

		for (int i = 0; i < splitAdd.length; i++) {

			String str = AbbrReplacement.getAbbr(splitAdd[i], state);

			str = AbbrReplacement.getsuffixFull(str, state);
			if (str != null) {
				if (!abbrArr.contains(str)) {
					abbrArr.add(str);
					String strAbbri = AbbrReplacement.getAbbr(str, "AZ");
					if (!splitAdd[i].equalsIgnoreCase(strAbbri)) {
						strAbbri = splitAdd[i];
					}

					// U.log("\nstrabbr====="+strAbbri);

					streetName = streetName.replace(strAbbri, "");

				}
			}

		}

		for (int i = 0; i < splitAdd.length; i++) {
			// U.log("Passed String: " + splitAdd[i]);
			String str = AbbrReplacement.getAbbr(splitAdd[i], "AZ");

			str = AbbrReplacement.getsuffixFull(str, "AZ");
			if (str != null) {
				if (!abbrArr.contains(str)) {
					abbrArr.add(str);

					String strAbbri = AbbrReplacement.getAbbr(str, "AZ");
					if (!splitAdd[i].equalsIgnoreCase(strAbbri)) {
						strAbbri = splitAdd[i];
					}
					streetName = streetName.replace(strAbbri, "");

				}
			}

		}

		// String [] abbPreAndSuf = abbrArr.toArray(new String[abbrArr.size()]);

		return streetName;

	}

	private static String extractPattern(String str, String pattarn) {

		StringBuffer buf = new StringBuffer();
		Pattern pat = Pattern.compile(pattarn);
		Matcher mat = pat.matcher(str);

		// StringBuffer buf = new StringBuffer();
		while (mat.find()) {

			buf.append(mat.group());

		}

		return buf.toString();

	}

	private static boolean checkDiffChar(CharSequence cs1, CharSequence cs2) {

		if ((cs1 == null) || (cs2 == null)) {
			throw new IllegalArgumentException("String must not be null");
		}
		for (int i = 1; (i < cs1.length()) && (i < cs2.length()); i++) {
			if (cs1.charAt(i) != cs2.charAt(i)) {
				return false;
			}
		}
		return false;
	}

	//**********************************************************************************************************************************

    private static final double RETRY_IF_LOWER_THAN_SCORE = 2.0;
    private AddressStruct     addStruct = null;
    private AbstractIndexType indexType = null;

    public DistanceMatchForResult(AddressStruct a, final AbstractIndexType it)
    {
        addStruct = a;
        indexType = it;
    }

    public boolean isResultMatched() throws Exception
    {
        float score = score(addStruct) ;

        if (score> 3)
        {
        	U.log("Hitscore are greater than 3");
           // return true; 
        }

        final String foundStreet = addStruct.getFoundName();
        final String foundCity = addStruct.get(AddColumns.CITY).toUpperCase();
        final String foundZip = addStruct.get(AddColumns.ZIP).toUpperCase();
        U.log("C Found :" + foundStreet + " , " + foundCity+", "+DistanceMatchForResult.isMatchGoodEnough(foundStreet, addStruct, indexType,"street"));

        if (DistanceMatchForResult.isMatchGoodEnough(foundStreet, addStruct, indexType,"street"))
        {

          U.log("street Match***************");
            if (DistanceMatchForResult.isMatchGoodEnough(foundCity, addStruct, indexType,"city") || DistanceMatchForResult.isMatchGoodEnough(foundZip, addStruct, indexType,"zip"))
            {
                U.log("D Found :" + foundStreet + " , " + foundCity);
                return true;
            }

        }
        return false;
    }    



    /*
    private static int levenshteinDistance(String s, String t)
    {
        // degenerate cases
        if (s == t)
            return 0;
        if (s.length() == 0)
            return t.length();
        if (t.length() == 0)
            return s.length();

        // create two work vectors of integer distances
        int[] v0 = new int[t.length() + 1];
        int[] v1 = new int[t.length() + 1];

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++)
            v0[i] = i;

        for (int i = 0; i < s.length(); i++)
        {
            // calculate v1 (current row distances) from the previous row v0

            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < t.length(); j++)
            {
                int cost = (s.charAt(i) == t.charAt(j)) ? 0 : 1;
                v1[j + 1] = minimum(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost);
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            for (int j = 0; j < v0.length; j++)
                v0[j] = v1[j];
        }

        return v1[t.length()];
    }

    private static int minimum(int i, int j, int k)
    {
        return Math.min(i, Math.min(j, k));
    }

    */
    private static String findLargestString(String[] arr)
    {
        String max = "";
        for (String s : arr)
        {
            if (StrUtil.isNum(s))
                return s;

            if (s.equalsIgnoreCase("highway") || s.equalsIgnoreCase("road")||s.equalsIgnoreCase("CREEK"))
                continue;

            if (max.length() < s.length())
            {
                max = s;
            }
        }
        return max;
    }
    
    private static float score(AddressStruct a)
    {
        return a != null ? a.hitScore : 0;
    }    
}//class DistanceMatchForResult