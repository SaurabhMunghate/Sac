/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.PriorityQueue;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;

public abstract class TopTermsRewrite<Q extends Query> extends
		TermCollectingRewrite<Q> {

	private final int size;

	public TopTermsRewrite(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	protected abstract int getMaxSize();

	@Override
	public Q rewrite(final IndexReader reader, final MultiTermQuery query)
			throws IOException {
		final int maxSize = Math.min(size, getMaxSize());
		final PriorityQueue<ScoreTerm> stQueue = new PriorityQueue<ScoreTerm>();
		collectTerms(reader, query, new TermCollector() {
			public boolean collect(Term t, float boost) {

				if (stQueue.size() >= maxSize && boost <= stQueue.peek().boost)
					return true;

				st.term = t;
				st.boost = boost;
				stQueue.offer(st);

				st = (stQueue.size() > maxSize) ? stQueue.poll()
						: new ScoreTerm();
				return true;
			}

			private ScoreTerm st = new ScoreTerm();
		});

		final Q q = getTopLevelQuery();
		for (final ScoreTerm st : stQueue) {
			addClause(q, st.term, query.getBoost() * st.boost);
		}
		query.incTotalNumberOfTerms(stQueue.size());

		return q;
	}

	@Override
	public int hashCode() {
		return 31 * size;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TopTermsRewrite other = (TopTermsRewrite) obj;
		if (size != other.size)
			return false;
		return true;
	}

	private static class ScoreTerm implements Comparable<ScoreTerm> {
		public Term term;
		public float boost;

		public int compareTo(ScoreTerm other) {
			if (this.boost == other.boost)
				return other.term.compareTo(this.term);
			else
				return Float.compare(this.boost, other.boost);
		}
	}

}
