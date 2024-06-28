/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.util.DummyConcurrentLock;
import com.shatam.shatamindex.util.ReaderUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

@Deprecated
public class MultiSearcher extends Searcher {

	private static class CachedDfSource extends Searcher {
		private final Map<Term, Integer> dfMap;
		private final int maxDoc;

		public CachedDfSource(Map<Term, Integer> dfMap, int maxDoc,
				Similarity similarity) {
			this.dfMap = dfMap;
			this.maxDoc = maxDoc;
			setSimilarity(similarity);
		}

		@Override
		public int docFreq(Term term) {
			int df;
			try {
				df = dfMap.get(term).intValue();
			} catch (NullPointerException e) {
				throw new IllegalArgumentException("df for term " + term.text()
						+ " not available");
			}
			return df;
		}

		@Override
		public int[] docFreqs(Term[] terms) {
			final int[] result = new int[terms.length];
			for (int i = 0; i < terms.length; i++) {
				result[i] = docFreq(terms[i]);
			}
			return result;
		}

		@Override
		public int maxDoc() {
			return maxDoc;
		}

		@Override
		public Query rewrite(Query query) {

			return query;
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Document doc(int i) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Document doc(int i, FieldSelector fieldSelector) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Explanation explain(Weight weight, int doc) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void search(Weight weight, Filter filter, Collector results) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TopDocs search(Weight weight, Filter filter, int n) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TopFieldDocs search(Weight weight, Filter filter, int n,
				Sort sort) {
			throw new UnsupportedOperationException();
		}
	}

	private Searchable[] searchables;
	private int[] starts;
	private int maxDoc = 0;

	public MultiSearcher(Searchable... searchables) throws IOException {
		this.searchables = searchables;

		starts = new int[searchables.length + 1];
		for (int i = 0; i < searchables.length; i++) {
			starts[i] = maxDoc;
			maxDoc += searchables[i].maxDoc();
		}
		starts[searchables.length] = maxDoc;
	}

	public Searchable[] getSearchables() {
		return searchables;
	}

	protected int[] getStarts() {
		return starts;
	}

	@Override
	public void close() throws IOException {
		for (int i = 0; i < searchables.length; i++)
			searchables[i].close();
	}

	@Override
	public int docFreq(Term term) throws IOException {
		int docFreq = 0;
		for (int i = 0; i < searchables.length; i++)
			docFreq += searchables[i].docFreq(term);
		return docFreq;
	}

	@Override
	public Document doc(int n) throws CorruptIndexException, IOException {
		int i = subSearcher(n);
		return searchables[i].doc(n - starts[i]);
	}

	@Override
	public Document doc(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		int i = subSearcher(n);
		return searchables[i].doc(n - starts[i], fieldSelector);
	}

	public int subSearcher(int n) {
		return ReaderUtil.subIndex(n, starts);
	}

	public int subDoc(int n) {
		return n - starts[subSearcher(n)];
	}

	@Override
	public int maxDoc() throws IOException {
		return maxDoc;
	}

	@Override
	public TopDocs search(Weight weight, Filter filter, int nDocs)
			throws IOException {

		nDocs = Math.min(nDocs, maxDoc());
		final HitQueue hq = new HitQueue(nDocs, false);
		int totalHits = 0;

		for (int i = 0; i < searchables.length; i++) {
			final TopDocs docs = new MultiSearcherCallableNoSort(
					DummyConcurrentLock.INSTANCE, searchables[i], weight,
					filter, nDocs, hq, i, starts).call();
			totalHits += docs.totalHits;
		}

		final ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
		for (int i = hq.size() - 1; i >= 0; i--)
			scoreDocs[i] = hq.pop();

		float maxScore = (totalHits == 0) ? Float.NEGATIVE_INFINITY
				: scoreDocs[0].score;

		return new TopDocs(totalHits, scoreDocs, maxScore);
	}

	@Override
	public TopFieldDocs search(Weight weight, Filter filter, int n, Sort sort)
			throws IOException {
		n = Math.min(n, maxDoc());
		FieldDocSortedHitQueue hq = new FieldDocSortedHitQueue(n);
		int totalHits = 0;

		float maxScore = Float.NEGATIVE_INFINITY;

		for (int i = 0; i < searchables.length; i++) {
			final TopFieldDocs docs = new MultiSearcherCallableWithSort(
					DummyConcurrentLock.INSTANCE, searchables[i], weight,
					filter, n, hq, sort, i, starts).call();
			totalHits += docs.totalHits;
			maxScore = Math.max(maxScore, docs.getMaxScore());
		}

		final ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
		for (int i = hq.size() - 1; i >= 0; i--)
			scoreDocs[i] = hq.pop();

		return new TopFieldDocs(totalHits, scoreDocs, hq.getFields(), maxScore);
	}

	@Override
	public void search(Weight weight, Filter filter, final Collector collector)
			throws IOException {
		for (int i = 0; i < searchables.length; i++) {

			final int start = starts[i];

			final Collector hc = new Collector() {
				@Override
				public void setScorer(Scorer scorer) throws IOException {
					collector.setScorer(scorer);
				}

				@Override
				public void collect(int doc) throws IOException {
					collector.collect(doc);
				}

				@Override
				public void setNextReader(IndexReader reader, int docBase)
						throws IOException {
					collector.setNextReader(reader, start + docBase);
				}

				@Override
				public boolean acceptsDocsOutOfOrder() {
					return collector.acceptsDocsOutOfOrder();
				}
			};

			searchables[i].search(weight, filter, hc);
		}
	}

