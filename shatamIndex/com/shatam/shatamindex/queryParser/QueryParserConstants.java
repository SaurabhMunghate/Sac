/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

public interface QueryParserConstants {

	int EOF = 0;

	int _NUM_CHAR = 1;

	int _ESCAPED_CHAR = 2;

	int _TERM_START_CHAR = 3;

	int _TERM_CHAR = 4;

	int _WHITESPACE = 5;

	int _QUOTED_CHAR = 6;

	int AND = 8;

	int OR = 9;

	int NOT = 10;

	int PLUS = 11;

	int MINUS = 12;

	int LPAREN = 13;

	int RPAREN = 14;

	int COLON = 15;

	int STAR = 16;

	int CARAT = 17;

	int QUOTED = 18;

	int TERM = 19;

	int FUZZY_SLOP = 20;

	int PREFIXTERM = 21;

	int WILDTERM = 22;

	int RANGEIN_START = 23;

	int RANGEEX_START = 24;

	int NUMBER = 25;

	int RANGEIN_TO = 26;

	int RANGEIN_END = 27;

	int RANGEIN_QUOTED = 28;

	int RANGEIN_GOOP = 29;

	int RANGEEX_TO = 30;

	int RANGEEX_END = 31;

	int RANGEEX_QUOTED = 32;

	int RANGEEX_GOOP = 33;

	int Boost = 0;

	int RangeEx = 1;

	int RangeIn = 2;

	int DEFAULT = 3;

	String[] tokenImage = { "<EOF>", "<_NUM_CHAR>", "<_ESCAPED_CHAR>",
			"<_TERM_START_CHAR>", "<_TERM_CHAR>", "<_WHITESPACE>",
			"<_QUOTED_CHAR>", "<token of kind 7>", "<AND>", "<OR>", "<NOT>",
			"\"+\"", "\"-\"", "\"(\"", "\")\"", "\":\"", "\"*\"", "\"^\"",
			"<QUOTED>", "<TERM>", "<FUZZY_SLOP>", "<PREFIXTERM>", "<WILDTERM>",
			"\"[\"", "\"{\"", "<NUMBER>", "\"TO\"", "\"]\"",
			"<RANGEIN_QUOTED>", "<RANGEIN_GOOP>", "\"TO\"", "\"}\"",
			"<RANGEEX_QUOTED>", "<RANGEEX_GOOP>", };

}
