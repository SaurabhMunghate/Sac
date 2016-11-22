/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import com.shatam.shatamindex.util.Attribute;

@Deprecated
public interface TermAttribute extends Attribute {

	public String term();

	public void setTermBuffer(char[] buffer, int offset, int length);

	public void setTermBuffer(String buffer);

	public void setTermBuffer(String buffer, int offset, int length);

	public char[] termBuffer();

	public char[] resizeTermBuffer(int newSize);

	public int termLength();

	public void setTermLength(int length);
}
