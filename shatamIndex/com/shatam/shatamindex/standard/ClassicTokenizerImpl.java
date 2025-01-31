/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.standard;

import java.io.Reader;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;

class ClassicTokenizerImpl implements StandardTokenizerInterface {

	public static final int YYEOF = -1;

	private static final int ZZ_BUFFERSIZE = 16384;

	public static final int YYINITIAL = 0;

	private static final int ZZ_LEXSTATE[] = { 0, 0 };

	private static final String ZZ_CMAP_PACKED = "\11\0\1\0\1\15\1\0\1\0\1\14\22\0\1\0\5\0\1\5"
			+ "\1\3\4\0\1\11\1\7\1\4\1\11\12\2\6\0\1\6\32\12"
			+ "\4\0\1\10\1\0\32\12\57\0\1\12\12\0\1\12\4\0\1\12"
			+ "\5\0\27\12\1\0\37\12\1\0\u0128\12\2\0\22\12\34\0\136\12"
			+ "\2\0\11\12\2\0\7\12\16\0\2\12\16\0\5\12\11\0\1\12"
			+ "\213\0\1\12\13\0\1\12\1\0\3\12\1\0\1\12\1\0\24\12"
			+ "\1\0\54\12\1\0\10\12\2\0\32\12\14\0\202\12\12\0\71\12"
			+ "\2\0\2\12\2\0\2\12\3\0\46\12\2\0\2\12\67\0\46\12"
			+ "\2\0\1\12\7\0\47\12\110\0\33\12\5\0\3\12\56\0\32\12"
			+ "\5\0\13\12\25\0\12\2\7\0\143\12\1\0\1\12\17\0\2\12"
			+ "\11\0\12\2\3\12\23\0\1\12\1\0\33\12\123\0\46\12\u015f\0"
			+ "\65\12\3\0\1\12\22\0\1\12\7\0\12\12\4\0\12\2\25\0"
			+ "\10\12\2\0\2\12\2\0\26\12\1\0\7\12\1\0\1\12\3\0"
			+ "\4\12\42\0\2\12\1\0\3\12\4\0\12\2\2\12\23\0\6\12"
			+ "\4\0\2\12\2\0\26\12\1\0\7\12\1\0\2\12\1\0\2\12"
			+ "\1\0\2\12\37\0\4\12\1\0\1\12\7\0\12\2\2\0\3\12"
			+ "\20\0\7\12\1\0\1\12\1\0\3\12\1\0\26\12\1\0\7\12"
			+ "\1\0\2\12\1\0\5\12\3\0\1\12\22\0\1\12\17\0\1\12"
			+ "\5\0\12\2\25\0\10\12\2\0\2\12\2\0\26\12\1\0\7\12"
			+ "\1\0\2\12\2\0\4\12\3\0\1\12\36\0\2\12\1\0\3\12"
			+ "\4\0\12\2\25\0\6\12\3\0\3\12\1\0\4\12\3\0\2\12"
			+ "\1\0\1\12\1\0\2\12\3\0\2\12\3\0\3\12\3\0\10\12"
			+ "\1\0\3\12\55\0\11\2\25\0\10\12\1\0\3\12\1\0\27\12"
			+ "\1\0\12\12\1\0\5\12\46\0\2\12\4\0\12\2\25\0\10\12"
			+ "\1\0\3\12\1\0\27\12\1\0\12\12\1\0\5\12\44\0\1\12"
			+ "\1\0\2\12\4\0\12\2\25\0\10\12\1\0\3\12\1\0\27\12"
			+ "\1\0\20\12\46\0\2\12\4\0\12\2\25\0\22\12\3\0\30\12"
			+ "\1\0\11\12\1\0\1\12\2\0\7\12\71\0\1\1\60\12\1\1"
			+ "\2\12\14\1\7\12\11\1\12\2\47\0\2\12\1\0\1\12\2\0"
			+ "\2\12\1\0\1\12\2\0\1\12\6\0\4\12\1\0\7\12\1\0"
			+ "\3\12\1\0\1\12\1\0\1\12\2\0\2\12\1\0\4\12\1\0"
			+ "\2\12\11\0\1\12\2\0\5\12\1\0\1\12\11\0\12\2\2\0"
			+ "\2\12\42\0\1\12\37\0\12\2\26\0\10\12\1\0\42\12\35\0"
			+ "\4\12\164\0\42\12\1\0\5\12\1\0\2\12\25\0\12\2\6\0"
			+ "\6\12\112\0\46\12\12\0\47\12\11\0\132\12\5\0\104\12\5\0"
			+ "\122\12\6\0\7\12\1\0\77\12\1\0\1\12\1\0\4\12\2\0"
			+ "\7\12\1\0\1\12\1\0\4\12\2\0\47\12\1\0\1\12\1\0"
			+ "\4\12\2\0\37\12\1\0\1\12\1\0\4\12\2\0\7\12\1\0"
			+ "\1\12\1\0\4\12\2\0\7\12\1\0\7\12\1\0\27\12\1\0"
			+ "\37\12\1\0\1\12\1\0\4\12\2\0\7\12\1\0\47\12\1\0"
			+ "\23\12\16\0\11\2\56\0\125\12\14\0\u026c\12\2\0\10\12\12\0"
			+ "\32\12\5\0\113\12\225\0\64\12\54\0\12\2\46\0\12\2\6\0"
			+ "\130\12\10\0\51\12\u0557\0\234\12\4\0\132\12\6\0\26\12\2\0"
			+ "\6\12\2\0\46\12\2\0\6\12\2\0\10\12\1\0\1\12\1\0"
			+ "\1\12\1\0\1\12\1\0\37\12\2\0\65\12\1\0\7\12\1\0"
			+ "\1\12\3\0\3\12\1\0\7\12\3\0\4\12\2\0\6\12\4\0"
			+ "\15\12\5\0\3\12\1\0\7\12\202\0\1\12\202\0\1\12\4\0"
			+ "\1\12\2\0\12\12\1\0\1\12\3\0\5\12\6\0\1\12\1\0"
			+ "\1\12\1\0\1\12\1\0\4\12\1\0\3\12\1\0\7\12\u0ecb\0"
			+ "\2\12\52\0\5\12\12\0\1\13\124\13\10\13\2\13\2\13\132\13"
			+ "\1\13\3\13\6\13\50\13\3\13\1\0\136\12\21\0\30\12\70\0"
			+ "\20\13\u0100\0\200\13\200\0\u19b6\13\12\13\100\0\u51a6\13\132\13\u048d\12"
			+ "\u0773\0\u2ba4\12\u215c\0\u012e\13\322\13\7\12\14\0\5\12\5\0\1\12"
			+ "\1\0\12\12\1\0\15\12\1\0\5\12\1\0\1\12\1\0\2\12"
			+ "\1\0\2\12\1\0\154\12\41\0\u016b\12\22\0\100\12\2\0\66\12"
			+ "\50\0\14\12\164\0\3\12\1\0\1\12\1\0\207\12\23\0\12\2"
			+ "\7\0\32\12\6\0\32\12\12\0\1\13\72\13\37\12\3\0\6\12"
			+ "\2\0\6\12\2\0\6\12\2\0\3\12\43\0";

