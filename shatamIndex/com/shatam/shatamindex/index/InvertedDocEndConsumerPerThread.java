/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

abstract class InvertedDocEndConsumerPerThread {
	abstract void startDocument();

	abstract InvertedDocEndConsumerPerField addField(
			DocInverterPerField docInverterPerField, FieldInfo fieldInfo);

	abstract void finishDocument();

	abstract void abort();
}
