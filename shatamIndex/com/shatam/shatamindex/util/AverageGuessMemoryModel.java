/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.IdentityHashMap;
import java.util.Map;

public class AverageGuessMemoryModel extends MemoryModel {

	private final Map<Class<?>, Integer> sizes = new IdentityHashMap<Class<?>, Integer>() {
		{
			put(boolean.class, Integer.valueOf(1));
			put(byte.class, Integer.valueOf(1));
			put(char.class, Integer.valueOf(2));
			put(short.class, Integer.valueOf(2));
			put(int.class, Integer.valueOf(4));
			put(float.class, Integer.valueOf(4));
			put(double.class, Integer.valueOf(8));
			put(long.class, Integer.valueOf(8));
		}
	};

	@Override
	public int getArraySize() {
		return 16;
	}

	@Override
	public int getClassSize() {
		return 8;
	}

	@Override
	public int getPrimitiveSize(Class<?> clazz) {
		return sizes.get(clazz).intValue();
	}

	@Override
	public int getReferenceSize() {
		return 4;
	}

}
