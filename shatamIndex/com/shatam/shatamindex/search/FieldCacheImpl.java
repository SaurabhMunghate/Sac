/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermDocs;
import com.shatam.shatamindex.index.TermEnum;
import com.shatam.shatamindex.util.Bits;
import com.shatam.shatamindex.util.FieldCacheSanityChecker;
import com.shatam.shatamindex.util.FixedBitSet;
import com.shatam.shatamindex.util.StringHelper;

class FieldCacheImpl implements FieldCache {

	private Map<Class<?>, Cache> caches;

	FieldCacheImpl() {
		init();
	}

	private synchronized void init() {
		caches = new HashMap<Class<?>, Cache>(9);
		caches.put(Byte.TYPE, new ByteCache(this));
		caches.put(Short.TYPE, new ShortCache(this));
		caches.put(Integer.TYPE, new IntCache(this));
		caches.put(Float.TYPE, new FloatCache(this));
		caches.put(Long.TYPE, new LongCache(this));
		caches.put(Double.TYPE, new DoubleCache(this));
		caches.put(String.class, new StringCache(this));
		caches.put(StringIndex.class, new StringIndexCache(this));
		caches.put(DocsWithFieldCache.class, new DocsWithFieldCache(this));
	}

	public synchronized void purgeAllCaches() {
		init();
	}

	public synchronized void purge(IndexReader r) {
		for (Cache c : caches.values()) {
			c.purge(r);
		}
	}

	public synchronized CacheEntry[] getCacheEntries() {
		List<CacheEntry> result = new ArrayList<CacheEntry>(17);
		for (final Map.Entry<Class<?>, Cache> cacheEntry : caches.entrySet()) {
			final Cache cache = cacheEntry.getValue();
			final Class<?> cacheType = cacheEntry.getKey();
			synchronized (cache.readerCache) {
				for (final Map.Entry<Object, Map<Entry, Object>> readerCacheEntry : cache.readerCache
						.entrySet()) {
					final Object readerKey = readerCacheEntry.getKey();
					if (readerKey == null)
						continue;
					final Map<Entry, Object> innerCache = readerCacheEntry
							.getValue();
					for (final Map.Entry<Entry, Object> mapEntry : innerCache
							.entrySet()) {
						Entry entry = mapEntry.getKey();
						result.add(new CacheEntryImpl(readerKey, entry.field,
								cacheType, entry.custom, mapEntry.getValue()));
					}
				}
			}
		}
		return result.toArray(new CacheEntry[result.size()]);
	}

	private static final class CacheEntryImpl extends CacheEntry {
		private final Object readerKey;
		private final String fieldName;
		private final Class<?> cacheType;
		private final Object custom;
		private final Object value;

		CacheEntryImpl(Object readerKey, String fieldName, Class<?> cacheType,
				Object custom, Object value) {
			this.readerKey = readerKey;
			this.fieldName = fieldName;
			this.cacheType = cacheType;
			this.custom = custom;
			this.value = value;

		}

		@Override
		public Object getReaderKey() {
			return readerKey;
		}

		@Override
		public String getFieldName() {
			return fieldName;
		}

		@Override
		public Class<?> getCacheType() {
			return cacheType;
		}

		@Override
		public Object getCustom() {
			return custom;
		}

		@Override
		public Object getValue() {
			return value;
		}
	}

	static final class StopFillCacheException extends RuntimeException {
	}

	final static IndexReader.ReaderFinishedListener purgeReader = new IndexReader.ReaderFinishedListener() {

		public void finished(IndexReader reader) {
			FieldCache.DEFAULT.purge(reader);
		}
	};

	abstract static class Cache {
		Cache() {
			this.wrapper = null;
		}

		Cache(FieldCacheImpl wrapper) {
			this.wrapper = wrapper;
		}

		final FieldCacheImpl wrapper;

		final Map<Object, Map<Entry, Object>> readerCache = new WeakHashMap<Object, Map<Entry, Object>>();

		protected abstract Object createValue(IndexReader reader, Entry key,
				boolean setDocsWithField) throws IOException;

		public void purge(IndexReader r) {
			Object readerKey = r.getCoreCacheKey();
			synchronized (readerCache) {
				readerCache.remove(readerKey);
			}
		}

