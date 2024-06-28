/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.util.PriorityQueue;

public abstract class FieldValueHitQueue<T extends FieldValueHitQueue.Entry>
		extends PriorityQueue<T> {

	public static class Entry extends ScoreDoc {
		public int slot;

		public Entry(int slot, int doc, float score) {
			super(doc, score);
			this.slot = slot;
		}

		@Override
		public String toString() {
			return "slot:" + slot + " " + super.toString();
		}
	}

	private static final class OneComparatorFieldValueHitQueue<T extends FieldValueHitQueue.Entry>
			extends FieldValueHitQueue<T> {

		private final FieldComparator comparator;
		private final int oneReverseMul;

		public OneComparatorFieldValueHitQueue(SortField[] fields, int size)
				throws IOException {
			super(fields);

			SortField field = fields[0];
			comparator = field.getComparator(size, 0);
			oneReverseMul = field.reverse ? -1 : 1;

			comparators[0] = comparator;
			reverseMul[0] = oneReverseMul;

			initialize(size);
		}

		@Override
		protected boolean lessThan(final Entry hitA, final Entry hitB) {

			assert hitA != hitB;
			assert hitA.slot != hitB.slot;

			final int c = oneReverseMul
					* comparator.compare(hitA.slot, hitB.slot);
			if (c != 0) {
				return c > 0;
			}

			return hitA.doc > hitB.doc;
		}

	}

	private static final class MultiComparatorsFieldValueHitQueue<T extends FieldValueHitQueue.Entry>
			extends FieldValueHitQueue<T> {

		public MultiComparatorsFieldValueHitQueue(SortField[] fields, int size)
				throws IOException {
			super(fields);

			int numComparators = comparators.length;
			for (int i = 0; i < numComparators; ++i) {
				SortField field = fields[i];

				reverseMul[i] = field.reverse ? -1 : 1;
				comparators[i] = field.getComparator(size, i);
			}

			initialize(size);
		}

		@Override
		protected boolean lessThan(final Entry hitA, final Entry hitB) {

			assert hitA != hitB;
			assert hitA.slot != hitB.slot;

			int numComparators = comparators.length;
			for (int i = 0; i < numComparators; ++i) {
				final int c = reverseMul[i]
						* comparators[i].compare(hitA.slot, hitB.slot);
				if (c != 0) {

					return c > 0;
				}
			}

			return hitA.doc > hitB.doc;
		}

	}

	private FieldValueHitQueue(SortField[] fields) {

		this.fields = fields;
		int numComparators = fields.length;
		comparators = new FieldComparator[numComparators];
		reverseMul = new int[numComparators];
	}

	public static <T extends FieldValueHitQueue.Entry> FieldValueHitQueue<T> create(
			SortField[] fields, int size) throws IOException {

		if (fields.length == 0) {
			throw new IllegalArgumentException(
					"Sort must contain at least one field");
		}

		if (fields.length == 1) {
			return new OneComparatorFieldValueHitQueue<T>(fields, size);
		} else {
			return new MultiComparatorsFieldValueHitQueue<T>(fields, size);
		}
	}

	public FieldComparator[] getComparators() {
		return comparators;
	}

	public int[] getReverseMul() {
		return reverseMul;
	}

	protected final SortField[] fields;
	protected final FieldComparator[] comparators;
	protected final int[] reverseMul;

	@Override
	protected abstract boolean lessThan(final Entry a, final Entry b);

	FieldDoc fillFields(final Entry entry) {
		final int n = comparators.length;
		final Object[] fields = new Object[n];
		for (int i = 0; i < n; ++i) {
			fields[i] = comparators[i].value(entry.slot);
		}

		return new FieldDoc(entry.doc, entry.score, fields);
	}

	SortField[] getFields() {
		return fields;
	}
}
