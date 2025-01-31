/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.RamUsageEstimator;

public final class ASCIIFoldingFilter extends TokenFilter {
	public ASCIIFoldingFilter(TokenStream input) {
		super(input);
	}

	private char[] output = new char[512];
	private int outputPos;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	@Override
	public boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			final char[] buffer = termAtt.buffer();
			final int length = termAtt.length();

			for (int i = 0; i < length; ++i) {
				final char c = buffer[i];
				if (c >= '\u0080') {
					foldToASCII(buffer, length);
					termAtt.copyBuffer(output, 0, outputPos);
					break;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public void foldToASCII(char[] input, int length) {

		final int maxSizeNeeded = 4 * length;
		if (output.length < maxSizeNeeded) {
			output = new char[ArrayUtil.oversize(maxSizeNeeded,
					RamUsageEstimator.NUM_BYTES_CHAR)];
		}

		outputPos = foldToASCII(input, 0, output, 0, length);
	}

	public static final int foldToASCII(char input[], int inputPos,
			char output[], int outputPos, int length) {
		final int end = inputPos + length;
		for (int pos = inputPos; pos < end; ++pos) {
			final char c = input[pos];

			
			if (c < '\u0080') {
				output[outputPos++] = c;
			} else {
				switch (c) {
				case '\u00C0': 
				case '\u00C1': 
				case '\u00C2': 
				case '\u00C3': 
				case '\u00C4': 
				case '\u00C5': 
				case '\u0100': 
				case '\u0102': 
				case '\u0104': 
				case '\u018F': 
								
				case '\u01CD': 
				case '\u01DE': 
								
				case '\u01E0': 
								
				case '\u01FA': 
								
				case '\u0200': 
				case '\u0202': 
								
				case '\u0226': 
				case '\u023A': 
				case '\u1D00': 
				case '\u1E00': 
				case '\u1EA0': 
				case '\u1EA2': 
				case '\u1EA4': 
								
				case '\u1EA6': 
								
				case '\u1EA8': 
								
				case '\u1EAA': 
								
				case '\u1EAC': 
								
				case '\u1EAE': 
								
				case '\u1EB0': 
								
				case '\u1EB2': 
								
				case '\u1EB4': 
								
				case '\u1EB6': 
								
				case '\u24B6': 
				case '\uFF21': 
					output[outputPos++] = 'A';
					break;
				case '\u00E0': 
				case '\u00E1': 
				case '\u00E2': 
				case '\u00E3': 
				case '\u00E4': 
				case '\u00E5': 
				case '\u0101': 
				case '\u0103': 
				case '\u0105': 
				case '\u01CE': 
				case '\u01DF': 
								
				case '\u01E1': 
								
				case '\u01FB': 
								
				case '\u0201': 
				case '\u0203': 
				case '\u0227': 
				case '\u0250': 
				case '\u0259': 
				case '\u025A': 
				case '\u1D8F': 
				case '\u1D95': 
								
				case '\u1E01': 
				case '\u1E9A': 
								
				case '\u1EA1': 
				case '\u1EA3': 
				case '\u1EA5': 
								
				case '\u1EA7': 
								
				case '\u1EA9': 
								
				case '\u1EAB': 
								
				case '\u1EAD': 
								
				case '\u1EAF': 
								
				case '\u1EB1': 
								
				case '\u1EB3': 
								
				case '\u1EB5': 
								
				case '\u1EB7': 
								
				case '\u2090': 
				case '\u2094': 
				case '\u24D0': 
				case '\u2C65': 
				case '\u2C6F': 
				case '\uFF41': 
					output[outputPos++] = 'a';
					break;
				case '\uA732': 
					output[outputPos++] = 'A';
					output[outputPos++] = 'A';
					break;
				case '\u00C6': 
				case '\u01E2': 
				case '\u01FC': 
				case '\u1D01': 
					output[outputPos++] = 'A';
					output[outputPos++] = 'E';
					break;
				case '\uA734': 
					output[outputPos++] = 'A';
					output[outputPos++] = 'O';
					break;
				case '\uA736': 
					output[outputPos++] = 'A';
					output[outputPos++] = 'U';
					break;
				case '\uA738': 
				case '\uA73A': 
								
					output[outputPos++] = 'A';
					output[outputPos++] = 'V';
					break;
				case '\uA73C': 
					output[outputPos++] = 'A';
					output[outputPos++] = 'Y';
					break;
				case '\u249C': 
					output[outputPos++] = '(';
					output[outputPos++] = 'a';
					output[outputPos++] = ')';
					break;
				case '\uA733': 
					output[outputPos++] = 'a';
					output[outputPos++] = 'a';
					break;
				case '\u00E6': 
				case '\u01E3': 
				case '\u01FD': 
				case '\u1D02': 
					output[outputPos++] = 'a';
					output[outputPos++] = 'e';
					break;
				case '\uA735': 
					output[outputPos++] = 'a';
					output[outputPos++] = 'o';
					break;
				case '\uA737': 
					output[outputPos++] = 'a';
					output[outputPos++] = 'u';
					break;
				case '\uA739': 
				case '\uA73B': 
								
					output[outputPos++] = 'a';
					output[outputPos++] = 'v';
					break;
				case '\uA73D': 
					output[outputPos++] = 'a';
					output[outputPos++] = 'y';
					break;
				case '\u0181': 
				case '\u0182': 
				case '\u0243': 
				case '\u0299': 
				case '\u1D03': 
				case '\u1E02': 
				case '\u1E04': 
				case '\u1E06': 
				case '\u24B7': 
				case '\uFF22': 
					output[outputPos++] = 'B';
					break;
				case '\u0180': 
				case '\u0183': 
				case '\u0253': 
				case '\u1D6C': 
				case '\u1D80': 
				case '\u1E03': 
				case '\u1E05': 
				case '\u1E07': 
				case '\u24D1': 
				case '\uFF42': 
					output[outputPos++] = 'b';
					break;
				case '\u249D': 
					output[outputPos++] = '(';
					output[outputPos++] = 'b';
					output[outputPos++] = ')';
					break;
				case '\u00C7': 
				case '\u0106': 
				case '\u0108': 
				case '\u010A': 
				case '\u010C': 
				case '\u0187': 
				case '\u023B': 
				case '\u0297': 
				case '\u1D04': 
				case '\u1E08': 
								
				case '\u24B8': 
				case '\uFF23': 
					output[outputPos++] = 'C';
					break;
				case '\u00E7': 
				case '\u0107': 
				case '\u0109': 
				case '\u010B': 
				case '\u010D': 
				case '\u0188': 
				case '\u023C': 
				case '\u0255': 
				case '\u1E09': 
								
				case '\u2184': 
				case '\u24D2': 
				case '\uA73E': 
				case '\uA73F': 
				case '\uFF43': 
					output[outputPos++] = 'c';
					break;
				case '\u249E': 
					output[outputPos++] = '(';
					output[outputPos++] = 'c';
					output[outputPos++] = ')';
					break;
				case '\u00D0': 
				case '\u010E': 
				case '\u0110': 
				case '\u0189': 
				case '\u018A': 
				case '\u018B': 
				case '\u1D05': 
				case '\u1D06': 
				case '\u1E0A': 
				case '\u1E0C': 
				case '\u1E0E': 
				case '\u1E10': 
				case '\u1E12': 
								
				case '\u24B9': 
				case '\uA779': 
				case '\uFF24': 
					output[outputPos++] = 'D';
					break;
				case '\u00F0': 
				case '\u010F': 
				case '\u0111': 
				case '\u018C': 
				case '\u0221': 
				case '\u0256': 
				case '\u0257': 
				case '\u1D6D': 
				case '\u1D81': 
				case '\u1D91': 
				case '\u1E0B': 
				case '\u1E0D': 
				case '\u1E0F': 
				case '\u1E11': 
				case '\u1E13': 
								
				case '\u24D3': 
				case '\uA77A': 
				case '\uFF44': 
					output[outputPos++] = 'd';
					break;
				case '\u01C4': 
				case '\u01F1': 
					output[outputPos++] = 'D';
					output[outputPos++] = 'Z';
					break;
				case '\u01C5': 
								
				case '\u01F2': 
								
					output[outputPos++] = 'D';
					output[outputPos++] = 'z';
					break;
				case '\u249F': 
					output[outputPos++] = '(';
					output[outputPos++] = 'd';
					output[outputPos++] = ')';
					break;
				case '\u0238': 
					output[outputPos++] = 'd';
					output[outputPos++] = 'b';
					break;
				case '\u01C6': 
				case '\u01F3': 
				case '\u02A3': 
				case '\u02A5': 
					output[outputPos++] = 'd';
					output[outputPos++] = 'z';
					break;
				case '\u00C8': 
				case '\u00C9': 
				case '\u00CA': 
				case '\u00CB': 
				case '\u0112': 
				case '\u0114': 
				case '\u0116': 
				case '\u0118': 
				case '\u011A': 
				case '\u018E': 
				case '\u0190': 
				case '\u0204': 
				case '\u0206': 
								
				case '\u0228': 
				case '\u0246': 
				case '\u1D07': 
				case '\u1E14': 
								
				case '\u1E16': 
								
				case '\u1E18': 
								
				case '\u1E1A': 
				case '\u1E1C': 
								
				case '\u1EB8': 
				case '\u1EBA': 
				case '\u1EBC': 
				case '\u1EBE': 
								
				case '\u1EC0': 
								
				case '\u1EC2': 
								
				case '\u1EC4': 
								
				case '\u1EC6': 
								
				case '\u24BA': 
				case '\u2C7B': 
				case '\uFF25': 
					output[outputPos++] = 'E';
					break;
				case '\u00E8': 
				case '\u00E9': 
				case '\u00EA': 
				case '\u00EB': 
				case '\u0113': 
				case '\u0115': 
				case '\u0117': 
				case '\u0119': 
				case '\u011B': 
				case '\u01DD': 
				case '\u0205': 
				case '\u0207': 
				case '\u0229': 
				case '\u0247': 
				case '\u0258': 
				case '\u025B': 
				case '\u025C': 
				case '\u025D': 
								
				case '\u025E': 
				case '\u029A': 
				case '\u1D08': 
				case '\u1D92': 
				case '\u1D93': 
								
				case '\u1D94': 
								
				case '\u1E15': 
								
				case '\u1E17': 
								
				case '\u1E19': 
								
				case '\u1E1B': 
				case '\u1E1D': 
								
				case '\u1EB9': 
				case '\u1EBB': 
				case '\u1EBD': 
				case '\u1EBF': 
								
				case '\u1EC1': 
								
				case '\u1EC3': 
								
				case '\u1EC5': 
								
				case '\u1EC7': 
								
				case '\u2091': 
				case '\u24D4': 
				case '\u2C78': 
				case '\uFF45': 
					output[outputPos++] = 'e';
					break;
				case '\u24A0': 
					output[outputPos++] = '(';
					output[outputPos++] = 'e';
					output[outputPos++] = ')';
					break;
				case '\u0191': 
				case '\u1E1E': 
				case '\u24BB': 
				case '\uA730': 
				case '\uA77B': 
				case '\uA7FB': 
				case '\uFF26': 
					output[outputPos++] = 'F';
					break;
				case '\u0192': 
				case '\u1D6E': 
				case '\u1D82': 
				case '\u1E1F': 
				case '\u1E9B': 
				case '\u24D5': 
				case '\uA77C': 
				case '\uFF46': 
					output[outputPos++] = 'f';
					break;
				case '\u24A1': 
					output[outputPos++] = '(';
					output[outputPos++] = 'f';
					output[outputPos++] = ')';
					break;
				case '\uFB00': 
					output[outputPos++] = 'f';
					output[outputPos++] = 'f';
					break;
				case '\uFB03': 
					output[outputPos++] = 'f';
					output[outputPos++] = 'f';
					output[outputPos++] = 'i';
					break;
				case '\uFB04': 
					output[outputPos++] = 'f';
					output[outputPos++] = 'f';
					output[outputPos++] = 'l';
					break;
				case '\uFB01': 
					output[outputPos++] = 'f';
					output[outputPos++] = 'i';
					break;
				case '\uFB02': 
					output[outputPos++] = 'f';
					output[outputPos++] = 'l';
					break;
				case '\u011C': 
				case '\u011E': 
				case '\u0120': 
				case '\u0122': 
				case '\u0193': 
				case '\u01E4': 
				case '\u01E5': 
				case '\u01E6': 
				case '\u01E7': 
				case '\u01F4': 
				case '\u0262': 
				case '\u029B': 
				case '\u1E20': 
				case '\u24BC': 
				case '\uA77D': 
				case '\uA77E': 
				case '\uFF27': 
					output[outputPos++] = 'G';
					break;
				case '\u011D': 
				case '\u011F': 
				case '\u0121': 
				case '\u0123': 
				case '\u01F5': 
				case '\u0260': 
				case '\u0261': 
				case '\u1D77': 
				case '\u1D79': 
				case '\u1D83': 
				case '\u1E21': 
				case '\u24D6': 
				case '\uA77F': 
				case '\uFF47': 
					output[outputPos++] = 'g';
					break;
				case '\u24A2': 
					output[outputPos++] = '(';
					output[outputPos++] = 'g';
					output[outputPos++] = ')';
					break;
				case '\u0124': 
				case '\u0126': 
				case '\u021E': 
				case '\u029C': 
				case '\u1E22': 
				case '\u1E24': 
				case '\u1E26': 
				case '\u1E28': 
				case '\u1E2A': 
				case '\u24BD': 
				case '\u2C67': 
				case '\u2C75': 
				case '\uFF28': 
					output[outputPos++] = 'H';
					break;
				case '\u0125': 
				case '\u0127': 
				case '\u021F': 
				case '\u0265': 
				case '\u0266': 
				case '\u02AE': 
				case '\u02AF': 
								
				case '\u1E23': 
				case '\u1E25': 
				case '\u1E27': 
				case '\u1E29': 
				case '\u1E2B': 
				case '\u1E96': 
				case '\u24D7': 
				case '\u2C68': 
				case '\u2C76': 
				case '\uFF48': 
					output[outputPos++] = 'h';
					break;
				case '\u01F6': 
								
					output[outputPos++] = 'H';
					output[outputPos++] = 'V';
					break;
				case '\u24A3': 
					output[outputPos++] = '(';
					output[outputPos++] = 'h';
					output[outputPos++] = ')';
					break;
				case '\u0195': 
					output[outputPos++] = 'h';
					output[outputPos++] = 'v';
					break;
				case '\u00CC': 
				case '\u00CD': 
				case '\u00CE': 
				case '\u00CF': 
				case '\u0128': 
				case '\u012A': 
				case '\u012C': 
				case '\u012E': 
				case '\u0130': 
				case '\u0196': 
				case '\u0197': 
				case '\u01CF': 
				case '\u0208': 
				case '\u020A': 
								
				case '\u026A': 
				case '\u1D7B': 
				case '\u1E2C': 
				case '\u1E2E': 
								
				case '\u1EC8': 
				case '\u1ECA': 
				case '\u24BE': 
				case '\uA7FE': 
				case '\uFF29': 
					output[outputPos++] = 'I';
					break;
				case '\u00EC': 
				case '\u00ED': 
				case '\u00EE': 
				case '\u00EF': 
				case '\u0129': 
				case '\u012B': 
				case '\u012D': 
				case '\u012F': 
				case '\u0131': 
				case '\u01D0': 
				case '\u0209': 
				case '\u020B': 
				case '\u0268': 
				case '\u1D09': 
				case '\u1D62': 
				case '\u1D7C': 
				case '\u1D96': 
				case '\u1E2D': 
				case '\u1E2F': 
								
				case '\u1EC9': 
				case '\u1ECB': 
				case '\u2071': 
				case '\u24D8': 
				case '\uFF49': 
					output[outputPos++] = 'i';
					break;
				case '\u0132': 
					output[outputPos++] = 'I';
					output[outputPos++] = 'J';
					break;
				case '\u24A4': 
					output[outputPos++] = '(';
					output[outputPos++] = 'i';
					output[outputPos++] = ')';
					break;
				case '\u0133': 
					output[outputPos++] = 'i';
					output[outputPos++] = 'j';
					break;
				case '\u0134': 
				case '\u0248': 
				case '\u1D0A': 
				case '\u24BF': 
				case '\uFF2A': 
					output[outputPos++] = 'J';
					break;
				case '\u0135': 
				case '\u01F0': 
				case '\u0237': 
				case '\u0249': 
				case '\u025F': 
				case '\u0284': 
								
				case '\u029D': 
				case '\u24D9': 
				case '\u2C7C': 
				case '\uFF4A': 
					output[outputPos++] = 'j';
					break;
				case '\u24A5': 
					output[outputPos++] = '(';
					output[outputPos++] = 'j';
					output[outputPos++] = ')';
					break;
				case '\u0136': 
				case '\u0198': 
				case '\u01E8': 
				case '\u1D0B': 
				case '\u1E30': 
				case '\u1E32': 
				case '\u1E34': 
				case '\u24C0': 
				case '\u2C69': 
				case '\uA740': 
				case '\uA742': 
								
				case '\uA744': 
								
				case '\uFF2B': 
					output[outputPos++] = 'K';
					break;
				case '\u0137': 
				case '\u0199': 
				case '\u01E9': 
				case '\u029E': 
				case '\u1D84': 
				case '\u1E31': 
				case '\u1E33': 
				case '\u1E35': 
				case '\u24DA': 
				case '\u2C6A': 
				case '\uA741': 
				case '\uA743': 
								
				case '\uA745': 
								
				case '\uFF4B': 
					output[outputPos++] = 'k';
					break;
				case '\u24A6': 
					output[outputPos++] = '(';
					output[outputPos++] = 'k';
					output[outputPos++] = ')';
					break;
				case '\u0139': 
				case '\u013B': 
				case '\u013D': 
				case '\u013F': 
				case '\u0141': 
				case '\u023D': 
				case '\u029F': 
				case '\u1D0C': 
				case '\u1E36': 
				case '\u1E38': 
								
				case '\u1E3A': 
				case '\u1E3C': 
								
				case '\u24C1': 
				case '\u2C60': 
				case '\u2C62': 
				case '\uA746': 
				case '\uA748': 
				case '\uA780': 
				case '\uFF2C': 
					output[outputPos++] = 'L';
					break;
				case '\u013A': 
				case '\u013C': 
				case '\u013E': 
				case '\u0140': 
				case '\u0142': 
				case '\u019A': 
				case '\u0234': 
				case '\u026B': 
				case '\u026C': 
				case '\u026D': 
				case '\u1D85': 
				case '\u1E37': 
				case '\u1E39': 
								
				case '\u1E3B': 
				case '\u1E3D': 
								
				case '\u24DB': 
				case '\u2C61': 
				case '\uA747': 
				case '\uA749': 
				case '\uA781': 
				case '\uFF4C': 
					output[outputPos++] = 'l';
					break;
				case '\u01C7': 
					output[outputPos++] = 'L';
					output[outputPos++] = 'J';
					break;
				case '\u1EFA': 
					output[outputPos++] = 'L';
					output[outputPos++] = 'L';
					break;
				case '\u01C8': 
								
					output[outputPos++] = 'L';
					output[outputPos++] = 'j';
					break;
				case '\u24A7': 
					output[outputPos++] = '(';
					output[outputPos++] = 'l';
					output[outputPos++] = ')';
					break;
				case '\u01C9': 
					output[outputPos++] = 'l';
					output[outputPos++] = 'j';
					break;
				case '\u1EFB': 
					output[outputPos++] = 'l';
					output[outputPos++] = 'l';
					break;
				case '\u02AA': 
					output[outputPos++] = 'l';
					output[outputPos++] = 's';
					break;
				case '\u02AB': 
					output[outputPos++] = 'l';
					output[outputPos++] = 'z';
					break;
				case '\u019C': 
				case '\u1D0D': 
				case '\u1E3E': 
				case '\u1E40': 
				case '\u1E42': 
				case '\u24C2': 
				case '\u2C6E': 
				case '\uA7FD': 
				case '\uA7FF': 
				case '\uFF2D': 
					output[outputPos++] = 'M';
					break;
				case '\u026F': 
				case '\u0270': 
				case '\u0271': 
				case '\u1D6F': 
				case '\u1D86': 
				case '\u1E3F': 
				case '\u1E41': 
				case '\u1E43': 
				case '\u24DC': 
				case '\uFF4D': 
					output[outputPos++] = 'm';
					break;
				case '\u24A8': 
					output[outputPos++] = '(';
					output[outputPos++] = 'm';
					output[outputPos++] = ')';
					break;
				case '\u00D1': 
				case '\u0143': 
				case '\u0145': 
				case '\u0147': 
				case '\u014A': 
								
				case '\u019D': 
				case '\u01F8': 
				case '\u0220': 
								
				case '\u0274': 
				case '\u1D0E': 
				case '\u1E44': 
				case '\u1E46': 
				case '\u1E48': 
				case '\u1E4A': 
								
				case '\u24C3': 
				case '\uFF2E': 
					output[outputPos++] = 'N';
					break;
				case '\u00F1': 
				case '\u0144': 
				case '\u0146': 
				case '\u0148': 
				case '\u0149': 
								
				case '\u014B': 
								
				case '\u019E': 
				case '\u01F9': 
				case '\u0235': 
				case '\u0272': 
				case '\u0273': 
				case '\u1D70': 
				case '\u1D87': 
				case '\u1E45': 
				case '\u1E47': 
				case '\u1E49': 
				case '\u1E4B': 
								
				case '\u207F': 
				case '\u24DD': 
				case '\uFF4E': 
					output[outputPos++] = 'n';
					break;
				case '\u01CA': 
					output[outputPos++] = 'N';
					output[outputPos++] = 'J';
					break;
				case '\u01CB': 
								
					output[outputPos++] = 'N';
					output[outputPos++] = 'j';
					break;
				case '\u24A9': 
					output[outputPos++] = '(';
					output[outputPos++] = 'n';
					output[outputPos++] = ')';
					break;
				case '\u01CC': 
					output[outputPos++] = 'n';
					output[outputPos++] = 'j';
					break;
				case '\u00D2': 
				case '\u00D3': 
				case '\u00D4': 
				case '\u00D5': 
				case '\u00D6': 
				case '\u00D8': 
				case '\u014C': 
				case '\u014E': 
				case '\u0150': 
				case '\u0186': 
				case '\u019F': 
				case '\u01A0': 
				case '\u01D1': 
				case '\u01EA': 
				case '\u01EC': 
								
				case '\u01FE': 
								
				case '\u020C': 
				case '\u020E': 
								
				case '\u022A': 
								
				case '\u022C': 
								
				case '\u022E': 
				case '\u0230': 
								
				case '\u1D0F': 
				case '\u1D10': 
				case '\u1E4C': 
								
				case '\u1E4E': 
								
				case '\u1E50': 
								
				case '\u1E52': 
								
				case '\u1ECC': 
				case '\u1ECE': 
				case '\u1ED0': 
								
				case '\u1ED2': 
								
				case '\u1ED4': 
								
				case '\u1ED6': 
								
				case '\u1ED8': 
								
				case '\u1EDA': 
								
				case '\u1EDC': 
								
				case '\u1EDE': 
								
				case '\u1EE0': 
								
				case '\u1EE2': 
								
				case '\u24C4': 
				case '\uA74A': 
								
				case '\uA74C': 
				case '\uFF2F': 
					output[outputPos++] = 'O';
					break;
				case '\u00F2': 
				case '\u00F3': 
				case '\u00F4': 
				case '\u00F5': 
				case '\u00F6': 
				case '\u00F8': 
				case '\u014D': 
				case '\u014F': 
				case '\u0151': 
				case '\u01A1': 
				case '\u01D2': 
				case '\u01EB': 
				case '\u01ED': 
								
				case '\u01FF': 
								
				case '\u020D': 
				case '\u020F': 
				case '\u022B': 
								
				case '\u022D': 
								
				case '\u022F': 
				case '\u0231': 
								
				case '\u0254': 
				case '\u0275': 
				case '\u1D16': 
				case '\u1D17': 
				case '\u1D97': 
								
				case '\u1E4D': 
								
				case '\u1E4F': 
								
				case '\u1E51': 
								
				case '\u1E53': 
								
				case '\u1ECD': 
				case '\u1ECF': 
				case '\u1ED1': 
								
				case '\u1ED3': 
								
				case '\u1ED5': 
								
				case '\u1ED7': 
								
				case '\u1ED9': 
								
				case '\u1EDB': 
				case '\u1EDD': 
				case '\u1EDF': 
								
				case '\u1EE1': 
				case '\u1EE3': 
								
				case '\u2092': 
				case '\u24DE': 
				case '\u2C7A': 
								
				case '\uA74B': 
								
				case '\uA74D': 
				case '\uFF4F': 
					output[outputPos++] = 'o';
					break;
				case '\u0152': 
				case '\u0276': 
					output[outputPos++] = 'O';
					output[outputPos++] = 'E';
					break;
				case '\uA74E': 
					output[outputPos++] = 'O';
					output[outputPos++] = 'O';
					break;
				case '\u0222': 
								
				case '\u1D15': 
					output[outputPos++] = 'O';
					output[outputPos++] = 'U';
					break;
				case '\u24AA': 
					output[outputPos++] = '(';
					output[outputPos++] = 'o';
					output[outputPos++] = ')';
					break;
				case '\u0153': 
				case '\u1D14': 
					output[outputPos++] = 'o';
					output[outputPos++] = 'e';
					break;
				case '\uA74F': 
					output[outputPos++] = 'o';
					output[outputPos++] = 'o';
					break;
				case '\u0223': 
								
					output[outputPos++] = 'o';
					output[outputPos++] = 'u';
					break;
				case '\u01A4': 
				case '\u1D18': 
				case '\u1E54': 
				case '\u1E56': 
				case '\u24C5': 
				case '\u2C63': 
				case '\uA750': 
								
				case '\uA752': 
				case '\uA754': 
								
				case '\uFF30': 
					output[outputPos++] = 'P';
					break;
				case '\u01A5': 
				case '\u1D71': 
				case '\u1D7D': 
				case '\u1D88': 
				case '\u1E55': 
				case '\u1E57': 
				case '\u24DF': 
				case '\uA751': 
								
				case '\uA753': 
				case '\uA755': 
				case '\uA7FC': 
				case '\uFF50': 
					output[outputPos++] = 'p';
					break;
				case '\u24AB': 
					output[outputPos++] = '(';
					output[outputPos++] = 'p';
					output[outputPos++] = ')';
					break;
				case '\u024A': 
								
				case '\u24C6': 
				case '\uA756': 
								
				case '\uA758': 
								
				case '\uFF31': 
					output[outputPos++] = 'Q';
					break;
				case '\u0138': 
								
				case '\u024B': 
				case '\u02A0': 
				case '\u24E0': 
				case '\uA757': 
								
				case '\uA759': 
								
				case '\uFF51': 
					output[outputPos++] = 'q';
					break;
				case '\u24AC': 
					output[outputPos++] = '(';
					output[outputPos++] = 'q';
					output[outputPos++] = ')';
					break;
				case '\u0239': 
					output[outputPos++] = 'q';
					output[outputPos++] = 'p';
					break;
				case '\u0154': 
				case '\u0156': 
				case '\u0158': 
				case '\u0210': 
				case '\u0212': 
								
				case '\u024C': 
				case '\u0280': 
				case '\u0281': 
				case '\u1D19': 
				case '\u1D1A': 
				case '\u1E58': 
				case '\u1E5A': 
				case '\u1E5C': 
								
				case '\u1E5E': 
				case '\u24C7': 
				case '\u2C64': 
				case '\uA75A': 
				case '\uA782': 
				case '\uFF32': 
					output[outputPos++] = 'R';
					break;
				case '\u0155': 
				case '\u0157': 
				case '\u0159': 
				case '\u0211': 
				case '\u0213': 
				case '\u024D': 
				case '\u027C': 
				case '\u027D': 
				case '\u027E': 
				case '\u027F': 
								
				case '\u1D63': 
				case '\u1D72': 
				case '\u1D73': 
								
				case '\u1D89': 
				case '\u1E59': 
				case '\u1E5B': 
				case '\u1E5D': 
								
				case '\u1E5F': 
				case '\u24E1': 
				case '\uA75B': 
				case '\uA783': 
				case '\uFF52': 
					output[outputPos++] = 'r';
					break;
				case '\u24AD': 
					output[outputPos++] = '(';
					output[outputPos++] = 'r';
					output[outputPos++] = ')';
					break;
				case '\u015A': 
				case '\u015C': 
				case '\u015E': 
				case '\u0160': 
				case '\u0218': 
				case '\u1E60': 
				case '\u1E62': 
				case '\u1E64': 
								
				case '\u1E66': 
								
				case '\u1E68': 
								
				case '\u24C8': 
				case '\uA731': 
				case '\uA785': 
				case '\uFF33': 
					output[outputPos++] = 'S';
					break;
				case '\u015B': 
				case '\u015D': 
				case '\u015F': 
				case '\u0161': 
				case '\u017F': 
								
				case '\u0219': 
				case '\u023F': 
				case '\u0282': 
				case '\u1D74': 
				case '\u1D8A': 
				case '\u1E61': 
				case '\u1E63': 
				case '\u1E65': 
								
				case '\u1E67': 
								
				case '\u1E69': 
								
				case '\u1E9C': 
								
				case '\u1E9D': 
								
				case '\u24E2': 
				case '\uA784': 
				case '\uFF53': 
					output[outputPos++] = 's';
					break;
				case '\u1E9E': 
					output[outputPos++] = 'S';
					output[outputPos++] = 'S';
					break;
				case '\u24AE': 
					output[outputPos++] = '(';
					output[outputPos++] = 's';
					output[outputPos++] = ')';
					break;
				case '\u00DF': 
					output[outputPos++] = 's';
					output[outputPos++] = 's';
					break;
				case '\uFB06': 
					output[outputPos++] = 's';
					output[outputPos++] = 't';
					break;
				case '\u0162': 
				case '\u0164': 
				case '\u0166': 
				case '\u01AC': 
				case '\u01AE': 
								
				case '\u021A': 
				case '\u023E': 
								
				case '\u1D1B': 
				case '\u1E6A': 
				case '\u1E6C': 
				case '\u1E6E': 
				case '\u1E70': 
								
				case '\u24C9': 
				case '\uA786': 
				case '\uFF34': 
					output[outputPos++] = 'T';
					break;
				case '\u0163': 
				case '\u0165': 
				case '\u0167': 
				case '\u01AB': 
				case '\u01AD': 
				case '\u021B': 
				case '\u0236': 
				case '\u0287': 
				case '\u0288': 
				case '\u1D75': 
				case '\u1E6B': 
				case '\u1E6D': 
				case '\u1E6F': 
				case '\u1E71': 
								
				case '\u1E97': 
				case '\u24E3': 
				case '\u2C66': 
								
				case '\uFF54': 
					output[outputPos++] = 't';
					break;
				case '\u00DE': 
				case '\uA766': 
								
					output[outputPos++] = 'T';
					output[outputPos++] = 'H';
					break;
				case '\uA728': 
					output[outputPos++] = 'T';
					output[outputPos++] = 'Z';
					break;
				case '\u24AF': 
					output[outputPos++] = '(';
					output[outputPos++] = 't';
					output[outputPos++] = ')';
					break;
				case '\u02A8': 
					output[outputPos++] = 't';
					output[outputPos++] = 'c';
					break;
				case '\u00FE': 
				case '\u1D7A': 
				case '\uA767': 
								
					output[outputPos++] = 't';
					output[outputPos++] = 'h';
					break;
				case '\u02A6': 
					output[outputPos++] = 't';
					output[outputPos++] = 's';
					break;
				case '\uA729': 
					output[outputPos++] = 't';
					output[outputPos++] = 'z';
					break;
				case '\u00D9': 
				case '\u00DA': 
				case '\u00DB': 
				case '\u00DC': 
				case '\u0168': 
				case '\u016A': 
				case '\u016C': 
				case '\u016E': 
				case '\u0170': 
				case '\u0172': 
				case '\u01AF': 
				case '\u01D3': 
				case '\u01D5': 
								
				case '\u01D7': 
								
				case '\u01D9': 
								
				case '\u01DB': 
								
				case '\u0214': 
				case '\u0216': 
								
				case '\u0244': 
				case '\u1D1C': 
				case '\u1D7E': 
				case '\u1E72': 
								
				case '\u1E74': 
				case '\u1E76': 
								
				case '\u1E78': 
								
				case '\u1E7A': 
								
				case '\u1EE4': 
				case '\u1EE6': 
				case '\u1EE8': 
								
				case '\u1EEA': 
								
				case '\u1EEC': 
								
				case '\u1EEE': 
								
				case '\u1EF0': 
								
				case '\u24CA': 
				case '\uFF35': 
					output[outputPos++] = 'U';
					break;
				case '\u00F9': 
				case '\u00FA': 
				case '\u00FB': 
				case '\u00FC': 
				case '\u0169': 
				case '\u016B': 
				case '\u016D': 
				case '\u016F': 
				case '\u0171': 
				case '\u0173': 
				case '\u01B0': 
				case '\u01D4': 
				case '\u01D6': 
								
				case '\u01D8': 
								
				case '\u01DA': 
								
				case '\u01DC': 
								
				case '\u0215': 
				case '\u0217': 
				case '\u0289': 
				case '\u1D64': 
				case '\u1D99': 
				case '\u1E73': 
								
				case '\u1E75': 
				case '\u1E77': 
								
				case '\u1E79': 
								
				case '\u1E7B': 
								
				case '\u1EE5': 
				case '\u1EE7': 
				case '\u1EE9': 
				case '\u1EEB': 
				case '\u1EED': 
								
				case '\u1EEF': 
				case '\u1EF1': 
								
				case '\u24E4': 
				case '\uFF55': 
					output[outputPos++] = 'u';
					break;
				case '\u24B0': 
					output[outputPos++] = '(';
					output[outputPos++] = 'u';
					output[outputPos++] = ')';
					break;
				case '\u1D6B': 
					output[outputPos++] = 'u';
					output[outputPos++] = 'e';
					break;
				case '\u01B2': 
				case '\u0245': 
				case '\u1D20': 
				case '\u1E7C': 
				case '\u1E7E': 
				case '\u1EFC': 
				case '\u24CB': 
				case '\uA75E': 
								
				case '\uA768': 
				case '\uFF36': 
					output[outputPos++] = 'V';
					break;
				case '\u028B': 
				case '\u028C': 
				case '\u1D65': 
				case '\u1D8C': 
				case '\u1E7D': 
				case '\u1E7F': 
				case '\u24E5': 
				case '\u2C71': 
				case '\u2C74': 
				case '\uA75F': 
								
				case '\uFF56': 
					output[outputPos++] = 'v';
					break;
				case '\uA760': 
					output[outputPos++] = 'V';
					output[outputPos++] = 'Y';
					break;
				case '\u24B1': 
					output[outputPos++] = '(';
					output[outputPos++] = 'v';
					output[outputPos++] = ')';
					break;
				case '\uA761': 
					output[outputPos++] = 'v';
					output[outputPos++] = 'y';
					break;
				case '\u0174': 
				case '\u01F7': 
								
				case '\u1D21': 
				case '\u1E80': 
				case '\u1E82': 
				case '\u1E84': 
				case '\u1E86': 
				case '\u1E88': 
				case '\u24CC': 
				case '\u2C72': 
				case '\uFF37': 
					output[outputPos++] = 'W';
					break;
				case '\u0175': 
				case '\u01BF': 
								
				case '\u028D': 
				case '\u1E81': 
				case '\u1E83': 
				case '\u1E85': 
				case '\u1E87': 
				case '\u1E89': 
				case '\u1E98': 
				case '\u24E6': 
				case '\u2C73': 
				case '\uFF57': 
					output[outputPos++] = 'w';
					break;
				case '\u24B2': 
					output[outputPos++] = '(';
					output[outputPos++] = 'w';
					output[outputPos++] = ')';
					break;
				case '\u1E8A': 
				case '\u1E8C': 
				case '\u24CD': 
				case '\uFF38': 
					output[outputPos++] = 'X';
					break;
				case '\u1D8D': 
				case '\u1E8B': 
				case '\u1E8D': 
				case '\u2093': 
				case '\u24E7': 
				case '\uFF58': 
					output[outputPos++] = 'x';
					break;
				case '\u24B3': 
					output[outputPos++] = '(';
					output[outputPos++] = 'x';
					output[outputPos++] = ')';
					break;
				case '\u00DD': 
				case '\u0176': 
				case '\u0178': 
				case '\u01B3': 
				case '\u0232': 
				case '\u024E': 
				case '\u028F': 
				case '\u1E8E': 
				case '\u1EF2': 
				case '\u1EF4': 
				case '\u1EF6': 
				case '\u1EF8': 
				case '\u1EFE': 
				case '\u24CE': 
				case '\uFF39': 
					output[outputPos++] = 'Y';
					break;
				case '\u00FD': 
				case '\u00FF': 
				case '\u0177': 
				case '\u01B4': 
				case '\u0233': 
				case '\u024F': 
				case '\u028E': 
				case '\u1E8F': 
				case '\u1E99': 
				case '\u1EF3': 
				case '\u1EF5': 
				case '\u1EF7': 
				case '\u1EF9': 
				case '\u1EFF': 
				case '\u24E8': 
				case '\uFF59': 
					output[outputPos++] = 'y';
					break;
				case '\u24B4': 
					output[outputPos++] = '(';
					output[outputPos++] = 'y';
					output[outputPos++] = ')';
					break;
				case '\u0179': 
				case '\u017B': 
				case '\u017D': 
				case '\u01B5': 
				case '\u021C': 
								
				case '\u0224': 
				case '\u1D22': 
				case '\u1E90': 
				case '\u1E92': 
				case '\u1E94': 
				case '\u24CF': 
				case '\u2C6B': 
				case '\uA762': 
				case '\uFF3A': 
					output[outputPos++] = 'Z';
					break;
				case '\u017A': 
				case '\u017C': 
				case '\u017E': 
				case '\u01B6': 
				case '\u021D': 
								
				case '\u0225': 
				case '\u0240': 
				case '\u0290': 
				case '\u0291': 
				case '\u1D76': 
				case '\u1D8E': 
				case '\u1E91': 
				case '\u1E93': 
				case '\u1E95': 
				case '\u24E9': 
				case '\u2C6C': 
				case '\uA763': 
				case '\uFF5A': 
					output[outputPos++] = 'z';
					break;
				case '\u24B5': 
					output[outputPos++] = '(';
					output[outputPos++] = 'z';
					output[outputPos++] = ')';
					break;
				case '\u2070': 
				case '\u2080': 
				case '\u24EA': 
				case '\u24FF': 
				case '\uFF10': 
					output[outputPos++] = '0';
					break;
				case '\u00B9': 
				case '\u2081': 
				case '\u2460': 
				case '\u24F5': 
				case '\u2776': 
				case '\u2780': 
				case '\u278A': 
								
				case '\uFF11': 
					output[outputPos++] = '1';
					break;
				case '\u2488': 
					output[outputPos++] = '1';
					output[outputPos++] = '.';
					break;
				case '\u2474': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = ')';
					break;
				case '\u00B2': 
				case '\u2082': 
				case '\u2461': 
				case '\u24F6': 
				case '\u2777': 
				case '\u2781': 
				case '\u278B': 
								
				case '\uFF12': 
					output[outputPos++] = '2';
					break;
				case '\u2489': 
					output[outputPos++] = '2';
					output[outputPos++] = '.';
					break;
				case '\u2475': 
					output[outputPos++] = '(';
					output[outputPos++] = '2';
					output[outputPos++] = ')';
					break;
				case '\u00B3': 
				case '\u2083': 
				case '\u2462': 
				case '\u24F7': 
				case '\u2778': 
				case '\u2782': 
				case '\u278C': 
								
				case '\uFF13': 
					output[outputPos++] = '3';
					break;
				case '\u248A': 
					output[outputPos++] = '3';
					output[outputPos++] = '.';
					break;
				case '\u2476': 
					output[outputPos++] = '(';
					output[outputPos++] = '3';
					output[outputPos++] = ')';
					break;
				case '\u2074': 
				case '\u2084': 
				case '\u2463': 
				case '\u24F8': 
				case '\u2779': 
				case '\u2783': 
				case '\u278D': 
								
				case '\uFF14': 
					output[outputPos++] = '4';
					break;
				case '\u248B': 
					output[outputPos++] = '4';
					output[outputPos++] = '.';
					break;
				case '\u2477': 
					output[outputPos++] = '(';
					output[outputPos++] = '4';
					output[outputPos++] = ')';
					break;
				case '\u2075': 
				case '\u2085': 
				case '\u2464': 
				case '\u24F9': 
				case '\u277A': 
				case '\u2784': 
				case '\u278E': 
								
				case '\uFF15': 
					output[outputPos++] = '5';
					break;
				case '\u248C': 
					output[outputPos++] = '5';
					output[outputPos++] = '.';
					break;
				case '\u2478': 
					output[outputPos++] = '(';
					output[outputPos++] = '5';
					output[outputPos++] = ')';
					break;
				case '\u2076': 
				case '\u2086': 
				case '\u2465': 
				case '\u24FA': 
				case '\u277B': 
				case '\u2785': 
				case '\u278F': 
								
				case '\uFF16': 
					output[outputPos++] = '6';
					break;
				case '\u248D': 
					output[outputPos++] = '6';
					output[outputPos++] = '.';
					break;
				case '\u2479': 
					output[outputPos++] = '(';
					output[outputPos++] = '6';
					output[outputPos++] = ')';
					break;
				case '\u2077': 
				case '\u2087': 
				case '\u2466': 
				case '\u24FB': 
				case '\u277C': 
				case '\u2786': 
				case '\u2790': 
								
				case '\uFF17': 
					output[outputPos++] = '7';
					break;
				case '\u248E': 
					output[outputPos++] = '7';
					output[outputPos++] = '.';
					break;
				case '\u247A': 
					output[outputPos++] = '(';
					output[outputPos++] = '7';
					output[outputPos++] = ')';
					break;
				case '\u2078': 
				case '\u2088': 
				case '\u2467': 
				case '\u24FC': 
				case '\u277D': 
				case '\u2787': 
				case '\u2791': 
								
				case '\uFF18': 
					output[outputPos++] = '8';
					break;
				case '\u248F': 
					output[outputPos++] = '8';
					output[outputPos++] = '.';
					break;
				case '\u247B': 
					output[outputPos++] = '(';
					output[outputPos++] = '8';
					output[outputPos++] = ')';
					break;
				case '\u2079': 
				case '\u2089': 
				case '\u2468': 
				case '\u24FD': 
				case '\u277E': 
				case '\u2788': 
				case '\u2792': 
								
				case '\uFF19': 
					output[outputPos++] = '9';
					break;
				case '\u2490': 
					output[outputPos++] = '9';
					output[outputPos++] = '.';
					break;
				case '\u247C': 
					output[outputPos++] = '(';
					output[outputPos++] = '9';
					output[outputPos++] = ')';
					break;
				case '\u2469': 
				case '\u24FE': 
				case '\u277F': 
				case '\u2789': 
				case '\u2793': 
								
					output[outputPos++] = '1';
					output[outputPos++] = '0';
					break;
				case '\u2491': 
					output[outputPos++] = '1';
					output[outputPos++] = '0';
					output[outputPos++] = '.';
					break;
				case '\u247D': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '0';
					output[outputPos++] = ')';
					break;
				case '\u246A': 
				case '\u24EB': 
					output[outputPos++] = '1';
					output[outputPos++] = '1';
					break;
				case '\u2492': 
					output[outputPos++] = '1';
					output[outputPos++] = '1';
					output[outputPos++] = '.';
					break;
				case '\u247E': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '1';
					output[outputPos++] = ')';
					break;
				case '\u246B': 
				case '\u24EC': 
					output[outputPos++] = '1';
					output[outputPos++] = '2';
					break;
				case '\u2493': 
					output[outputPos++] = '1';
					output[outputPos++] = '2';
					output[outputPos++] = '.';
					break;
				case '\u247F': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '2';
					output[outputPos++] = ')';
					break;
				case '\u246C': 
				case '\u24ED': 
					output[outputPos++] = '1';
					output[outputPos++] = '3';
					break;
				case '\u2494': 
					output[outputPos++] = '1';
					output[outputPos++] = '3';
					output[outputPos++] = '.';
					break;
				case '\u2480': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '3';
					output[outputPos++] = ')';
					break;
				case '\u246D': 
				case '\u24EE': 
					output[outputPos++] = '1';
					output[outputPos++] = '4';
					break;
				case '\u2495': 
					output[outputPos++] = '1';
					output[outputPos++] = '4';
					output[outputPos++] = '.';
					break;
				case '\u2481': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '4';
					output[outputPos++] = ')';
					break;
				case '\u246E': 
				case '\u24EF': 
					output[outputPos++] = '1';
					output[outputPos++] = '5';
					break;
				case '\u2496': 
					output[outputPos++] = '1';
					output[outputPos++] = '5';
					output[outputPos++] = '.';
					break;
				case '\u2482': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '5';
					output[outputPos++] = ')';
					break;
				case '\u246F': 
				case '\u24F0': 
					output[outputPos++] = '1';
					output[outputPos++] = '6';
					break;
				case '\u2497': 
					output[outputPos++] = '1';
					output[outputPos++] = '6';
					output[outputPos++] = '.';
					break;
				case '\u2483': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '6';
					output[outputPos++] = ')';
					break;
				case '\u2470': 
				case '\u24F1': 
					output[outputPos++] = '1';
					output[outputPos++] = '7';
					break;
				case '\u2498': 
					output[outputPos++] = '1';
					output[outputPos++] = '7';
					output[outputPos++] = '.';
					break;
				case '\u2484': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '7';
					output[outputPos++] = ')';
					break;
				case '\u2471': 
				case '\u24F2': 
					output[outputPos++] = '1';
					output[outputPos++] = '8';
					break;
				case '\u2499': 
					output[outputPos++] = '1';
					output[outputPos++] = '8';
					output[outputPos++] = '.';
					break;
				case '\u2485': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '8';
					output[outputPos++] = ')';
					break;
				case '\u2472': 
				case '\u24F3': 
					output[outputPos++] = '1';
					output[outputPos++] = '9';
					break;
				case '\u249A': 
					output[outputPos++] = '1';
					output[outputPos++] = '9';
					output[outputPos++] = '.';
					break;
				case '\u2486': 
					output[outputPos++] = '(';
					output[outputPos++] = '1';
					output[outputPos++] = '9';
					output[outputPos++] = ')';
					break;
				case '\u2473': 
				case '\u24F4': 
					output[outputPos++] = '2';
					output[outputPos++] = '0';
					break;
				case '\u249B': 
					output[outputPos++] = '2';
					output[outputPos++] = '0';
					output[outputPos++] = '.';
					break;
				case '\u2487': 
					output[outputPos++] = '(';
					output[outputPos++] = '2';
					output[outputPos++] = '0';
					output[outputPos++] = ')';
					break;
				case '\u00AB': 
				case '\u00BB': 
								
				case '\u201C': 
				case '\u201D': 
				case '\u201E': 
				case '\u2033': 
				case '\u2036': 
				case '\u275D': 
								
				case '\u275E': 
								
				case '\u276E': 
								
				case '\u276F': 
								
				case '\uFF02': 
					output[outputPos++] = '"';
					break;
				case '\u2018': 
				case '\u2019': 
				case '\u201A': 
				case '\u201B': 
				case '\u2032': 
				case '\u2035': 
				case '\u2039': 
								
				case '\u203A': 
								
				case '\u275B': 
								
				case '\u275C': 
								
				case '\uFF07': 
					output[outputPos++] = '\'';
					break;
				case '\u2010': 
				case '\u2011': 
				case '\u2012': 
				case '\u2013': 
				case '\u2014': 
				case '\u207B': 
				case '\u208B': 
				case '\uFF0D': 
					output[outputPos++] = '-';
					break;
				case '\u2045': 
				case '\u2772': 
								
				case '\uFF3B': 
					output[outputPos++] = '[';
					break;
				case '\u2046': 
				case '\u2773': 
								
				case '\uFF3D': 
					output[outputPos++] = ']';
					break;
				case '\u207D': 
				case '\u208D': 
				case '\u2768': 
				case '\u276A': 
								
				case '\uFF08': 
					output[outputPos++] = '(';
					break;
				case '\u2E28': 
					output[outputPos++] = '(';
					output[outputPos++] = '(';
					break;
				case '\u207E': 
				case '\u208E': 
				case '\u2769': 
				case '\u276B': 
								
				case '\uFF09': 
					output[outputPos++] = ')';
					break;
				case '\u2E29': 
					output[outputPos++] = ')';
					output[outputPos++] = ')';
					break;
				case '\u276C': 
								
				case '\u2770': 
								
				case '\uFF1C': 
					output[outputPos++] = '<';
					break;
				case '\u276D': 
								
				case '\u2771': 
								
				case '\uFF1E': 
					output[outputPos++] = '>';
					break;
				case '\u2774': 
				case '\uFF5B': 
					output[outputPos++] = '{';
					break;
				case '\u2775': 
				case '\uFF5D': 
					output[outputPos++] = '}';
					break;
				case '\u207A': 
				case '\u208A': 
				case '\uFF0B': 
					output[outputPos++] = '+';
					break;
				case '\u207C': 
				case '\u208C': 
				case '\uFF1D': 
					output[outputPos++] = '=';
					break;
				case '\uFF01': 
					output[outputPos++] = '!';
					break;
				case '\u203C': 
					output[outputPos++] = '!';
					output[outputPos++] = '!';
					break;
				case '\u2049': 
					output[outputPos++] = '!';
					output[outputPos++] = '?';
					break;
				case '\uFF03': 
					output[outputPos++] = '#';
					break;
				case '\uFF04': 
					output[outputPos++] = '$';
					break;
				case '\u2052': 
				case '\uFF05': 
					output[outputPos++] = '%';
					break;
				case '\uFF06': 
					output[outputPos++] = '&';
					break;
				case '\u204E': 
				case '\uFF0A': 
					output[outputPos++] = '*';
					break;
				case '\uFF0C': 
					output[outputPos++] = ',';
					break;
				case '\uFF0E': 
					output[outputPos++] = '.';
					break;
				case '\u2044': 
				case '\uFF0F': 
					output[outputPos++] = '/';
					break;
				case '\uFF1A': 
					output[outputPos++] = ':';
					break;
				case '\u204F': 
				case '\uFF1B': 
					output[outputPos++] = ';';
					break;
				case '\uFF1F': 
					output[outputPos++] = '?';
					break;
				case '\u2047': 
					output[outputPos++] = '?';
					output[outputPos++] = '?';
					break;
				case '\u2048': 
					output[outputPos++] = '?';
					output[outputPos++] = '!';
					break;
				case '\uFF20': 
					output[outputPos++] = '@';
					break;
				case '\uFF3C': 
					output[outputPos++] = '\\';
					break;
				case '\u2038': 
				case '\uFF3E': 
					output[outputPos++] = '^';
					break;
				case '\uFF3F': 
					output[outputPos++] = '_';
					break;
				case '\u2053': 
				case '\uFF5E': 
					output[outputPos++] = '~';
					break;
				default:
					output[outputPos++] = c;
					break;
				}
			}
		}
		return outputPos;
	}
}
