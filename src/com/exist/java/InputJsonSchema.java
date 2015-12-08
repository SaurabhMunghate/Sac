package com.exist.java;

class InputJsonSchema implements Comparable<InputJsonSchema> {
	
	
	String address1;
	String address2;
	String city;
	String state;
	String zip;
	String key;
	

	@Override
	public int compareTo(InputJsonSchema input) {
		// TODO Auto-generated method stub

		return this.state.compareTo(input.state);

	}

}

