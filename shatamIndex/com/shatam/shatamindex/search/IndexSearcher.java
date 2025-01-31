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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.NIOFSDirectory;
import com.shatam.shatamindex.util.ReaderUtil;
import com.shatam.shatamindex.util.ThreadInterruptedException;

public class IndexSearcher extends Searcher {
	IndexReader reader;
	private boolean closeReader;

	protected final IndexReader[] subReaders;
	protected final int[] docStarts;

	private final ExecutorService executor;
	protected final IndexSearcher[] subSearchers;

	private final int docBase;

	@Deprecated
	public IndexSearcher(Directory path) throws CorruptIndexException,
			IOException {
		this(IndexReader.open(path, true), true, null);
	}

	@Deprecated
	public IndexSearcher(Directory path, boolean readOnly)
			throws CorruptIndexException, IOException {
		this(IndexReader.open(path, readOnly), true, null);
	}

	public IndexSearcher(IndexReader r) {
		this(r, false, null);
	}

	public IndexSearcher(IndexReader r, ExecutorService executor) {
		this(r, false, executor);
	}

	public IndexSearcher(IndexReader reader, IndexReader[] subReaders,
			int[] docStarts) {
		this(reader, subReaders, docStarts, null);
	}

	private IndexSearcher(IndexReader r, int docBase) {
		reader = r;
		this.executor = null;
		closeReader = false;
		this.docBase = docBase;
		subReaders = new IndexReader[] { r };
		docStarts = new int[] { 0 };
		subSearchers = null;
	}

	public IndexSearcher(IndexReader reader, IndexReader[] subReaders,
			int[] docStarts, ExecutorService executor) {
		this.reader = reader;
		this.subReaders = subReaders;
		this.docStarts = docStarts;
		if (executor == null) {
			subSearchers = null;
		} else {
			subSearchers = new IndexSearcher[subReaders.length];
			for (int i = 0; i < subReaders.length; i++) {
				subSearchers[i] = new IndexSearcher(subReaders[i], docStarts[i]);
			}
		}
		closeReader = false;
		this.executor = executor;
		docBase = 0;
	}

	private IndexSearcher(IndexReader r, boolean closeReader,
			ExecutorService executor) {
		reader = r;
		this.executor = executor;
		this.closeReader = closeReader;

		List<IndexReader> subReadersList = new ArrayList<IndexReader>();
		gatherSubReaders(subReadersList, reader);
		subReaders = subReadersList.toArray(new IndexReader[subReadersList
				.size()]);
		docStarts = new int[subReaders.length];
		int maxDoc = 0;
		for (int i = 0; i < subReaders.length; i++) {
			docStarts[i] = maxDoc;
			maxDoc += subReaders[i].maxDoc();
		}
		if (executor == null) {
			subSearchers = null;
		} else {
			subSearchers = new IndexSearcher[subReaders.length];
			for (int i = 0; i < subReaders.length; i++) {
				subSearchers[i] = new IndexSearcher(subReaders[i], docStarts[i]);
			}
		}
		docBase = 0;
	}

	protected void gatherSubReaders(List<IndexReader> allSubReaders,
			IndexReader r) {
		ReaderUtil.gatherSubReaders(allSubReaders, r);
	}

	public IndexReader getIndexReader() {
		return reader;
	}

	public IndexReader[] getSubReaders() {
		return subReaders;
	}

	@Override
	public int maxDoc() {
		return reader.maxDoc();
	}

	@Override
	public int docFreq(final Term term) throws IOException {
		if (executor == null) {
			return reader.docFreq(term);
		} else {
			final ExecutionHelper<Integer> runner = new ExecutionHelper<Integer>(
					executor);
			for (int i = 0; i < subReaders.length; i++) {
				final IndexSearcher searchable = subSearchers[i];
				runner.submit(new Callable<Integer>() {
					public Integer call() throws IOException {
						return Integer.valueOf(searchable.docFreq(term));
					}
				});
			}
			int docFreq = 0;
			for (Integer num : runner) {
				docFreq += num.intValue();
			}
			return docFreq;
		}
	}

