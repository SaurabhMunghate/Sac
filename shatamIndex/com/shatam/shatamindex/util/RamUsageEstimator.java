/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.lang.reflect.*;
import java.text.DecimalFormat;
import java.util.*;

public final class RamUsageEstimator {

	public final static int NUM_BYTES_SHORT = 2;
	public final static int NUM_BYTES_INT = 4;
	public final static int NUM_BYTES_LONG = 8;
	public final static int NUM_BYTES_FLOAT = 4;
	public final static int NUM_BYTES_DOUBLE = 8;
	public final static int NUM_BYTES_CHAR = 2;
	public final static int NUM_BYTES_OBJECT_HEADER = 8;
	public final static int NUM_BYTES_OBJECT_REF = Constants.JRE_IS_64BIT ? 8
			: 4;
	public final static int NUM_BYTES_ARRAY_HEADER = NUM_BYTES_OBJECT_HEADER
			+ NUM_BYTES_INT + NUM_BYTES_OBJECT_REF;

	private MemoryModel memoryModel;

	private final Map<Object, Object> seen;

	private int refSize;
	private int arraySize;
	private int classSize;

	private boolean checkInterned;

	public RamUsageEstimator() {
		this(new AverageGuessMemoryModel());
	}

	public RamUsageEstimator(boolean checkInterned) {
		this(new AverageGuessMemoryModel(), checkInterned);
	}

	public RamUsageEstimator(MemoryModel memoryModel) {
		this(memoryModel, true);
	}

	public RamUsageEstimator(MemoryModel memoryModel, boolean checkInterned) {
		this.memoryModel = memoryModel;
		this.checkInterned = checkInterned;

		seen = new IdentityHashMap<Object, Object>(64);
		this.refSize = memoryModel.getReferenceSize();
		this.arraySize = memoryModel.getArraySize();
		this.classSize = memoryModel.getClassSize();
	}

	public long estimateRamUsage(Object obj) {
		long size = size(obj);
		seen.clear();
		return size;
	}

	private long size(Object obj) {
		if (obj == null) {
			return 0;
		}

		if (checkInterned && obj instanceof String
				&& obj == ((String) obj).intern()) {

			return 0;
		}

		if (seen.containsKey(obj)) {
			return 0;
		}

		seen.put(obj, null);

		Class<?> clazz = obj.getClass();
		if (clazz.isArray()) {
			return sizeOfArray(obj);
		}

		long size = 0;

		while (clazz != null) {
			Field[] fields = clazz.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				if (Modifier.isStatic(fields[i].getModifiers())) {
					continue;
				}

				if (fields[i].getType().isPrimitive()) {
					size += memoryModel.getPrimitiveSize(fields[i].getType());
				} else {
					size += refSize;
					fields[i].setAccessible(true);
					try {
						Object value = fields[i].get(obj);
						if (value != null) {
							size += size(value);
						}
					} catch (IllegalAccessException ex) {

					}
				}

			}
			clazz = clazz.getSuperclass();
		}
		size += classSize;
		return size;
	}

	private long sizeOfArray(Object obj) {
		int len = Array.getLength(obj);
		if (len == 0) {
			return 0;
		}
		long size = arraySize;
		Class<?> arrayElementClazz = obj.getClass().getComponentType();
		if (arrayElementClazz.isPrimitive()) {
			size += len * memoryModel.getPrimitiveSize(arrayElementClazz);
		} else {
			for (int i = 0; i < len; i++) {
				size += refSize + size(Array.get(obj, i));
			}
		}

		return size;
	}

	private static final long ONE_KB = 1024;
	private static final long ONE_MB = ONE_KB * ONE_KB;
	private static final long ONE_GB = ONE_KB * ONE_MB;

	public static String humanReadableUnits(long bytes, DecimalFormat df) {
		String newSizeAndUnits;

		if (bytes / ONE_GB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float) bytes / ONE_GB))
					+ " GB";
		} else if (bytes / ONE_MB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float) bytes / ONE_MB))
					+ " MB";
		} else if (bytes / ONE_KB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float) bytes / ONE_KB))
					+ " KB";
		} else {
			newSizeAndUnits = String.valueOf(bytes) + " bytes";
		}

		return newSizeAndUnits;
	}
}
