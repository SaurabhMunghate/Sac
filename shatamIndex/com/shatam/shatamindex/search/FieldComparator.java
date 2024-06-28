/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.FieldCache.ByteParser;
import com.shatam.shatamindex.search.FieldCache.DoubleParser;
import com.shatam.shatamindex.search.FieldCache.FloatParser;
import com.shatam.shatamindex.search.FieldCache.IntParser;
import com.shatam.shatamindex.search.FieldCache.LongParser;
import com.shatam.shatamindex.search.FieldCache.ShortParser;
import com.shatam.shatamindex.search.FieldCache.StringIndex;
import com.shatam.shatamindex.util.Bits;

public abstract class FieldComparator<T> {

	public abstract int compare(int slot1, int slot2);

	public abstract void setBottom(final int slot);

	public abstract int compareBottom(int doc) throws IOException;

	public abstract void copy(int slot, int doc) throws IOException;

	public abstract void setNextReader(IndexReader reader, int docBase)
			throws IOException;

	public void setScorer(Scorer scorer) {

	}

	public abstract T value(int slot);

	@SuppressWarnings("unchecked")
	public int compareValues(T first, T second) {
		if (first == null) {
			if (second == null) {
				return 0;
			} else {
				return -1;
			}
		} else if (second == null) {
			return 1;
		} else {
			return ((Comparable<T>) first).compareTo(second);
		}
	}

