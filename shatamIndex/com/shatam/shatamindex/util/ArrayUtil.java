/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.Collection;
import java.util.Comparator;

public final class ArrayUtil {

	@Deprecated
	public ArrayUtil() {
	}

	public static int parseInt(char[] chars) throws NumberFormatException {
		return parseInt(chars, 0, chars.length, 10);
	}

	public static int parseInt(char[] chars, int offset, int len)
			throws NumberFormatException {
		return parseInt(chars, offset, len, 10);
	}

	public static int parseInt(char[] chars, int offset, int len, int radix)
			throws NumberFormatException {
		if (chars == null || radix < Character.MIN_RADIX
				|| radix > Character.MAX_RADIX) {
			throw new NumberFormatException();
		}
		int i = 0;
		if (len == 0) {
			throw new NumberFormatException("chars length is 0");
		}
		boolean negative = chars[offset + i] == '-';
		if (negative && ++i == len) {
			throw new NumberFormatException("can't convert to an int");
		}
		if (negative == true) {
			offset++;
			len--;
		}
		return parse(chars, offset, len, radix, negative);
	}

	private static int parse(char[] chars, int offset, int len, int radix,
			boolean negative) throws NumberFormatException {
		int max = Integer.MIN_VALUE / radix;
		int result = 0;
		for (int i = 0; i < len; i++) {
			int digit = Character.digit(chars[i + offset], radix);
			if (digit == -1) {
				throw new NumberFormatException("Unable to parse");
			}
			if (max > result) {
				throw new NumberFormatException("Unable to parse");
			}
			int next = result * radix - digit;
			if (next > result) {
				throw new NumberFormatException("Unable to parse");
			}
			result = next;
		}
		/*
		 * while (offset < len) {
		 * 
		 * }
		 */
		if (!negative) {
			result = -result;
			if (result < 0) {
				throw new NumberFormatException("Unable to parse");
			}
		}
		return result;
	}

	public static int oversize(int minTargetSize, int bytesPerElement) {

		if (minTargetSize < 0) {

			throw new IllegalArgumentException("invalid array size "
					+ minTargetSize);
		}

		if (minTargetSize == 0) {

			return 0;
		}

		int extra = minTargetSize >> 3;

		if (extra < 3) {

			extra = 3;
		}

		int newSize = minTargetSize + extra;

		if (newSize + 7 < 0) {

			return Integer.MAX_VALUE;
		}

		if (Constants.JRE_IS_64BIT) {

			switch (bytesPerElement) {
			case 4:

				return (newSize + 1) & 0x7ffffffe;
			case 2:

				return (newSize + 3) & 0x7ffffffc;
			case 1:

				return (newSize + 7) & 0x7ffffff8;
			case 8:

			default:

				return newSize;
			}
		} else {

			switch (bytesPerElement) {
			case 2:

				return (newSize + 1) & 0x7ffffffe;
			case 1:

				return (newSize + 3) & 0x7ffffffc;
			case 4:
			case 8:

			default:

				return newSize;
			}
		}
	}

	public static int getShrinkSize(int currentSize, int targetSize,
			int bytesPerElement) {
		final int newSize = oversize(targetSize, bytesPerElement);

		if (newSize < currentSize / 2)
			return newSize;
		else
			return currentSize;
	}

