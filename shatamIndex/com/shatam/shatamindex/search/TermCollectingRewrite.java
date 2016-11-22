/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;

abstract class TermCollectingRewrite<Q extends Query> extends
		MultiTermQuery.RewriteMethod {

	protected abstract Q getTopLevelQuery() throws IOException;

	protected abstract void addClause(Q topLevel, Term term, float boost)
			throws IOException;

	protected final void collectTerms(IndexReader reader, MultiTermQuery query,
			TermCollector collector) throws IOException {
		final FilteredTermEnum enumerator = query.getEnum(reader);
		try {
			do {
				final Term t = enumerator.term();
				if (t == null || !collector.collect(t, enumerator.difference()))
					break;
			} while (enumerator.next());
		} finally {
			enumerator.close();
		}
	}

	protected interface TermCollector {

		boolean collect(Term t, float boost) throws IOException;
	}
}