	private static final char[] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

	private static final int[] ZZ_ACTION = zzUnpackAction();

	private static final String ZZ_ACTION_PACKED_0 = "\1\0\1\1\3\2\1\3\1\1\13\0\1\2\3\4"
			+ "\2\0\1\5\1\0\1\5\3\4\6\5\1\6\1\4"
			+ "\2\7\1\10\1\0\1\10\3\0\2\10\1\11\1\12" + "\1\4";

	private static int[] zzUnpackAction() {
		int[] result = new int[51];
		int offset = 0;
		offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackAction(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			do
				result[j++] = value;
			while (--count > 0);
		}
		return j;
	}

	private static final int[] ZZ_ROWMAP = zzUnpackRowMap();

	private static final String ZZ_ROWMAP_PACKED_0 = "\0\0\0\16\0\34\0\52\0\70\0\16\0\106\0\124"
			+ "\0\142\0\160\0\176\0\214\0\232\0\250\0\266\0\304"
			+ "\0\322\0\340\0\356\0\374\0\u010a\0\u0118\0\u0126\0\u0134"
			+ "\0\u0142\0\u0150\0\u015e\0\u016c\0\u017a\0\u0188\0\u0196\0\u01a4"
			+ "\0\u01b2\0\u01c0\0\u01ce\0\u01dc\0\u01ea\0\u01f8\0\322\0\u0206"
			+ "\0\u0214\0\u0222\0\u0230\0\u023e\0\u024c\0\u025a\0\124\0\214"
			+ "\0\u0268\0\u0276\0\u0284";