	public static short[] grow(short[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			short[] newArray = new short[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_SHORT)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static short[] grow(short[] array) {
		return grow(array, 1 + array.length);
	}

	public static float[] grow(float[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			float[] newArray = new float[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_FLOAT)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static float[] grow(float[] array) {
		return grow(array, 1 + array.length);
	}

	public static double[] grow(double[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			double[] newArray = new double[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_DOUBLE)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static double[] grow(double[] array) {
		return grow(array, 1 + array.length);
	}

	public static short[] shrink(short[] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize,
				RamUsageEstimator.NUM_BYTES_SHORT);
		if (newSize != array.length) {
			short[] newArray = new short[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static int[] grow(int[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			int[] newArray = new int[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_INT)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static int[] grow(int[] array) {
		return grow(array, 1 + array.length);
	}

	public static int[] shrink(int[] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize,
				RamUsageEstimator.NUM_BYTES_INT);
		if (newSize != array.length) {
			int[] newArray = new int[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static long[] grow(long[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			long[] newArray = new long[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_LONG)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static long[] grow(long[] array) {
		return grow(array, 1 + array.length);
	}

	public static long[] shrink(long[] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize,
				RamUsageEstimator.NUM_BYTES_LONG);
		if (newSize != array.length) {
			long[] newArray = new long[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static byte[] grow(byte[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			byte[] newArray = new byte[oversize(minSize, 1)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static byte[] grow(byte[] array) {
		return grow(array, 1 + array.length);
	}

	public static byte[] shrink(byte[] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize, 1);
		if (newSize != array.length) {
			byte[] newArray = new byte[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static boolean[] grow(boolean[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			boolean[] newArray = new boolean[oversize(minSize, 1)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static boolean[] grow(boolean[] array) {
		return grow(array, 1 + array.length);
	}

	public static boolean[] shrink(boolean[] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize, 1);
		if (newSize != array.length) {
			boolean[] newArray = new boolean[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static char[] grow(char[] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			char[] newArray = new char[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_CHAR)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else
			return array;
	}

	public static char[] grow(char[] array) {
		return grow(array, 1 + array.length);
	}

	public static char[] shrink(char[] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize,
				RamUsageEstimator.NUM_BYTES_CHAR);
		if (newSize != array.length) {
			char[] newArray = new char[newSize];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else
			return array;
	}

	public static int[][] grow(int[][] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			int[][] newArray = new int[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_OBJECT_REF)][];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else {
			return array;
		}
	}

	public static int[][] grow(int[][] array) {
		return grow(array, 1 + array.length);
	}

	public static int[][] shrink(int[][] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize,
				RamUsageEstimator.NUM_BYTES_OBJECT_REF);
		if (newSize != array.length) {
			int[][] newArray = new int[newSize][];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else {
			return array;
		}
	}

	public static float[][] grow(float[][] array, int minSize) {
		assert minSize >= 0 : "size must be positive (got " + minSize
				+ "): likely integer overflow?";
		if (array.length < minSize) {
			float[][] newArray = new float[oversize(minSize,
					RamUsageEstimator.NUM_BYTES_OBJECT_REF)][];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		} else {
			return array;
		}
	}

	public static float[][] grow(float[][] array) {
		return grow(array, 1 + array.length);
	}

	public static float[][] shrink(float[][] array, int targetSize) {
		assert targetSize >= 0 : "size must be positive (got " + targetSize
				+ "): likely integer overflow?";
		final int newSize = getShrinkSize(array.length, targetSize,
				RamUsageEstimator.NUM_BYTES_OBJECT_REF);
		if (newSize != array.length) {
			float[][] newArray = new float[newSize][];
			System.arraycopy(array, 0, newArray, 0, newSize);
			return newArray;
		} else {
			return array;
		}
	}

	public static int hashCode(char[] array, int start, int end) {
		int code = 0;
		for (int i = end - 1; i >= start; i--)
			code = code * 31 + array[i];
		return code;
	}

	public static int hashCode(byte[] array, int start, int end) {
		int code = 0;
		for (int i = end - 1; i >= start; i--)
			code = code * 31 + array[i];
		return code;
	}

	public static boolean equals(char[] left, int offsetLeft, char[] right,
			int offsetRight, int length) {
		if ((offsetLeft + length <= left.length)
				&& (offsetRight + length <= right.length)) {
			for (int i = 0; i < length; i++) {
				if (left[offsetLeft + i] != right[offsetRight + i]) {
					return false;
				}

			}
			return true;
		}
		return false;
	}

	public static boolean equals(int[] left, int offsetLeft, int[] right,
			int offsetRight, int length) {
		if ((offsetLeft + length <= left.length)
				&& (offsetRight + length <= right.length)) {
			for (int i = 0; i < length; i++) {
				if (left[offsetLeft + i] != right[offsetRight + i]) {
					return false;
				}

			}
			return true;
		}
		return false;
	}

	public static int[] toIntArray(Collection<Integer> ints) {

		final int[] result = new int[ints.size()];
		int upto = 0;
		for (int v : ints) {
			result[upto++] = v;
		}

		assert upto == result.length;

		return result;
	}

	private static <T> SorterTemplate getSorter(final T[] a,
			final Comparator<? super T> comp) {
		return new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				final T o = a[i];
				a[i] = a[j];
				a[j] = o;
			}

			@Override
			protected int compare(int i, int j) {
				return comp.compare(a[i], a[j]);
			}

			@Override
			protected void setPivot(int i) {
				pivot = a[i];
			}

			@Override
			protected int comparePivot(int j) {
				return comp.compare(pivot, a[j]);
			}

			private T pivot;
		};
	}

	private static <T extends Comparable<? super T>> SorterTemplate getSorter(
			final T[] a) {
		return new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				final T o = a[i];
				a[i] = a[j];
				a[j] = o;
			}

			@Override
			protected int compare(int i, int j) {
				return a[i].compareTo(a[j]);
			}

			@Override
			protected void setPivot(int i) {
				pivot = a[i];
			}

			@Override
			protected int comparePivot(int j) {
				return pivot.compareTo(a[j]);
			}

			private T pivot;
		};
	}

	public static <T> void quickSort(T[] a, int fromIndex, int toIndex,
			Comparator<? super T> comp) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a, comp).quickSort(fromIndex, toIndex - 1);
	}

	public static <T> void quickSort(T[] a, Comparator<? super T> comp) {
		quickSort(a, 0, a.length, comp);
	}

	public static <T extends Comparable<? super T>> void quickSort(T[] a,
			int fromIndex, int toIndex) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a).quickSort(fromIndex, toIndex - 1);
	}

	public static <T extends Comparable<? super T>> void quickSort(T[] a) {
		quickSort(a, 0, a.length);
	}

	public static <T> void mergeSort(T[] a, int fromIndex, int toIndex,
			Comparator<? super T> comp) {
		if (toIndex - fromIndex <= 1)
			return;

		getSorter(a, comp).mergeSort(fromIndex, toIndex - 1);
	}

	public static <T> void mergeSort(T[] a, Comparator<? super T> comp) {
		mergeSort(a, 0, a.length, comp);
	}

	public static <T extends Comparable<? super T>> void mergeSort(T[] a,
			int fromIndex, int toIndex) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a).mergeSort(fromIndex, toIndex - 1);
	}

	public static <T extends Comparable<? super T>> void mergeSort(T[] a) {
		mergeSort(a, 0, a.length);
	}

	public static <T> void insertionSort(T[] a, int fromIndex, int toIndex,
			Comparator<? super T> comp) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a, comp).insertionSort(fromIndex, toIndex - 1);
	}

	public static <T> void insertionSort(T[] a, Comparator<? super T> comp) {
		insertionSort(a, 0, a.length, comp);
	}

	public static <T extends Comparable<? super T>> void insertionSort(T[] a,
			int fromIndex, int toIndex) {
		if (toIndex - fromIndex <= 1)
			return;
		getSorter(a).insertionSort(fromIndex, toIndex - 1);
	}

	public static <T extends Comparable<? super T>> void insertionSort(T[] a) {
		insertionSort(a, 0, a.length);
	}

}