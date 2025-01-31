/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public final class BitUtil {

	private BitUtil() {
	}

	public static int pop(long x) {

		x = x - ((x >>> 1) & 0x5555555555555555L);
		x = (x & 0x3333333333333333L) + ((x >>> 2) & 0x3333333333333333L);
		x = (x + (x >>> 4)) & 0x0F0F0F0F0F0F0F0FL;
		x = x + (x >>> 8);
		x = x + (x >>> 16);
		x = x + (x >>> 32);
		return ((int) x) & 0x7F;
	}

	public static long pop_array(long A[], int wordOffset, int numWords) {

		int n = wordOffset + numWords;
		long tot = 0, tot8 = 0;
		long ones = 0, twos = 0, fours = 0;

		int i;
		for (i = wordOffset; i <= n - 8; i += 8) {

			long twosA, twosB, foursA, foursB, eights;

			{
				long b = A[i], c = A[i + 1];
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = A[i + 2], c = A[i + 3];
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long b = A[i + 4], c = A[i + 5];
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = A[i + 6], c = A[i + 7];
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursB = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long u = fours ^ foursA;
				eights = (fours & foursA) | (u & foursB);
				fours = u ^ foursB;
			}
			tot8 += pop(eights);
		}

		if (i <= n - 4) {
			long twosA, twosB, foursA, eights;
			{
				long b = A[i], c = A[i + 1];
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long b = A[i + 2], c = A[i + 3];
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}
			eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 4;
		}

		if (i <= n - 2) {
			long b = A[i], c = A[i + 1];
			long u = ones ^ b;
			long twosA = (ones & b) | (u & c);
			ones = u ^ c;

			long foursA = twos & twosA;
			twos = twos ^ twosA;

			long eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 2;
		}

		if (i < n) {
			tot += pop(A[i]);
		}

		tot += (pop(fours) << 2) + (pop(twos) << 1) + pop(ones) + (tot8 << 3);

		return tot;
	}

	public static long pop_intersect(long A[], long B[], int wordOffset,
			int numWords) {

		int n = wordOffset + numWords;
		long tot = 0, tot8 = 0;
		long ones = 0, twos = 0, fours = 0;

		int i;
		for (i = wordOffset; i <= n - 8; i += 8) {
			long twosA, twosB, foursA, foursB, eights;

			{
				long b = (A[i] & B[i]), c = (A[i + 1] & B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 2] & B[i + 2]), c = (A[i + 3] & B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long b = (A[i + 4] & B[i + 4]), c = (A[i + 5] & B[i + 5]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 6] & B[i + 6]), c = (A[i + 7] & B[i + 7]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursB = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long u = fours ^ foursA;
				eights = (fours & foursA) | (u & foursB);
				fours = u ^ foursB;
			}
			tot8 += pop(eights);
		}

		if (i <= n - 4) {
			long twosA, twosB, foursA, eights;
			{
				long b = (A[i] & B[i]), c = (A[i + 1] & B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long b = (A[i + 2] & B[i + 2]), c = (A[i + 3] & B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}
			eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 4;
		}

		if (i <= n - 2) {
			long b = (A[i] & B[i]), c = (A[i + 1] & B[i + 1]);
			long u = ones ^ b;
			long twosA = (ones & b) | (u & c);
			ones = u ^ c;

			long foursA = twos & twosA;
			twos = twos ^ twosA;

			long eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 2;
		}

		if (i < n) {
			tot += pop((A[i] & B[i]));
		}

		tot += (pop(fours) << 2) + (pop(twos) << 1) + pop(ones) + (tot8 << 3);

		return tot;
	}

	public static long pop_union(long A[], long B[], int wordOffset,
			int numWords) {

		int n = wordOffset + numWords;
		long tot = 0, tot8 = 0;
		long ones = 0, twos = 0, fours = 0;

		int i;
		for (i = wordOffset; i <= n - 8; i += 8) {

			long twosA, twosB, foursA, foursB, eights;

			{
				long b = (A[i] | B[i]), c = (A[i + 1] | B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 2] | B[i + 2]), c = (A[i + 3] | B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long b = (A[i + 4] | B[i + 4]), c = (A[i + 5] | B[i + 5]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 6] | B[i + 6]), c = (A[i + 7] | B[i + 7]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursB = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long u = fours ^ foursA;
				eights = (fours & foursA) | (u & foursB);
				fours = u ^ foursB;
			}
			tot8 += pop(eights);
		}

		if (i <= n - 4) {
			long twosA, twosB, foursA, eights;
			{
				long b = (A[i] | B[i]), c = (A[i + 1] | B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long b = (A[i + 2] | B[i + 2]), c = (A[i + 3] | B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}
			eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 4;
		}

		if (i <= n - 2) {
			long b = (A[i] | B[i]), c = (A[i + 1] | B[i + 1]);
			long u = ones ^ b;
			long twosA = (ones & b) | (u & c);
			ones = u ^ c;

			long foursA = twos & twosA;
			twos = twos ^ twosA;

			long eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 2;
		}

		if (i < n) {
			tot += pop((A[i] | B[i]));
		}

		tot += (pop(fours) << 2) + (pop(twos) << 1) + pop(ones) + (tot8 << 3);

		return tot;
	}

	public static long pop_andnot(long A[], long B[], int wordOffset,
			int numWords) {

		int n = wordOffset + numWords;
		long tot = 0, tot8 = 0;
		long ones = 0, twos = 0, fours = 0;

		int i;
		for (i = wordOffset; i <= n - 8; i += 8) {

			long twosA, twosB, foursA, foursB, eights;

			{
				long b = (A[i] & ~B[i]), c = (A[i + 1] & ~B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 2] & ~B[i + 2]), c = (A[i + 3] & ~B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long b = (A[i + 4] & ~B[i + 4]), c = (A[i + 5] & ~B[i + 5]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 6] & ~B[i + 6]), c = (A[i + 7] & ~B[i + 7]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursB = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long u = fours ^ foursA;
				eights = (fours & foursA) | (u & foursB);
				fours = u ^ foursB;
			}
			tot8 += pop(eights);
		}

		if (i <= n - 4) {
			long twosA, twosB, foursA, eights;
			{
				long b = (A[i] & ~B[i]), c = (A[i + 1] & ~B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long b = (A[i + 2] & ~B[i + 2]), c = (A[i + 3] & ~B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}
			eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 4;
		}

		if (i <= n - 2) {
			long b = (A[i] & ~B[i]), c = (A[i + 1] & ~B[i + 1]);
			long u = ones ^ b;
			long twosA = (ones & b) | (u & c);
			ones = u ^ c;

			long foursA = twos & twosA;
			twos = twos ^ twosA;

			long eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 2;
		}

		if (i < n) {
			tot += pop((A[i] & ~B[i]));
		}

		tot += (pop(fours) << 2) + (pop(twos) << 1) + pop(ones) + (tot8 << 3);

		return tot;
	}

	public static long pop_xor(long A[], long B[], int wordOffset, int numWords) {
		int n = wordOffset + numWords;
		long tot = 0, tot8 = 0;
		long ones = 0, twos = 0, fours = 0;

		int i;
		for (i = wordOffset; i <= n - 8; i += 8) {

			long twosA, twosB, foursA, foursB, eights;

			{
				long b = (A[i] ^ B[i]), c = (A[i + 1] ^ B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 2] ^ B[i + 2]), c = (A[i + 3] ^ B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long b = (A[i + 4] ^ B[i + 4]), c = (A[i + 5] ^ B[i + 5]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long b = (A[i + 6] ^ B[i + 6]), c = (A[i + 7] ^ B[i + 7]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}

			{
				long u = twos ^ twosA;
				foursB = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}

			{
				long u = fours ^ foursA;
				eights = (fours & foursA) | (u & foursB);
				fours = u ^ foursB;
			}
			tot8 += pop(eights);
		}

		if (i <= n - 4) {
			long twosA, twosB, foursA, eights;
			{
				long b = (A[i] ^ B[i]), c = (A[i + 1] ^ B[i + 1]);
				long u = ones ^ b;
				twosA = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long b = (A[i + 2] ^ B[i + 2]), c = (A[i + 3] ^ B[i + 3]);
				long u = ones ^ b;
				twosB = (ones & b) | (u & c);
				ones = u ^ c;
			}
			{
				long u = twos ^ twosA;
				foursA = (twos & twosA) | (u & twosB);
				twos = u ^ twosB;
			}
			eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 4;
		}

		if (i <= n - 2) {
			long b = (A[i] ^ B[i]), c = (A[i + 1] ^ B[i + 1]);
			long u = ones ^ b;
			long twosA = (ones & b) | (u & c);
			ones = u ^ c;

			long foursA = twos & twosA;
			twos = twos ^ twosA;

			long eights = fours & foursA;
			fours = fours ^ foursA;

			tot8 += pop(eights);
			i += 2;
		}

		if (i < n) {
			tot += pop((A[i] ^ B[i]));
		}

		tot += (pop(fours) << 2) + (pop(twos) << 1) + pop(ones) + (tot8 << 3);

		return tot;
	}

	public static final byte[] ntzTable = { 8, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0,
			2, 0, 1, 0, 4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 5, 0,
			1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 4, 0, 1, 0, 2, 0, 1, 0,
			3, 0, 1, 0, 2, 0, 1, 0, 6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0,
			1, 0, 4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 5, 0, 1, 0,
			2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 4, 0, 1, 0, 2, 0, 1, 0, 3, 0,
			1, 0, 2, 0, 1, 0, 7, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
			4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 5, 0, 1, 0, 2, 0,
			1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0,
			2, 0, 1, 0, 6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 4, 0,
			1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 5, 0, 1, 0, 2, 0, 1, 0,
			3, 0, 1, 0, 2, 0, 1, 0, 4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0,
			1, 0 };

	public static int ntz(long val) {

		//

		int lower = (int) val;
		int lowByte = lower & 0xff;
		if (lowByte != 0)
			return ntzTable[lowByte];

		if (lower != 0) {
			lowByte = (lower >>> 8) & 0xff;
			if (lowByte != 0)
				return ntzTable[lowByte] + 8;
			lowByte = (lower >>> 16) & 0xff;
			if (lowByte != 0)
				return ntzTable[lowByte] + 16;

			return ntzTable[lower >>> 24] + 24;
		} else {

			int upper = (int) (val >> 32);
			lowByte = upper & 0xff;
			if (lowByte != 0)
				return ntzTable[lowByte] + 32;
			lowByte = (upper >>> 8) & 0xff;
			if (lowByte != 0)
				return ntzTable[lowByte] + 40;
			lowByte = (upper >>> 16) & 0xff;
			if (lowByte != 0)
				return ntzTable[lowByte] + 48;

			return ntzTable[upper >>> 24] + 56;
		}
	}

	public static int ntz(int val) {

		int lowByte = val & 0xff;
		if (lowByte != 0)
			return ntzTable[lowByte];
		lowByte = (val >>> 8) & 0xff;
		if (lowByte != 0)
			return ntzTable[lowByte] + 8;
		lowByte = (val >>> 16) & 0xff;
		if (lowByte != 0)
			return ntzTable[lowByte] + 16;

		return ntzTable[val >>> 24] + 24;
	}

	public static int ntz2(long x) {
		int n = 0;
		int y = (int) x;
		if (y == 0) {
			n += 32;
			y = (int) (x >>> 32);
		}
		if ((y & 0x0000FFFF) == 0) {
			n += 16;
			y >>>= 16;
		}
		if ((y & 0x000000FF) == 0) {
			n += 8;
			y >>>= 8;
		}
		return (ntzTable[y & 0xff]) + n;
	}

	public static int ntz3(long x) {

		int n = 1;

		int y = (int) x;
		if (y == 0) {
			n += 32;
			y = (int) (x >>> 32);
		}
		if ((y & 0x0000FFFF) == 0) {
			n += 16;
			y >>>= 16;
		}
		if ((y & 0x000000FF) == 0) {
			n += 8;
			y >>>= 8;
		}
		if ((y & 0x0000000F) == 0) {
			n += 4;
			y >>>= 4;
		}
		if ((y & 0x00000003) == 0) {
			n += 2;
			y >>>= 2;
		}
		return n - (y & 1);
	}

	public static final byte[] nlzTable = { 8, 7, 6, 6, 5, 5, 5, 5, 4, 4, 4, 4,
			4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2,
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
			2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0 };

	public static int nlz(long x) {
		int n = 0;

		int y = (int) (x >>> 32);
		if (y == 0) {
			n += 32;
			y = (int) (x);
		}
		if ((y & 0xFFFF0000) == 0) {
			n += 16;
			y <<= 16;
		}
		if ((y & 0xFF000000) == 0) {
			n += 8;
			y <<= 8;
		}
		return n + nlzTable[y >>> 24];

	}

	public static boolean isPowerOfTwo(int v) {
		return ((v & (v - 1)) == 0);
	}

	public static boolean isPowerOfTwo(long v) {
		return ((v & (v - 1)) == 0);
	}

	public static int nextHighestPowerOfTwo(int v) {
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v++;
		return v;
	}

	public static long nextHighestPowerOfTwo(long v) {
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v |= v >> 32;
		v++;
		return v;
	}

}
