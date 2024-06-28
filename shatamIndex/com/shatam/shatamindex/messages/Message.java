/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.messages;

import java.io.Serializable;
import java.util.Locale;

public interface Message extends Serializable {

	public String getKey();

	public Object[] getArguments();

	public String getLocalizedMessage();

	public String getLocalizedMessage(Locale locale);

}