	private static int[] zzUnpackRowMap() {
		int[] result = new int[51];
		int offset = 0;
		offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackRowMap(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int high = packed.charAt(i++) << 16;
			result[j++] = high | packed.charAt(i++);
		}
		return j;
	}

	private static final int[] ZZ_TRANS = zzUnpackTrans();

	private static final String ZZ_TRANS_PACKED_0 = "\1\2\1\3\1\4\7\2\1\5\1\6\1\7\1\2"
			+ "\17\0\2\3\1\0\1\10\1\0\1\11\2\12\1\13"
			+ "\1\3\4\0\1\3\1\4\1\0\1\14\1\0\1\11"
			+ "\2\15\1\16\1\4\4\0\1\3\1\4\1\17\1\20"
			+ "\1\21\1\22\2\12\1\13\1\23\20\0\1\2\1\0"
			+ "\1\24\1\25\7\0\1\26\4\0\2\27\7\0\1\27"
			+ "\4\0\1\30\1\31\7\0\1\32\5\0\1\33\7\0"
			+ "\1\13\4\0\1\34\1\35\7\0\1\36\4\0\1\37"
			+ "\1\40\7\0\1\41\4\0\1\42\1\43\7\0\1\44"
			+ "\15\0\1\45\4\0\1\24\1\25\7\0\1\46\15\0"
			+ "\1\47\4\0\2\27\7\0\1\50\4\0\1\3\1\4"
			+ "\1\17\1\10\1\21\1\22\2\12\1\13\1\23\4\0"
			+ "\2\24\1\0\1\51\1\0\1\11\2\52\1\0\1\24"
			+ "\4\0\1\24\1\25\1\0\1\53\1\0\1\11\2\54"
			+ "\1\55\1\25\4\0\1\24\1\25\1\0\1\51\1\0"
			+ "\1\11\2\52\1\0\1\26\4\0\2\27\1\0\1\56"
			+ "\2\0\1\56\2\0\1\27\4\0\2\30\1\0\1\52"
			+ "\1\0\1\11\2\52\1\0\1\30\4\0\1\30\1\31"
			+ "\1\0\1\54\1\0\1\11\2\54\1\55\1\31\4\0"
			+ "\1\30\1\31\1\0\1\52\1\0\1\11\2\52\1\0"
			+ "\1\32\5\0\1\33\1\0\1\55\2\0\3\55\1\33"
			+ "\4\0\2\34\1\0\1\57\1\0\1\11\2\12\1\13"
			+ "\1\34\4\0\1\34\1\35\1\0\1\60\1\0\1\11"
			+ "\2\15\1\16\1\35\4\0\1\34\1\35\1\0\1\57"
			+ "\1\0\1\11\2\12\1\13\1\36\4\0\2\37\1\0"
			+ "\1\12\1\0\1\11\2\12\1\13\1\37\4\0\1\37"
			+ "\1\40\1\0\1\15\1\0\1\11\2\15\1\16\1\40"
			+ "\4\0\1\37\1\40\1\0\1\12\1\0\1\11\2\12"
			+ "\1\13\1\41\4\0\2\42\1\0\1\13\2\0\3\13"
			+ "\1\42\4\0\1\42\1\43\1\0\1\16\2\0\3\16"
			+ "\1\43\4\0\1\42\1\43\1\0\1\13\2\0\3\13"
			+ "\1\44\6\0\1\17\6\0\1\45\4\0\1\24\1\25"
			+ "\1\0\1\61\1\0\1\11\2\52\1\0\1\26\4\0"
			+ "\2\27\1\0\1\56\2\0\1\56\2\0\1\50\4\0"
			+ "\2\24\7\0\1\24\4\0\2\30\7\0\1\30\4\0"
			+ "\2\34\7\0\1\34\4\0\2\37\7\0\1\37\4\0"
			+ "\2\42\7\0\1\42\4\0\2\62\7\0\1\62\4\0"
			+ "\2\24\7\0\1\63\4\0\2\62\1\0\1\56\2\0"
			+ "\1\56\2\0\1\62\4\0\2\24\1\0\1\61\1\0"
			+ "\1\11\2\52\1\0\1\24\3\0";