		public void put(IndexReader reader, Entry key, Object value) {
			final Object readerKey = reader.getCoreCacheKey();
			synchronized (readerCache) {
				Map<Entry, Object> innerCache = readerCache.get(readerKey);
				if (innerCache == null) {

					innerCache = new HashMap<Entry, Object>();
					readerCache.put(readerKey, innerCache);
					reader.addReaderFinishedListener(purgeReader);
				}
				if (innerCache.get(key) == null) {
					innerCache.put(key, value);
				} else {

				}
			}
		}

		public Object get(IndexReader reader, Entry key,
				boolean setDocsWithField) throws IOException {
			Map<Entry, Object> innerCache;
			Object value;
			final Object readerKey = reader.getCoreCacheKey();
			synchronized (readerCache) {
				innerCache = readerCache.get(readerKey);
				if (innerCache == null) {

					innerCache = new HashMap<Entry, Object>();
					readerCache.put(readerKey, innerCache);
					reader.addReaderFinishedListener(purgeReader);
					value = null;
				} else {
					value = innerCache.get(key);
				}
				if (value == null) {
					value = new CreationPlaceholder();
					innerCache.put(key, value);
				}
			}
			if (value instanceof CreationPlaceholder) {
				synchronized (value) {
					CreationPlaceholder progress = (CreationPlaceholder) value;
					if (progress.value == null) {
						progress.value = createValue(reader, key,
								setDocsWithField);
						synchronized (readerCache) {
							innerCache.put(key, progress.value);
						}

						if (key.custom != null && wrapper != null) {
							final PrintStream infoStream = wrapper
									.getInfoStream();
							if (infoStream != null) {
								printNewInsanity(infoStream, progress.value);
							}
						}
					}
					return progress.value;
				}
			}
			return value;
		}

		private void printNewInsanity(PrintStream infoStream, Object value) {
			final FieldCacheSanityChecker.Insanity[] insanities = FieldCacheSanityChecker
					.checkSanity(wrapper);
			for (int i = 0; i < insanities.length; i++) {
				final FieldCacheSanityChecker.Insanity insanity = insanities[i];
				final CacheEntry[] entries = insanity.getCacheEntries();
				for (int j = 0; j < entries.length; j++) {
					if (entries[j].getValue() == value) {

						infoStream
								.println("WARNING: new FieldCache insanity created\nDetails: "
										+ insanity.toString());
						infoStream.println("\nStack:\n");
						new Throwable().printStackTrace(infoStream);
						break;
					}
				}
			}
		}
	}

	static class Entry {
		final String field;
		final Object custom;

