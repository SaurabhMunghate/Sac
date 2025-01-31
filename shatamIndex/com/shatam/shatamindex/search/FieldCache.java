/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.analysis.NumericTokenStream;
import com.shatam.shatamindex.document.NumericField;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.Bits;
import com.shatam.shatamindex.util.NumericUtils;
import com.shatam.shatamindex.util.RamUsageEstimator;

import java.io.IOException;
import java.io.Serializable;
import java.io.PrintStream;

import java.text.DecimalFormat;

public interface FieldCache {

	public static final class CreationPlaceholder {
		Object value;
	}

	public static final int STRING_INDEX = -1;

	public static class StringIndex {

		public int binarySearchLookup(String key) {

			if (key == null)
				return 0;

			int low = 1;
			int high = lookup.length - 1;

			while (low <= high) {
				int mid = (low + high) >>> 1;
				int cmp = lookup[mid].compareTo(key);

				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return mid;
			}
			return -(low + 1);
		}

		public final String[] lookup;

		public final int[] order;

		public StringIndex(int[] values, String[] lookup) {
			this.order = values;
			this.lookup = lookup;
		}
	}

	public interface Parser extends Serializable {
	}

	public interface ByteParser extends Parser {

		public byte parseByte(String string);
	}

	public interface ShortParser extends Parser {

		public short parseShort(String string);
	}

	public interface IntParser extends Parser {

		public int parseInt(String string);
	}

	public interface FloatParser extends Parser {

		public float parseFloat(String string);
	}

	public interface LongParser extends Parser {

		public long parseLong(String string);
	}

	public interface DoubleParser extends Parser {

		public double parseDouble(String string);
	}

	public static FieldCache DEFAULT = new FieldCacheImpl();

	public static final ByteParser DEFAULT_BYTE_PARSER = new ByteParser() {
		public byte parseByte(String value) {
			return Byte.parseByte(value);
		}

		protected Object readResolve() {
			return DEFAULT_BYTE_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".DEFAULT_BYTE_PARSER";
		}
	};

	public static final ShortParser DEFAULT_SHORT_PARSER = new ShortParser() {
		public short parseShort(String value) {
			return Short.parseShort(value);
		}

		protected Object readResolve() {
			return DEFAULT_SHORT_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".DEFAULT_SHORT_PARSER";
		}
	};

	public static final IntParser DEFAULT_INT_PARSER = new IntParser() {
		public int parseInt(String value) {
			return Integer.parseInt(value);
		}

		protected Object readResolve() {
			return DEFAULT_INT_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".DEFAULT_INT_PARSER";
		}
	};

	public static final FloatParser DEFAULT_FLOAT_PARSER = new FloatParser() {
		public float parseFloat(String value) {
			return Float.parseFloat(value);
		}

		protected Object readResolve() {
			return DEFAULT_FLOAT_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".DEFAULT_FLOAT_PARSER";
		}
	};

	public static final LongParser DEFAULT_LONG_PARSER = new LongParser() {
		public long parseLong(String value) {
			return Long.parseLong(value);
		}

		protected Object readResolve() {
			return DEFAULT_LONG_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".DEFAULT_LONG_PARSER";
		}
	};

	public static final DoubleParser DEFAULT_DOUBLE_PARSER = new DoubleParser() {
		public double parseDouble(String value) {
			return Double.parseDouble(value);
		}

		protected Object readResolve() {
			return DEFAULT_DOUBLE_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".DEFAULT_DOUBLE_PARSER";
		}
	};

	public static final IntParser NUMERIC_UTILS_INT_PARSER = new IntParser() {
		public int parseInt(String val) {
			final int shift = val.charAt(0) - NumericUtils.SHIFT_START_INT;
			if (shift > 0 && shift <= 31)
				throw new FieldCacheImpl.StopFillCacheException();
			return NumericUtils.prefixCodedToInt(val);
		}

		protected Object readResolve() {
			return NUMERIC_UTILS_INT_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".NUMERIC_UTILS_INT_PARSER";
		}
	};

	public static final FloatParser NUMERIC_UTILS_FLOAT_PARSER = new FloatParser() {
		public float parseFloat(String val) {
			final int shift = val.charAt(0) - NumericUtils.SHIFT_START_INT;
			if (shift > 0 && shift <= 31)
				throw new FieldCacheImpl.StopFillCacheException();
			return NumericUtils.sortableIntToFloat(NumericUtils
					.prefixCodedToInt(val));
		}

		protected Object readResolve() {
			return NUMERIC_UTILS_FLOAT_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".NUMERIC_UTILS_FLOAT_PARSER";
		}
	};

