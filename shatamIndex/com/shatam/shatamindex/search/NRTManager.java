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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.IndexWriter;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.ThreadInterruptedException;

public class NRTManager implements Closeable {
	private static final long MAX_SEARCHER_GEN = Long.MAX_VALUE;
	private final IndexWriter writer;
	private final SearcherManagerRef withoutDeletes;
	private final SearcherManagerRef withDeletes;
	private final AtomicLong indexingGen;
	private final List<WaitingListener> waitingListeners = new CopyOnWriteArrayList<WaitingListener>();
	private final ReentrantLock reopenLock = new ReentrantLock();
	private final Condition newGeneration = reopenLock.newCondition();

	public NRTManager(IndexWriter writer, SearcherWarmer warmer)
			throws IOException {
		this(writer, null, warmer, true);
	}

	public NRTManager(IndexWriter writer, ExecutorService es,
			SearcherWarmer warmer) throws IOException {
		this(writer, es, warmer, true);
	}

	public NRTManager(IndexWriter writer, ExecutorService es,
			SearcherWarmer warmer, boolean alwaysApplyDeletes)
			throws IOException {
		this.writer = writer;
		if (alwaysApplyDeletes) {
			withoutDeletes = withDeletes = new SearcherManagerRef(true, 0,
					new SearcherManager(writer, true, warmer, es));
		} else {
			withDeletes = new SearcherManagerRef(true, 0, new SearcherManager(
					writer, true, warmer, es));
			withoutDeletes = new SearcherManagerRef(false, 0,
					new SearcherManager(writer, false, warmer, es));
		}
		indexingGen = new AtomicLong(1);
	}

	public static interface WaitingListener {
		public void waiting(boolean requiresDeletes, long targetGen);
	}

	public void addWaitingListener(WaitingListener l) {
		waitingListeners.add(l);
	}

	public void removeWaitingListener(WaitingListener l) {
		waitingListeners.remove(l);
	}

	public long updateDocument(Term t, Document d, Analyzer a)
			throws IOException {
		writer.updateDocument(t, d, a);

		return indexingGen.get();
	}

	public long updateDocument(Term t, Document d) throws IOException {
		writer.updateDocument(t, d);

		return indexingGen.get();
	}

	public long updateDocuments(Term t, Collection<Document> docs, Analyzer a)
			throws IOException {
		writer.updateDocuments(t, docs, a);

		return indexingGen.get();
	}

	public long updateDocuments(Term t, Collection<Document> docs)
			throws IOException {
		writer.updateDocuments(t, docs);

		return indexingGen.get();
	}

	public long deleteDocuments(Term t) throws IOException {
		writer.deleteDocuments(t);

		return indexingGen.get();
	}

	public long deleteDocuments(Term... terms) throws IOException {
		writer.deleteDocuments(terms);

		return indexingGen.get();
	}

	public long deleteDocuments(Query q) throws IOException {
		writer.deleteDocuments(q);

		return indexingGen.get();
	}

	public long deleteDocuments(Query... queries) throws IOException {
		writer.deleteDocuments(queries);

		return indexingGen.get();
	}

	public long deleteAll() throws IOException {
		writer.deleteAll();

		return indexingGen.get();
	}

	public long addDocument(Document d, Analyzer a) throws IOException {
		writer.addDocument(d, a);

		return indexingGen.get();
	}

	public long addDocuments(Collection<Document> docs, Analyzer a)
			throws IOException {
		writer.addDocuments(docs, a);

		return indexingGen.get();
	}

	public long addDocument(Document d) throws IOException {
		writer.addDocument(d);

		return indexingGen.get();
	}

	public long addDocuments(Collection<Document> docs) throws IOException {
		writer.addDocuments(docs);

		return indexingGen.get();
	}

	public long addIndexes(Directory... dirs) throws CorruptIndexException,
			IOException {
		writer.addIndexes(dirs);

		return indexingGen.get();
	}

