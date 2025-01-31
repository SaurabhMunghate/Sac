/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.payloads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.BooleanClause;
import com.shatam.shatamindex.search.BooleanQuery;
import com.shatam.shatamindex.search.DisjunctionMaxQuery;
import com.shatam.shatamindex.search.FilteredQuery;
import com.shatam.shatamindex.search.MultiPhraseQuery;
import com.shatam.shatamindex.search.PhraseQuery;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.TermQuery;
import com.shatam.shatamindex.search.spans.SpanNearQuery;
import com.shatam.shatamindex.search.spans.SpanOrQuery;
import com.shatam.shatamindex.search.spans.SpanQuery;
import com.shatam.shatamindex.search.spans.SpanTermQuery;
import com.shatam.shatamindex.search.spans.Spans;

public class PayloadSpanUtil {
	private IndexReader reader;

	public PayloadSpanUtil(IndexReader reader) {
		this.reader = reader;
	}

	public Collection<byte[]> getPayloadsForQuery(Query query)
			throws IOException {
		Collection<byte[]> payloads = new ArrayList<byte[]>();
		queryToSpanQuery(query, payloads);
		return payloads;
	}

	private void queryToSpanQuery(Query query, Collection<byte[]> payloads)
			throws IOException {
		if (query instanceof BooleanQuery) {
			BooleanClause[] queryClauses = ((BooleanQuery) query).getClauses();

			for (int i = 0; i < queryClauses.length; i++) {
				if (!queryClauses[i].isProhibited()) {
					queryToSpanQuery(queryClauses[i].getQuery(), payloads);
				}
			}

		} else if (query instanceof PhraseQuery) {
			Term[] phraseQueryTerms = ((PhraseQuery) query).getTerms();
			SpanQuery[] clauses = new SpanQuery[phraseQueryTerms.length];
			for (int i = 0; i < phraseQueryTerms.length; i++) {
				clauses[i] = new SpanTermQuery(phraseQueryTerms[i]);
			}

			int slop = ((PhraseQuery) query).getSlop();
			boolean inorder = false;

			if (slop == 0) {
				inorder = true;
			}

			SpanNearQuery sp = new SpanNearQuery(clauses, slop, inorder);
			sp.setBoost(query.getBoost());
			getPayloads(payloads, sp);
		} else if (query instanceof TermQuery) {
			SpanTermQuery stq = new SpanTermQuery(((TermQuery) query).getTerm());
			stq.setBoost(query.getBoost());
			getPayloads(payloads, stq);
		} else if (query instanceof SpanQuery) {
			getPayloads(payloads, (SpanQuery) query);
		} else if (query instanceof FilteredQuery) {
			queryToSpanQuery(((FilteredQuery) query).getQuery(), payloads);
		} else if (query instanceof DisjunctionMaxQuery) {

			for (Iterator<Query> iterator = ((DisjunctionMaxQuery) query)
					.iterator(); iterator.hasNext();) {
				queryToSpanQuery(iterator.next(), payloads);
			}

		} else if (query instanceof MultiPhraseQuery) {
			final MultiPhraseQuery mpq = (MultiPhraseQuery) query;
			final List<Term[]> termArrays = mpq.getTermArrays();
			final int[] positions = mpq.getPositions();
			if (positions.length > 0) {

				int maxPosition = positions[positions.length - 1];
				for (int i = 0; i < positions.length - 1; ++i) {
					if (positions[i] > maxPosition) {
						maxPosition = positions[i];
					}
				}

				@SuppressWarnings("unchecked")
				final List<Query>[] disjunctLists = new List[maxPosition + 1];
				int distinctPositions = 0;

				for (int i = 0; i < termArrays.size(); ++i) {
					final Term[] termArray = termArrays.get(i);
					List<Query> disjuncts = disjunctLists[positions[i]];
					if (disjuncts == null) {
						disjuncts = (disjunctLists[positions[i]] = new ArrayList<Query>(
								termArray.length));
						++distinctPositions;
					}
					for (final Term term : termArray) {
						disjuncts.add(new SpanTermQuery(term));
					}
				}

				int positionGaps = 0;
				int position = 0;
				final SpanQuery[] clauses = new SpanQuery[distinctPositions];
				for (int i = 0; i < disjunctLists.length; ++i) {
					List<Query> disjuncts = disjunctLists[i];
					if (disjuncts != null) {
						clauses[position++] = new SpanOrQuery(
								disjuncts.toArray(new SpanQuery[disjuncts
										.size()]));
					} else {
						++positionGaps;
					}
				}

				final int slop = mpq.getSlop();
				final boolean inorder = (slop == 0);

				SpanNearQuery sp = new SpanNearQuery(clauses, slop
						+ positionGaps, inorder);
				sp.setBoost(query.getBoost());
				getPayloads(payloads, sp);
			}
		}
	}

	private void getPayloads(Collection<byte[]> payloads, SpanQuery query)
			throws IOException {
		Spans spans = query.getSpans(reader);

		while (spans.next() == true) {
			if (spans.isPayloadAvailable()) {
				Collection<byte[]> payload = spans.getPayload();
				for (byte[] bytes : payload) {
					payloads.add(bytes);
				}

			}
		}
	}
}
