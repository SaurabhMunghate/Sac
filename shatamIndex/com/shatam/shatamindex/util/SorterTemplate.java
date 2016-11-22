/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public abstract class SorterTemplate {

	private static final int MERGESORT_THRESHOLD = 12;
	private static final int QUICKSORT_THRESHOLD = 7;

	protected abstract void swap(int i, int j);

	protected abstract int compare(int i, int j);

	protected abstract void setPivot(int i);

	protected abstract int comparePivot(int j);

	public final void insertionSort(int lo, int hi) {
		for (int i = lo + 1; i <= hi; i++) {
			for (int j = i; j > lo; j--) {
				if (compare(j - 1, j) > 0) {
					swap(j - 1, j);
				} else {
					break;
				}
			}
		}
	}

	public final void quickSort(final int lo, final int hi) {
		if (hi <= lo)
			return;

		quickSort(lo, hi,
				(Integer.SIZE - Integer.numberOfLeadingZeros(hi - lo)) << 1);
	}

	private void quickSort(int lo, int hi, int maxDepth) {

		final int diff = hi - lo;
		if (diff <= QUICKSORT_THRESHOLD) {
			insertionSort(lo, hi);
			return;
		}

		if (--maxDepth == 0) {
			mergeSort(lo, hi);
			return;
		}

		final int mid = lo + (diff >>> 1);

		if (compare(lo, mid) > 0) {
			swap(lo, mid);
		}

		if (compare(mid, hi) > 0) {
			swap(mid, hi);
			if (compare(lo, mid) > 0) {
				swap(lo, mid);
			}
		}

		int left = lo + 1;
		int right = hi - 1;

		setPivot(mid);
		for (;;) {
			while (comparePivot(right) < 0)
				--right;

			while (left < right && comparePivot(left) >= 0)
				++left;

			if (left < right) {
				swap(left, right);
				--right;
			} else {
				break;
			}
		}

		quickSort(lo, left, maxDepth);
		quickSort(left + 1, hi, maxDepth);
	}

	public final void mergeSort(int lo, int hi) {
		final int diff = hi - lo;
		if (diff <= MERGESORT_THRESHOLD) {
			insertionSort(lo, hi);
			return;
		}

		final int mid = lo + (diff >>> 1);

		mergeSort(lo, mid);
		mergeSort(mid, hi);
		merge(lo, mid, hi, mid - lo, hi - mid);
	}

	private void merge(int lo, int pivot, int hi, int len1, int len2) {
		if (len1 == 0 || len2 == 0) {
			return;
		}
		if (len1 + len2 == 2) {
			if (compare(pivot, lo) < 0) {
				swap(pivot, lo);
			}
			return;
		}
		int first_cut, second_cut;
		int len11, len22;
		if (len1 > len2) {
			len11 = len1 >>> 1;
			first_cut = lo + len11;
			second_cut = lower(pivot, hi, first_cut);
			len22 = second_cut - pivot;
		} else {
			len22 = len2 >>> 1;
			second_cut = pivot + len22;
			first_cut = upper(lo, pivot, second_cut);
			len11 = first_cut - lo;
		}
		rotate(first_cut, pivot, second_cut);
		final int new_mid = first_cut + len22;
		merge(lo, first_cut, new_mid, len11, len22);
		merge(new_mid, second_cut, hi, len1 - len11, len2 - len22);
	}

	private void rotate(int lo, int mid, int hi) {
		int lot = lo;
		int hit = mid - 1;
		while (lot < hit) {
			swap(lot++, hit--);
		}
		lot = mid;
		hit = hi - 1;
		while (lot < hit) {
			swap(lot++, hit--);
		}
		lot = lo;
		hit = hi - 1;
		while (lot < hit) {
			swap(lot++, hit--);
		}
	}

	private int lower(int lo, int hi, int val) {
		int len = hi - lo;
		while (len > 0) {
			final int half = len >>> 1, mid = lo + half;
			if (compare(mid, val) < 0) {
				lo = mid + 1;
				len = len - half - 1;
			} else {
				len = half;
			}
		}
		return lo;
	}

	private int upper(int lo, int hi, int val) {
		int len = hi - lo;
		while (len > 0) {
			final int half = len >>> 1, mid = lo + half;
			if (compare(val, mid) < 0) {
				len = half;
			} else {
				lo = mid + 1;
				len = len - half - 1;
			}
		}
		return lo;
	}

}
