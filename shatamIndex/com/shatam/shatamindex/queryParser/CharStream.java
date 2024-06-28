/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

public interface CharStream {

	char readChar() throws java.io.IOException;

	int getColumn();

	int getLine();

	int getEndColumn();

	int getEndLine();

	int getBeginColumn();

	int getBeginLine();

	void backup(int amount);

	char BeginToken() throws java.io.IOException;

	String GetImage();

	char[] GetSuffix(int len);

	void Done();

}
