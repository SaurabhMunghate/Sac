/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.Serializable;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.FixedBitSet;

public class CachingWrapperFilter extends Filter {
	Filter filter;

	public static enum DeletesMode {
		IGNORE, RECACHE, DYNAMIC
	};

	protected final FilterCache<DocIdSet> cache;

	static abstract class FilterCache<T> implements Serializable {

		transient Map<Object, T> cache;

		private final DeletesMode deletesMode;

		public FilterCache(DeletesMode deletesMode) {
			this.deletesMode = deletesMode;
		}

		public synchronized T get(IndexReader reader, Object coreKey,
				Object delCoreKey) throws IOException {
			T value;

			if (cache == null) {
				cache = new WeakHashMap<Object, T>();
			}

			if (deletesMode == DeletesMode.IGNORE) {

				value = cache.get(coreKey);
			} else if (deletesMode == DeletesMode.RECACHE) {

				value = cache.get(delCoreKey);
			} else {
				assert deletesMode == DeletesMode.DYNAMIC;

				value = cache.get(delCoreKey);

				if (value == null) {

					value = cache.get(coreKey);
					if (value != null && reader.hasDeletions()) {
						value = mergeDeletes(reader, value);
					}
				}
			}

			return value;
		}

		protected abstract T mergeDeletes(IndexReader reader, T value);

		public synchronized void put(Object coreKey, Object delCoreKey, T value) {
			if (deletesMode == DeletesMode.IGNORE) {
				cache.put(coreKey, value);
			} else if (deletesMode == DeletesMode.RECACHE) {
				cache.put(delCoreKey, value);
			} else {
				cache.put(coreKey, value);
				cache.put(delCoreKey, value);
			}
		}
	}

	public CachingWrapperFilter(Filter filter) {
		this(filter, DeletesMode.IGNORE);
	}

	public CachingWrapperFilter(Filter filter, DeletesMode deletesMode) {
		this.filter = filter;
		cache = new FilterCache<DocIdSet>(deletesMode) {
			@Override
			public DocIdSet mergeDeletes(final IndexReader r,
					final DocIdSet docIdSet) {
				return new FilteredDocIdSet(docIdSet) {
					@Override
					protected boolean match(int docID) {
						return !r.isDeleted(docID);
					}
				};
			}
		};
	}

	protected DocIdSet docIdSetToCache(DocIdSet docIdSet, IndexReader reader)
			throws IOException {
		if (docIdSet == null) {

			return DocIdSet.EMPTY_DOCIDSET;
		} else if (docIdSet.isCacheable()) {
			return docIdSet;
		} else {
			final DocIdSetIterator it = docIdSet.iterator();

			if (it == null) {
				return DocIdSet.EMPTY_DOCIDSET;
			} else {
				final FixedBitSet bits = new FixedBitSet(reader.maxDoc());
				bits.or(it);
				return bits;
			}
		}
	}

	int hitCount, missCount;

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {

		final Object coreKey = reader.getCoreCacheKey();
		final Object delCoreKey = reader.hasDeletions() ? reader
				.getDeletesCacheKey() : coreKey;

		DocIdSet docIdSet = cache.get(reader, coreKey, delCoreKey);
		if (docIdSet != null) {
			hitCount++;
			return docIdSet;
		}

		missCount++;

		docIdSet = docIdSetToCache(filter.getDocIdSet(reader), reader);

		if (docIdSet != null) {
			cache.put(coreKey, delCoreKey, docIdSet);
		}

		return docIdSet;
	}

	@Override
	public String toString() {
		return "CachingWrapperFilter(" + filter + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CachingWrapperFilter))
			return false;
		return this.filter.equals(((CachingWrapperFilter) o).filter);
	}

	@Override
	public int hashCode() {
		return filter.hashCode() ^ 0x1117BF25;
	}
}