	@Override
	public Document doc(int docID) throws CorruptIndexException, IOException {
		return reader.document(docID);
	}

	@Override
	public Document doc(int docID, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		return reader.document(docID, fieldSelector);
	}

	@Override
	public void setSimilarity(Similarity similarity) {
		super.setSimilarity(similarity);
	}

	@Override
	public Similarity getSimilarity() {
		return super.getSimilarity();
	}

	@Override
	public void close() throws IOException {
		if (closeReader) {
			reader.close();
		}
	}

	public TopDocs searchAfter(ScoreDoc after, Query query, int n)
			throws IOException {
		return searchAfter(after, query, null, n);
	}

	public TopDocs searchAfter(ScoreDoc after, Query query, Filter filter, int n)
			throws IOException {
		return search(createNormalizedWeight(query), filter, after, n);
	}

	@Override
	public TopDocs search(Query query, int n) throws IOException {
		return search(query, null, n);
	}

	@Override
	public TopDocs search(Query query, Filter filter, int n) throws IOException {
		return search(createNormalizedWeight(query), filter, n);
	}

	@Override
	public void search(Query query, Filter filter, Collector results)
			throws IOException {
		search(createNormalizedWeight(query), filter, results);
	}

	@Override
	public void search(Query query, Collector results) throws IOException {
		search(createNormalizedWeight(query), null, results);
	}

	@Override
	public TopFieldDocs search(Query query, Filter filter, int n, Sort sort)
			throws IOException {
		return search(createNormalizedWeight(query), filter, n, sort);
	}

	@Override
	public TopFieldDocs search(Query query, int n, Sort sort)
			throws IOException {
		return search(createNormalizedWeight(query), null, n, sort);
	}

	@Override
	public TopDocs search(Weight weight, Filter filter, int nDocs)
			throws IOException {
		return search(weight, filter, null, nDocs);
	}

	protected TopDocs search(Weight weight, Filter filter, ScoreDoc after,
			int nDocs) throws IOException {
		if (executor == null) {

			int limit = reader.maxDoc();
			if (limit == 0) {
				limit = 1;
			}
			nDocs = Math.min(nDocs, limit);
			TopScoreDocCollector collector = TopScoreDocCollector.create(nDocs,
					after, !weight.scoresDocsOutOfOrder());
			search(weight, filter, collector);
			return collector.topDocs();
		} else {
			final HitQueue hq = new HitQueue(nDocs, false);
			final Lock lock = new ReentrantLock();
			final ExecutionHelper<TopDocs> runner = new ExecutionHelper<TopDocs>(
					executor);

			for (int i = 0; i < subReaders.length; i++) {
				runner.submit(new MultiSearcherCallableNoSort(lock,
						subSearchers[i], weight, filter, after, nDocs, hq));
			}

			int totalHits = 0;
			float maxScore = Float.NEGATIVE_INFINITY;
			for (final TopDocs topDocs : runner) {
				if (topDocs.totalHits != 0) {
					totalHits += topDocs.totalHits;
					maxScore = Math.max(maxScore, topDocs.getMaxScore());
				}
			}

			final ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
			for (int i = hq.size() - 1; i >= 0; i--)
				scoreDocs[i] = hq.pop();

			return new TopDocs(totalHits, scoreDocs, maxScore);
		}
	}

	@Override
	public TopFieldDocs search(Weight weight, Filter filter, final int nDocs,
			Sort sort) throws IOException {
		return search(weight, filter, nDocs, sort, true);
	}

