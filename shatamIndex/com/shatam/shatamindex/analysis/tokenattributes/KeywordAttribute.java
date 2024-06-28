/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.util.Attribute;

public interface KeywordAttribute extends Attribute {

	public boolean isKeyword();

	public void setKeyword(boolean isKeyword);
}
