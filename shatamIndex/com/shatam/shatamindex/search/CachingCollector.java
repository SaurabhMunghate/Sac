/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.RamUsageEstimator;

public abstract class CachingCollector extends Collector {

	private static final int MAX_ARRAY_SIZE = 512 * 1024;
	private static final int INITIAL_ARRAY_SIZE = 128;
	private final static int[] EMPTY_INT_ARRAY = new int[0];

	private static class SegStart {
		public final IndexReader reader;
		public final int base;
		public final int end;

		public SegStart(IndexReader reader, int base, int end) {
			this.reader = reader;
			this.base = base;
			this.end = end;
		}
	}

	private static final class CachedScorer extends Scorer {

		int doc;
		float score;

		private CachedScorer() {
			super((Weight) null);
		}

		@Override
		public final float score() {
			return score;
		}

		@Override
		public final int advance(int target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final int docID() {
			return doc;
		}

		@Override
		public final float freq() {
			throw new UnsupportedOperationException();
		}

		@Override
		public final int nextDoc() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class ScoreCachingCollector extends CachingCollector {

		private final CachedScorer cachedScorer;
		private final List<float[]> cachedScores;

		private Scorer scorer;
		private float[] curScores;

		ScoreCachingCollector(Collector other, double maxRAMMB) {
			super(other, maxRAMMB, true);

			cachedScorer = new CachedScorer();
			cachedScores = new ArrayList<float[]>();
			curScores = new float[128];
			cachedScores.add(curScores);
		}

		ScoreCachingCollector(Collector other, int maxDocsToCache) {
			super(other, maxDocsToCache);

			cachedScorer = new CachedScorer();
			cachedScores = new ArrayList<float[]>();
			curScores = new float[INITIAL_ARRAY_SIZE];
			cachedScores.add(curScores);
		}

		@Override
		public void collect(int doc) throws IOException {

			if (curDocs == null) {

				cachedScorer.score = scorer.score();
				cachedScorer.doc = doc;
				other.collect(doc);
				return;
			}

			if (upto == curDocs.length) {
				base += upto;

				int nextLength = 8 * curDocs.length;
				if (nextLength > MAX_ARRAY_SIZE) {
					nextLength = MAX_ARRAY_SIZE;
				}

				if (base + nextLength > maxDocsToCache) {

					nextLength = maxDocsToCache - base;
					if (nextLength <= 0) {

						curDocs = null;
						curScores = null;
						cachedSegs.clear();
						cachedDocs.clear();
						cachedScores.clear();
						cachedScorer.score = scorer.score();
						cachedScorer.doc = doc;
						other.collect(doc);
						return;
					}
				}

				curDocs = new int[nextLength];
				cachedDocs.add(curDocs);
				curScores = new float[nextLength];
				cachedScores.add(curScores);
				upto = 0;
			}

			curDocs[upto] = doc;
			cachedScorer.score = curScores[upto] = scorer.score();
			upto++;
			cachedScorer.doc = doc;
			other.collect(doc);
		}

		@Override
		public void replay(Collector other) throws IOException {
			replayInit(other);

			int curUpto = 0;
			int curBase = 0;
			int chunkUpto = 0;
			curDocs = EMPTY_INT_ARRAY;
			for (SegStart seg : cachedSegs) {
				other.setNextReader(seg.reader, seg.base);
				other.setScorer(cachedScorer);
				while (curBase + curUpto < seg.end) {
					if (curUpto == curDocs.length) {
						curBase += curDocs.length;
						curDocs = cachedDocs.get(chunkUpto);
						curScores = cachedScores.get(chunkUpto);
						chunkUpto++;
						curUpto = 0;
					}
					cachedScorer.score = curScores[curUpto];
					cachedScorer.doc = curDocs[curUpto];
					other.collect(curDocs[curUpto++]);
				}
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
			other.setScorer(cachedScorer);
		}

		@Override
		public String toString() {
			if (isCached()) {
				return "CachingCollector (" + (base + upto)
						+ " docs & scores cached)";
			} else {
				return "CachingCollector (cache was cleared)";
			}
		}

	}

	private static final class NoScoreCachingCollector extends CachingCollector {

		NoScoreCachingCollector(Collector other, double maxRAMMB) {
			super(other, maxRAMMB, false);
		}

		NoScoreCachingCollector(Collector other, int maxDocsToCache) {
			super(other, maxDocsToCache);
		}

		@Override
		public void collect(int doc) throws IOException {

			if (curDocs == null) {

				other.collect(doc);
				return;
			}

			if (upto == curDocs.length) {
				base += upto;

				int nextLength = 8 * curDocs.length;
				if (nextLength > MAX_ARRAY_SIZE) {
					nextLength = MAX_ARRAY_SIZE;
				}

				if (base + nextLength > maxDocsToCache) {

					nextLength = maxDocsToCache - base;
					if (nextLength <= 0) {

						curDocs = null;
						cachedSegs.clear();
						cachedDocs.clear();
						other.collect(doc);
						return;
					}
				}

				curDocs = new int[nextLength];
				cachedDocs.add(curDocs);
				upto = 0;
			}

			curDocs[upto] = doc;
			upto++;
			other.collect(doc);
		}

		@Override
		public void replay(Collector other) throws IOException {
			replayInit(other);

			int curUpto = 0;
			int curbase = 0;
			int chunkUpto = 0;
			curDocs = EMPTY_INT_ARRAY;
			for (SegStart seg : cachedSegs) {
				other.setNextReader(seg.reader, seg.base);
				while (curbase + curUpto < seg.end) {
					if (curUpto == curDocs.length) {
						curbase += curDocs.length;
						curDocs = cachedDocs.get(chunkUpto);
						chunkUpto++;
						curUpto = 0;
					}
					other.collect(curDocs[curUpto++]);
				}
			}
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			other.setScorer(scorer);
		}

		@Override
		public String toString() {
			if (isCached()) {
				return "CachingCollector (" + (base + upto) + " docs cached)";
			} else {
				return "CachingCollector (cache was cleared)";
			}
		}

	}

	protected final Collector other;

	protected final int maxDocsToCache;
	protected final List<SegStart> cachedSegs = new ArrayList<SegStart>();
	protected final List<int[]> cachedDocs;

	private IndexReader lastReader;

	protected int[] curDocs;
	protected int upto;
	protected int base;
	protected int lastDocBase;

	public static CachingCollector create(final boolean acceptDocsOutOfOrder,
			boolean cacheScores, double maxRAMMB) {
		Collector other = new Collector() {
			@Override
			public boolean acceptsDocsOutOfOrder() {
				return acceptDocsOutOfOrder;
			}

			@Override
			public void setScorer(Scorer scorer) throws IOException {
			}

			@Override
			public void collect(int doc) throws IOException {
			}

			@Override
			public void setNextReader(IndexReader reader, int docBase)
					throws IOException {
			}

		};
		return create(other, cacheScores, maxRAMMB);
	}

	public static CachingCollector create(Collector other, boolean cacheScores,
			double maxRAMMB) {
		return cacheScores ? new ScoreCachingCollector(other, maxRAMMB)
				: new NoScoreCachingCollector(other, maxRAMMB);
	}

	public static CachingCollector create(Collector other, boolean cacheScores,
			int maxDocsToCache) {
		return cacheScores ? new ScoreCachingCollector(other, maxDocsToCache)
				: new NoScoreCachingCollector(other, maxDocsToCache);
	}

	private CachingCollector(Collector other, double maxRAMMB,
			boolean cacheScores) {
		this.other = other;

		cachedDocs = new ArrayList<int[]>();
		curDocs = new int[INITIAL_ARRAY_SIZE];
		cachedDocs.add(curDocs);

		int bytesPerDoc = RamUsageEstimator.NUM_BYTES_INT;
		if (cacheScores) {
			bytesPerDoc += RamUsageEstimator.NUM_BYTES_FLOAT;
		}
		maxDocsToCache = (int) ((maxRAMMB * 1024 * 1024) / bytesPerDoc);
	}

	private CachingCollector(Collector other, int maxDocsToCache) {
		this.other = other;

		cachedDocs = new ArrayList<int[]>();
		curDocs = new int[INITIAL_ARRAY_SIZE];
		cachedDocs.add(curDocs);
		this.maxDocsToCache = maxDocsToCache;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return other.acceptsDocsOutOfOrder();
	}

	public boolean isCached() {
		return curDocs != null;
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase)
			throws IOException {
		other.setNextReader(reader, docBase);
		if (lastReader != null) {
			cachedSegs.add(new SegStart(lastReader, lastDocBase, base + upto));
		}
		lastDocBase = docBase;
		lastReader = reader;
	}

	void replayInit(Collector other) {
		if (!isCached()) {
			throw new IllegalStateException(
					"cannot replay: cache was cleared because too much RAM was required");
		}

		if (!other.acceptsDocsOutOfOrder()
				&& this.other.acceptsDocsOutOfOrder()) {
			throw new IllegalArgumentException(
					"cannot replay: given collector does not support "
							+ "out-of-order collection, while the wrapped collector does. "
							+ "Therefore cached documents may be out-of-order.");
		}

		if (lastReader != null) {
			cachedSegs.add(new SegStart(lastReader, lastDocBase, base + upto));
			lastReader = null;
		}
	}

	public abstract void replay(Collector other) throws IOException;

}
