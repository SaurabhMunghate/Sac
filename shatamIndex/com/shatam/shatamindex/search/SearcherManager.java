/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.IndexWriter;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.NRTManager;
import com.shatam.shatamindex.store.AlreadyClosedException;
import com.shatam.shatamindex.store.Directory;

public final class SearcherManager {

	private volatile IndexSearcher currentSearcher;
	private final ExecutorService es;
	private final SearcherWarmer warmer;
	private final Semaphore reopenLock = new Semaphore(1);

	public SearcherManager(IndexWriter writer, boolean applyAllDeletes,
			final SearcherWarmer warmer, final ExecutorService es)
			throws IOException {
		this.es = es;
		this.warmer = warmer;
		currentSearcher = new IndexSearcher(IndexReader.open(writer,
				applyAllDeletes));
		if (warmer != null) {
			writer.getConfig().setMergedSegmentWarmer(
					new IndexWriter.IndexReaderWarmer() {
						@Override
						public void warm(IndexReader reader) throws IOException {
							warmer.warm(new IndexSearcher(reader, es));
						}
					});
		}
	}

	public SearcherManager(Directory dir, SearcherWarmer warmer,
			ExecutorService es) throws IOException {
		this.es = es;
		this.warmer = warmer;
		currentSearcher = new IndexSearcher(IndexReader.open(dir, true), es);
	}

	public boolean maybeReopen() throws IOException {
		ensureOpen();

		if (reopenLock.tryAcquire()) {
			try {

				final IndexReader newReader = IndexReader
						.openIfChanged(currentSearcher.getIndexReader());
				if (newReader != null) {
					final IndexSearcher newSearcher = new IndexSearcher(
							newReader, es);
					boolean success = false;
					try {
						if (warmer != null) {
							warmer.warm(newSearcher);
						}
						swapSearcher(newSearcher);
						success = true;
					} finally {
						if (!success) {
							release(newSearcher);
						}
					}
				}
				return true;
			} finally {
				reopenLock.release();
			}
		} else {
			return false;
		}
	}

	public boolean isSearcherCurrent() throws CorruptIndexException,
			IOException {
		final IndexSearcher searcher = acquire();
		try {
			return searcher.getIndexReader().isCurrent();
		} finally {
			release(searcher);
		}
	}

	public void release(IndexSearcher searcher) throws IOException {
		assert searcher != null;
		searcher.getIndexReader().decRef();
	}

	public synchronized void close() throws IOException {
		if (currentSearcher != null) {

			swapSearcher(null);
		}
	}

	public IndexSearcher acquire() {
		IndexSearcher searcher;
		do {
			if ((searcher = currentSearcher) == null) {
				throw new AlreadyClosedException(
						"this SearcherManager is closed");
			}
		} while (!searcher.getIndexReader().tryIncRef());
		return searcher;
	}

	private void ensureOpen() {
		if (currentSearcher == null) {
			throw new AlreadyClosedException("this SearcherManager is closed");
		}
	}

	private synchronized void swapSearcher(IndexSearcher newSearcher)
			throws IOException {
		ensureOpen();
		final IndexSearcher oldSearcher = currentSearcher;
		currentSearcher = newSearcher;
		release(oldSearcher);
	}

}
