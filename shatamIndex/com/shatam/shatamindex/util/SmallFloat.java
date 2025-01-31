/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public class SmallFloat {

	public static byte floatToByte(float f, int numMantissaBits, int zeroExp) {

		int fzero = (63 - zeroExp) << numMantissaBits;
		int bits = Float.floatToRawIntBits(f);
		int smallfloat = bits >> (24 - numMantissaBits);
		if (smallfloat <= fzero) {
			return (bits <= 0) ? (byte) 0 : (byte) 1;
		} else if (smallfloat >= fzero + 0x100) {
			return -1;
		} else {
			return (byte) (smallfloat - fzero);
		}
	}

	public static float byteToFloat(byte b, int numMantissaBits, int zeroExp) {

		if (b == 0)
			return 0.0f;
		int bits = (b & 0xff) << (24 - numMantissaBits);
		bits += (63 - zeroExp) << 24;
		return Float.intBitsToFloat(bits);
	}

	public static byte floatToByte315(float f) {
		int bits = Float.floatToRawIntBits(f);
		int smallfloat = bits >> (24 - 3);
		if (smallfloat <= ((63 - 15) << 3)) {
			return (bits <= 0) ? (byte) 0 : (byte) 1;
		}
		if (smallfloat >= ((63 - 15) << 3) + 0x100) {
			return -1;
		}
		return (byte) (smallfloat - ((63 - 15) << 3));
	}

	public static float byte315ToFloat(byte b) {

		if (b == 0)
			return 0.0f;
		int bits = (b & 0xff) << (24 - 3);
		bits += (63 - 15) << 24;
		return Float.intBitsToFloat(bits);
	}

	public static byte floatToByte52(float f) {
		int bits = Float.floatToRawIntBits(f);
		int smallfloat = bits >> (24 - 5);
		if (smallfloat <= (63 - 2) << 5) {
			return (bits <= 0) ? (byte) 0 : (byte) 1;
		}
		if (smallfloat >= ((63 - 2) << 5) + 0x100) {
			return -1;
		}
		return (byte) (smallfloat - ((63 - 2) << 5));
	}

	public static float byte52ToFloat(byte b) {

		if (b == 0)
			return 0.0f;
		int bits = (b & 0xff) << (24 - 5);
		bits += (63 - 2) << 24;
		return Float.intBitsToFloat(bits);
	}
}
