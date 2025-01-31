/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;

public final class IndexableBinaryStringTools {

	private static final CodingCase[] CODING_CASES = {

	new CodingCase(7, 1),

	new CodingCase(14, 6, 2), new CodingCase(13, 5, 3),
			new CodingCase(12, 4, 4), new CodingCase(11, 3, 5),
			new CodingCase(10, 2, 6), new CodingCase(9, 1, 7),
			new CodingCase(8, 0) };

	private IndexableBinaryStringTools() {
	}

	@Deprecated
	public static int getEncodedLength(ByteBuffer original)
			throws IllegalArgumentException {
		if (original.hasArray()) {
			return getEncodedLength(original.array(), original.arrayOffset(),
					original.limit() - original.arrayOffset());
		} else {
			throw new IllegalArgumentException(
					"original argument must have a backing array");
		}
	}

	public static int getEncodedLength(byte[] inputArray, int inputOffset,
			int inputLength) {

		return (int) ((8L * inputLength + 14L) / 15L) + 1;
	}

	@Deprecated
	public static int getDecodedLength(CharBuffer encoded)
			throws IllegalArgumentException {
		if (encoded.hasArray()) {
			return getDecodedLength(encoded.array(), encoded.arrayOffset(),
					encoded.limit() - encoded.arrayOffset());
		} else {
			throw new IllegalArgumentException(
					"encoded argument must have a backing array");
		}
	}

	public static int getDecodedLength(char[] encoded, int offset, int length) {
		final int numChars = length - 1;
		if (numChars <= 0) {
			return 0;
		} else {

			final long numFullBytesInFinalChar = encoded[offset + length - 1];
			final long numEncodedChars = numChars - 1;
			return (int) ((numEncodedChars * 15L + 7L) / 8L + numFullBytesInFinalChar);
		}
	}

	@Deprecated
	public static void encode(ByteBuffer input, CharBuffer output) {
		if (input.hasArray() && output.hasArray()) {
			final int inputOffset = input.arrayOffset();
			final int inputLength = input.limit() - inputOffset;
			final int outputOffset = output.arrayOffset();
			final int outputLength = getEncodedLength(input.array(),
					inputOffset, inputLength);
			output.limit(outputLength + outputOffset);
			output.position(0);
			encode(input.array(), inputOffset, inputLength, output.array(),
					outputOffset, outputLength);
		} else {
			throw new IllegalArgumentException(
					"Arguments must have backing arrays");
		}
	}

	public static void encode(byte[] inputArray, int inputOffset,
			int inputLength, char[] outputArray, int outputOffset,
			int outputLength) {
		assert (outputLength == getEncodedLength(inputArray, inputOffset,
				inputLength));
		if (inputLength > 0) {
			int inputByteNum = inputOffset;
			int caseNum = 0;
			int outputCharNum = outputOffset;
			CodingCase codingCase;
			for (; inputByteNum + CODING_CASES[caseNum].numBytes <= inputLength; ++outputCharNum) {
				codingCase = CODING_CASES[caseNum];
				if (2 == codingCase.numBytes) {
					outputArray[outputCharNum] = (char) (((inputArray[inputByteNum] & 0xFF) << codingCase.initialShift)
							+ (((inputArray[inputByteNum + 1] & 0xFF) >>> codingCase.finalShift) & codingCase.finalMask) & (short) 0x7FFF);
				} else {
					outputArray[outputCharNum] = (char) (((inputArray[inputByteNum] & 0xFF) << codingCase.initialShift)
							+ ((inputArray[inputByteNum + 1] & 0xFF) << codingCase.middleShift)
							+ (((inputArray[inputByteNum + 2] & 0xFF) >>> codingCase.finalShift) & codingCase.finalMask) & (short) 0x7FFF);
				}
				inputByteNum += codingCase.advanceBytes;
				if (++caseNum == CODING_CASES.length) {
					caseNum = 0;
				}
			}

			codingCase = CODING_CASES[caseNum];

			if (inputByteNum + 1 < inputLength) {
				outputArray[outputCharNum++] = (char) ((((inputArray[inputByteNum] & 0xFF) << codingCase.initialShift) + ((inputArray[inputByteNum + 1] & 0xFF) << codingCase.middleShift)) & (short) 0x7FFF);

				outputArray[outputCharNum++] = (char) 1;
			} else if (inputByteNum < inputLength) {
				outputArray[outputCharNum++] = (char) (((inputArray[inputByteNum] & 0xFF) << codingCase.initialShift) & (short) 0x7FFF);

				outputArray[outputCharNum++] = caseNum == 0 ? (char) 1
						: (char) 0;
			} else {

				outputArray[outputCharNum++] = (char) 1;
			}
		}
	}

