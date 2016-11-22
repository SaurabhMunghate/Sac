/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public abstract class MemoryModel {

	public abstract int getArraySize();

	public abstract int getClassSize();

	public abstract int getPrimitiveSize(Class<?> clazz);

	public abstract int getReferenceSize();

}
