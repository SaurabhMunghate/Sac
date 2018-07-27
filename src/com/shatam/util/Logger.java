package com.shatam.util;

import org.apache.commons.codec.language.Soundex;

public class Logger {

	public static void main(String args[]) {

		log(Logger.class, "d");
		Soundex d = new Soundex();
		log(Logger.class,d.encode("Rhatam"));
		log(Logger.class,d.encode("Ratam"));

	}

	public static void log(Class c, Object o) {

		System.out.println(String.format("%s:\t%s\t", c.getCanonicalName(), o));

	}
	

}
