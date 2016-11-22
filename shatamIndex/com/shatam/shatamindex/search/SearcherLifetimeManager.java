/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.NRTManager;
import com.shatam.shatamindex.store.AlreadyClosedException;
import com.shatam.shatamindex.util.IOUtils;

public class SearcherLifetimeManager implements Closeable {

	private static class SearcherTracker implements
			Comparable<SearcherTracker>, Closeable {
		public final IndexSearcher searcher;
		public final long recordTimeSec;
		public final long version;

		public SearcherTracker(IndexSearcher searcher) {
			this.searcher = searcher;
			version = searcher.getIndexReader().getVersion();
			searcher.getIndexReader().incRef();

			recordTimeSec = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		}

		public int compareTo(SearcherTracker other) {

			if (recordTimeSec < other.recordTimeSec) {
				return 1;
			} else if (other.recordTimeSec < recordTimeSec) {
				return -1;
			} else {
				return 0;
			}
		}

		public synchronized void close() throws IOException {
			searcher.getIndexReader().decRef();
		}
	}

	private volatile boolean closed;

	private final ConcurrentHashMap<Long, SearcherTracker> searchers = new ConcurrentHashMap<Long, SearcherTracker>();

	private void ensureOpen() {
		if (closed) {
			throw new AlreadyClosedException(
					"this SearcherLifetimeManager instance is closed");
		}
	}

	public long record(IndexSearcher searcher) throws IOException {
		ensureOpen();

		final long version = searcher.getIndexReader().getVersion();
		SearcherTracker tracker = searchers.get(version);
		if (tracker == null) {
			tracker = new SearcherTracker(searcher);
			if (searchers.putIfAbsent(version, tracker) != null) {

				tracker.close();
			}
		} else if (tracker.searcher != searcher) {
			throw new IllegalArgumentException(
					"the provided searcher has the same underlying reader version yet the searcher instance differs from before (new="
							+ searcher + " vs old=" + tracker.searcher);
		}

		return version;
	}

	public IndexSearcher acquire(long version) {
		ensureOpen();
		final SearcherTracker tracker = searchers.get(version);
		if (tracker != null && tracker.searcher.getIndexReader().tryIncRef()) {
			return tracker.searcher;
		}

		return null;
	}

	public void release(IndexSearcher s) throws IOException {
		s.getIndexReader().decRef();
	}

	public interface Pruner {

		public boolean doPrune(int ageSec, IndexSearcher searcher);
	}

	public final static class PruneByAge implements Pruner {
		private final int maxAgeSec;

		public PruneByAge(int maxAgeSec) {
			if (maxAgeSec < 1) {
				throw new IllegalArgumentException(
						"maxAgeSec must be > 0 (got " + maxAgeSec + ")");
			}
			this.maxAgeSec = maxAgeSec;
		}

		public boolean doPrune(int ageSec, IndexSearcher searcher) {
			return ageSec > maxAgeSec;
		}
	}

	public synchronized void prune(Pruner pruner) throws IOException {

		final List<SearcherTracker> trackers = new ArrayList<SearcherTracker>();
		for (SearcherTracker tracker : searchers.values()) {
			trackers.add(tracker);
		}
		Collections.sort(trackers);
		final long newestSec = trackers.isEmpty() ? 0L
				: trackers.get(0).recordTimeSec;
		for (SearcherTracker tracker : trackers) {
			final int ageSec = (int) (newestSec - tracker.recordTimeSec);
			assert ageSec >= 0;
			if (pruner.doPrune(ageSec, tracker.searcher)) {
				searchers.remove(tracker.version);
				tracker.close();
			}
		}
	}

	public synchronized void close() throws IOException {
		closed = true;
		final List<SearcherTracker> toClose = new ArrayList<SearcherTracker>(
				searchers.values());

		for (SearcherTracker tracker : toClose) {
			searchers.remove(tracker.version);
		}

		IOUtils.close(toClose);

		if (searchers.size() != 0) {
			throw new IllegalStateException(
					"another thread called record while this SearcherLifetimeManager instance was being closed; not all searchers were closed");
		}
	}
}
