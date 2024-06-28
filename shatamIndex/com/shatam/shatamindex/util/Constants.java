/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import com.shatam.shatamindex.ShatamPackage;

public final class Constants {
	private Constants() {
	}

	public static final String JAVA_VERSION = System
			.getProperty("java.version");

	@Deprecated
	public static final boolean JAVA_1_1 = JAVA_VERSION.startsWith("1.1.");

	@Deprecated
	public static final boolean JAVA_1_2 = JAVA_VERSION.startsWith("1.2.");

	@Deprecated
	public static final boolean JAVA_1_3 = JAVA_VERSION.startsWith("1.3.");

	public static final String OS_NAME = System.getProperty("os.name");

	public static final boolean LINUX = OS_NAME.startsWith("Linux");

	public static final boolean WINDOWS = OS_NAME.startsWith("Windows");

	public static final boolean SUN_OS = OS_NAME.startsWith("SunOS");

	public static final boolean MAC_OS_X = OS_NAME.startsWith("Mac OS X");

	public static final String OS_ARCH = System.getProperty("os.arch");
	public static final String OS_VERSION = System.getProperty("os.version");
	public static final String JAVA_VENDOR = System.getProperty("java.vendor");

	public static final boolean JRE_IS_64BIT;
	public static final boolean JRE_IS_MINIMUM_JAVA6;
	public static final boolean JRE_IS_MINIMUM_JAVA7;
	static {

		final String x = System.getProperty("sun.arch.data.model");
		if (x != null) {
			JRE_IS_64BIT = x.indexOf("64") != -1;
		} else {
			if (OS_ARCH != null && OS_ARCH.indexOf("64") != -1) {
				JRE_IS_64BIT = true;
			} else {
				JRE_IS_64BIT = false;
			}
		}

		boolean v6 = true;
		try {
			String.class.getMethod("isEmpty");
		} catch (NoSuchMethodException nsme) {
			v6 = false;
		}
		JRE_IS_MINIMUM_JAVA6 = v6;

		boolean v7 = true;
		try {
			Throwable.class.getMethod("getSuppressed");
		} catch (NoSuchMethodException nsme) {
			v7 = false;
		}
		JRE_IS_MINIMUM_JAVA7 = v7;
	}

	private static String ident(final String s) {
		return s.toString();
	}

	public static final String SHATAM_MAIN_VERSION = ident("3.5");

	public static final String SHATAM_VERSION;
	static {
		Package pkg = ShatamPackage.get();
		String v = (pkg == null) ? null : pkg.getImplementationVersion();
		if (v == null) {
			v = SHATAM_MAIN_VERSION + "-SNAPSHOT";
		} else if (!v.startsWith(SHATAM_MAIN_VERSION)) {
			v = SHATAM_MAIN_VERSION + "-SNAPSHOT " + v;
		}
		SHATAM_VERSION = ident(v);
	}
}
