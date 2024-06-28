/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.standard.std31;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.standard.StandardTokenizer;
import com.shatam.shatamindex.standard.StandardTokenizerInterface;

@Deprecated
public final class StandardTokenizerImpl31 implements
		StandardTokenizerInterface {

	public static final int YYEOF = -1;

	private static final int ZZ_BUFFERSIZE = 16384;

	public static final int YYINITIAL = 0;

	private static final int ZZ_LEXSTATE[] = { 0, 0 };

	private static final String ZZ_CMAP_PACKED = "\47\0\1\140\4\0\1\137\1\0\1\140\1\0\12\134\1\136\1\137"
			+ "\5\0\32\132\4\0\1\141\1\0\32\132\57\0\1\132\2\0\1\133"
			+ "\7\0\1\132\1\0\1\136\2\0\1\132\5\0\27\132\1\0\37\132"
			+ "\1\0\u01ca\132\4\0\14\132\16\0\5\132\7\0\1\132\1\0\1\132"
			+ "\21\0\160\133\5\132\1\0\2\132\2\0\4\132\1\137\7\0\1\132"
			+ "\1\136\3\132\1\0\1\132\1\0\24\132\1\0\123\132\1\0\213\132"
			+ "\1\0\7\133\236\132\11\0\46\132\2\0\1\132\7\0\47\132\1\0"
			+ "\1\137\7\0\55\133\1\0\1\133\1\0\2\133\1\0\2\133\1\0"
			+ "\1\133\10\0\33\132\5\0\4\132\1\136\13\0\4\133\10\0\2\137"
			+ "\2\0\13\133\5\0\53\132\25\133\12\134\1\0\1\134\1\137\1\0"
			+ "\2\132\1\133\143\132\1\0\1\132\7\133\1\133\1\0\6\133\2\132"
			+ "\2\133\1\0\4\133\2\132\12\134\3\132\2\0\1\132\17\0\1\133"
			+ "\1\132\1\133\36\132\33\133\2\0\131\132\13\133\1\132\16\0\12\134"
			+ "\41\132\11\133\2\132\2\0\1\137\1\0\1\132\5\0\26\132\4\133"
			+ "\1\132\11\133\1\132\3\133\1\132\5\133\22\0\31\132\3\133\244\0"
			+ "\4\133\66\132\3\133\1\132\22\133\1\132\7\133\12\132\2\133\2\0"
			+ "\12\134\1\0\7\132\1\0\7\132\1\0\3\133\1\0\10\132\2\0"
			+ "\2\132\2\0\26\132\1\0\7\132\1\0\1\132\3\0\4\132\2\0"
			+ "\1\133\1\132\7\133\2\0\2\133\2\0\3\133\1\132\10\0\1\133"
			+ "\4\0\2\132\1\0\3\132\2\133\2\0\12\134\2\132\17\0\3\133"
			+ "\1\0\6\132\4\0\2\132\2\0\26\132\1\0\7\132\1\0\2\132"
			+ "\1\0\2\132\1\0\2\132\2\0\1\133\1\0\5\133\4\0\2\133"
			+ "\2\0\3\133\3\0\1\133\7\0\4\132\1\0\1\132\7\0\12\134"
			+ "\2\133\3\132\1\133\13\0\3\133\1\0\11\132\1\0\3\132\1\0"
			+ "\26\132\1\0\7\132\1\0\2\132\1\0\5\132\2\0\1\133\1\132"
			+ "\10\133\1\0\3\133\1\0\3\133\2\0\1\132\17\0\2\132\2\133"
			+ "\2\0\12\134\21\0\3\133\1\0\10\132\2\0\2\132\2\0\26\132"
			+ "\1\0\7\132\1\0\2\132\1\0\5\132\2\0\1\133\1\132\7\133"
			+ "\2\0\2\133\2\0\3\133\10\0\2\133\4\0\2\132\1\0\3\132"
			+ "\2\133\2\0\12\134\1\0\1\132\20\0\1\133\1\132\1\0\6\132"
			+ "\3\0\3\132\1\0\4\132\3\0\2\132\1\0\1\132\1\0\2\132"
			+ "\3\0\2\132\3\0\3\132\3\0\14\132\4\0\5\133\3\0\3\133"
			+ "\1\0\4\133\2\0\1\132\6\0\1\133\16\0\12\134\21\0\3\133"
			+ "\1\0\10\132\1\0\3\132\1\0\27\132\1\0\12\132\1\0\5\132"
			+ "\3\0\1\132\7\133\1\0\3\133\1\0\4\133\7\0\2\133\1\0"
			+ "\2\132\6\0\2\132\2\133\2\0\12\134\22\0\2\133\1\0\10\132"
			+ "\1\0\3\132\1\0\27\132\1\0\12\132\1\0\5\132\2\0\1\133"
			+ "\1\132\7\133\1\0\3\133\1\0\4\133\7\0\2\133\7\0\1\132"
			+ "\1\0\2\132\2\133\2\0\12\134\1\0\2\132\17\0\2\133\1\0"
			+ "\10\132\1\0\3\132\1\0\51\132\2\0\1\132\7\133\1\0\3\133"
			+ "\1\0\4\133\1\132\10\0\1\133\10\0\2\132\2\133\2\0\12\134"
			+ "\12\0\6\132\2\0\2\133\1\0\22\132\3\0\30\132\1\0\11\132"
			+ "\1\0\1\132\2\0\7\132\3\0\1\133\4\0\6\133\1\0\1\133"
			+ "\1\0\10\133\22\0\2\133\15\0\60\142\1\143\2\142\7\143\5\0"
			+ "\7\142\10\143\1\0\12\134\47\0\2\142\1\0\1\142\2\0\2\142"
			+ "\1\0\1\142\2\0\1\142\6\0\4\142\1\0\7\142\1\0\3\142"
			+ "\1\0\1\142\1\0\1\142\2\0\2\142\1\0\4\142\1\143\2\142"
			+ "\6\143\1\0\2\143\1\142\2\0\5\142\1\0\1\142\1\0\6\143"
			+ "\2\0\12\134\2\0\2\142\42\0\1\132\27\0\2\133\6\0\12\134"
			+ "\13\0\1\133\1\0\1\133\1\0\1\133\4\0\2\133\10\132\1\0"
			+ "\44\132\4\0\24\133\1\0\2\133\5\132\13\133\1\0\44\133\11\0"
			+ "\1\133\71\0\53\142\24\143\1\142\12\134\6\0\6\142\4\143\4\142"
			+ "\3\143\1\142\3\143\2\142\7\143\3\142\4\143\15\142\14\143\1\142"
			+ "\1\143\12\134\4\143\2\142\46\132\12\0\53\132\1\0\1\132\3\0"
			+ "\u0100\146\111\132\1\0\4\132\2\0\7\132\1\0\1\132\1\0\4\132"
			+ "\2\0\51\132\1\0\4\132\2\0\41\132\1\0\4\132\2\0\7\132"
			+ "\1\0\1\132\1\0\4\132\2\0\17\132\1\0\71\132\1\0\4\132"
			+ "\2\0\103\132\2\0\3\133\40\0\20\132\20\0\125\132\14\0\u026c\132"
			+ "\2\0\21\132\1\0\32\132\5\0\113\132\3\0\3\132\17\0\15\132"
			+ "\1\0\4\132\3\133\13\0\22\132\3\133\13\0\22\132\2\133\14\0"
			+ "\15\132\1\0\3\132\1\0\2\133\14\0\64\142\2\143\36\143\3\0"
			+ "\1\142\4\0\1\142\1\143\2\0\12\134\41\0\3\133\2\0\12\134"
			+ "\6\0\130\132\10\0\51\132\1\133\1\132\5\0\106\132\12\0\35\132"
			+ "\3\0\14\133\4\0\14\133\12\0\12\134\36\142\2\0\5\142\13\0"
			+ "\54\142\4\0\21\143\7\142\2\143\6\0\12\134\1\142\3\0\2\142"
			+ "\40\0\27\132\5\133\4\0\65\142\12\143\1\0\35\143\2\0\1\133"
			+ "\12\134\6\0\12\134\6\0\16\142\122\0\5\133\57\132\21\133\7\132"
			+ "\4\0\12\134\21\0\11\133\14\0\3\133\36\132\12\133\3\0\2\132"
			+ "\12\134\6\0\46\132\16\133\14\0\44\132\24\133\10\0\12\134\3\0"
			+ "\3\132\12\134\44\132\122\0\3\133\1\0\25\133\4\132\1\133\4\132"
			+ "\1\133\15\0\300\132\47\133\25\0\4\133\u0116\132\2\0\6\132\2\0"
			+ "\46\132\2\0\6\132\2\0\10\132\1\0\1\132\1\0\1\132\1\0"
			+ "\1\132\1\0\37\132\2\0\65\132\1\0\7\132\1\0\1\132\3\0"
			+ "\3\132\1\0\7\132\3\0\4\132\2\0\6\132\4\0\15\132\5\0"
			+ "\3\132\1\0\7\132\17\0\2\133\2\133\10\0\2\140\12\0\1\140"
			+ "\2\0\1\136\2\0\5\133\20\0\2\141\3\0\1\137\17\0\1\141"
			+ "\13\0\5\133\5\0\6\133\1\0\1\132\15\0\1\132\20\0\15\132"
			+ "\63\0\41\133\21\0\1\132\4\0\1\132\2\0\12\132\1\0\1\132"
			+ "\3\0\5\132\6\0\1\132\1\0\1\132\1\0\1\132\1\0\4\132"
			+ "\1\0\13\132\2\0\4\132\5\0\5\132\4\0\1\132\21\0\51\132"
			+ "\u032d\0\64\132\u0716\0\57\132\1\0\57\132\1\0\205\132\6\0\4\132"
			+ "\3\133\16\0\46\132\12\0\66\132\11\0\1\132\17\0\1\133\27\132"
			+ "\11\0\7\132\1\0\7\132\1\0\7\132\1\0\7\132\1\0\7\132"
			+ "\1\0\7\132\1\0\7\132\1\0\7\132\1\0\40\133\57\0\1\132"
			+ "\120\0\32\144\1\0\131\144\14\0\326\144\57\0\1\132\1\0\1\144"
			+ "\31\0\11\144\4\133\2\133\1\0\5\135\2\0\3\144\1\132\1\132"
			+ "\4\0\126\145\2\0\2\133\2\135\3\145\133\135\1\0\4\135\5\0"
			+ "\51\132\3\0\136\146\21\0\33\132\65\0\20\135\37\0\101\0\37\0"
			+ "\121\0\57\135\1\0\130\135\250\0\u19b6\144\112\0\u51cc\144\64\0\u048d\132"
			+ "\103\0\56\132\2\0\u010d\132\3\0\20\132\12\134\2\132\24\0\57\132"
			+ "\4\133\11\0\2\133\1\0\31\132\10\0\120\132\2\133\45\0\11\132"
			+ "\2\0\147\132\2\0\4\132\1\0\2\132\16\0\12\132\120\0\10\132"
			+ "\1\133\3\132\1\133\4\132\1\133\27\132\5\133\30\0\64\132\14\0"
			+ "\2\133\62\132\21\133\13\0\12\134\6\0\22\133\6\132\3\0\1\132"
			+ "\4\0\12\134\34\132\10\133\2\0\27\132\15\133\14\0\35\146\3\0"
			+ "\4\133\57\132\16\133\16\0\1\132\12\134\46\0\51\132\16\133\11\0"
			+ "\3\132\1\133\10\132\2\133\2\0\12\134\6\0\33\142\1\143\4\0"
			+ "\60\142\1\143\1\142\3\143\2\142\2\143\5\142\2\143\1\142\1\143"
			+ "\1\142\30\0\5\142\41\0\6\132\2\0\6\132\2\0\6\132\11\0"
			+ "\7\132\1\0\7\132\221\0\43\132\10\133\1\0\2\133\2\0\12\134"
			+ "\6\0\u2ba4\146\14\0\27\146\4\0\61\146\4\0\1\31\1\25\1\46"
			+ "\1\43\1\13\3\0\1\7\1\5\2\0\1\3\1\1\14\0\1\11"
			+ "\21\0\1\112\7\0\1\65\1\17\6\0\1\130\3\0\1\120\1\120"
			+ "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120"
			+ "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120"
			+ "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120"
			+ "\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\120\1\121"
			+ "\1\120\1\120\1\120\1\125\1\123\17\0\1\114\u02c1\0\1\70\277\0"
			+ "\1\113\1\71\1\2\3\124\2\35\1\124\1\35\2\124\1\14\21\124"
			+ "\2\60\7\73\1\72\7\73\7\52\1\15\1\52\1\75\2\45\1\44"
			+ "\1\75\1\45\1\44\10\75\2\63\5\61\2\54\5\61\1\6\10\37"
			+ "\5\21\3\27\12\106\20\27\3\42\32\30\1\26\2\24\2\110\1\111"
			+ "\2\110\2\111\2\110\1\111\3\24\1\16\2\24\12\64\1\74\1\41"
			+ "\1\34\1\64\6\41\1\34\66\41\5\115\6\103\1\51\4\103\2\51"
			+ "\10\103\1\51\7\100\1\12\2\100\32\103\1\12\4\100\1\12\5\102"
			+ "\1\101\1\102\3\101\7\102\1\101\23\102\5\67\3\102\6\67\2\67"
			+ "\6\66\10\66\2\100\7\66\36\100\4\66\102\100\15\115\1\77\2\115"
			+ "\1\131\3\117\1\115\2\117\5\115\4\117\4\116\1\115\3\116\1\115"
			+ "\5\116\26\56\4\23\1\105\2\104\4\122\1\104\2\122\3\76\33\122"
			+ "\35\55\3\122\35\126\3\122\6\126\2\33\31\126\1\33\17\126\6\122"
			+ "\4\22\1\10\37\22\1\10\4\22\25\62\1\127\11\62\21\55\5\62"
			+ "\1\57\12\40\13\62\4\55\1\50\6\55\12\122\17\55\1\47\3\53"
			+ "\15\20\11\36\1\32\24\36\2\20\11\36\1\32\31\36\1\32\4\20"
			+ "\4\36\2\32\2\107\1\4\5\107\52\4\u1900\0\u012e\144\2\0\76\144"
			+ "\2\0\152\144\46\0\7\132\14\0\5\132\5\0\1\132\1\133\12\132"
			+ "\1\0\15\132\1\0\5\132\1\0\1\132\1\0\2\132\1\0\2\132"
			+ "\1\0\154\132\41\0\u016b\132\22\0\100\132\2\0\66\132\50\0\14\132"
			+ "\4\0\20\133\1\137\2\0\1\136\1\137\13\0\7\133\14\0\2\141"
			+ "\30\0\3\141\1\137\1\0\1\140\1\0\1\137\1\136\32\0\5\132"
			+ "\1\0\207\132\2\0\1\133\7\0\1\140\4\0\1\137\1\0\1\140"
			+ "\1\0\12\134\1\136\1\137\5\0\32\132\4\0\1\141\1\0\32\132"
			+ "\13\0\70\135\2\133\37\146\3\0\6\146\2\0\6\146\2\0\6\146"
			+ "\2\0\3\146\34\0\3\133\4\0";

	private static final char[] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

	private static final int[] ZZ_ACTION = zzUnpackAction();

	private static final String ZZ_ACTION_PACKED_0 = "\1\0\23\1\1\2\1\3\1\4\1\1\1\5\1\6"
			+ "\1\7\1\10\15\0\1\2\1\0\1\2\10\0\1\3" + "\15\0\1\2\57\0";

	private static int[] zzUnpackAction() {
		int[] result = new int[114];
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

	private static final String ZZ_ROWMAP_PACKED_0 = "\0\0\0\147\0\316\0\u0135\0\u019c\0\u0203\0\u026a\0\u02d1"
			+ "\0\u0338\0\u039f\0\u0406\0\u046d\0\u04d4\0\u053b\0\u05a2\0\u0609"
			+ "\0\u0670\0\u06d7\0\u073e\0\u07a5\0\u080c\0\u0873\0\u08da\0\u0941"
			+ "\0\u09a8\0\147\0\147\0\u0a0f\0\316\0\u0135\0\u019c\0\u0203"
			+ "\0\u026a\0\u0a76\0\u0add\0\u0b44\0\u0bab\0\u046d\0\u0c12\0\u0c79"
			+ "\0\u0ce0\0\u0d47\0\u0dae\0\u0e15\0\u0e7c\0\u0338\0\u039f\0\u0ee3"
			+ "\0\u0f4a\0\u0fb1\0\u1018\0\u107f\0\u10e6\0\u114d\0\u11b4\0\u121b"
			+ "\0\u1282\0\u12e9\0\u1350\0\u13b7\0\u141e\0\u1485\0\u14ec\0\u1553"
			+ "\0\u15ba\0\u0941\0\u1621\0\u1688\0\u16ef\0\u1756\0\u17bd\0\u1824"
			+ "\0\u188b\0\u18f2\0\u1959\0\u19c0\0\u1a27\0\u1a8e\0\u1af5\0\u1b5c"
			+ "\0\u1bc3\0\u1c2a\0\u1c91\0\u1cf8\0\u1d5f\0\u1dc6\0\u1e2d\0\u1e94"
			+ "\0\u1efb\0\u1f62\0\u1fc9\0\u2030\0\u2097\0\u20fe\0\u2165\0\u21cc"
			+ "\0\u2233\0\u229a\0\u2301\0\u2368\0\u23cf\0\u2436\0\u249d\0\u2504"
			+ "\0\u256b\0\u25d2\0\u2639\0\u26a0\0\u2707\0\u276e\0\u27d5\0\u283c"
			+ "\0\u28a3\0\u290a";

	private static int[] zzUnpackRowMap() {
		int[] result = new int[114];
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

	private static final String ZZ_TRANS_PACKED_0 = "\1\2\1\3\1\2\1\4\1\2\1\5\1\2\1\6"
			+ "\1\2\1\7\1\2\1\10\3\2\1\11\5\2\1\12"
			+ "\3\2\1\13\11\2\1\14\2\2\1\15\43\2\1\16"
			+ "\1\2\1\17\3\2\1\20\1\21\1\2\1\22\1\2"
			+ "\1\23\2\2\1\24\1\2\1\25\1\2\1\26\1\27"
			+ "\3\2\1\30\2\31\1\32\1\33\1\34\151\0\1\25"
			+ "\11\0\1\25\20\0\1\25\22\0\1\25\10\0\3\25"
			+ "\17\0\1\25\10\0\1\25\24\0\1\25\1\0\1\25"
			+ "\1\0\1\25\1\0\1\25\1\0\1\25\1\0\3\25"
			+ "\1\0\5\25\1\0\3\25\1\0\11\25\1\0\2\25"
			+ "\1\0\16\25\1\0\2\25\1\0\21\25\1\0\1\25"
			+ "\1\0\3\25\2\0\1\25\1\0\1\25\1\0\2\25"
			+ "\1\0\1\25\17\0\1\25\3\0\1\25\5\0\2\25"
			+ "\3\0\1\25\13\0\1\25\1\0\1\25\4\0\2\25"
			+ "\4\0\1\25\1\0\1\25\3\0\2\25\1\0\1\25"
			+ "\5\0\3\25\1\0\1\25\15\0\1\25\10\0\1\25"
			+ "\24\0\1\25\3\0\1\25\1\0\1\25\1\0\1\25"
			+ "\1\0\3\25\2\0\4\25\1\0\3\25\2\0\3\25"
			+ "\1\0\4\25\1\0\2\25\2\0\3\25\1\0\11\25"
			+ "\1\0\2\25\1\0\16\25\1\0\2\25\1\0\1\25"
			+ "\1\0\3\25\2\0\1\25\1\0\1\25\1\0\2\25"
			+ "\1\0\1\25\17\0\1\25\3\0\1\25\3\0\1\25"
			+ "\1\0\3\25\2\0\1\25\1\0\2\25\1\0\3\25"
			+ "\3\0\2\25\1\0\1\25\1\0\2\25\1\0\2\25"
			+ "\3\0\2\25\1\0\1\25\1\0\1\25\1\0\2\25"
			+ "\1\0\2\25\1\0\2\25\1\0\5\25\1\0\5\25"
			+ "\1\0\2\25\1\0\2\25\1\0\1\25\1\0\3\25"
			+ "\4\0\1\25\4\0\1\25\31\0\3\25\5\0\1\25"
			+ "\1\0\1\25\1\0\1\25\4\0\1\25\14\0\1\25"
			+ "\5\0\1\25\11\0\2\25\12\0\1\26\1\0\2\25"
			+ "\12\0\1\25\24\0\1\25\1\0\1\26\7\0\2\25"
			+ "\2\0\5\25\2\0\2\25\4\0\6\25\1\0\2\25"
			+ "\4\0\5\25\1\0\5\25\1\0\2\25\1\0\3\25"
			+ "\1\0\4\25\1\0\5\25\1\26\1\0\1\25\1\0"
			+ "\1\25\1\0\3\25\2\0\1\25\1\0\1\25\1\0"
			+ "\1\25\2\0\1\25\17\0\1\25\3\0\1\25\5\0"
			+ "\2\25\3\0\1\25\4\0\3\25\4\0\1\25\1\0"
			+ "\1\25\2\0\1\25\1\0\2\25\4\0\1\25\1\0"
			+ "\1\25\3\0\2\25\1\0\1\25\5\0\3\25\1\0"
			+ "\1\25\10\0\1\25\1\0\2\26\1\0\1\25\10\0"
			+ "\1\25\24\0\1\25\3\0\1\25\6\0\2\25\5\0"
			+ "\1\25\1\0\1\25\1\0\1\25\1\0\11\25\2\0"
			+ "\1\25\4\0\1\25\4\0\6\25\2\0\1\25\1\0"
			+ "\1\25\1\0\3\25\3\0\2\25\4\0\3\25\1\0"
			+ "\1\25\10\0\1\25\1\0\2\25\21\0\1\25\11\0"
			+ "\2\25\17\0\1\25\6\0\2\25\4\0\1\25\5\0"
			+ "\1\25\2\0\1\25\5\0\3\25\1\0\1\25\15\0"
			+ "\1\25\10\0\1\25\24\0\1\25\3\0\1\25\5\0"
			+ "\1\25\32\0\15\25\5\0\3\25\1\0\1\25\5\0"
			+ "\1\25\7\0\1\25\2\0\1\25\5\0\1\25\2\0"
			+ "\1\25\1\0\1\25\106\0\1\33\21\0\1\27\35\0"
			+ "\1\32\3\0\1\32\3\0\1\32\1\0\3\32\2\0"
			+ "\1\32\2\0\1\32\1\0\3\32\3\0\2\32\1\0"
			+ "\1\32\1\0\2\32\1\0\2\32\3\0\2\32\1\0"
			+ "\1\32\3\0\2\32\1\0\2\32\1\0\2\32\1\0"
			+ "\5\32\1\0\5\32\2\0\1\32\1\0\2\32\1\0"
			+ "\1\32\1\0\3\32\4\0\1\32\4\0\1\32\17\0"
			+ "\1\32\1\0\1\32\1\0\1\32\1\0\1\32\1\0"
			+ "\1\32\1\0\3\32\1\0\5\32\1\0\3\32\1\0"
			+ "\11\32\1\0\2\32\1\0\16\32\1\0\2\32\1\0"
			+ "\21\32\1\0\1\32\1\0\3\32\2\0\1\32\1\0"
			+ "\1\32\1\0\2\32\1\0\1\32\17\0\1\32\1\0"
			+ "\1\32\1\0\1\32\3\0\1\32\1\0\3\32\1\0"
			+ "\2\32\1\0\2\32\1\0\3\32\1\0\11\32\1\0"
			+ "\2\32\1\0\16\32\1\0\2\32\1\0\21\32\1\0"
			+ "\1\32\1\0\3\32\2\0\1\32\1\0\1\32\1\0"
			+ "\2\32\1\0\1\32\17\0\1\32\11\0\1\32\20\0"
			+ "\1\32\33\0\1\32\21\0\1\32\10\0\1\32\24\0"
			+ "\1\32\1\0\1\32\1\0\1\32\1\0\1\32\1\0"
			+ "\1\32\1\0\3\32\1\0\5\32\1\0\3\32\1\0"
			+ "\6\32\1\0\2\32\1\0\2\32\1\0\10\32\1\0"
			+ "\5\32\1\0\2\32\1\0\21\32\1\0\1\32\1\0"
			+ "\3\32\2\0\1\32\1\0\1\32\1\0\2\32\1\0"
			+ "\1\32\146\0\1\33\16\0\1\35\1\0\1\36\1\0"
			+ "\1\37\1\0\1\40\1\0\1\41\1\0\1\42\3\0"
			+ "\1\43\5\0\1\44\3\0\1\45\11\0\1\46\2\0"
			+ "\1\47\16\0\1\50\2\0\1\51\41\0\2\25\1\52"
			+ "\1\0\1\53\1\0\1\53\1\54\1\0\1\25\2\0"
			+ "\1\25\1\0\1\35\1\0\1\36\1\0\1\37\1\0"
			+ "\1\40\1\0\1\41\1\0\1\55\3\0\1\56\5\0"
			+ "\1\57\3\0\1\60\11\0\1\46\2\0\1\61\16\0"
			+ "\1\62\2\0\1\63\41\0\1\25\2\26\2\0\2\64"
			+ "\1\65\1\0\1\26\2\0\1\25\13\0\1\66\15\0"
			+ "\1\67\14\0\1\70\16\0\1\71\2\0\1\72\21\0"
			+ "\1\73\20\0\1\27\1\0\1\27\3\0\1\54\1\0"
			+ "\1\27\4\0\1\35\1\0\1\36\1\0\1\37\1\0"
			+ "\1\40\1\0\1\41\1\0\1\74\3\0\1\56\5\0"
			+ "\1\57\3\0\1\75\11\0\1\46\2\0\1\76\16\0"
			+ "\1\77\2\0\1\100\21\0\1\101\17\0\1\25\1\102"
			+ "\1\26\1\103\3\0\1\102\1\0\1\102\2\0\1\25"
			+ "\142\0\2\31\4\0\1\35\1\0\1\36\1\0\1\37"
			+ "\1\0\1\40\1\0\1\41\1\0\1\104\3\0\1\43"
			+ "\5\0\1\44\3\0\1\105\11\0\1\46\2\0\1\106"
			+ "\16\0\1\107\2\0\1\110\41\0\1\25\1\34\1\52"
			+ "\1\0\1\53\1\0\1\53\1\54\1\0\1\34\2\0"
			+ "\1\34\2\0\1\25\11\0\3\25\5\0\1\25\1\0"
			+ "\1\25\1\0\1\25\4\0\1\25\4\0\1\25\1\0"
			+ "\2\25\4\0\1\25\5\0\1\25\3\0\1\25\4\0"
			+ "\5\25\10\0\1\52\1\0\2\25\1\0\1\25\10\0"
			+ "\1\25\24\0\1\25\1\0\1\52\7\0\2\25\2\0"
			+ "\5\25\2\0\2\25\4\0\6\25\1\0\2\25\4\0"
			+ "\5\25\1\0\5\25\1\0\2\25\1\0\3\25\1\0"
			+ "\4\25\1\0\5\25\1\52\1\0\1\25\1\0\1\25"
			+ "\1\0\3\25\2\0\1\25\1\0\1\25\1\0\1\25"
			+ "\2\0\1\25\17\0\1\25\3\0\1\25\5\0\2\25"
			+ "\3\0\1\25\4\0\3\25\4\0\1\25\1\0\1\25"
			+ "\2\0\1\25\1\0\2\25\4\0\1\25\1\0\1\25"
			+ "\3\0\2\25\1\0\1\25\5\0\3\25\1\0\1\25"
			+ "\10\0\1\25\1\0\2\52\1\0\1\25\10\0\1\25"
			+ "\24\0\1\25\3\0\1\25\6\0\2\25\5\0\1\25"
			+ "\1\0\1\25\1\0\1\25\1\0\11\25\2\0\1\25"
			+ "\4\0\1\25\4\0\6\25\2\0\1\25\1\0\1\25"
			+ "\1\0\3\25\1\0\1\25\1\0\2\25\4\0\3\25"
			+ "\1\0\1\25\10\0\1\25\1\0\2\25\21\0\1\25"
			+ "\3\0\1\25\5\0\1\25\32\0\15\25\5\0\3\25"
			+ "\1\0\1\25\5\0\3\25\5\0\1\25\2\0\2\25"
			+ "\4\0\1\25\2\0\1\25\1\0\1\25\103\0\2\25"
			+ "\6\0\1\25\56\0\1\25\3\0\1\25\2\0\1\25"
			+ "\3\0\1\25\5\0\1\25\7\0\1\25\4\0\2\25"
			+ "\3\0\2\25\1\0\1\25\4\0\1\25\1\0\1\25"
			+ "\2\0\2\25\1\0\3\25\1\0\1\25\2\0\4\25"
			+ "\2\0\1\25\41\0\1\35\1\0\1\36\1\0\1\37"
			+ "\1\0\1\40\1\0\1\41\1\0\1\111\3\0\1\43"
			+ "\5\0\1\44\3\0\1\112\11\0\1\46\2\0\1\113"
			+ "\16\0\1\114\2\0\1\115\41\0\1\25\2\52\2\0"
			+ "\2\116\1\54\1\0\1\52\2\0\1\25\1\0\1\35"
			+ "\1\0\1\36\1\0\1\37\1\0\1\40\1\0\1\41"
			+ "\1\0\1\117\3\0\1\120\5\0\1\121\3\0\1\122"
			+ "\11\0\1\46\2\0\1\123\16\0\1\124\2\0\1\125"
			+ "\41\0\1\25\1\53\7\0\1\53\2\0\1\25\1\0"
			+ "\1\35\1\0\1\36\1\0\1\37\1\0\1\40\1\0"
			+ "\1\41\1\0\1\126\3\0\1\43\5\0\1\44\3\0"
			+ "\1\127\11\0\1\46\2\0\1\130\16\0\1\131\2\0"
			+ "\1\132\21\0\1\101\17\0\1\25\1\54\1\52\1\103"
			+ "\3\0\1\54\1\0\1\54\2\0\1\25\2\0\1\26"
			+ "\11\0\3\25\5\0\1\25\1\0\1\25\1\0\1\25"
			+ "\4\0\1\25\4\0\1\26\1\0\2\26\4\0\1\25"
			+ "\5\0\1\25\3\0\1\26\4\0\1\26\2\25\2\26"
			+ "\10\0\1\26\1\0\2\25\1\0\1\26\10\0\1\25"
			+ "\24\0\1\25\3\0\1\25\6\0\2\25\5\0\1\25"
			+ "\1\0\1\25\1\0\1\25\1\0\11\25\2\0\1\25"
			+ "\4\0\1\25\4\0\6\25\2\0\1\25\1\0\1\25"
			+ "\1\0\3\25\1\0\1\26\1\0\2\25\4\0\3\25"
			+ "\1\0\1\25\10\0\1\25\1\0\2\25\21\0\1\25"
			+ "\3\0\1\25\5\0\1\25\32\0\15\25\5\0\3\25"
			+ "\1\0\1\25\5\0\1\25\2\26\5\0\1\25\2\0"
			+ "\1\25\1\26\4\0\1\25\2\0\1\25\1\0\1\25"
			+ "\103\0\2\26\6\0\1\26\56\0\1\26\3\0\1\26"
			+ "\2\0\1\26\3\0\1\26\5\0\1\26\7\0\1\26"
			+ "\4\0\2\26\3\0\2\26\1\0\1\26\4\0\1\26"
			+ "\1\0\1\26\2\0\2\26\1\0\3\26\1\0\1\26"
			+ "\2\0\4\26\2\0\1\26\53\0\1\133\3\0\1\134"
			+ "\5\0\1\135\3\0\1\136\14\0\1\137\16\0\1\140"
			+ "\2\0\1\141\42\0\1\64\1\26\6\0\1\64\4\0"
			+ "\1\35\1\0\1\36\1\0\1\37\1\0\1\40\1\0"
			+ "\1\41\1\0\1\142\3\0\1\56\5\0\1\57\3\0"
			+ "\1\143\11\0\1\46\2\0\1\144\16\0\1\145\2\0"
			+ "\1\146\21\0\1\101\17\0\1\25\1\65\1\26\1\103"
			+ "\3\0\1\65\1\0\1\65\2\0\1\25\2\0\1\27"
			+ "\37\0\1\27\1\0\2\27\16\0\1\27\4\0\1\27"
			+ "\2\0\2\27\15\0\1\27\132\0\1\27\153\0\2\27"
			+ "\11\0\1\27\115\0\2\27\6\0\1\27\56\0\1\27"
			+ "\3\0\1\27\2\0\1\27\3\0\1\27\5\0\1\27"
			+ "\7\0\1\27\4\0\2\27\3\0\2\27\1\0\1\27"
			+ "\4\0\1\27\1\0\1\27\2\0\2\27\1\0\3\27"
			+ "\1\0\1\27\2\0\4\27\2\0\1\27\153\0\1\27"
			+ "\35\0\1\102\11\0\3\25\5\0\1\25\1\0\1\25"
			+ "\1\0\1\25\4\0\1\25\4\0\1\102\1\0\2\102"
			+ "\4\0\1\25\5\0\1\25\3\0\1\102\4\0\1\102"
			+ "\2\25\2\102\10\0\1\26\1\0\2\25\1\0\1\102"
			+ "\10\0\1\25\24\0\1\25\3\0\1\25\6\0\2\25"
			+ "\5\0\1\25\1\0\1\25\1\0\1\25\1\0\11\25"
			+ "\2\0\1\25\4\0\1\25\4\0\6\25\2\0\1\25"
			+ "\1\0\1\25\1\0\3\25\1\0\1\102\1\0\2\25"
			+ "\4\0\3\25\1\0\1\25\10\0\1\25\1\0\2\25"
			+ "\21\0\1\25\3\0\1\25\5\0\1\25\32\0\15\25"
			+ "\5\0\3\25\1\0\1\25\5\0\1\25\2\102\5\0"
			+ "\1\25\2\0\1\25\1\102\4\0\1\25\2\0\1\25"
			+ "\1\0\1\25\103\0\2\102\6\0\1\102\56\0\1\102"
			+ "\3\0\1\102\2\0\1\102\3\0\1\102\5\0\1\102"
			+ "\7\0\1\102\4\0\2\102\3\0\2\102\1\0\1\102"
			+ "\4\0\1\102\1\0\1\102\2\0\2\102\1\0\3\102"
			+ "\1\0\1\102\2\0\4\102\2\0\1\102\153\0\1\103"
			+ "\46\0\1\147\15\0\1\150\14\0\1\151\16\0\1\152"
			+ "\2\0\1\153\21\0\1\101\20\0\1\103\1\0\1\103"
			+ "\3\0\1\54\1\0\1\103\5\0\1\34\11\0\3\25"
			+ "\5\0\1\25\1\0\1\25\1\0\1\25\4\0\1\25"
			+ "\4\0\1\34\1\0\2\34\4\0\1\25\5\0\1\25"
			+ "\3\0\1\34\4\0\1\34\2\25\2\34\10\0\1\52"
			+ "\1\0\2\25\1\0\1\34\10\0\1\25\24\0\1\25"
			+ "\3\0\1\25\6\0\2\25\5\0\1\25\1\0\1\25"
			+ "\1\0\1\25\1\0\11\25\2\0\1\25\4\0\1\25"
			+ "\4\0\6\25\2\0\1\25\1\0\1\25\1\0\3\25"
			+ "\1\0\1\34\1\0\2\25\4\0\3\25\1\0\1\25"
			+ "\10\0\1\25\1\0\2\25\21\0\1\25\3\0\1\25"
			+ "\5\0\1\25\32\0\15\25\5\0\3\25\1\0\1\25"
			+ "\5\0\1\25\2\34\5\0\1\25\2\0\1\25\1\34"
			+ "\4\0\1\25\2\0\1\25\1\0\1\25\103\0\2\34"
			+ "\6\0\1\34\56\0\1\34\3\0\1\34\2\0\1\34"
			+ "\3\0\1\34\5\0\1\34\7\0\1\34\4\0\2\34"
			+ "\3\0\2\34\1\0\1\34\4\0\1\34\1\0\1\34"
			+ "\2\0\2\34\1\0\3\34\1\0\1\34\2\0\4\34"
			+ "\2\0\1\34\42\0\1\52\11\0\3\25\5\0\1\25"
			+ "\1\0\1\25\1\0\1\25\4\0\1\25\4\0\1\52"
			+ "\1\0\2\52\4\0\1\25\5\0\1\25\3\0\1\52"
			+ "\4\0\1\52\2\25\2\52\10\0\1\52\1\0\2\25"
			+ "\1\0\1\52\10\0\1\25\24\0\1\25\3\0\1\25"
			+ "\6\0\2\25\5\0\1\25\1\0\1\25\1\0\1\25"
			+ "\1\0\11\25\2\0\1\25\4\0\1\25\4\0\6\25"
			+ "\2\0\1\25\1\0\1\25\1\0\3\25\1\0\1\52"
			+ "\1\0\2\25\4\0\3\25\1\0\1\25\10\0\1\25"
			+ "\1\0\2\25\21\0\1\25\3\0\1\25\5\0\1\25"
			+ "\32\0\15\25\5\0\3\25\1\0\1\25\5\0\1\25"
			+ "\2\52\5\0\1\25\2\0\1\25\1\52\4\0\1\25"
			+ "\2\0\1\25\1\0\1\25\103\0\2\52\6\0\1\52"
			+ "\56\0\1\52\3\0\1\52\2\0\1\52\3\0\1\52"
			+ "\5\0\1\52\7\0\1\52\4\0\2\52\3\0\2\52"
			+ "\1\0\1\52\4\0\1\52\1\0\1\52\2\0\2\52"
			+ "\1\0\3\52\1\0\1\52\2\0\4\52\2\0\1\52"
			+ "\53\0\1\154\3\0\1\155\5\0\1\156\3\0\1\157"
			+ "\14\0\1\160\16\0\1\161\2\0\1\162\42\0\1\116"
			+ "\1\52\6\0\1\116\5\0\1\53\11\0\3\25\5\0"
			+ "\1\25\1\0\1\25\1\0\1\25\4\0\1\25\4\0"
			+ "\1\53\1\0\2\53\4\0\1\25\5\0\1\25\3\0"
			+ "\1\53\4\0\1\53\2\25\2\53\12\0\2\25\1\0"
			+ "\1\53\10\0\1\25\24\0\1\25\11\0\2\25\2\0"
			+ "\5\25\2\0\2\25\4\0\6\25\1\0\2\25\4\0"
			+ "\5\25\1\0\5\25\1\0\2\25\1\0\3\25\1\0"
			+ "\4\25\1\0\5\25\2\0\1\25\1\0\1\25\1\0"
			+ "\3\25\2\0\1\25\1\0\1\25\1\0\1\25\2\0"
			+ "\1\25\17\0\1\25\3\0\1\25\5\0\2\25\3\0"
			+ "\1\25\4\0\3\25\4\0\1\25\1\0\1\25\2\0"
			+ "\1\25\1\0\2\25\4\0\1\25\1\0\1\25\3\0"
			+ "\2\25\1\0\1\25\5\0\3\25\1\0\1\25\10\0"
			+ "\1\25\4\0\1\25\10\0\1\25\24\0\1\25\3\0"
			+ "\1\25\6\0\2\25\5\0\1\25\1\0\1\25\1\0"
			+ "\1\25\1\0\11\25\2\0\1\25\4\0\1\25\4\0"
			+ "\6\25\2\0\1\25\1\0\1\25\1\0\3\25\1\0"
			+ "\1\53\1\0\2\25\4\0\3\25\1\0\1\25\10\0"
			+ "\1\25\1\0\2\25\21\0\1\25\3\0\1\25\5\0"
			+ "\1\25\32\0\15\25\5\0\3\25\1\0\1\25\5\0"
			+ "\1\25\2\53\5\0\1\25\2\0\1\25\1\53\4\0"
			+ "\1\25\2\0\1\25\1\0\1\25\103\0\2\53\6\0"
			+ "\1\53\56\0\1\53\3\0\1\53\2\0\1\53\3\0"
			+ "\1\53\5\0\1\53\7\0\1\53\4\0\2\53\3\0"
			+ "\2\53\1\0\1\53\4\0\1\53\1\0\1\53\2\0"
			+ "\2\53\1\0\3\53\1\0\1\53\2\0\4\53\2\0"
			+ "\1\53\42\0\1\54\11\0\3\25\5\0\1\25\1\0"
			+ "\1\25\1\0\1\25\4\0\1\25\4\0\1\54\1\0"
			+ "\2\54\4\0\1\25\5\0\1\25\3\0\1\54\4\0"
			+ "\1\54\2\25\2\54\10\0\1\52\1\0\2\25\1\0"
			+ "\1\54\10\0\1\25\24\0\1\25\3\0\1\25\6\0"
			+ "\2\25\5\0\1\25\1\0\1\25\1\0\1\25\1\0"
			+ "\11\25\2\0\1\25\4\0\1\25\4\0\6\25\2\0"
			+ "\1\25\1\0\1\25\1\0\3\25\1\0\1\54\1\0"
			+ "\2\25\4\0\3\25\1\0\1\25\10\0\1\25\1\0"
			+ "\2\25\21\0\1\25\3\0\1\25\5\0\1\25\32\0"
			+ "\15\25\5\0\3\25\1\0\1\25\5\0\1\25\2\54"
			+ "\5\0\1\25\2\0\1\25\1\54\4\0\1\25\2\0"
			+ "\1\25\1\0\1\25\103\0\2\54\6\0\1\54\56\0"
			+ "\1\54\3\0\1\54\2\0\1\54\3\0\1\54\5\0"
			+ "\1\54\7\0\1\54\4\0\2\54\3\0\2\54\1\0"
			+ "\1\54\4\0\1\54\1\0\1\54\2\0\2\54\1\0"
			+ "\3\54\1\0\1\54\2\0\4\54\2\0\1\54\42\0"
			+ "\1\64\37\0\1\64\1\0\2\64\16\0\1\64\4\0"
			+ "\1\64\2\0\2\64\10\0\1\26\4\0\1\64\37\0"
			+ "\1\26\102\0\1\26\147\0\2\26\134\0\1\64\153\0"
			+ "\2\64\11\0\1\64\115\0\2\64\6\0\1\64\56\0"
			+ "\1\64\3\0\1\64\2\0\1\64\3\0\1\64\5\0"
			+ "\1\64\7\0\1\64\4\0\2\64\3\0\2\64\1\0"
			+ "\1\64\4\0\1\64\1\0\1\64\2\0\2\64\1\0"
			+ "\3\64\1\0\1\64\2\0\4\64\2\0\1\64\42\0"
			+ "\1\65\11\0\3\25\5\0\1\25\1\0\1\25\1\0"
			+ "\1\25\4\0\1\25\4\0\1\65\1\0\2\65\4\0"
			+ "\1\25\5\0\1\25\3\0\1\65\4\0\1\65\2\25"
			+ "\2\65\10\0\1\26\1\0\2\25\1\0\1\65\10\0"
			+ "\1\25\24\0\1\25\3\0\1\25\6\0\2\25\5\0"
			+ "\1\25\1\0\1\25\1\0\1\25\1\0\11\25\2\0"
			+ "\1\25\4\0\1\25\4\0\6\25\2\0\1\25\1\0"
			+ "\1\25\1\0\3\25\1\0\1\65\1\0\2\25\4\0"
			+ "\3\25\1\0\1\25\10\0\1\25\1\0\2\25\21\0"
			+ "\1\25\3\0\1\25\5\0\1\25\32\0\15\25\5\0"
			+ "\3\25\1\0\1\25\5\0\1\25\2\65\5\0\1\25"
			+ "\2\0\1\25\1\65\4\0\1\25\2\0\1\25\1\0"
			+ "\1\25\103\0\2\65\6\0\1\65\56\0\1\65\3\0"
			+ "\1\65\2\0\1\65\3\0\1\65\5\0\1\65\7\0"
			+ "\1\65\4\0\2\65\3\0\2\65\1\0\1\65\4\0"
			+ "\1\65\1\0\1\65\2\0\2\65\1\0\3\65\1\0"
			+ "\1\65\2\0\4\65\2\0\1\65\42\0\1\103\37\0"
			+ "\1\103\1\0\2\103\16\0\1\103\4\0\1\103\2\0"
			+ "\2\103\15\0\1\103\132\0\1\103\153\0\2\103\11\0"
			+ "\1\103\115\0\2\103\6\0\1\103\56\0\1\103\3\0"
			+ "\1\103\2\0\1\103\3\0\1\103\5\0\1\103\7\0"
			+ "\1\103\4\0\2\103\3\0\2\103\1\0\1\103\4\0"
			+ "\1\103\1\0\1\103\2\0\2\103\1\0\3\103\1\0"
			+ "\1\103\2\0\4\103\2\0\1\103\42\0\1\116\37\0"
			+ "\1\116\1\0\2\116\16\0\1\116\4\0\1\116\2\0"
			+ "\2\116\10\0\1\52\4\0\1\116\37\0\1\52\102\0"
			+ "\1\52\147\0\2\52\134\0\1\116\153\0\2\116\11\0"
			+ "\1\116\115\0\2\116\6\0\1\116\56\0\1\116\3\0"
			+ "\1\116\2\0\1\116\3\0\1\116\5\0\1\116\7\0"
			+ "\1\116\4\0\2\116\3\0\2\116\1\0\1\116\4\0"
			+ "\1\116\1\0\1\116\2\0\2\116\1\0\3\116\1\0"
			+ "\1\116\2\0\4\116\2\0\1\116\40\0";

	private static int[] zzUnpackTrans() {
		int[] result = new int[10609];
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

	private static final String ZZ_ATTRIBUTE_PACKED_0 = "\1\0\1\11\27\1\2\11\1\1\15\0\1\1\1\0"
			+ "\1\1\10\0\1\1\15\0\1\1\57\0";

	private static int[] zzUnpackAttribute() {
		int[] result = new int[114];
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

	public static final int WORD_TYPE = StandardTokenizer.ALPHANUM;

	public static final int NUMERIC_TYPE = StandardTokenizer.NUM;

	public static final int SOUTH_EAST_ASIAN_TYPE = StandardTokenizer.SOUTHEAST_ASIAN;

	public static final int IDEOGRAPHIC_TYPE = StandardTokenizer.IDEOGRAPHIC;

	public static final int HIRAGANA_TYPE = StandardTokenizer.HIRAGANA;

	public static final int KATAKANA_TYPE = StandardTokenizer.KATAKANA;

	public static final int HANGUL_TYPE = StandardTokenizer.HANGUL;

	public final int yychar() {
		return yychar;
	}

	public final void getText(CharTermAttribute t) {
		t.copyBuffer(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
	}

	public StandardTokenizerImpl31(java.io.Reader in) {
		this.zzReader = in;
	}

	public StandardTokenizerImpl31(java.io.InputStream in) {
		this(new java.io.InputStreamReader(in));
	}

	private static char[] zzUnpackCMap(String packed) {
		char[] map = new char[0x10000];
		int i = 0;
		int j = 0;
		while (i < 2650) {
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
			case 2: {
				return WORD_TYPE;
			}
			case 9:
				break;
			case 5: {
				return SOUTH_EAST_ASIAN_TYPE;
			}
			case 10:
				break;
			case 4: {
				return KATAKANA_TYPE;
			}
			case 11:
				break;
			case 6: {
				return IDEOGRAPHIC_TYPE;
			}
			case 12:
				break;
			case 8: {
				return HANGUL_TYPE;
			}
			case 13:
				break;
			case 3: {
				return NUMERIC_TYPE;
			}
			case 14:
				break;
			case 7: {
				return HIRAGANA_TYPE;
			}
			case 15:
				break;
			case 1: {
				break;
			}
			case 16:
				break;
			default:
				if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
					zzAtEOF = true;
					{
						return StandardTokenizerInterface.YYEOF;
					}
				} else {
					zzScanError(ZZ_NO_MATCH);
				}
			}
		}
	}

}
