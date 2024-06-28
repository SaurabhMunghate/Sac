/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

public final class CollectionUtil {

	private CollectionUtil() {
	}

	private static <T> SorterTemplate getSorter(final List<T> list,
			final Comparator<? super T> comp) {
		if (!(list instanceof RandomAccess))
			throw new IllegalArgumentException(
					"CollectionUtil can only sort random access lists in-place.");
		return new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				Collections.swap(list, i, j);
			}

			@Override
			protected int compare(int i, int j) {
				return comp.compare(list.get(i), list.get(j));
			}

			@Override
			protected void setPivot(int i) {
				pivot = list.get(i);
			}

			@Override
			protected int comparePivot(int j) {
				return comp.compare(pivot, list.get(j));
			}

			private T pivot;
		};
	}

	private static <T extends Comparable<? super T>> SorterTemplate getSorter(
			final List<T> list) {
		if (!(list instanceof RandomAccess))
			throw new IllegalArgumentException(
					"CollectionUtil can only sort random access lists in-place.");
		return new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				Collections.swap(list, i, j);
			}

			@Override
			protected int compare(int i, int j) {
				return list.get(i).compareTo(list.get(j));
			}

			@Override
			protected void setPivot(int i) {
				pivot = list.get(i);
			}

			@Override
			protected int comparePivot(int j) {
				return pivot.compareTo(list.get(j));
			}

			private T pivot;
		};
	}

	public static <T> void quickSort(List<T> list, Comparator<? super T> comp) {
		final int size = list.size();
		if (size <= 1)
			return;
		getSorter(list, comp).quickSort(0, size - 1);
	}

	public static <T extends Comparable<? super T>> void quickSort(List<T> list) {
		final int size = list.size();
		if (size <= 1)
			return;
		getSorter(list).quickSort(0, size - 1);
	}

	public static <T> void mergeSort(List<T> list, Comparator<? super T> comp) {
		final int size = list.size();
		if (size <= 1)
			return;
		getSorter(list, comp).mergeSort(0, size - 1);
	}

	public static <T extends Comparable<? super T>> void mergeSort(List<T> list) {
		final int size = list.size();
		if (size <= 1)
			return;
		getSorter(list).mergeSort(0, size - 1);
	}

	public static <T> void insertionSort(List<T> list,
			Comparator<? super T> comp) {
		final int size = list.size();
		if (size <= 1)
			return;
		getSorter(list, comp).insertionSort(0, size - 1);
	}

	public static <T extends Comparable<? super T>> void insertionSort(
			List<T> list) {
		final int size = list.size();
		if (size <= 1)
			return;
		getSorter(list).insertionSort(0, size - 1);
	}

}