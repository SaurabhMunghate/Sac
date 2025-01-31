/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

import java.io.IOException;
import java.io.StringReader;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.analysis.CachingTokenFilter;
import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.PositionIncrementAttribute;
import com.shatam.shatamindex.document.DateField;
import com.shatam.shatamindex.document.DateTools;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.BooleanClause;
import com.shatam.shatamindex.search.BooleanQuery;
import com.shatam.shatamindex.search.FuzzyQuery;
import com.shatam.shatamindex.search.MatchAllDocsQuery;
import com.shatam.shatamindex.search.MultiPhraseQuery;
import com.shatam.shatamindex.search.MultiTermQuery;
import com.shatam.shatamindex.search.PhraseQuery;
import com.shatam.shatamindex.search.PrefixQuery;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.TermQuery;
import com.shatam.shatamindex.search.TermRangeQuery;
import com.shatam.shatamindex.search.WildcardQuery;
import com.shatam.shatamindex.util.Version;
import com.shatam.shatamindex.util.VirtualMethod;

public class QueryParserTokenManager implements QueryParserConstants {

	public java.io.PrintStream debugStream = System.out;

	public void setDebugStream(java.io.PrintStream ds) {
		debugStream = ds;
	}

	private final int jjStopStringLiteralDfa_3(int pos, long active0) {
		switch (pos) {
		default:
			return -1;
		}
	}

	private final int jjStartNfa_3(int pos, long active0) {
		return jjMoveNfa_3(jjStopStringLiteralDfa_3(pos, active0), pos + 1);
	}