	public static final LongParser NUMERIC_UTILS_LONG_PARSER = new LongParser() {
		public long parseLong(String val) {
			final int shift = val.charAt(0) - NumericUtils.SHIFT_START_LONG;
			if (shift > 0 && shift <= 63)
				throw new FieldCacheImpl.StopFillCacheException();
			return NumericUtils.prefixCodedToLong(val);
		}

		protected Object readResolve() {
			return NUMERIC_UTILS_LONG_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".NUMERIC_UTILS_LONG_PARSER";
		}
	};

	public static final DoubleParser NUMERIC_UTILS_DOUBLE_PARSER = new DoubleParser() {
		public double parseDouble(String val) {
			final int shift = val.charAt(0) - NumericUtils.SHIFT_START_LONG;
			if (shift > 0 && shift <= 63)
				throw new FieldCacheImpl.StopFillCacheException();
			return NumericUtils.sortableLongToDouble(NumericUtils
					.prefixCodedToLong(val));
		}

		protected Object readResolve() {
			return NUMERIC_UTILS_DOUBLE_PARSER;
		}

		@Override
		public String toString() {
			return FieldCache.class.getName() + ".NUMERIC_UTILS_DOUBLE_PARSER";
		}
	};

	public Bits getDocsWithField(IndexReader reader, String field)
			throws IOException;

	public byte[] getBytes(IndexReader reader, String field) throws IOException;

	public byte[] getBytes(IndexReader reader, String field, ByteParser parser)
			throws IOException;

	public byte[] getBytes(IndexReader reader, String field, ByteParser parser,
			boolean setDocsWithField) throws IOException;

	public short[] getShorts(IndexReader reader, String field)
			throws IOException;

	public short[] getShorts(IndexReader reader, String field,
			ShortParser parser) throws IOException;

	public short[] getShorts(IndexReader reader, String field,
			ShortParser parser, boolean setDocsWithField) throws IOException;

	public int[] getInts(IndexReader reader, String field) throws IOException;

	public int[] getInts(IndexReader reader, String field, IntParser parser)
			throws IOException;

	public int[] getInts(IndexReader reader, String field, IntParser parser,
			boolean setDocsWithField) throws IOException;

	public float[] getFloats(IndexReader reader, String field)
			throws IOException;

	public float[] getFloats(IndexReader reader, String field,
			FloatParser parser) throws IOException;

	public float[] getFloats(IndexReader reader, String field,
			FloatParser parser, boolean setDocsWithField) throws IOException;

	public long[] getLongs(IndexReader reader, String field) throws IOException;

	public long[] getLongs(IndexReader reader, String field, LongParser parser)
			throws IOException;

	public long[] getLongs(IndexReader reader, String field, LongParser parser,
			boolean setDocsWithField) throws IOException;

	public double[] getDoubles(IndexReader reader, String field)
			throws IOException;

	public double[] getDoubles(IndexReader reader, String field,
			DoubleParser parser) throws IOException;

	public double[] getDoubles(IndexReader reader, String field,
			DoubleParser parser, boolean setDocsWithField) throws IOException;

	public String[] getStrings(IndexReader reader, String field)
			throws IOException;

	public StringIndex getStringIndex(IndexReader reader, String field)
			throws IOException;

	public static abstract class CacheEntry {
		public abstract Object getReaderKey();

		public abstract String getFieldName();

		public abstract Class<?> getCacheType();

		public abstract Object getCustom();

		public abstract Object getValue();

		private String size = null;

		protected final void setEstimatedSize(String size) {
			this.size = size;
		}

		public void estimateSize() {
			estimateSize(new RamUsageEstimator(false));
		}

		public void estimateSize(RamUsageEstimator ramCalc) {
			long size = ramCalc.estimateRamUsage(getValue());
			setEstimatedSize(RamUsageEstimator.humanReadableUnits(size,
					new DecimalFormat("0.#")));

		}

		public final String getEstimatedSize() {
			return size;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("'").append(getReaderKey()).append("'=>");
			b.append("'").append(getFieldName()).append("',");
			b.append(getCacheType()).append(",").append(getCustom());
			b.append("=>").append(getValue().getClass().getName()).append("#");
			b.append(System.identityHashCode(getValue()));

			String s = getEstimatedSize();
			if (null != s) {
				b.append(" (size =~ ").append(s).append(')');
			}

			return b.toString();
		}

	}

	public abstract CacheEntry[] getCacheEntries();

	public abstract void purgeAllCaches();

	public abstract void purge(IndexReader r);

	public void setInfoStream(PrintStream stream);

	public PrintStream getInfoStream();
}
