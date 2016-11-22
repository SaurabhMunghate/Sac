/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import com.shatam.shatamindex.analysis.NumericTokenStream;
import com.shatam.shatamindex.document.NumericField;
import com.shatam.shatamindex.search.NumericRangeFilter;
import com.shatam.shatamindex.search.NumericRangeQuery;

public final class NumericUtils {

	private NumericUtils() {
	}

	public static final int PRECISION_STEP_DEFAULT = 4;

	public static final char SHIFT_START_LONG = (char) 0x20;

	public static final int BUF_SIZE_LONG = 63 / 7 + 2;

	public static final char SHIFT_START_INT = (char) 0x60;

	public static final int BUF_SIZE_INT = 31 / 7 + 2;

	public static int longToPrefixCoded(final long val, final int shift,
			final char[] buffer) {
		if (shift > 63 || shift < 0)
			throw new IllegalArgumentException(
					"Illegal shift value, must be 0..63");
		int nChars = (63 - shift) / 7 + 1, len = nChars + 1;
		buffer[0] = (char) (SHIFT_START_LONG + shift);
		long sortableBits = val ^ 0x8000000000000000L;
		sortableBits >>>= shift;
		while (nChars >= 1) {

			buffer[nChars--] = (char) (sortableBits & 0x7f);
			sortableBits >>>= 7;
		}
		return len;
	}

	public static String longToPrefixCoded(final long val, final int shift) {
		final char[] buffer = new char[BUF_SIZE_LONG];
		final int len = longToPrefixCoded(val, shift, buffer);
		return new String(buffer, 0, len);
	}

	public static String longToPrefixCoded(final long val) {
		return longToPrefixCoded(val, 0);
	}

	public static int intToPrefixCoded(final int val, final int shift,
			final char[] buffer) {
		if (shift > 31 || shift < 0)
			throw new IllegalArgumentException(
					"Illegal shift value, must be 0..31");
		int nChars = (31 - shift) / 7 + 1, len = nChars + 1;
		buffer[0] = (char) (SHIFT_START_INT + shift);
		int sortableBits = val ^ 0x80000000;
		sortableBits >>>= shift;
		while (nChars >= 1) {

			buffer[nChars--] = (char) (sortableBits & 0x7f);
			sortableBits >>>= 7;
		}
		return len;
	}

	public static String intToPrefixCoded(final int val, final int shift) {
		final char[] buffer = new char[BUF_SIZE_INT];
		final int len = intToPrefixCoded(val, shift, buffer);
		return new String(buffer, 0, len);
	}

	public static String intToPrefixCoded(final int val) {
		return intToPrefixCoded(val, 0);
	}

	public static long prefixCodedToLong(final String prefixCoded) {
		final int shift = prefixCoded.charAt(0) - SHIFT_START_LONG;
		if (shift > 63 || shift < 0)
			throw new NumberFormatException(
					"Invalid shift value in prefixCoded string (is encoded value really a LONG?)");
		long sortableBits = 0L;
		for (int i = 1, len = prefixCoded.length(); i < len; i++) {
			sortableBits <<= 7;
			final char ch = prefixCoded.charAt(i);
			if (ch > 0x7f) {
				throw new NumberFormatException(
						"Invalid prefixCoded numerical value representation (char "
								+ Integer.toHexString(ch) + " at position " + i
								+ " is invalid)");
			}
			sortableBits |= ch;
		}
		return (sortableBits << shift) ^ 0x8000000000000000L;
	}

	public static int prefixCodedToInt(final String prefixCoded) {
		final int shift = prefixCoded.charAt(0) - SHIFT_START_INT;
		if (shift > 31 || shift < 0)
			throw new NumberFormatException(
					"Invalid shift value in prefixCoded string (is encoded value really an INT?)");
		int sortableBits = 0;
		for (int i = 1, len = prefixCoded.length(); i < len; i++) {
			sortableBits <<= 7;
			final char ch = prefixCoded.charAt(i);
			if (ch > 0x7f) {
				throw new NumberFormatException(
						"Invalid prefixCoded numerical value representation (char "
								+ Integer.toHexString(ch) + " at position " + i
								+ " is invalid)");
			}
			sortableBits |= ch;
		}
		return (sortableBits << shift) ^ 0x80000000;
	}