	public long addIndexes(IndexReader... readers)
			throws CorruptIndexException, IOException {
		writer.addIndexes(readers);

		return indexingGen.get();
	}

	public SearcherManager waitForGeneration(long targetGen,
			boolean requireDeletes) {
		return waitForGeneration(targetGen, requireDeletes, -1,
				TimeUnit.NANOSECONDS);
	}

	public SearcherManager waitForGeneration(long targetGen,
			boolean requireDeletes, long time, TimeUnit unit) {
		try {
			final long curGen = indexingGen.get();
			if (targetGen > curGen) {
				throw new IllegalArgumentException(
						"targetGen="
								+ targetGen
								+ " was never returned by this NRTManager instance (current gen="
								+ curGen + ")");
			}
			reopenLock.lockInterruptibly();
			try {
				if (targetGen > getCurrentSearchingGen(requireDeletes)) {
					for (WaitingListener listener : waitingListeners) {
						listener.waiting(requireDeletes, targetGen);
					}
					while (targetGen > getCurrentSearchingGen(requireDeletes)) {
						if (!waitOnGenCondition(time, unit)) {
							return getSearcherManager(requireDeletes);
						}
					}
				}

			} finally {
				reopenLock.unlock();
			}
		} catch (InterruptedException ie) {
			throw new ThreadInterruptedException(ie);
		}
		return getSearcherManager(requireDeletes);
	}

	private boolean waitOnGenCondition(long time, TimeUnit unit)
			throws InterruptedException {
		assert reopenLock.isHeldByCurrentThread();
		if (time < 0) {
			newGeneration.await();
			return true;
		} else {
			return newGeneration.await(time, unit);
		}
	}

	public long getCurrentSearchingGen(boolean applyAllDeletes) {
		if (applyAllDeletes) {
			return withDeletes.generation;
		} else {
			return Math.max(withoutDeletes.generation, withDeletes.generation);
		}
	}

	public boolean maybeReopen(boolean applyAllDeletes) throws IOException {
		if (reopenLock.tryLock()) {
			try {
				final SearcherManagerRef reference = applyAllDeletes ? withDeletes
						: withoutDeletes;

				final long newSearcherGen = indexingGen.getAndIncrement();
				boolean setSearchGen = false;
				if (reference.generation == MAX_SEARCHER_GEN) {
					newGeneration.signalAll();
					return false;
				}
				if (!(setSearchGen = reference.manager.isSearcherCurrent())) {
					setSearchGen = reference.manager.maybeReopen();
				}
				if (setSearchGen) {
					reference.generation = newSearcherGen;
					newGeneration.signalAll();
				}
				return setSearchGen;
			} finally {
				reopenLock.unlock();
			}
		}
		return false;
	}

	public void close() throws IOException {
		reopenLock.lock();
		try {
			try {
				IOUtils.close(withDeletes, withoutDeletes);
			} finally {
				newGeneration.signalAll();
			}
		} finally {
			reopenLock.unlock();
			assert withDeletes.generation == MAX_SEARCHER_GEN
					&& withoutDeletes.generation == MAX_SEARCHER_GEN;
		}
	}

	public SearcherManager getSearcherManager(boolean applyAllDeletes) {
		if (applyAllDeletes) {
			return withDeletes.manager;
		} else {
			if (withDeletes.generation > withoutDeletes.generation) {
				return withDeletes.manager;
			} else {
				return withoutDeletes.manager;
			}
		}
	}

	static final class SearcherManagerRef implements Closeable {
		final boolean applyDeletes;
		volatile long generation;
		final SearcherManager manager;

		SearcherManagerRef(boolean applyDeletes, long generation,
				SearcherManager manager) {
			super();
			this.applyDeletes = applyDeletes;
			this.generation = generation;
			this.manager = manager;
		}

		public void close() throws IOException {
			generation = MAX_SEARCHER_GEN;
			manager.close();
		}
	}
}