	protected TopFieldDocs search(Weight weight, Filter filter, int nDocs,
			Sort sort, boolean fillFields) throws IOException {

		if (sort == null)
			throw new NullPointerException();

		if (executor == null) {

			int limit = reader.maxDoc();
			if (limit == 0) {
				limit = 1;
			}
			nDocs = Math.min(nDocs, limit);

			TopFieldCollector collector = TopFieldCollector.create(sort, nDocs,
					fillFields, fieldSortDoTrackScores, fieldSortDoMaxScore,
					!weight.scoresDocsOutOfOrder());
			search(weight, filter, collector);
			return (TopFieldDocs) collector.topDocs();
		} else {
			final TopFieldCollector topCollector = TopFieldCollector.create(
					sort, nDocs, fillFields, fieldSortDoTrackScores,
					fieldSortDoMaxScore, false);

			final Lock lock = new ReentrantLock();
			final ExecutionHelper<TopFieldDocs> runner = new ExecutionHelper<TopFieldDocs>(
					executor);
			for (int i = 0; i < subReaders.length; i++) {
				runner.submit(new MultiSearcherCallableWithSort(lock,
						subSearchers[i], weight, filter, nDocs, topCollector,
						sort));
			}
			int totalHits = 0;
			float maxScore = Float.NEGATIVE_INFINITY;
			for (final TopFieldDocs topFieldDocs : runner) {
				if (topFieldDocs.totalHits != 0) {
					totalHits += topFieldDocs.totalHits;
					maxScore = Math.max(maxScore, topFieldDocs.getMaxScore());
				}
			}

			final TopFieldDocs topDocs = (TopFieldDocs) topCollector.topDocs();

			return new TopFieldDocs(totalHits, topDocs.scoreDocs,
					topDocs.fields, topDocs.getMaxScore());
		}
	}

	@Override
	public void search(Weight weight, Filter filter, Collector collector)
			throws IOException {

		for (int i = 0; i < subReaders.length; i++) {
			collector.setNextReader(subReaders[i], docBase + docStarts[i]);
			final Scorer scorer = (filter == null) ? weight.scorer(
					subReaders[i], !collector.acceptsDocsOutOfOrder(), true)
					: FilteredQuery.getFilteredScorer(subReaders[i],
							getSimilarity(), weight, weight, filter);
			if (scorer != null) {
				scorer.score(collector);
			}
		}
	}

	@Override
	public Query rewrite(Query original) throws IOException {
		Query query = original;
		for (Query rewrittenQuery = query.rewrite(reader); rewrittenQuery != query; rewrittenQuery = query
				.rewrite(reader)) {
			query = rewrittenQuery;
		}
		return query;
	}

	@Override
	public Explanation explain(Query query, int doc) throws IOException {
		return explain(createNormalizedWeight(query), doc);
	}

	@Override
	public Explanation explain(Weight weight, int doc) throws IOException {
		int n = ReaderUtil.subIndex(doc, docStarts);
		int deBasedDoc = doc - docStarts[n];

		return weight.explain(subReaders[n], deBasedDoc);
	}

	private boolean fieldSortDoTrackScores;
	private boolean fieldSortDoMaxScore;

	public void setDefaultFieldSortScoring(boolean doTrackScores,
			boolean doMaxScore) {
		fieldSortDoTrackScores = doTrackScores;
		fieldSortDoMaxScore = doMaxScore;
		if (subSearchers != null) {
			for (IndexSearcher sub : subSearchers) {
				sub.setDefaultFieldSortScoring(doTrackScores, doMaxScore);
			}
		}
	}

	public Weight createNormalizedWeight(Query query) throws IOException {
		return super.createNormalizedWeight(query);
	}