	public static long doubleToSortableLong(double val) {
		long f = Double.doubleToLongBits(val);
		if (f < 0)
			f ^= 0x7fffffffffffffffL;
		return f;
	}

	public static String doubleToPrefixCoded(double val) {
		return longToPrefixCoded(doubleToSortableLong(val));
	}

	public static double sortableLongToDouble(long val) {
		if (val < 0)
			val ^= 0x7fffffffffffffffL;
		return Double.longBitsToDouble(val);
	}

	public static double prefixCodedToDouble(String val) {
		return sortableLongToDouble(prefixCodedToLong(val));
	}

	public static int floatToSortableInt(float val) {
		int f = Float.floatToIntBits(val);
		if (f < 0)
			f ^= 0x7fffffff;
		return f;
	}

	public static String floatToPrefixCoded(float val) {
		return intToPrefixCoded(floatToSortableInt(val));
	}

	public static float sortableIntToFloat(int val) {
		if (val < 0)
			val ^= 0x7fffffff;
		return Float.intBitsToFloat(val);
	}

	public static float prefixCodedToFloat(String val) {
		return sortableIntToFloat(prefixCodedToInt(val));
	}

	public static void splitLongRange(final LongRangeBuilder builder,
			final int precisionStep, final long minBound, final long maxBound) {
		splitRange(builder, 64, precisionStep, minBound, maxBound);
	}

	public static void splitIntRange(final IntRangeBuilder builder,
			final int precisionStep, final int minBound, final int maxBound) {
		splitRange(builder, 32, precisionStep, minBound, maxBound);
	}

	private static void splitRange(final Object builder, final int valSize,
			final int precisionStep, long minBound, long maxBound) {
		if (precisionStep < 1)
			throw new IllegalArgumentException("precisionStep must be >=1");
		if (minBound > maxBound)
			return;
		for (int shift = 0;; shift += precisionStep) {

			final long diff = 1L << (shift + precisionStep), mask = ((1L << precisionStep) - 1L) << shift;
			final boolean hasLower = (minBound & mask) != 0L, hasUpper = (maxBound & mask) != mask;
			final long nextMinBound = (hasLower ? (minBound + diff) : minBound)
					& ~mask, nextMaxBound = (hasUpper ? (maxBound - diff)
					: maxBound) & ~mask;
			final boolean lowerWrapped = nextMinBound < minBound, upperWrapped = nextMaxBound > maxBound;

			if (shift + precisionStep >= valSize || nextMinBound > nextMaxBound
					|| lowerWrapped || upperWrapped) {

				addRange(builder, valSize, minBound, maxBound, shift);

				break;
			}

			if (hasLower)
				addRange(builder, valSize, minBound, minBound | mask, shift);
			if (hasUpper)
				addRange(builder, valSize, maxBound & ~mask, maxBound, shift);

			minBound = nextMinBound;
			maxBound = nextMaxBound;
		}
	}

	private static void addRange(final Object builder, final int valSize,
			long minBound, long maxBound, final int shift) {

		maxBound |= (1L << shift) - 1L;

		switch (valSize) {
		case 64:
			((LongRangeBuilder) builder).addRange(minBound, maxBound, shift);
			break;
		case 32:
			((IntRangeBuilder) builder).addRange((int) minBound,
					(int) maxBound, shift);
			break;
		default:

			throw new IllegalArgumentException("valSize must be 32 or 64.");
		}
	}

	public static abstract class LongRangeBuilder {

		public void addRange(String minPrefixCoded, String maxPrefixCoded) {
			throw new UnsupportedOperationException();
		}

		public void addRange(final long min, final long max, final int shift) {
			addRange(longToPrefixCoded(min, shift),
					longToPrefixCoded(max, shift));
		}

	}

	public static abstract class IntRangeBuilder {

		public void addRange(String minPrefixCoded, String maxPrefixCoded) {
			throw new UnsupportedOperationException();
		}

		public void addRange(final int min, final int max, final int shift) {
			addRange(intToPrefixCoded(min, shift), intToPrefixCoded(max, shift));
		}

	}

}