	@Override
	public Query rewrite(Query original) throws IOException {
		final Query[] queries = new Query[searchables.length];
		for (int i = 0; i < searchables.length; i++) {
			queries[i] = searchables[i].rewrite(original);
		}
		return queries[0].combine(queries);
	}

	@Override
	public Explanation explain(Weight weight, int doc) throws IOException {
		final int i = subSearcher(doc);
		return searchables[i].explain(weight, doc - starts[i]);
	}

	@Override
	public Weight createNormalizedWeight(Query original) throws IOException {

		final Query rewrittenQuery = rewrite(original);

		final Set<Term> terms = new HashSet<Term>();
		rewrittenQuery.extractTerms(terms);

		final Map<Term, Integer> dfMap = createDocFrequencyMap(terms);

		final int numDocs = maxDoc();
		final CachedDfSource cacheSim = new CachedDfSource(dfMap, numDocs,
				getSimilarity());

		return cacheSim.createNormalizedWeight(rewrittenQuery);
	}

	Map<Term, Integer> createDocFrequencyMap(final Set<Term> terms)
			throws IOException {
		final Term[] allTermsArray = terms.toArray(new Term[terms.size()]);
		final int[] aggregatedDfs = new int[allTermsArray.length];
		for (Searchable searchable : searchables) {
			final int[] dfs = searchable.docFreqs(allTermsArray);
			for (int j = 0; j < aggregatedDfs.length; j++) {
				aggregatedDfs[j] += dfs[j];
			}
		}
		final HashMap<Term, Integer> dfMap = new HashMap<Term, Integer>();
		for (int i = 0; i < allTermsArray.length; i++) {
			dfMap.put(allTermsArray[i], Integer.valueOf(aggregatedDfs[i]));
		}
		return dfMap;
	}

	static final class MultiSearcherCallableNoSort implements Callable<TopDocs> {

		private final Lock lock;
		private final Searchable searchable;
		private final Weight weight;
		private final Filter filter;
		private final int nDocs;
		private final int i;
		private final HitQueue hq;
		private final int[] starts;

		public MultiSearcherCallableNoSort(Lock lock, Searchable searchable,
				Weight weight, Filter filter, int nDocs, HitQueue hq, int i,
				int[] starts) {
			this.lock = lock;
			this.searchable = searchable;
			this.weight = weight;
			this.filter = filter;
			this.nDocs = nDocs;
			this.hq = hq;
			this.i = i;
			this.starts = starts;
		}

		public TopDocs call() throws IOException {
			final TopDocs docs = searchable.search(weight, filter, nDocs);
			final ScoreDoc[] scoreDocs = docs.scoreDocs;
			for (int j = 0; j < scoreDocs.length; j++) {
				final ScoreDoc scoreDoc = scoreDocs[j];
				scoreDoc.doc += starts[i];

				lock.lock();
				try {
					if (scoreDoc == hq.insertWithOverflow(scoreDoc))
						break;
				} finally {
					lock.unlock();
				}
			}
			return docs;
		}
	}

	static final class MultiSearcherCallableWithSort implements
			Callable<TopFieldDocs> {

		private final Lock lock;
		private final Searchable searchable;
		private final Weight weight;
		private final Filter filter;
		private final int nDocs;
		private final int i;
		private final FieldDocSortedHitQueue hq;
		private final int[] starts;
		private final Sort sort;

		public MultiSearcherCallableWithSort(Lock lock, Searchable searchable,
				Weight weight, Filter filter, int nDocs,
				FieldDocSortedHitQueue hq, Sort sort, int i, int[] starts) {
			this.lock = lock;
			this.searchable = searchable;
			this.weight = weight;
			this.filter = filter;
			this.nDocs = nDocs;
			this.hq = hq;
			this.i = i;
			this.starts = starts;
			this.sort = sort;
		}

		public TopFieldDocs call() throws IOException {
			final TopFieldDocs docs = searchable.search(weight, filter, nDocs,
					sort);

			for (int j = 0; j < docs.fields.length; j++) {
				if (docs.fields[j].getType() == SortField.DOC) {

					for (int j2 = 0; j2 < docs.scoreDocs.length; j2++) {
						FieldDoc fd = (FieldDoc) docs.scoreDocs[j2];
						fd.fields[j] = Integer.valueOf(((Integer) fd.fields[j])
								.intValue() + starts[i]);
					}
					break;
				}
			}

			lock.lock();
			try {
				hq.setFields(docs.fields);
			} finally {
				lock.unlock();
			}

			final ScoreDoc[] scoreDocs = docs.scoreDocs;
			for (int j = 0; j < scoreDocs.length; j++) {
				final FieldDoc fieldDoc = (FieldDoc) scoreDocs[j];
				fieldDoc.doc += starts[i];

				lock.lock();
				try {
					if (fieldDoc == hq.insertWithOverflow(fieldDoc))
						break;
				} finally {
					lock.unlock();
				}
			}
			return docs;
		}
	}

}
