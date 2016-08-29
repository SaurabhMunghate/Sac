package com.shatam.main;

public class InputSchema implements Comparable<InputSchema> {

	public String address1;
	public String address2;
	public String city;
	public String state;
	public String zip;

	@Override
	public int compareTo(InputSchema objectInput) {

		return objectInput.state.compareTo(this.state);

	}

}