	private static int[] zzUnpackTrans() {
		int[] result = new int[658];
		int offset = 0;
		offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackTrans(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			value--;
			do
				result[j++] = value;
			while (--count > 0);
		}
		return j;
	}

	private static final int ZZ_UNKNOWN_ERROR = 0;
	private static final int ZZ_NO_MATCH = 1;
	private static final int ZZ_PUSHBACK_2BIG = 2;

	private static final String ZZ_ERROR_MSG[] = {
			"Unkown internal scanner error", "Error: could not match input",
			"Error: pushback value was too large" };

	private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();

	private static final String ZZ_ATTRIBUTE_PACKED_0 = "\1\0\1\11\3\1\1\11\1\1\13\0\4\1\2\0"
			+ "\1\1\1\0\17\1\1\0\1\1\3\0\5\1";

	private static int[] zzUnpackAttribute() {
		int[] result = new int[51];
		int offset = 0;
		offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackAttribute(String packed, int offset, int[] result) {
		int i = 0;
		int j = offset;
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			do
				result[j++] = value;
			while (--count > 0);
		}
		return j;
	}

	private java.io.Reader zzReader;

	private int zzState;

	private int zzLexicalState = YYINITIAL;

	private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

	private int zzMarkedPos;

	private int zzCurrentPos;

	private int zzStartRead;

	private int zzEndRead;

	private int yyline;

	private int yychar;

	private int yycolumn;

	private boolean zzAtBOL = true;

	private boolean zzAtEOF;

	private boolean zzEOFDone;

	public static final int ALPHANUM = StandardTokenizer.ALPHANUM;
	public static final int APOSTROPHE = StandardTokenizer.APOSTROPHE;
	public static final int ACRONYM = StandardTokenizer.ACRONYM;
	public static final int COMPANY = StandardTokenizer.COMPANY;
	public static final int EMAIL = StandardTokenizer.EMAIL;
	public static final int HOST = StandardTokenizer.HOST;
	public static final int NUM = StandardTokenizer.NUM;
	public static final int CJ = StandardTokenizer.CJ;

	@Deprecated
	public static final int ACRONYM_DEP = StandardTokenizer.ACRONYM_DEP;

	public static final String[] TOKEN_TYPES = StandardTokenizer.TOKEN_TYPES;

	public final int yychar() {
		return yychar;
	}

