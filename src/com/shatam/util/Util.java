package com.shatam.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.shatam.data.USPSCityModel;
import com.shatam.data.USPSUtil;


public class Util {

    public static String match(String txt, String findPattern) {

        Matcher m = Pattern.compile(findPattern, Pattern.CASE_INSENSITIVE).matcher(txt);
        while (m.find()) {
            String val = txt.substring(m.start(), m.end());
            val = val.trim();

            val = val.replaceAll("\\s+", " ").trim();
            return val;
        }
        return null;
    }// match


    public static String match(String html, String expression, int groupNum) {

        Matcher m = Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(html);

        if (groupNum <= 0) {
            if (m.find()) {
                return m.group();
            }
            else {
                return null;
            }
        }

        if (m.find(groupNum)) {
        	try{
            return m.group(groupNum).trim();
        	}
        	catch(Exception e){
        		 return null;
        	}
        }
        else {
            return null;
        }
    }


    public static ArrayList<String> matchAll(String html, String expression, int groupNum) {

        Matcher m = Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(html);


        ArrayList<String> list = new ArrayList<String>();

        while (m.find()) {
            //Util.log(m.group(groupNum));
            list.add(m.group(groupNum).trim());
        }
        return list;

    }


    public static String toString(int[] arr) {

        String s = "";
        for (int i : arr) {
            if (s.length() > 1)
                s += ",";
            s += i;
        }
        return s;
    }


    public static void log(Object o) {

        System.out.println("" + o);
    }

    public static HashMap<String, HashSet<String>> getCityList() {
    	
    	
		return zipToCity;
    	
    }
    
    
    
    public static HashMap<String, HashSet<String>> getCity() 
    {
  

        if (zipToCity.size() == 0)
        {
            try {
				readFile(System.getProperty("user.dir") +"/Settings/ZIP_CODES_2.txt");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            try {
				readUSPSCityStateFile();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
/*File file = new File("C:\\cache\\City_Observer.txt");
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(
					new FileWriter(
							file,
							true));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Set s = cityObserver.keySet();
		 java.util.Iterator ir = s.iterator();
		  while (ir.hasNext()) {
		    String key = (String) ir.next();
		    key=key.replace("&#039;", " ");
		    Object value = cityObserver.get(key);
		  
		try {
			writer.append(key+"  ="+value.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  }
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
       
        return zipToCity;

    }
    private static void readUSPSCityStateFile() throws Exception
    {
       // String cityStateFile = System.getProperty("user.dir") + "\\settings\\ctystate.txt";
        FileInputStream fstream = new FileInputStream(System.getProperty("user.dir") +"/Settings/ctystate.txt");

        byte[] data = new byte[129];

        while (fstream.read(data) > 0)
        {
            if (data[0] == 'S'){
                //OLD ZIP 05        02   06
                //NEW ZIP  05        11   15
                String labelZip = new String(data, 1, 5).trim().toUpperCase();
                String combinedZip = new String(data, 6, 5).trim().toUpperCase();
                if (labelZip.startsWith("724") || combinedZip.startsWith("724") )
                {
                    U.log("*********** labelZip:"+ labelZip + " combinedZip:"+combinedZip);
                }
            }
            if (data[0] != 68)
                continue;
            USPSCityModel rec = new USPSCityModel(data);
            //U.log("zip:"+rec.zip + "\t cityStateKey:"+ rec.cityStateKey + "\t cityStateName:"+rec.cityStateName+ "\t cityStateNameAbbr:"+rec.cityStateNameAbbr);

            HashSet<String> cityArr = zipToCity.get(rec.zip);
            if (cityArr == null)
                cityArr = new HashSet<String>();

            
            cityArr.add(rec.cityStateName.trim().toUpperCase());
            zipToCity.put(rec.zip, cityArr);
            if(rec.cityStateName.trim().split(" ").length>1)
            cityObserver.put(rec.cityStateName.trim().replace(" ", "").toUpperCase(),rec.cityStateName.trim().toUpperCase());
        }//while 
        fstream.close();

    }

    public static HashMap<String, HashSet<String>> zipToCity = new HashMap<String, HashSet<String>>();
    public static HashMap<String, String> cityObserver = new HashMap<String, String>();
    private static void readFile(String file) throws IOException
    {

        // Open the file that is the first
        // command line parameter
        FileInputStream fstream = new FileInputStream(file);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        // Read File Line By Line
        while ((strLine = br.readLine()) != null)
        {
            String[] arr = strLine.split(",");

            String zip = numToStringZip(Integer.parseInt(arr[0]));
            String city = arr[1].trim().toUpperCase();

            HashSet<String> cityArr = zipToCity.get(zip);
            if (cityArr == null)
            {
                cityArr = new HashSet<String>();
            }
            cityArr.add(city);
            zipToCity.put(zip, cityArr);
            if(city.trim().split(" ").length>1)
                cityObserver.put(city.trim().replace(" ", "").toUpperCase(),city.trim().toUpperCase());
            // Print the content on the console
            // System.out.println(zip + "=" + city);
        }

        // Close the input stream
        in.close();

    }
    private static String numToStringZip(int z)
    {
        String zip = String.format("%05d", z);
        return zip;
    }
}