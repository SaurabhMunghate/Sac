/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;

import com.shatam.shatamindex.document.Fieldable;

abstract class InvertedDocConsumerPerField {

	abstract boolean start(Fieldable[] fields, int count) throws IOException;

	abstract void start(Fieldable field);

	abstract void add() throws IOException;

	abstract void finish() throws IOException;

	abstract void abort();
}
