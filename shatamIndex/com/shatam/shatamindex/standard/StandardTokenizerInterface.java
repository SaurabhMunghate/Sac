/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.standard;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;

import java.io.Reader;
import java.io.IOException;

public interface StandardTokenizerInterface {

	public static final int YYEOF = -1;

	public void getText(CharTermAttribute t);

	public int yychar();

	public void yyreset(Reader reader);

	public int yylength();

	public int getNextToken() throws IOException;

}