	public final void getText(CharTermAttribute t) {
		t.copyBuffer(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
	}

	ClassicTokenizerImpl(java.io.Reader in) {
		this.zzReader = in;
	}

	ClassicTokenizerImpl(java.io.InputStream in) {
		this(new java.io.InputStreamReader(in));
	}

	private static char[] zzUnpackCMap(String packed) {
		char[] map = new char[0x10000];
		int i = 0;
		int j = 0;
		while (i < 1154) {
			int count = packed.charAt(i++);
			char value = packed.charAt(i++);
			do
				map[j++] = value;
			while (--count > 0);
		}
		return map;
	}

	private boolean zzRefill() throws java.io.IOException {

		if (zzStartRead > 0) {
			System.arraycopy(zzBuffer, zzStartRead, zzBuffer, 0, zzEndRead
					- zzStartRead);

			zzEndRead -= zzStartRead;
			zzCurrentPos -= zzStartRead;
			zzMarkedPos -= zzStartRead;
			zzStartRead = 0;
		}

		if (zzCurrentPos >= zzBuffer.length) {

			char newBuffer[] = new char[zzCurrentPos * 2];
			System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
			zzBuffer = newBuffer;
		}

		int numRead = zzReader.read(zzBuffer, zzEndRead, zzBuffer.length
				- zzEndRead);

		if (numRead > 0) {
			zzEndRead += numRead;
			return false;
		}

		if (numRead == 0) {
			int c = zzReader.read();
			if (c == -1) {
				return true;
			} else {
				zzBuffer[zzEndRead++] = (char) c;
				return false;
			}
		}

		return true;
	}

	public final void yyclose() throws java.io.IOException {
		zzAtEOF = true;
		zzEndRead = zzStartRead;

		if (zzReader != null)
			zzReader.close();
	}

	public final void yyreset(java.io.Reader reader) {
		zzReader = reader;
		zzAtBOL = true;
		zzAtEOF = false;
		zzEOFDone = false;
		zzEndRead = zzStartRead = 0;
		zzCurrentPos = zzMarkedPos = 0;
		yyline = yychar = yycolumn = 0;
		zzLexicalState = YYINITIAL;
		if (zzBuffer.length > ZZ_BUFFERSIZE)
			zzBuffer = new char[ZZ_BUFFERSIZE];
	}

	public final int yystate() {
		return zzLexicalState;
	}

	public final void yybegin(int newState) {
		zzLexicalState = newState;
	}

	public final String yytext() {
		return new String(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
	}

	public final char yycharat(int pos) {
		return zzBuffer[zzStartRead + pos];
	}

	public final int yylength() {
		return zzMarkedPos - zzStartRead;
	}

	private void zzScanError(int errorCode) {
		String message;
		try {
			message = ZZ_ERROR_MSG[errorCode];
		} catch (ArrayIndexOutOfBoundsException e) {
			message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
		}

		throw new Error(message);
	}

	public void yypushback(int number) {
		if (number > yylength())
			zzScanError(ZZ_PUSHBACK_2BIG);

		zzMarkedPos -= number;
	}

	public int getNextToken() throws java.io.IOException {
		int zzInput;
		int zzAction;

		int zzCurrentPosL;
		int zzMarkedPosL;
		int zzEndReadL = zzEndRead;
		char[] zzBufferL = zzBuffer;
		char[] zzCMapL = ZZ_CMAP;

		int[] zzTransL = ZZ_TRANS;
		int[] zzRowMapL = ZZ_ROWMAP;
		int[] zzAttrL = ZZ_ATTRIBUTE;

		while (true) {
			zzMarkedPosL = zzMarkedPos;

			yychar += zzMarkedPosL - zzStartRead;

			zzAction = -1;

			zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

			zzState = ZZ_LEXSTATE[zzLexicalState];

			int zzAttributes = zzAttrL[zzState];
			if ((zzAttributes & 1) == 1) {
				zzAction = zzState;
			}

			zzForAction: {
				while (true) {

					if (zzCurrentPosL < zzEndReadL)
						zzInput = zzBufferL[zzCurrentPosL++];
					else if (zzAtEOF) {
						zzInput = YYEOF;
						break zzForAction;
					} else {

						zzCurrentPos = zzCurrentPosL;
						zzMarkedPos = zzMarkedPosL;
						boolean eof = zzRefill();

						zzCurrentPosL = zzCurrentPos;
						zzMarkedPosL = zzMarkedPos;
						zzBufferL = zzBuffer;
						zzEndReadL = zzEndRead;
						if (eof) {
							zzInput = YYEOF;
							break zzForAction;
						} else {
							zzInput = zzBufferL[zzCurrentPosL++];
						}
					}
					int zzNext = zzTransL[zzRowMapL[zzState] + zzCMapL[zzInput]];
					if (zzNext == -1)
						break zzForAction;
					zzState = zzNext;

					zzAttributes = zzAttrL[zzState];
					if ((zzAttributes & 1) == 1) {
						zzAction = zzState;
						zzMarkedPosL = zzCurrentPosL;
						if ((zzAttributes & 8) == 8)
							break zzForAction;
					}

				}
			}

			zzMarkedPos = zzMarkedPosL;

			switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
			case 10: {
				return EMAIL;
			}
			case 11:
				break;
			case 2: {
				return ALPHANUM;
			}
			case 12:
				break;
			case 4: {
				return HOST;
			}
			case 13:
				break;
			case 8: {
				return ACRONYM_DEP;
			}
			case 14:
				break;
			case 5: {
				return NUM;
			}
			case 15:
				break;
			case 1: {
				break;
			}
			case 16:
				break;
			case 9: {
				return ACRONYM;
			}
			case 17:
				break;
			case 7: {
				return COMPANY;
			}
			case 18:
				break;
			case 6: {
				return APOSTROPHE;
			}
			case 19:
				break;
			case 3: {
				return CJ;
			}
			case 20:
				break;
			default:
				if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
					zzAtEOF = true;
					return YYEOF;
				} else {
					zzScanError(ZZ_NO_MATCH);
				}
			}
		}
	}

}
