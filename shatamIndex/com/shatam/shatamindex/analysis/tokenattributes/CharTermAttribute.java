/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import com.shatam.shatamindex.util.Attribute;

public interface CharTermAttribute extends Attribute, CharSequence, Appendable {

	public void copyBuffer(char[] buffer, int offset, int length);

	public char[] buffer();

	public char[] resizeBuffer(int newSize);

	public CharTermAttribute setLength(int length);

	public CharTermAttribute setEmpty();

	public CharTermAttribute append(CharSequence csq);

	public CharTermAttribute append(CharSequence csq, int start, int end);

	public CharTermAttribute append(char c);

	public CharTermAttribute append(String s);

	public CharTermAttribute append(StringBuilder sb);

	public CharTermAttribute append(CharTermAttribute termAtt);
}
