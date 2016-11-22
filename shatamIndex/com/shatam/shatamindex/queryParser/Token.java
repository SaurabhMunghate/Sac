/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

public class Token {

	public int kind;

	public int beginLine;

	public int beginColumn;

	public int endLine;

	public int endColumn;

	public String image;

	public Token next;

	public Token specialToken;

	public Object getValue() {
		return null;
	}

	public Token() {
	}

	public Token(int kind) {
		this(kind, null);
	}

	public Token(int kind, String image) {
		this.kind = kind;
		this.image = image;
	}

	public String toString() {
		return image;
	}

	public static Token newToken(int ofKind, String image) {
		switch (ofKind) {
		default:
			return new Token(ofKind, image);
		}
	}

	public static Token newToken(int ofKind) {
		return newToken(ofKind, null);
	}

}