	private int jjStopAtPos(int pos, int kind) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		return pos + 1;
	}

	private int jjMoveStringLiteralDfa0_3() {
		switch (curChar) {
		case 40:
			return jjStopAtPos(0, 13);
		case 41:
			return jjStopAtPos(0, 14);
		case 42:
			return jjStartNfaWithStates_3(0, 16, 36);
		case 43:
			return jjStopAtPos(0, 11);
		case 45:
			return jjStopAtPos(0, 12);
		case 58:
			return jjStopAtPos(0, 15);
		case 91:
			return jjStopAtPos(0, 23);
		case 94:
			return jjStopAtPos(0, 17);
		case 123:
			return jjStopAtPos(0, 24);
		default:
			return jjMoveNfa_3(0, 0);
		}
	}

	private int jjStartNfaWithStates_3(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		try {
			curChar = input_stream.readChar();
		} catch (java.io.IOException e) {
			return pos + 1;
		}
		return jjMoveNfa_3(state, pos + 1);
	}

	static final long[] jjbitVec0 = { 0x1L, 0x0L, 0x0L, 0x0L };
	static final long[] jjbitVec1 = { 0xfffffffffffffffeL, 0xffffffffffffffffL,
			0xffffffffffffffffL, 0xffffffffffffffffL };
	static final long[] jjbitVec3 = { 0x0L, 0x0L, 0xffffffffffffffffL,
			0xffffffffffffffffL };
	static final long[] jjbitVec4 = { 0xfffefffffffffffeL, 0xffffffffffffffffL,
			0xffffffffffffffffL, 0xffffffffffffffffL };

	private int jjMoveNfa_3(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 36;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 0x7fffffff;
		for (;;) {
			if (++jjround == 0x7fffffff)
				ReInitRounds();
			if (curChar < 64) {
				long l = 1L << curChar;
				do {
					switch (jjstateSet[--i]) {
					case 36:
					case 25:
						if ((0xfbfffcf8ffffd9ffL & l) == 0L)
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 0:
						if ((0xfbffd4f8ffffd9ffL & l) != 0L) {
							if (kind > 22)
								kind = 22;
							jjCheckNAddTwoStates(25, 26);
						} else if ((0x100002600L & l) != 0L) {
							if (kind > 7)
								kind = 7;
						} else if (curChar == 34)
							jjCheckNAddStates(0, 2);
						else if (curChar == 33) {
							if (kind > 10)
								kind = 10;
						}
						if ((0x7bffd0f8ffffd9ffL & l) != 0L) {
							if (kind > 19)
								kind = 19;
							jjCheckNAddStates(3, 7);
						} else if (curChar == 42) {
							if (kind > 21)
								kind = 21;
						}
						if (curChar == 38)
							jjstateSet[jjnewStateCnt++] = 4;
						break;
					case 4:
						if (curChar == 38 && kind > 8)
							kind = 8;
						break;
					case 5:
						if (curChar == 38)
							jjstateSet[jjnewStateCnt++] = 4;
						break;
					case 13:
						if (curChar == 33 && kind > 10)
							kind = 10;
						break;
					case 14:
						if (curChar == 34)
							jjCheckNAddStates(0, 2);
						break;
					case 15:
						if ((0xfffffffbffffffffL & l) != 0L)
							jjCheckNAddStates(0, 2);
						break;
					case 17:
						jjCheckNAddStates(0, 2);
						break;
					case 18:
						if (curChar == 34 && kind > 18)
							kind = 18;
						break;
					case 20:
						if ((0x3ff000000000000L & l) == 0L)
							break;
						if (kind > 20)
							kind = 20;
						jjAddStates(8, 9);
						break;
					case 21:
						if (curChar == 46)
							jjCheckNAdd(22);
						break;
					case 22:
						if ((0x3ff000000000000L & l) == 0L)
							break;
						if (kind > 20)
							kind = 20;
						jjCheckNAdd(22);
						break;
					case 23:
						if (curChar == 42 && kind > 21)
							kind = 21;
						break;
					case 24:
						if ((0xfbffd4f8ffffd9ffL & l) == 0L)
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 27:
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 28:
						if ((0x7bffd0f8ffffd9ffL & l) == 0L)
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddStates(3, 7);
						break;
					case 29:
						if ((0x7bfff8f8ffffd9ffL & l) == 0L)
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddTwoStates(29, 30);
						break;
					case 31:
						if (kind > 19)
							kind = 19;
						jjCheckNAddTwoStates(29, 30);
						break;
					case 32:
						if ((0x7bfff8f8ffffd9ffL & l) != 0L)
							jjCheckNAddStates(10, 12);
						break;
					case 34:
						jjCheckNAddStates(10, 12);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else if (curChar < 128) {
				long l = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					case 36:
						if ((0x97ffffff87ffffffL & l) != 0L) {
							if (kind > 22)
								kind = 22;
							jjCheckNAddTwoStates(25, 26);
						} else if (curChar == 92)
							jjCheckNAddTwoStates(27, 27);
						break;
					case 0:
						if ((0x97ffffff87ffffffL & l) != 0L) {
							if (kind > 19)
								kind = 19;
							jjCheckNAddStates(3, 7);
						} else if (curChar == 92)
							jjCheckNAddStates(13, 15);
						else if (curChar == 126) {
							if (kind > 20)
								kind = 20;
							jjstateSet[jjnewStateCnt++] = 20;
						}
						if ((0x97ffffff87ffffffL & l) != 0L) {
							if (kind > 22)
								kind = 22;
							jjCheckNAddTwoStates(25, 26);
						}
						if (curChar == 78)
							jjstateSet[jjnewStateCnt++] = 11;
						else if (curChar == 124)
							jjstateSet[jjnewStateCnt++] = 8;
						else if (curChar == 79)
							jjstateSet[jjnewStateCnt++] = 6;
						else if (curChar == 65)
							jjstateSet[jjnewStateCnt++] = 2;
						break;
					case 1:
						if (curChar == 68 && kind > 8)
							kind = 8;
						break;
					case 2:
						if (curChar == 78)
							jjstateSet[jjnewStateCnt++] = 1;
						break;
					case 3:
						if (curChar == 65)
							jjstateSet[jjnewStateCnt++] = 2;
						break;
					case 6:
						if (curChar == 82 && kind > 9)
							kind = 9;
						break;
					case 7:
						if (curChar == 79)
							jjstateSet[jjnewStateCnt++] = 6;
						break;
					case 8:
						if (curChar == 124 && kind > 9)
							kind = 9;
						break;
					case 9:
						if (curChar == 124)
							jjstateSet[jjnewStateCnt++] = 8;
						break;
					case 10:
						if (curChar == 84 && kind > 10)
							kind = 10;
						break;
					case 11:
						if (curChar == 79)
							jjstateSet[jjnewStateCnt++] = 10;
						break;
					case 12:
						if (curChar == 78)
							jjstateSet[jjnewStateCnt++] = 11;
						break;
					case 15:
						if ((0xffffffffefffffffL & l) != 0L)
							jjCheckNAddStates(0, 2);
						break;
					case 16:
						if (curChar == 92)
							jjstateSet[jjnewStateCnt++] = 17;
						break;
					case 17:
						jjCheckNAddStates(0, 2);
						break;
					case 19:
						if (curChar != 126)
							break;
						if (kind > 20)
							kind = 20;
						jjstateSet[jjnewStateCnt++] = 20;
						break;
					case 24:
						if ((0x97ffffff87ffffffL & l) == 0L)
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 25:
						if ((0x97ffffff87ffffffL & l) == 0L)
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 26:
						if (curChar == 92)
							jjCheckNAddTwoStates(27, 27);
						break;
					case 27:
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 28:
						if ((0x97ffffff87ffffffL & l) == 0L)
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddStates(3, 7);
						break;
					case 29:
						if ((0x97ffffff87ffffffL & l) == 0L)
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddTwoStates(29, 30);
						break;
					case 30:
						if (curChar == 92)
							jjCheckNAddTwoStates(31, 31);
						break;
					case 31:
						if (kind > 19)
							kind = 19;
						jjCheckNAddTwoStates(29, 30);
						break;
					case 32:
						if ((0x97ffffff87ffffffL & l) != 0L)
							jjCheckNAddStates(10, 12);
						break;
					case 33:
						if (curChar == 92)
							jjCheckNAddTwoStates(34, 34);
						break;
					case 34:
						jjCheckNAddStates(10, 12);
						break;
					case 35:
						if (curChar == 92)
							jjCheckNAddStates(13, 15);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else {
				int hiByte = (int) (curChar >> 8);
				int i1 = hiByte >> 6;
				long l1 = 1L << (hiByte & 077);
				int i2 = (curChar & 0xff) >> 6;
				long l2 = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					case 36:
					case 25:
						if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 0:
						if (jjCanMove_0(hiByte, i1, i2, l1, l2)) {
							if (kind > 7)
								kind = 7;
						}
						if (jjCanMove_2(hiByte, i1, i2, l1, l2)) {
							if (kind > 22)
								kind = 22;
							jjCheckNAddTwoStates(25, 26);
						}
						if (jjCanMove_2(hiByte, i1, i2, l1, l2)) {
							if (kind > 19)
								kind = 19;
							jjCheckNAddStates(3, 7);
						}
						break;
					case 15:
					case 17:
						if (jjCanMove_1(hiByte, i1, i2, l1, l2))
							jjCheckNAddStates(0, 2);
						break;
					case 24:
						if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 27:
						if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 22)
							kind = 22;
						jjCheckNAddTwoStates(25, 26);
						break;
					case 28:
						if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddStates(3, 7);
						break;
					case 29:
						if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddTwoStates(29, 30);
						break;
					case 31:
						if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 19)
							kind = 19;
						jjCheckNAddTwoStates(29, 30);
						break;
					case 32:
						if (jjCanMove_2(hiByte, i1, i2, l1, l2))
							jjCheckNAddStates(10, 12);
						break;
					case 34:
						if (jjCanMove_1(hiByte, i1, i2, l1, l2))
							jjCheckNAddStates(10, 12);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			}
			if (kind != 0x7fffffff) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 0x7fffffff;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 36 - (jjnewStateCnt = startsAt)))
				return curPos;
			try {
				curChar = input_stream.readChar();
			} catch (java.io.IOException e) {
				return curPos;
			}
		}
	}

	private final int jjStopStringLiteralDfa_1(int pos, long active0) {
		switch (pos) {
		case 0:
			if ((active0 & 0x40000000L) != 0L) {
				jjmatchedKind = 33;
				return 6;
			}
			return -1;
		default:
			return -1;
		}
	}

	private final int jjStartNfa_1(int pos, long active0) {
		return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0), pos + 1);
	}

	private int jjMoveStringLiteralDfa0_1() {
		switch (curChar) {
		case 84:
			return jjMoveStringLiteralDfa1_1(0x40000000L);
		case 125:
			return jjStopAtPos(0, 31);
		default:
			return jjMoveNfa_1(0, 0);
		}
	}

	private int jjMoveStringLiteralDfa1_1(long active0) {
		try {
			curChar = input_stream.readChar();
		} catch (java.io.IOException e) {
			jjStopStringLiteralDfa_1(0, active0);
			return 1;
		}
		switch (curChar) {
		case 79:
			if ((active0 & 0x40000000L) != 0L)
				return jjStartNfaWithStates_1(1, 30, 6);
			break;
		default:
			break;
		}
		return jjStartNfa_1(0, active0);
	}

	private int jjStartNfaWithStates_1(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		try {
			curChar = input_stream.readChar();
		} catch (java.io.IOException e) {
			return pos + 1;
		}
		return jjMoveNfa_1(state, pos + 1);
	}

	private int jjMoveNfa_1(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 7;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 0x7fffffff;
		for (;;) {
			if (++jjround == 0x7fffffff)
				ReInitRounds();
			if (curChar < 64) {
				long l = 1L << curChar;
				do {
					switch (jjstateSet[--i]) {
					case 0:
						if ((0xfffffffeffffffffL & l) != 0L) {
							if (kind > 33)
								kind = 33;
							jjCheckNAdd(6);
						}
						if ((0x100002600L & l) != 0L) {
							if (kind > 7)
								kind = 7;
						} else if (curChar == 34)
							jjCheckNAddTwoStates(2, 4);
						break;
					case 1:
						if (curChar == 34)
							jjCheckNAddTwoStates(2, 4);
						break;
					case 2:
						if ((0xfffffffbffffffffL & l) != 0L)
							jjCheckNAddStates(16, 18);
						break;
					case 3:
						if (curChar == 34)
							jjCheckNAddStates(16, 18);
						break;
					case 5:
						if (curChar == 34 && kind > 32)
							kind = 32;
						break;
					case 6:
						if ((0xfffffffeffffffffL & l) == 0L)
							break;
						if (kind > 33)
							kind = 33;
						jjCheckNAdd(6);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else if (curChar < 128) {
				long l = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					case 0:
					case 6:
						if ((0xdfffffffffffffffL & l) == 0L)
							break;
						if (kind > 33)
							kind = 33;
						jjCheckNAdd(6);
						break;
					case 2:
						jjAddStates(16, 18);
						break;
					case 4:
						if (curChar == 92)
							jjstateSet[jjnewStateCnt++] = 3;
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else {
				int hiByte = (int) (curChar >> 8);
				int i1 = hiByte >> 6;
				long l1 = 1L << (hiByte & 077);
				int i2 = (curChar & 0xff) >> 6;
				long l2 = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					case 0:
						if (jjCanMove_0(hiByte, i1, i2, l1, l2)) {
							if (kind > 7)
								kind = 7;
						}
						if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
							if (kind > 33)
								kind = 33;
							jjCheckNAdd(6);
						}
						break;
					case 2:
						if (jjCanMove_1(hiByte, i1, i2, l1, l2))
							jjAddStates(16, 18);
						break;
					case 6:
						if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 33)
							kind = 33;
						jjCheckNAdd(6);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			}
			if (kind != 0x7fffffff) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 0x7fffffff;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 7 - (jjnewStateCnt = startsAt)))
				return curPos;
			try {
				curChar = input_stream.readChar();
			} catch (java.io.IOException e) {
				return curPos;
			}
		}
	}

	private int jjMoveStringLiteralDfa0_0() {
		return jjMoveNfa_0(0, 0);
	}

	private int jjMoveNfa_0(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 3;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 0x7fffffff;
		for (;;) {
			if (++jjround == 0x7fffffff)
				ReInitRounds();
			if (curChar < 64) {
				long l = 1L << curChar;
				do {
					switch (jjstateSet[--i]) {
					case 0:
						if ((0x3ff000000000000L & l) == 0L)
							break;
						if (kind > 25)
							kind = 25;
						jjAddStates(19, 20);
						break;
					case 1:
						if (curChar == 46)
							jjCheckNAdd(2);
						break;
					case 2:
						if ((0x3ff000000000000L & l) == 0L)
							break;
						if (kind > 25)
							kind = 25;
						jjCheckNAdd(2);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else if (curChar < 128) {
				long l = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					default:
						break;
					}
				} while (i != startsAt);
			} else {
				int hiByte = (int) (curChar >> 8);
				int i1 = hiByte >> 6;
				long l1 = 1L << (hiByte & 077);
				int i2 = (curChar & 0xff) >> 6;
				long l2 = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					default:
						break;
					}
				} while (i != startsAt);
			}
			if (kind != 0x7fffffff) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 0x7fffffff;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 3 - (jjnewStateCnt = startsAt)))
				return curPos;
			try {
				curChar = input_stream.readChar();
			} catch (java.io.IOException e) {
				return curPos;
			}
		}
	}

	private final int jjStopStringLiteralDfa_2(int pos, long active0) {
		switch (pos) {
		case 0:
			if ((active0 & 0x4000000L) != 0L) {
				jjmatchedKind = 29;
				return 6;
			}
			return -1;
		default:
			return -1;
		}
	}

	private final int jjStartNfa_2(int pos, long active0) {
		return jjMoveNfa_2(jjStopStringLiteralDfa_2(pos, active0), pos + 1);
	}

	private int jjMoveStringLiteralDfa0_2() {
		switch (curChar) {
		case 84:
			return jjMoveStringLiteralDfa1_2(0x4000000L);
		case 93:
			return jjStopAtPos(0, 27);
		default:
			return jjMoveNfa_2(0, 0);
		}
	}

	private int jjMoveStringLiteralDfa1_2(long active0) {
		try {
			curChar = input_stream.readChar();
		} catch (java.io.IOException e) {
			jjStopStringLiteralDfa_2(0, active0);
			return 1;
		}
		switch (curChar) {
		case 79:
			if ((active0 & 0x4000000L) != 0L)
				return jjStartNfaWithStates_2(1, 26, 6);
			break;
		default:
			break;
		}
		return jjStartNfa_2(0, active0);
	}

	private int jjStartNfaWithStates_2(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		try {
			curChar = input_stream.readChar();
		} catch (java.io.IOException e) {
			return pos + 1;
		}
		return jjMoveNfa_2(state, pos + 1);
	}

	private int jjMoveNfa_2(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 7;
		int i = 1;
		jjstateSet[0] = startState;
		int kind = 0x7fffffff;
		for (;;) {
			if (++jjround == 0x7fffffff)
				ReInitRounds();
			if (curChar < 64) {
				long l = 1L << curChar;
				do {
					switch (jjstateSet[--i]) {
					case 0:
						if ((0xfffffffeffffffffL & l) != 0L) {
							if (kind > 29)
								kind = 29;
							jjCheckNAdd(6);
						}
						if ((0x100002600L & l) != 0L) {
							if (kind > 7)
								kind = 7;
						} else if (curChar == 34)
							jjCheckNAddTwoStates(2, 4);
						break;
					case 1:
						if (curChar == 34)
							jjCheckNAddTwoStates(2, 4);
						break;
					case 2:
						if ((0xfffffffbffffffffL & l) != 0L)
							jjCheckNAddStates(16, 18);
						break;
					case 3:
						if (curChar == 34)
							jjCheckNAddStates(16, 18);
						break;
					case 5:
						if (curChar == 34 && kind > 28)
							kind = 28;
						break;
					case 6:
						if ((0xfffffffeffffffffL & l) == 0L)
							break;
						if (kind > 29)
							kind = 29;
						jjCheckNAdd(6);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else if (curChar < 128) {
				long l = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					case 0:
					case 6:
						if ((0xffffffffdfffffffL & l) == 0L)
							break;
						if (kind > 29)
							kind = 29;
						jjCheckNAdd(6);
						break;
					case 2:
						jjAddStates(16, 18);
						break;
					case 4:
						if (curChar == 92)
							jjstateSet[jjnewStateCnt++] = 3;
						break;
					default:
						break;
					}
				} while (i != startsAt);
			} else {
				int hiByte = (int) (curChar >> 8);
				int i1 = hiByte >> 6;
				long l1 = 1L << (hiByte & 077);
				int i2 = (curChar & 0xff) >> 6;
				long l2 = 1L << (curChar & 077);
				do {
					switch (jjstateSet[--i]) {
					case 0:
						if (jjCanMove_0(hiByte, i1, i2, l1, l2)) {
							if (kind > 7)
								kind = 7;
						}
						if (jjCanMove_1(hiByte, i1, i2, l1, l2)) {
							if (kind > 29)
								kind = 29;
							jjCheckNAdd(6);
						}
						break;
					case 2:
						if (jjCanMove_1(hiByte, i1, i2, l1, l2))
							jjAddStates(16, 18);
						break;
					case 6:
						if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
							break;
						if (kind > 29)
							kind = 29;
						jjCheckNAdd(6);
						break;
					default:
						break;
					}
				} while (i != startsAt);
			}
			if (kind != 0x7fffffff) {
				jjmatchedKind = kind;
				jjmatchedPos = curPos;
				kind = 0x7fffffff;
			}
			++curPos;
			if ((i = jjnewStateCnt) == (startsAt = 7 - (jjnewStateCnt = startsAt)))
				return curPos;
			try {
				curChar = input_stream.readChar();
			} catch (java.io.IOException e) {
				return curPos;
			}
		}
	}

	static final int[] jjnextStates = { 15, 16, 18, 29, 32, 23, 33, 30, 20, 21,
			32, 23, 33, 31, 34, 27, 2, 4, 5, 0, 1, };

	private static final boolean jjCanMove_0(int hiByte, int i1, int i2,
			long l1, long l2) {
		switch (hiByte) {
		case 48:
			return ((jjbitVec0[i2] & l2) != 0L);
		default:
			return false;
		}
	}

	private static final boolean jjCanMove_1(int hiByte, int i1, int i2,
			long l1, long l2) {
		switch (hiByte) {
		case 0:
			return ((jjbitVec3[i2] & l2) != 0L);
		default:
			if ((jjbitVec1[i1] & l1) != 0L)
				return true;
			return false;
		}
	}

	private static final boolean jjCanMove_2(int hiByte, int i1, int i2,
			long l1, long l2) {
		switch (hiByte) {
		case 0:
			return ((jjbitVec3[i2] & l2) != 0L);
		case 48:
			return ((jjbitVec1[i2] & l2) != 0L);
		default:
			if ((jjbitVec4[i1] & l1) != 0L)
				return true;
			return false;
		}
	}

	public static final String[] jjstrLiteralImages = { "", null, null, null,
			null, null, null, null, null, null, null, "\53", "\55", "\50",
			"\51", "\72", "\52", "\136", null, null, null, null, null, "\133",
			"\173", null, "\124\117", "\135", null, null, "\124\117", "\175",
			null, null, };

	public static final String[] lexStateNames = { "Boost", "RangeEx",
			"RangeIn", "DEFAULT", };

	public static final int[] jjnewLexState = { -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, 2, 1, 3,
			-1, 3, -1, -1, -1, 3, -1, -1, };
	static final long[] jjtoToken = { 0x3ffffff01L, };
	static final long[] jjtoSkip = { 0x80L, };
	protected CharStream input_stream;
	private final int[] jjrounds = new int[36];
	private final int[] jjstateSet = new int[72];
	protected char curChar;

	public QueryParserTokenManager(CharStream stream) {
		input_stream = stream;
	}

	public QueryParserTokenManager(CharStream stream, int lexState) {
		this(stream);
		SwitchTo(lexState);
	}

	public void ReInit(CharStream stream) {
		jjmatchedPos = jjnewStateCnt = 0;
		curLexState = defaultLexState;
		input_stream = stream;
		ReInitRounds();
	}

	private void ReInitRounds() {
		int i;
		jjround = 0x80000001;
		for (i = 36; i-- > 0;)
			jjrounds[i] = 0x80000000;
	}

	public void ReInit(CharStream stream, int lexState) {
		ReInit(stream);
		SwitchTo(lexState);
	}

	public void SwitchTo(int lexState) {
		if (lexState >= 4 || lexState < 0)
			throw new TokenMgrError("Error: Ignoring invalid lexical state : "
					+ lexState + ". State unchanged.",
					TokenMgrError.INVALID_LEXICAL_STATE);
		else
			curLexState = lexState;
	}

	protected Token jjFillToken() {
		final Token t;
		final String curTokenImage;
		final int beginLine;
		final int endLine;
		final int beginColumn;
		final int endColumn;
		String im = jjstrLiteralImages[jjmatchedKind];
		curTokenImage = (im == null) ? input_stream.GetImage() : im;
		beginLine = input_stream.getBeginLine();
		beginColumn = input_stream.getBeginColumn();
		endLine = input_stream.getEndLine();
		endColumn = input_stream.getEndColumn();
		t = Token.newToken(jjmatchedKind, curTokenImage);

		t.beginLine = beginLine;
		t.endLine = endLine;
		t.beginColumn = beginColumn;
		t.endColumn = endColumn;

		return t;
	}

	int curLexState = 3;
	int defaultLexState = 3;
	int jjnewStateCnt;
	int jjround;
	int jjmatchedPos;
	int jjmatchedKind;

	public Token getNextToken() {
		Token matchedToken;
		int curPos = 0;

		EOFLoop: for (;;) {
			try {
				curChar = input_stream.BeginToken();
			} catch (java.io.IOException e) {
				jjmatchedKind = 0;
				matchedToken = jjFillToken();
				return matchedToken;
			}

			switch (curLexState) {
			case 0:
				jjmatchedKind = 0x7fffffff;
				jjmatchedPos = 0;
				curPos = jjMoveStringLiteralDfa0_0();
				break;
			case 1:
				jjmatchedKind = 0x7fffffff;
				jjmatchedPos = 0;
				curPos = jjMoveStringLiteralDfa0_1();
				break;
			case 2:
				jjmatchedKind = 0x7fffffff;
				jjmatchedPos = 0;
				curPos = jjMoveStringLiteralDfa0_2();
				break;
			case 3:
				jjmatchedKind = 0x7fffffff;
				jjmatchedPos = 0;
				curPos = jjMoveStringLiteralDfa0_3();
				break;
			}
			if (jjmatchedKind != 0x7fffffff) {
				if (jjmatchedPos + 1 < curPos)
					input_stream.backup(curPos - jjmatchedPos - 1);
				if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L) {
					matchedToken = jjFillToken();
					if (jjnewLexState[jjmatchedKind] != -1)
						curLexState = jjnewLexState[jjmatchedKind];
					return matchedToken;
				} else {
					if (jjnewLexState[jjmatchedKind] != -1)
						curLexState = jjnewLexState[jjmatchedKind];
					continue EOFLoop;
				}
			}
			int error_line = input_stream.getEndLine();
			int error_column = input_stream.getEndColumn();
			String error_after = null;
			boolean EOFSeen = false;
			try {
				input_stream.readChar();
				input_stream.backup(1);
			} catch (java.io.IOException e1) {
				EOFSeen = true;
				error_after = curPos <= 1 ? "" : input_stream.GetImage();
				if (curChar == '\n' || curChar == '\r') {
					error_line++;
					error_column = 0;
				} else
					error_column++;
			}
			if (!EOFSeen) {
				input_stream.backup(1);
				error_after = curPos <= 1 ? "" : input_stream.GetImage();
			}
			throw new TokenMgrError(EOFSeen, curLexState, error_line,
					error_column, error_after, curChar,
					TokenMgrError.LEXICAL_ERROR);
		}
	}

	private void jjCheckNAdd(int state) {
		if (jjrounds[state] != jjround) {
			jjstateSet[jjnewStateCnt++] = state;
			jjrounds[state] = jjround;
		}
	}

	private void jjAddStates(int start, int end) {
		do {
			jjstateSet[jjnewStateCnt++] = jjnextStates[start];
		} while (start++ != end);
	}

	private void jjCheckNAddTwoStates(int state1, int state2) {
		jjCheckNAdd(state1);
		jjCheckNAdd(state2);
	}

	private void jjCheckNAddStates(int start, int end) {
		do {
			jjCheckNAdd(jjnextStates[start]);
		} while (start++ != end);
	}

}
