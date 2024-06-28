/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis.tokenattributes;

import com.shatam.shatamindex.index.Payload;
import com.shatam.shatamindex.util.Attribute;

public interface PayloadAttribute extends Attribute {

	public Payload getPayload();

	public void setPayload(Payload payload);
}
