/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;

@Deprecated
public final class ISOLatin1AccentFilter extends TokenFilter {
	public ISOLatin1AccentFilter(TokenStream input) {
		super(input);
	}

	private char[] output = new char[256];
	private int outputPos;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	@Override
	public final boolean incrementToken() throws java.io.IOException {
		if (input.incrementToken()) {
			final char[] buffer = termAtt.buffer();
			final int length = termAtt.length();

			for (int i = 0; i < length; i++) {
				final char c = buffer[i];
				if (c >= '\u00c0' && c <= '\uFB06') {
					removeAccents(buffer, length);
					termAtt.copyBuffer(output, 0, outputPos);
					break;
				}
			}
			return true;
		} else
			return false;
	}

	public final void removeAccents(char[] input, int length) {

		final int maxSizeNeeded = 2 * length;

		int size = output.length;
		while (size < maxSizeNeeded)
			size *= 2;

		if (size != output.length)
			output = new char[size];

		outputPos = 0;

		int pos = 0;

		for (int i = 0; i < length; i++, pos++) {
			final char c = input[pos];

			if (c < '\u00c0' || c > '\uFB06')
				output[outputPos++] = c;
			else {
				switch (c) {
				case '\u00C0':
				case '\u00C1':
				case '\u00C2':
				case '\u00C3':
				case '\u00C4':
				case '\u00C5':
					output[outputPos++] = 'A';
					break;
				case '\u00C6':
					output[outputPos++] = 'A';
					output[outputPos++] = 'E';
					break;
				case '\u00C7':
					output[outputPos++] = 'C';
					break;
				case '\u00C8':
				case '\u00C9':
				case '\u00CA':
				case '\u00CB':
					output[outputPos++] = 'E';
					break;
				case '\u00CC':
				case '\u00CD':
				case '\u00CE':
				case '\u00CF':
					output[outputPos++] = 'I';
					break;
				case '\u0132':
					output[outputPos++] = 'I';
					output[outputPos++] = 'J';
					break;
				case '\u00D0':
					output[outputPos++] = 'D';
					break;
				case '\u00D1':
					output[outputPos++] = 'N';
					break;
				case '\u00D2':
				case '\u00D3':
				case '\u00D4':
				case '\u00D5':
				case '\u00D6':
				case '\u00D8':
					output[outputPos++] = 'O';
					break;
				case '\u0152':
					output[outputPos++] = 'O';
					output[outputPos++] = 'E';
					break;
				case '\u00DE':
					output[outputPos++] = 'T';
					output[outputPos++] = 'H';
					break;
				case '\u00D9':
				case '\u00DA':
				case '\u00DB':
				case '\u00DC':
					output[outputPos++] = 'U';
					break;
				case '\u00DD':
				case '\u0178':
					output[outputPos++] = 'Y';
					break;
				case '\u00E0':
				case '\u00E1':
				case '\u00E2':
				case '\u00E3':
				case '\u00E4':
				case '\u00E5':
					output[outputPos++] = 'a';
					break;
				case '\u00E6':
					output[outputPos++] = 'a';
					output[outputPos++] = 'e';
					break;
				case '\u00E7':
					output[outputPos++] = 'c';
					break;
				case '\u00E8':
				case '\u00E9':
				case '\u00EA':
				case '\u00EB':
					output[outputPos++] = 'e';
					break;
				case '\u00EC':
				case '\u00ED':
				case '\u00EE':
				case '\u00EF':
					output[outputPos++] = 'i';
					break;
				case '\u0133':
					output[outputPos++] = 'i';
					output[outputPos++] = 'j';
					break;
				case '\u00F0':
					output[outputPos++] = 'd';
					break;
				case '\u00F1':
					output[outputPos++] = 'n';
					break;
				case '\u00F2':
				case '\u00F3':
				case '\u00F4':
				case '\u00F5':
				case '\u00F6':
				case '\u00F8':
					output[outputPos++] = 'o';
					break;
				case '\u0153':
					output[outputPos++] = 'o';
					output[outputPos++] = 'e';
					break;
				case '\u00DF':
					output[outputPos++] = 's';
					output[outputPos++] = 's';
					break;
				case '\u00FE':
					output[outputPos++] = 't';
					output[outputPos++] = 'h';
					break;
				case '\u00F9':
				case '\u00FA':
				case '\u00FB':
				case '\u00FC':
					output[outputPos++] = 'u';
					break;
				case '\u00FD':
				case '\u00FF':
					output[outputPos++] = 'y';
					break;
				case '\uFB00':
					output[outputPos++] = 'f';
					output[outputPos++] = 'f';
					break;
				case '\uFB01':
					output[outputPos++] = 'f';
					output[outputPos++] = 'i';
					break;
				case '\uFB02':
					output[outputPos++] = 'f';
					output[outputPos++] = 'l';
					break;

				case '\uFB05':
					output[outputPos++] = 'f';
					output[outputPos++] = 't';
					break;
				case '\uFB06':
					output[outputPos++] = 's';
					output[outputPos++] = 't';
					break;
				default:
					output[outputPos++] = c;
					break;
				}
			}
		}
	}
}