	@Deprecated
	public static void decode(CharBuffer input, ByteBuffer output) {
		if (input.hasArray() && output.hasArray()) {
			final int inputOffset = input.arrayOffset();
			final int inputLength = input.limit() - inputOffset;
			final int outputOffset = output.arrayOffset();
			final int outputLength = getDecodedLength(input.array(),
					inputOffset, inputLength);
			output.limit(outputLength + outputOffset);
			output.position(0);
			decode(input.array(), inputOffset, inputLength, output.array(),
					outputOffset, outputLength);
		} else {
			throw new IllegalArgumentException(
					"Arguments must have backing arrays");
		}
	}

	public static void decode(char[] inputArray, int inputOffset,
			int inputLength, byte[] outputArray, int outputOffset,
			int outputLength) {
		assert (outputLength == getDecodedLength(inputArray, inputOffset,
				inputLength));
		final int numInputChars = inputLength - 1;
		final int numOutputBytes = outputLength;

		if (numOutputBytes > 0) {
			int caseNum = 0;
			int outputByteNum = outputOffset;
			int inputCharNum = inputOffset;
			short inputChar;
			CodingCase codingCase;
			for (; inputCharNum < numInputChars - 1; ++inputCharNum) {
				codingCase = CODING_CASES[caseNum];
				inputChar = (short) inputArray[inputCharNum];
				if (2 == codingCase.numBytes) {
					if (0 == caseNum) {
						outputArray[outputByteNum] = (byte) (inputChar >>> codingCase.initialShift);
					} else {
						outputArray[outputByteNum] += (byte) (inputChar >>> codingCase.initialShift);
					}
					outputArray[outputByteNum + 1] = (byte) ((inputChar & codingCase.finalMask) << codingCase.finalShift);
				} else {
					outputArray[outputByteNum] += (byte) (inputChar >>> codingCase.initialShift);
					outputArray[outputByteNum + 1] = (byte) ((inputChar & codingCase.middleMask) >>> codingCase.middleShift);
					outputArray[outputByteNum + 2] = (byte) ((inputChar & codingCase.finalMask) << codingCase.finalShift);
				}
				outputByteNum += codingCase.advanceBytes;
				if (++caseNum == CODING_CASES.length) {
					caseNum = 0;
				}
			}

			inputChar = (short) inputArray[inputCharNum];
			codingCase = CODING_CASES[caseNum];
			if (0 == caseNum) {
				outputArray[outputByteNum] = 0;
			}
			outputArray[outputByteNum] += (byte) (inputChar >>> codingCase.initialShift);
			final int bytesLeft = numOutputBytes - outputByteNum;
			if (bytesLeft > 1) {
				if (2 == codingCase.numBytes) {
					outputArray[outputByteNum + 1] = (byte) ((inputChar & codingCase.finalMask) >>> codingCase.finalShift);
				} else {
					outputArray[outputByteNum + 1] = (byte) ((inputChar & codingCase.middleMask) >>> codingCase.middleShift);
					if (bytesLeft > 2) {
						outputArray[outputByteNum + 2] = (byte) ((inputChar & codingCase.finalMask) << codingCase.finalShift);
					}
				}
			}
		}
	}

	@Deprecated
	public static ByteBuffer decode(CharBuffer input) {
		byte[] outputArray = new byte[getDecodedLength(input)];
		ByteBuffer output = ByteBuffer.wrap(outputArray);
		decode(input, output);
		return output;
	}

	@Deprecated
	public static CharBuffer encode(ByteBuffer input) {
		char[] outputArray = new char[getEncodedLength(input)];
		CharBuffer output = CharBuffer.wrap(outputArray);
		encode(input, output);
		return output;
	}

	static class CodingCase {
		int numBytes, initialShift, middleShift, finalShift, advanceBytes = 2;
		short middleMask, finalMask;

		CodingCase(int initialShift, int middleShift, int finalShift) {
			this.numBytes = 3;
			this.initialShift = initialShift;
			this.middleShift = middleShift;
			this.finalShift = finalShift;
			this.finalMask = (short) ((short) 0xFF >>> finalShift);
			this.middleMask = (short) ((short) 0xFF << middleShift);
		}

		CodingCase(int initialShift, int finalShift) {
			this.numBytes = 2;
			this.initialShift = initialShift;
			this.finalShift = finalShift;
			this.finalMask = (short) ((short) 0xFF >>> finalShift);
			if (finalShift != 0) {
				advanceBytes = 1;
			}
		}
	}
}
