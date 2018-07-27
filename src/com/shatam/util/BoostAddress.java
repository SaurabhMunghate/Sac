package com.shatam.util;

public class BoostAddress {
	private String cityWeight = "^4";
	private String zipWeight = "^4";

	public BoostAddress(){
		cityWeight = "^4";
		zipWeight = "^4";
	}
	public BoostAddress(int cityBoost, int zipBoost){
		setCityWeight(cityBoost);
		setZipWeight(zipBoost);
	}

	public void setCityWeight(int weight){
		if(weight >= 2 && weight <=5){
			cityWeight = "^"+weight;
		}
	}

	public void setZipWeight(int weight){
		if(weight >= 2 && weight <=5){
			zipWeight = "^"+weight;
		}
	}

	public String getCityWeight() {
		return cityWeight;
	}

	public String getZipWeight() {
		return zipWeight;
	}

}
