package com.exist.java;

import com.shatam.util.U;

public class JsonSchema implements Comparable<JsonSchema> 
{

	public String key;
	public String address;
	public String house_number= "";
	public String prefix_direction= "";
	public String prefix_qualifier= "";
	public String prefix_type="";
	public String street_name= "";
	public String suffix_type= "";
	public String suffix_direction= "";
	public String city= "";
	public String zip= "";
	public String state="";
	public float score;
	@Override
	public int compareTo(JsonSchema objectAdStruct) {
		
		// TODO Auto-generated method stub
		
	    Double hitsc = Double.parseDouble(this.score + "");
		Double destsc = Double.parseDouble(objectAdStruct.score + "");
		return destsc.compareTo(hitsc);
		
		
	}
}
