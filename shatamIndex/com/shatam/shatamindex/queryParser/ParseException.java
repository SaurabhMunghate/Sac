/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

public class ParseException extends Exception {

	public ParseException(Token currentTokenVal,
			int[][] expectedTokenSequencesVal, String[] tokenImageVal) {
		super("");
		specialConstructor = true;
		currentToken = currentTokenVal;
		expectedTokenSequences = expectedTokenSequencesVal;
		tokenImage = tokenImageVal;
	}

	public ParseException() {
		super();
		specialConstructor = false;
	}

	public ParseException(String message) {
		super(message);
		specialConstructor = false;
	}

	protected boolean specialConstructor;

	public Token currentToken;

	public int[][] expectedTokenSequences;

	public String[] tokenImage;

	public String getMessage() {
		if (!specialConstructor) {
			return super.getMessage();
		}
		StringBuffer expected = new StringBuffer();
		int maxSize = 0;
		for (int i = 0; i < expectedTokenSequences.length; i++) {
			if (maxSize < expectedTokenSequences[i].length) {
				maxSize = expectedTokenSequences[i].length;
			}
			for (int j = 0; j < expectedTokenSequences[i].length; j++) {
				expected.append(tokenImage[expectedTokenSequences[i][j]])
						.append(' ');
			}
			if (expectedTokenSequences[i][expectedTokenSequences[i].length - 1] != 0) {
				expected.append("...");
			}
			expected.append(eol).append("    ");
		}
		String retval = "Encountered \"";
		Token tok = currentToken.next;
		for (int i = 0; i < maxSize; i++) {
			if (i != 0)
				retval += " ";
			if (tok.kind == 0) {
				retval += tokenImage[0];
				break;
			}
			retval += " " + tokenImage[tok.kind];
			retval += " \"";
			retval += add_escapes(tok.image);
			retval += " \"";
			tok = tok.next;
		}
		retval += "\" at line " + currentToken.next.beginLine + ", column "
				+ currentToken.next.beginColumn;
		retval += "." + eol;
		if (expectedTokenSequences.length == 1) {
			retval += "Was expecting:" + eol + "    ";
		} else {
			retval += "Was expecting one of:" + eol + "    ";
		}
		retval += expected.toString();
		return retval;
	}

	protected String eol = System.getProperty("line.separator", "\n");

	protected String add_escapes(String str) {
		StringBuffer retval = new StringBuffer();
		char ch;
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case 0:
				continue;
			case '\b':
				retval.append("\\b");
				continue;
			case '\t':
				retval.append("\\t");
				continue;
			case '\n':
				retval.append("\\n");
				continue;
			case '\f':
				retval.append("\\f");
				continue;
			case '\r':
				retval.append("\\r");
				continue;
			case '\"':
				retval.append("\\\"");
				continue;
			case '\'':
				retval.append("\\\'");
				continue;
			case '\\':
				retval.append("\\\\");
				continue;
			default:
				if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
					String s = "0000" + Integer.toString(ch, 16);
					retval.append("\\u"
							+ s.substring(s.length() - 4, s.length()));
				} else {
					retval.append(ch);
				}
				continue;
			}
		}
		return retval.toString();
	}

}