	private static final class MultiSearcherCallableNoSort implements
			Callable<TopDocs> {

		private final Lock lock;
		private final IndexSearcher searchable;
		private final Weight weight;
		private final Filter filter;
		private final ScoreDoc after;
		private final int nDocs;
		private final HitQueue hq;

		public MultiSearcherCallableNoSort(Lock lock, IndexSearcher searchable,
				Weight weight, Filter filter, ScoreDoc after, int nDocs,
				HitQueue hq) {
			this.lock = lock;
			this.searchable = searchable;
			this.weight = weight;
			this.filter = filter;
			this.after = after;
			this.nDocs = nDocs;
			this.hq = hq;
		}

		public TopDocs call() throws IOException {
			final TopDocs docs;

			if (after == null) {
				docs = searchable.search(weight, filter, nDocs);
			} else {
				docs = searchable.search(weight, filter, after, nDocs);
			}
			final ScoreDoc[] scoreDocs = docs.scoreDocs;

			lock.lock();
			try {
				for (int j = 0; j < scoreDocs.length; j++) {
					final ScoreDoc scoreDoc = scoreDocs[j];
					if (scoreDoc == hq.insertWithOverflow(scoreDoc)) {
						break;
					}
				}
			} finally {
				lock.unlock();
			}
			return docs;
		}
	}

	private static final class MultiSearcherCallableWithSort implements
			Callable<TopFieldDocs> {

		private final Lock lock;
		private final IndexSearcher searchable;
		private final Weight weight;
		private final Filter filter;
		private final int nDocs;
		private final TopFieldCollector hq;
		private final Sort sort;

		public MultiSearcherCallableWithSort(Lock lock,
				IndexSearcher searchable, Weight weight, Filter filter,
				int nDocs, TopFieldCollector hq, Sort sort) {
			this.lock = lock;
			this.searchable = searchable;
			this.weight = weight;
			this.filter = filter;
			this.nDocs = nDocs;
			this.hq = hq;
			this.sort = sort;
		}

		private final class FakeScorer extends Scorer {
			float score;
			int doc;

			public FakeScorer() {
				super(null, null);
			}

			@Override
			public int advance(int target) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int docID() {
				return doc;
			}

			@Override
			public float freq() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int nextDoc() {
				throw new UnsupportedOperationException();
			}

			@Override
			public float score() {
				return score;
			}
		}

		private final FakeScorer fakeScorer = new FakeScorer();

		public TopFieldDocs call() throws IOException {
			final TopFieldDocs docs = searchable.search(weight, filter, nDocs,
					sort);

			for (int j = 0; j < docs.fields.length; j++) {
				if (docs.fields[j].getType() == SortField.DOC) {

					for (int j2 = 0; j2 < docs.scoreDocs.length; j2++) {
						FieldDoc fd = (FieldDoc) docs.scoreDocs[j2];
						fd.fields[j] = Integer.valueOf(((Integer) fd.fields[j])
								.intValue());
					}
					break;
				}
			}

			lock.lock();
			try {
				hq.setNextReader(searchable.getIndexReader(),
						searchable.docBase);
				hq.setScorer(fakeScorer);
				for (ScoreDoc scoreDoc : docs.scoreDocs) {
					final int docID = scoreDoc.doc - searchable.docBase;
					fakeScorer.doc = docID;
					fakeScorer.score = scoreDoc.score;
					hq.collect(docID);
				}
			} finally {
				lock.unlock();
			}

			return docs;
		}
	}

	private static final class ExecutionHelper<T> implements Iterator<T>,
			Iterable<T> {
		private final CompletionService<T> service;
		private int numTasks;

		ExecutionHelper(final Executor executor) {
			this.service = new ExecutorCompletionService<T>(executor);
		}

		public boolean hasNext() {
			return numTasks > 0;
		}

		public void submit(Callable<T> task) {
			this.service.submit(task);
			++numTasks;
		}

		public T next() {
			if (!this.hasNext())
				throw new NoSuchElementException();
			try {
				return service.take().get();
			} catch (InterruptedException e) {
				throw new ThreadInterruptedException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			} finally {
				--numTasks;
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public Iterator<T> iterator() {

			return this;
		}
	}

	@Override
	public String toString() {
		return "IndexSearcher(" + reader + ")";
	}
}
