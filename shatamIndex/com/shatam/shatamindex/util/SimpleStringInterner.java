/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public class SimpleStringInterner extends StringInterner {

	private static class Entry {
		final private String str;
		final private int hash;
		private Entry next;

		private Entry(String str, int hash, Entry next) {
			this.str = str;
			this.hash = hash;
			this.next = next;
		}
	}

	private final Entry[] cache;
	private final int maxChainLength;

	public SimpleStringInterner(int tableSize, int maxChainLength) {
		cache = new Entry[Math.max(1, BitUtil.nextHighestPowerOfTwo(tableSize))];
		this.maxChainLength = Math.max(2, maxChainLength);
	}

	@Override
	public String intern(String s) {
		int h = s.hashCode();

		int slot = h & (cache.length - 1);

		Entry first = this.cache[slot];
		Entry nextToLast = null;

		int chainLength = 0;

		for (Entry e = first; e != null; e = e.next) {
			if (e.hash == h && (e.str == s || e.str.compareTo(s) == 0)) {

				return e.str;
			}

			chainLength++;
			if (e.next != null) {
				nextToLast = e;
			}
		}

		s = s.intern();
		this.cache[slot] = new Entry(s, h, first);
		if (chainLength >= maxChainLength) {

			nextToLast.next = null;
		}
		return s;
	}
}