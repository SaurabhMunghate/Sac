/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public interface Bits {
	public boolean get(int index);

	public int length();

	public static final Bits[] EMPTY_ARRAY = new Bits[0];

	public static class MatchAllBits implements Bits {
		final int len;

		public MatchAllBits(int len) {
			this.len = len;
		}

		public boolean get(int index) {
			return true;
		}

		public int length() {
			return len;
		}
	}

	public static class MatchNoBits implements Bits {
		final int len;

		public MatchNoBits(int len) {
			this.len = len;
		}

		public boolean get(int index) {
			return false;
		}

		public int length() {
			return len;
		}
	}
}