		Entry(String field, Object custom) {
			this.field = StringHelper.intern(field);
			this.custom = custom;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Entry) {
				Entry other = (Entry) o;
				if (other.field == field) {
					if (other.custom == null) {
						if (custom == null)
							return true;
					} else if (other.custom.equals(custom)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return field.hashCode() ^ (custom == null ? 0 : custom.hashCode());
		}
	}

	public byte[] getBytes(IndexReader reader, String field) throws IOException {
		return getBytes(reader, field, null, false);
	}

	public byte[] getBytes(IndexReader reader, String field, ByteParser parser)
			throws IOException {
		return getBytes(reader, field, parser, false);
	}

	public byte[] getBytes(IndexReader reader, String field, ByteParser parser,
			boolean setDocsWithField) throws IOException {
		return (byte[]) caches.get(Byte.TYPE).get(reader,
				new Entry(field, parser), setDocsWithField);
	}

	static final class ByteCache extends Cache {
		ByteCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			Entry entry = entryKey;
			String field = entry.field;
			ByteParser parser = (ByteParser) entry.custom;
			if (parser == null) {
				return wrapper.getBytes(reader, field,
						FieldCache.DEFAULT_BYTE_PARSER, setDocsWithField);
			}
			final int maxDoc = reader.maxDoc();
			final byte[] retArray = new byte[maxDoc];
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			FixedBitSet docsWithField = null;
			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					byte termval = parser.parseByte(term.text());
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						final int docID = termDocs.doc();
						retArray[docID] = termval;
						if (setDocsWithField) {
							if (docsWithField == null) {

								docsWithField = new FixedBitSet(maxDoc);
							}
							docsWithField.set(docID);
						}
					}
				} while (termEnum.next());
			} catch (StopFillCacheException stop) {
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (setDocsWithField) {
				wrapper.setDocsWithField(reader, field, docsWithField);
			}
			return retArray;
		}
	}

	public short[] getShorts(IndexReader reader, String field)
			throws IOException {
		return getShorts(reader, field, null, false);
	}

	public short[] getShorts(IndexReader reader, String field,
			ShortParser parser) throws IOException {
		return getShorts(reader, field, parser, false);
	}

	public short[] getShorts(IndexReader reader, String field,
			ShortParser parser, boolean setDocsWithField) throws IOException {
		return (short[]) caches.get(Short.TYPE).get(reader,
				new Entry(field, parser), setDocsWithField);
	}

	static final class ShortCache extends Cache {
		ShortCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			Entry entry = entryKey;
			String field = entry.field;
			ShortParser parser = (ShortParser) entry.custom;
			if (parser == null) {
				return wrapper.getShorts(reader, field,
						FieldCache.DEFAULT_SHORT_PARSER, setDocsWithField);
			}
			final int maxDoc = reader.maxDoc();
			final short[] retArray = new short[maxDoc];
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			FixedBitSet docsWithField = null;
			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					short termval = parser.parseShort(term.text());
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						final int docID = termDocs.doc();
						retArray[docID] = termval;
						if (setDocsWithField) {
							if (docsWithField == null) {

								docsWithField = new FixedBitSet(maxDoc);
							}
							docsWithField.set(docID);
						}
					}
				} while (termEnum.next());
			} catch (StopFillCacheException stop) {
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (setDocsWithField) {
				wrapper.setDocsWithField(reader, field, docsWithField);
			}
			return retArray;
		}
	}

	void setDocsWithField(IndexReader reader, String field, Bits docsWithField) {
		final int maxDoc = reader.maxDoc();
		final Bits bits;
		if (docsWithField == null) {
			bits = new Bits.MatchNoBits(maxDoc);
		} else if (docsWithField instanceof FixedBitSet) {
			final int numSet = ((FixedBitSet) docsWithField).cardinality();
			if (numSet >= maxDoc) {

				assert numSet == maxDoc;
				bits = new Bits.MatchAllBits(maxDoc);
			} else {
				bits = docsWithField;
			}
		} else {
			bits = docsWithField;
		}
		caches.get(DocsWithFieldCache.class).put(reader,
				new Entry(field, null), bits);
	}

	public int[] getInts(IndexReader reader, String field) throws IOException {
		return getInts(reader, field, null);
	}

	public int[] getInts(IndexReader reader, String field, IntParser parser)
			throws IOException {
		return getInts(reader, field, parser, false);
	}

	public int[] getInts(IndexReader reader, String field, IntParser parser,
			boolean setDocsWithField) throws IOException {
		return (int[]) caches.get(Integer.TYPE).get(reader,
				new Entry(field, parser), setDocsWithField);
	}

	static final class IntCache extends Cache {
		IntCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			Entry entry = entryKey;
			String field = entry.field;
			IntParser parser = (IntParser) entry.custom;
			if (parser == null) {
				try {
					return wrapper.getInts(reader, field, DEFAULT_INT_PARSER,
							setDocsWithField);
				} catch (NumberFormatException ne) {
					return wrapper.getInts(reader, field,
							NUMERIC_UTILS_INT_PARSER, setDocsWithField);
				}
			}
			final int maxDoc = reader.maxDoc();
			int[] retArray = null;
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			FixedBitSet docsWithField = null;
			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					int termval = parser.parseInt(term.text());
					if (retArray == null) {
						retArray = new int[maxDoc];
					}
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						final int docID = termDocs.doc();
						retArray[docID] = termval;
						if (setDocsWithField) {
							if (docsWithField == null) {

								docsWithField = new FixedBitSet(maxDoc);
							}
							docsWithField.set(docID);
						}
					}
				} while (termEnum.next());
			} catch (StopFillCacheException stop) {
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (setDocsWithField) {
				wrapper.setDocsWithField(reader, field, docsWithField);
			}
			if (retArray == null) {
				retArray = new int[maxDoc];
			}
			return retArray;
		}
	}

	public Bits getDocsWithField(IndexReader reader, String field)
			throws IOException {
		return (Bits) caches.get(DocsWithFieldCache.class).get(reader,
				new Entry(field, null), false);
	}

	static final class DocsWithFieldCache extends Cache {
		DocsWithFieldCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			final Entry entry = entryKey;
			final String field = entry.field;
			FixedBitSet res = null;
			final TermDocs termDocs = reader.termDocs();
			final TermEnum termEnum = reader.terms(new Term(field));
			try {
				do {
					final Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					if (res == null)
						res = new FixedBitSet(reader.maxDoc());
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						res.set(termDocs.doc());
					}
				} while (termEnum.next());
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (res == null)
				return new Bits.MatchNoBits(reader.maxDoc());
			final int numSet = res.cardinality();
			if (numSet >= reader.numDocs()) {

				assert numSet == reader.numDocs();
				return new Bits.MatchAllBits(reader.maxDoc());
			}
			return res;
		}
	}

	public float[] getFloats(IndexReader reader, String field)
			throws IOException {
		return getFloats(reader, field, null, false);
	}

	public float[] getFloats(IndexReader reader, String field,
			FloatParser parser) throws IOException {
		return getFloats(reader, field, parser, false);
	}

	public float[] getFloats(IndexReader reader, String field,
			FloatParser parser, boolean setDocsWithField) throws IOException {

		return (float[]) caches.get(Float.TYPE).get(reader,
				new Entry(field, parser), setDocsWithField);
	}

	static final class FloatCache extends Cache {
		FloatCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			Entry entry = entryKey;
			String field = entry.field;
			FloatParser parser = (FloatParser) entry.custom;
			if (parser == null) {
				try {
					return wrapper.getFloats(reader, field,
							DEFAULT_FLOAT_PARSER, setDocsWithField);
				} catch (NumberFormatException ne) {
					return wrapper.getFloats(reader, field,
							NUMERIC_UTILS_FLOAT_PARSER, setDocsWithField);
				}
			}
			final int maxDoc = reader.maxDoc();
			float[] retArray = null;
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			FixedBitSet docsWithField = null;
			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					float termval = parser.parseFloat(term.text());
					if (retArray == null) {
						retArray = new float[maxDoc];
					}
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						final int docID = termDocs.doc();
						retArray[docID] = termval;
						if (setDocsWithField) {
							if (docsWithField == null) {

								docsWithField = new FixedBitSet(maxDoc);
							}
							docsWithField.set(docID);
						}
					}
				} while (termEnum.next());
			} catch (StopFillCacheException stop) {
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (setDocsWithField) {
				wrapper.setDocsWithField(reader, field, docsWithField);
			}
			if (retArray == null) {
				retArray = new float[maxDoc];
			}
			return retArray;
		}
	}

	public long[] getLongs(IndexReader reader, String field) throws IOException {
		return getLongs(reader, field, null, false);
	}

	public long[] getLongs(IndexReader reader, String field,
			FieldCache.LongParser parser) throws IOException {
		return getLongs(reader, field, parser, false);
	}

	public long[] getLongs(IndexReader reader, String field,
			FieldCache.LongParser parser, boolean setDocsWithField)
			throws IOException {
		return (long[]) caches.get(Long.TYPE).get(reader,
				new Entry(field, parser), setDocsWithField);
	}

	static final class LongCache extends Cache {
		LongCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entry,
				boolean setDocsWithField) throws IOException {
			String field = entry.field;
			FieldCache.LongParser parser = (FieldCache.LongParser) entry.custom;
			if (parser == null) {
				try {
					return wrapper.getLongs(reader, field, DEFAULT_LONG_PARSER,
							setDocsWithField);
				} catch (NumberFormatException ne) {
					return wrapper.getLongs(reader, field,
							NUMERIC_UTILS_LONG_PARSER, setDocsWithField);
				}
			}
			final int maxDoc = reader.maxDoc();
			long[] retArray = null;
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			FixedBitSet docsWithField = null;
			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					long termval = parser.parseLong(term.text());
					if (retArray == null) {
						retArray = new long[maxDoc];
					}
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						final int docID = termDocs.doc();
						retArray[docID] = termval;
						if (setDocsWithField) {
							if (docsWithField == null) {

								docsWithField = new FixedBitSet(maxDoc);
							}
							docsWithField.set(docID);
						}
					}
				} while (termEnum.next());
			} catch (StopFillCacheException stop) {
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (setDocsWithField) {
				wrapper.setDocsWithField(reader, field, docsWithField);
			}
			if (retArray == null) {
				retArray = new long[maxDoc];
			}
			return retArray;
		}
	}

	public double[] getDoubles(IndexReader reader, String field)
			throws IOException {
		return getDoubles(reader, field, null, false);
	}

	public double[] getDoubles(IndexReader reader, String field,
			FieldCache.DoubleParser parser) throws IOException {
		return getDoubles(reader, field, parser, false);
	}

	public double[] getDoubles(IndexReader reader, String field,
			FieldCache.DoubleParser parser, boolean setDocsWithField)
			throws IOException {
		return (double[]) caches.get(Double.TYPE).get(reader,
				new Entry(field, parser), setDocsWithField);
	}

	static final class DoubleCache extends Cache {
		DoubleCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			Entry entry = entryKey;
			String field = entry.field;
			FieldCache.DoubleParser parser = (FieldCache.DoubleParser) entry.custom;
			if (parser == null) {
				try {
					return wrapper.getDoubles(reader, field,
							DEFAULT_DOUBLE_PARSER, setDocsWithField);
				} catch (NumberFormatException ne) {
					return wrapper.getDoubles(reader, field,
							NUMERIC_UTILS_DOUBLE_PARSER, setDocsWithField);
				}
			}
			final int maxDoc = reader.maxDoc();
			double[] retArray = null;
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			FixedBitSet docsWithField = null;
			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					double termval = parser.parseDouble(term.text());
					if (retArray == null) {
						retArray = new double[maxDoc];
					}
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						final int docID = termDocs.doc();
						retArray[docID] = termval;
						if (setDocsWithField) {
							if (docsWithField == null) {

								docsWithField = new FixedBitSet(maxDoc);
							}
							docsWithField.set(docID);
						}
					}
				} while (termEnum.next());
			} catch (StopFillCacheException stop) {
			} finally {
				termDocs.close();
				termEnum.close();
			}
			if (setDocsWithField) {
				wrapper.setDocsWithField(reader, field, docsWithField);
			}
			if (retArray == null) {
				retArray = new double[maxDoc];
			}
			return retArray;
		}
	}

	public String[] getStrings(IndexReader reader, String field)
			throws IOException {
		return (String[]) caches.get(String.class).get(reader,
				new Entry(field, (Parser) null), false);
	}

	static final class StringCache extends Cache {
		StringCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			String field = StringHelper.intern(entryKey.field);
			final String[] retArray = new String[reader.maxDoc()];
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			final int termCountHardLimit = reader.maxDoc();
			int termCount = 0;
			try {
				do {
					if (termCount++ == termCountHardLimit) {

						break;
					}

					Term term = termEnum.term();
					if (term == null || term.field() != field)
						break;
					String termval = term.text();
					termDocs.seek(termEnum);
					while (termDocs.next()) {
						retArray[termDocs.doc()] = termval;
					}
				} while (termEnum.next());
			} finally {
				termDocs.close();
				termEnum.close();
			}
			return retArray;
		}
	}

	public StringIndex getStringIndex(IndexReader reader, String field)
			throws IOException {
		return (StringIndex) caches.get(StringIndex.class).get(reader,
				new Entry(field, (Parser) null), false);
	}

	static final class StringIndexCache extends Cache {
		StringIndexCache(FieldCacheImpl wrapper) {
			super(wrapper);
		}

		@Override
		protected Object createValue(IndexReader reader, Entry entryKey,
				boolean setDocsWithField) throws IOException {
			String field = StringHelper.intern(entryKey.field);
			final int[] retArray = new int[reader.maxDoc()];
			String[] mterms = new String[reader.maxDoc() + 1];
			TermDocs termDocs = reader.termDocs();
			TermEnum termEnum = reader.terms(new Term(field));
			int t = 0;

			mterms[t++] = null;

			try {
				do {
					Term term = termEnum.term();
					if (term == null || term.field() != field
							|| t >= mterms.length)
						break;

					mterms[t] = term.text();

					termDocs.seek(termEnum);
					while (termDocs.next()) {
						retArray[termDocs.doc()] = t;
					}

					t++;
				} while (termEnum.next());
			} finally {
				termDocs.close();
				termEnum.close();
			}

			if (t == 0) {

				mterms = new String[1];
			} else if (t < mterms.length) {

				String[] terms = new String[t];
				System.arraycopy(mterms, 0, terms, 0, t);
				mterms = terms;
			}

			StringIndex value = new StringIndex(retArray, mterms);
			return value;
		}
	}

	private volatile PrintStream infoStream;

	public void setInfoStream(PrintStream stream) {
		infoStream = stream;
	}

	public PrintStream getInfoStream() {
		return infoStream;
	}
}
