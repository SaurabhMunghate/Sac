/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrUtil
{
    
    public static final String         WORD_DELIMETER      = "[\\s,\\.#)(]+";
    public static final String         WORD_DELIMETER_UNIT      = "[\\s,\\.#)(]*";
    private static String              _UNIT_ETRACT_SUFFIX = "[\\s,\\.:#\\-]+(.*)" + WORD_DELIMETER;
    private static String              _UNIT_ETRACT_SUFFIX_UNIT = "[\\s,\\.:#\\-]*(.*)" + WORD_DELIMETER;
    private static ArrayList<String[]> unitRegexMap        = new ArrayList<String[]>();

    static
    {
        
        unitRegexMap
                .add(new String[] {
            "po box",
            "(post office box|po draw|p\\.o\\. boxx| po boxx|p o boxx|po box|po bxo|po bx|p\\. *o\\. *b\\.|pob |p\\.o\\.bo|p\\.o\\.|p o box|post box|p o bx|firm caller|caller|drawer |lock *box|pobx|po boc|bx | bin )"
                    + _UNIT_ETRACT_SUFFIX_UNIT });

        
        unitRegexMap.add(new String[] { "apt", "(#) (.* pmb .*)" });
        unitRegexMap.add(new String[] { "apt", "(#) (.* # .*)" });
        unitRegexMap.add(new String[] { "", "((#\\s*\\d+[a-z]*\\-*\\d*))" });
        
        
        unitRegexMap.add(new String[] { "lot", "(lot)" + "[\\s#\\-]*(\\d+) *$" });

        unitRegexMap.add(new String[] { "box", WORD_DELIMETER + "(box|hbox)" + "[\\s,\\.:#\\-]*([a-z0-9]+)[\\s,\\.#)(]+"});

        
        unitRegexMap.add(new String[] { "#", "( # )" + _UNIT_ETRACT_SUFFIX_UNIT });

        unitRegexMap.add(new String[] { "apt", "(apt |apartment|aptmt|aptmnt|bld )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "ste", WORD_DELIMETER + "(suite|suit|ste )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "floor", "(floor|flr|fl )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "pmb", "(pmb )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "frnt", "(frnt)" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "flat", "(flt )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "unit", WORD_DELIMETER + "(unit|unt )" + "[\\s,\\.:#\\-]*([a-z0-9]+)[\\s,\\.#)(]+"});
        unitRegexMap.add(new String[] { "unit", WORD_DELIMETER + "(unit|unt )" + "[\\s,\\.:#\\-]*([a-z0-9]+\\-[a-z0-9]*)"});
        unitRegexMap.add(new String[] { "box", "( box )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "ph", "( ph )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "#", "(#)" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "stop", "( STOP )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "bsmt", "( basement )" + _UNIT_ETRACT_SUFFIX_UNIT });
        unitRegexMap.add(new String[] { "buzon", "(buzon )" + _UNIT_ETRACT_SUFFIX_UNIT });

        unitRegexMap.add(new String[] { "spc", "(spc | sp )" + _UNIT_ETRACT_SUFFIX_UNIT });

        unitRegexMap.add(new String[] { "trlr", "(trlr)" + _UNIT_ETRACT_SUFFIX_UNIT });
        
      

    }

    public static String fixNumericStreetSuffixes(String address)
    {

      
        {
           
            String endingWithStreetNum = extractPattern(address.trim().toLowerCase(), "\\d+ (ne|nw|se|sw|e|w|n|s|northeast|northwest|southeast|southwest|east|west|north|south) (\\d+)$", 2);
           
            if (!StrUtil.isEmpty(endingWithStreetNum))
                address = address + "th";
           
        }

        address += " ";

        String reg = null;
        String mr = null;
        
        if (!address.contains(" of the ")){
        for (int i = 19; i >= 0; i--)
        {
            String suf = U._getNumSuf(i);

           
            reg = i + "( *th|st| *nd|rd) "; 
            mr = ".*" + reg + ".*";
            if (address.matches(mr))
            {
                address = address.replaceFirst(reg, i + suf + " ");
                break;
            }

        }
        }

        
        return address.trim();
    }

    public static String[] extractApartment(String add)
    {
       
			add=add.replaceAll("#{2,}", "");
   	 
   	 
        add = " " + add.toLowerCase() + " ";

     
        for (String[] pair : unitRegexMap)
        {
            String v = StrUtil.extractPattern(add, pair[1], 2);
           

            if (!StrUtil.isEmpty(v) && (StrUtil.containsNum(v) || v.length() == 1))
            {
                String[] arr = v.trim().split(WORD_DELIMETER);
                if (arr.length >= 3 && !StrUtil.isNum(arr[arr.length - 1]))
                {
                    v = arr[0];
                }
           
                return new String[] { pair[0].trim(), v.trim() };
            }
        }

        return null;
    }

    public static String[] extractApartment(String address1, String address2)
    {
        if (StrUtil.isNum(address2))
        {
            address1 = address1 + " # " + address2;
            address2 = "";
        }

        String[] arr = extractApartment(address1);
        if (arr == null && !StrUtil.isEmpty(address2))
        {
            arr = extractApartment(address2);
        }
        if (arr == null)
        {
            arr = new String[] { "", "" };
        }

        return arr;

    }

    public static String removeUnitFromAddress(String add)
    {
        if (StrUtil.isEmpty(add))
            return "";

        add = " " + add.toLowerCase() + " ";
        for (String[] pair : unitRegexMap)
        {
        
            add = add.replaceFirst(pair[1], "");
        
        }

        return add.trim();
    }

    public static boolean isNum(String s)
    {
        try
        {
            Double.parseDouble(s);
        } catch (NumberFormatException nfe)
        {
        	
            return false;
        }
        return true;
    }

    public static String extractPattern(String input, String pattern)
    {
        return extractPattern(input, pattern, 1);
    }

    public static String extractPattern(String input, String pattern, int groupNum)
    {
        Pattern MY_PATTERN = Pattern.compile(pattern);
        Matcher m = MY_PATTERN.matcher(input);
        if (m.find())
        {
            return m.group(groupNum);
        } else
        {
            return null;
        }
    }

    public static String removeNonNumber(String phone)
    {
       
        String[] arr = phone.split("[^\\d]");
        return arr[arr.length - 1];

        
    }

    public static String[] convertToStringArr(Object[] rowObjects, int maxColns)
    {
        String[] rowStrings = new String[rowObjects.length];
        for (int j = 0; j < maxColns; j++)
        {
            rowStrings[j] = rowObjects[j].toString().trim().toLowerCase();
        }
        return rowStrings;

    }

    public static boolean containsString(String num)
    {
        Pattern p = Pattern.compile("[^\\d]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(num);
        boolean b = m.find();
        return b;

    }

    public static boolean isEmpty(String v)
    {

        return v == null || v.trim().length() == 0 || v.equals("null");
    }

    public static boolean containsNum(String v)
    {
        return v.matches(".*\\d.*");
    }

    public static ArrayList<String> getFips() throws IOException
    {
        String file = Paths.combine(Paths.SETTINGS_PATH, "FIPS.txt");
        return FileUtil.readLines(file);
    }

    public static ArrayList<String> getFipsForStateCode(String stateCode) throws IOException{
        ArrayList<String> list = new ArrayList<String>();
        
        ArrayList<String> fipsArr = getFips();
        for (String fips : fipsArr)
        {
            if (fips.startsWith(stateCode))
                list.add(fips);
        }

        return list;
    }
    

    public static boolean containsOnlyAlphaAndSpaces(String address1)
    {
        String s = address1.toLowerCase().replaceAll("[a-z ]", "");
        
        return s.length() == 0;
    }

}