	public static abstract class NumericComparator<T extends Number> extends
			FieldComparator<T> {
		protected final T missingValue;
		protected final String field;
		protected Bits docsWithField;

		public NumericComparator(String field, T missingValue) {
			this.field = field;
			this.missingValue = missingValue;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {
			if (missingValue != null) {
				docsWithField = FieldCache.DEFAULT.getDocsWithField(reader,
						field);

				if (docsWithField instanceof Bits.MatchAllBits) {
					docsWithField = null;
				}
			} else {
				docsWithField = null;
			}
		}
	}

	public static final class ByteComparator extends NumericComparator<Byte> {
		private final byte[] values;
		private final ByteParser parser;
		private byte[] currentReaderValues;
		private byte bottom;

		ByteComparator(int numHits, String field, FieldCache.Parser parser,
				Byte missingValue) {
			super(field, missingValue);
			values = new byte[numHits];
			this.parser = (ByteParser) parser;
		}

		@Override
		public int compare(int slot1, int slot2) {
			return values[slot1] - values[slot2];
		}

		@Override
		public int compareBottom(int doc) {
			byte v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			return bottom - v2;
		}

		@Override
		public void copy(int slot, int doc) {
			byte v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {

			currentReaderValues = FieldCache.DEFAULT.getBytes(reader, field,
					parser, missingValue != null);
			super.setNextReader(reader, docBase);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public Byte value(int slot) {
			return Byte.valueOf(values[slot]);
		}
	}

	public static final class DocComparator extends FieldComparator<Integer> {
		private final int[] docIDs;
		private int docBase;
		private int bottom;

		DocComparator(int numHits) {
			docIDs = new int[numHits];
		}

		@Override
		public int compare(int slot1, int slot2) {

			return docIDs[slot1] - docIDs[slot2];
		}

		@Override
		public int compareBottom(int doc) {

			return bottom - (docBase + doc);
		}

		@Override
		public void copy(int slot, int doc) {
			docIDs[slot] = docBase + doc;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase) {

			this.docBase = docBase;
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = docIDs[bottom];
		}

		@Override
		public Integer value(int slot) {
			return Integer.valueOf(docIDs[slot]);
		}
	}

	public static final class DoubleComparator extends
			NumericComparator<Double> {
		private final double[] values;
		private final DoubleParser parser;
		private double[] currentReaderValues;
		private double bottom;

		DoubleComparator(int numHits, String field, FieldCache.Parser parser,
				Double missingValue) {
			super(field, missingValue);
			values = new double[numHits];
			this.parser = (DoubleParser) parser;
		}

		@Override
		public int compare(int slot1, int slot2) {
			final double v1 = values[slot1];
			final double v2 = values[slot2];
			if (v1 > v2) {
				return 1;
			} else if (v1 < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public int compareBottom(int doc) {
			double v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			if (bottom > v2) {
				return 1;
			} else if (bottom < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			double v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {

			currentReaderValues = FieldCache.DEFAULT.getDoubles(reader, field,
					parser, missingValue != null);
			super.setNextReader(reader, docBase);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public Double value(int slot) {
			return Double.valueOf(values[slot]);
		}
	}

	public static final class FloatComparator extends NumericComparator<Float> {
		private final float[] values;
		private final FloatParser parser;
		private float[] currentReaderValues;
		private float bottom;

		FloatComparator(int numHits, String field, FieldCache.Parser parser,
				Float missingValue) {
			super(field, missingValue);
			values = new float[numHits];
			this.parser = (FloatParser) parser;
		}

		@Override
		public int compare(int slot1, int slot2) {

			final float v1 = values[slot1];
			final float v2 = values[slot2];
			if (v1 > v2) {
				return 1;
			} else if (v1 < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public int compareBottom(int doc) {

			float v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			if (bottom > v2) {
				return 1;
			} else if (bottom < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			float v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {

			currentReaderValues = FieldCache.DEFAULT.getFloats(reader, field,
					parser, missingValue != null);
			super.setNextReader(reader, docBase);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public Float value(int slot) {
			return Float.valueOf(values[slot]);
		}
	}

	public static final class IntComparator extends NumericComparator<Integer> {
		private final int[] values;
		private final IntParser parser;
		private int[] currentReaderValues;
		private int bottom;

		IntComparator(int numHits, String field, FieldCache.Parser parser,
				Integer missingValue) {
			super(field, missingValue);
			values = new int[numHits];
			this.parser = (IntParser) parser;
		}

		@Override
		public int compare(int slot1, int slot2) {

			final int v1 = values[slot1];
			final int v2 = values[slot2];
			if (v1 > v2) {
				return 1;
			} else if (v1 < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public int compareBottom(int doc) {

			int v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			if (bottom > v2) {
				return 1;
			} else if (bottom < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			int v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {

			currentReaderValues = FieldCache.DEFAULT.getInts(reader, field,
					parser, missingValue != null);
			super.setNextReader(reader, docBase);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public Integer value(int slot) {
			return Integer.valueOf(values[slot]);
		}
	}

	public static final class LongComparator extends NumericComparator<Long> {
		private final long[] values;
		private final LongParser parser;
		private long[] currentReaderValues;
		private long bottom;

		LongComparator(int numHits, String field, FieldCache.Parser parser,
				Long missingValue) {
			super(field, missingValue);
			values = new long[numHits];
			this.parser = (LongParser) parser;
		}

		@Override
		public int compare(int slot1, int slot2) {

			final long v1 = values[slot1];
			final long v2 = values[slot2];
			if (v1 > v2) {
				return 1;
			} else if (v1 < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public int compareBottom(int doc) {

			long v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			if (bottom > v2) {
				return 1;
			} else if (bottom < v2) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			long v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {

			currentReaderValues = FieldCache.DEFAULT.getLongs(reader, field,
					parser, missingValue != null);
			super.setNextReader(reader, docBase);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public Long value(int slot) {
			return Long.valueOf(values[slot]);
		}
	}

	public static final class RelevanceComparator extends
			FieldComparator<Float> {
		private final float[] scores;
		private float bottom;
		private Scorer scorer;

		RelevanceComparator(int numHits) {
			scores = new float[numHits];
		}

		@Override
		public int compare(int slot1, int slot2) {
			final float score1 = scores[slot1];
			final float score2 = scores[slot2];
			return score1 > score2 ? -1 : (score1 < score2 ? 1 : 0);
		}

		@Override
		public int compareBottom(int doc) throws IOException {
			float score = scorer.score();
			return bottom > score ? -1 : (bottom < score ? 1 : 0);
		}

		@Override
		public void copy(int slot, int doc) throws IOException {
			scores[slot] = scorer.score();
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase) {
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = scores[bottom];
		}

		@Override
		public void setScorer(Scorer scorer) {

			if (!(scorer instanceof ScoreCachingWrappingScorer)) {
				this.scorer = new ScoreCachingWrappingScorer(scorer);
			} else {
				this.scorer = scorer;
			}
		}

		@Override
		public Float value(int slot) {
			return Float.valueOf(scores[slot]);
		}

		@Override
		public int compareValues(Float first, Float second) {

			return second.compareTo(first);
		}
	}

	public static final class ShortComparator extends NumericComparator<Short> {
		private final short[] values;
		private final ShortParser parser;
		private short[] currentReaderValues;
		private short bottom;

		ShortComparator(int numHits, String field, FieldCache.Parser parser,
				Short missingValue) {
			super(field, missingValue);
			values = new short[numHits];
			this.parser = (ShortParser) parser;
		}

		@Override
		public int compare(int slot1, int slot2) {
			return values[slot1] - values[slot2];
		}

		@Override
		public int compareBottom(int doc) {
			short v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			return bottom - v2;
		}

		@Override
		public void copy(int slot, int doc) {
			short v2 = currentReaderValues[doc];

			if (docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
				v2 = missingValue;
			}
			values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {

			currentReaderValues = FieldCache.DEFAULT.getShorts(reader, field,
					parser, missingValue != null);
			super.setNextReader(reader, docBase);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public Short value(int slot) {
			return Short.valueOf(values[slot]);
		}
	}

	public static final class StringComparatorLocale extends
			FieldComparator<String> {

		private final String[] values;
		private String[] currentReaderValues;
		private final String field;
		final Collator collator;
		private String bottom;

		StringComparatorLocale(int numHits, String field, Locale locale) {
			values = new String[numHits];
			this.field = field;
			collator = Collator.getInstance(locale);
		}

		@Override
		public int compare(int slot1, int slot2) {
			final String val1 = values[slot1];
			final String val2 = values[slot2];
			if (val1 == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return collator.compare(val1, val2);
		}

		@Override
		public int compareBottom(int doc) {
			final String val2 = currentReaderValues[doc];
			if (bottom == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return collator.compare(bottom, val2);
		}

		@Override
		public void copy(int slot, int doc) {
			values[slot] = currentReaderValues[doc];
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {
			currentReaderValues = FieldCache.DEFAULT.getStrings(reader, field);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public String value(int slot) {
			return values[slot];
		}

		@Override
		public int compareValues(String val1, String val2) {
			if (val1 == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return collator.compare(val1, val2);
		}
	}

	public static final class StringOrdValComparator extends
			FieldComparator<String> {

		private final int[] ords;
		private final String[] values;
		private final int[] readerGen;

		private int currentReaderGen = -1;
		private String[] lookup;
		private int[] order;
		private final String field;

		private int bottomSlot = -1;
		private int bottomOrd;
		private boolean bottomSameReader;
		private String bottomValue;

		public StringOrdValComparator(int numHits, String field, int sortPos,
				boolean reversed) {
			ords = new int[numHits];
			values = new String[numHits];
			readerGen = new int[numHits];
			this.field = field;
		}

		@Override
		public int compare(int slot1, int slot2) {
			if (readerGen[slot1] == readerGen[slot2]) {
				return ords[slot1] - ords[slot2];
			}

			final String val1 = values[slot1];
			final String val2 = values[slot2];
			if (val1 == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return val1.compareTo(val2);
		}

		@Override
		public int compareBottom(int doc) {
			assert bottomSlot != -1;
			if (bottomSameReader) {

				return bottomOrd - this.order[doc];
			} else {

				final int order = this.order[doc];
				final int cmp = bottomOrd - order;
				if (cmp != 0) {
					return cmp;
				}

				final String val2 = lookup[order];
				if (bottomValue == null) {
					if (val2 == null) {
						return 0;
					}

					return -1;
				} else if (val2 == null) {

					return 1;
				}
				return bottomValue.compareTo(val2);
			}
		}

		@Override
		public void copy(int slot, int doc) {
			final int ord = order[doc];
			ords[slot] = ord;
			assert ord >= 0;
			values[slot] = lookup[ord];
			readerGen[slot] = currentReaderGen;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {
			StringIndex currentReaderValues = FieldCache.DEFAULT
					.getStringIndex(reader, field);
			currentReaderGen++;
			order = currentReaderValues.order;
			lookup = currentReaderValues.lookup;
			assert lookup.length > 0;
			if (bottomSlot != -1) {
				setBottom(bottomSlot);
			}
		}

		@Override
		public void setBottom(final int bottom) {
			bottomSlot = bottom;

			bottomValue = values[bottomSlot];
			if (currentReaderGen == readerGen[bottomSlot]) {
				bottomOrd = ords[bottomSlot];
				bottomSameReader = true;
			} else {
				if (bottomValue == null) {
					ords[bottomSlot] = 0;
					bottomOrd = 0;
					bottomSameReader = true;
					readerGen[bottomSlot] = currentReaderGen;
				} else {
					final int index = binarySearch(lookup, bottomValue);
					if (index < 0) {
						bottomOrd = -index - 2;
						bottomSameReader = false;
					} else {
						bottomOrd = index;

						bottomSameReader = true;
						readerGen[bottomSlot] = currentReaderGen;
						ords[bottomSlot] = bottomOrd;
					}
				}
			}
		}

		@Override
		public String value(int slot) {
			return values[slot];
		}

		@Override
		public int compareValues(String val1, String val2) {
			if (val1 == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return val1.compareTo(val2);
		}

		public String[] getValues() {
			return values;
		}

		public int getBottomSlot() {
			return bottomSlot;
		}

		public String getField() {
			return field;
		}
	}

	public static final class StringValComparator extends
			FieldComparator<String> {

		private String[] values;
		private String[] currentReaderValues;
		private final String field;
		private String bottom;

		StringValComparator(int numHits, String field) {
			values = new String[numHits];
			this.field = field;
		}

		@Override
		public int compare(int slot1, int slot2) {
			final String val1 = values[slot1];
			final String val2 = values[slot2];
			if (val1 == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}

			return val1.compareTo(val2);
		}

		@Override
		public int compareBottom(int doc) {
			final String val2 = currentReaderValues[doc];
			if (bottom == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return bottom.compareTo(val2);
		}

		@Override
		public void copy(int slot, int doc) {
			values[slot] = currentReaderValues[doc];
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException {
			currentReaderValues = FieldCache.DEFAULT.getStrings(reader, field);
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = values[bottom];
		}

		@Override
		public String value(int slot) {
			return values[slot];
		}

		@Override
		public int compareValues(String val1, String val2) {
			if (val1 == null) {
				if (val2 == null) {
					return 0;
				}
				return -1;
			} else if (val2 == null) {
				return 1;
			} else {
				return val1.compareTo(val2);
			}
		}
	}

	final protected static int binarySearch(String[] a, String key) {
		return binarySearch(a, key, 0, a.length - 1);
	}

	final protected static int binarySearch(String[] a, String key, int low,
			int high) {

		while (low <= high) {
			int mid = (low + high) >>> 1;
			String midVal = a[mid];
			int cmp;
			if (midVal != null) {
				cmp = midVal.compareTo(key);
			} else {
				cmp = -1;
			}

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid;
		}
		return -(low + 1);
	}
}
