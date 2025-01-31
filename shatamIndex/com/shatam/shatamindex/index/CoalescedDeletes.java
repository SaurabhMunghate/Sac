
/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shatam.shatamindex.index.BufferedDeletesStream.QueryAndLimit;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.PriorityQueue;

class CoalescedDeletes {
	final Map<Query, Integer> queries = new HashMap<Query, Integer>();
	final List<Iterable<Term>> iterables = new ArrayList<Iterable<Term>>();

	@Override
	public String toString() {

		return "CoalescedDeletes(termSets=" + iterables.size() + ",queries="
				+ queries.size() + ")";
	}

	void update(FrozenBufferedDeletes in) {
		iterables.add(in.termsIterable());

		for (int queryIdx = 0; queryIdx < in.queries.length; queryIdx++) {
			final Query query = in.queries[queryIdx];
			queries.put(query, BufferedDeletes.MAX_INT);
		}
	}

	public Iterable<Term> termsIterable() {
		return new Iterable<Term>() {
			public Iterator<Term> iterator() {
				ArrayList<Iterator<Term>> subs = new ArrayList<Iterator<Term>>(
						iterables.size());
				for (Iterable<Term> iterable : iterables) {
					subs.add(iterable.iterator());
				}
				return mergedIterator(subs);
			}
		};
	}

	public Iterable<QueryAndLimit> queriesIterable() {
		return new Iterable<QueryAndLimit>() {

			public Iterator<QueryAndLimit> iterator() {
				return new Iterator<QueryAndLimit>() {
					private final Iterator<Map.Entry<Query, Integer>> iter = queries
							.entrySet().iterator();

					public boolean hasNext() {
						return iter.hasNext();
					}

					public QueryAndLimit next() {
						final Map.Entry<Query, Integer> ent = iter.next();
						return new QueryAndLimit(ent.getKey(), ent.getValue());
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	static Iterator<Term> mergedIterator(final List<Iterator<Term>> iterators) {
		return new Iterator<Term>() {
			Term current;
			TermMergeQueue queue = new TermMergeQueue(iterators.size());
			SubIterator[] top = new SubIterator[iterators.size()];
			int numTop;

			{
				int index = 0;
				for (Iterator<Term> iterator : iterators) {
					if (iterator.hasNext()) {
						SubIterator sub = new SubIterator();
						sub.current = iterator.next();
						sub.iterator = iterator;
						sub.index = index++;
						queue.add(sub);
					}
				}
			}

			public boolean hasNext() {
				if (queue.size() > 0) {
					return true;
				}

				for (int i = 0; i < numTop; i++) {
					if (top[i].iterator.hasNext()) {
						return true;
					}
				}
				return false;
			}

			public Term next() {

				pushTop();

				if (queue.size() > 0) {
					pullTop();
				} else {
					current = null;
				}
				return current;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			private void pullTop() {

				assert numTop == 0;
				while (true) {
					top[numTop++] = queue.pop();
					if (queue.size() == 0
							|| !(queue.top()).current.equals(top[0].current)) {
						break;
					}
				}
				current = top[0].current;
			}

			private void pushTop() {

				for (int i = 0; i < numTop; i++) {
					if (top[i].iterator.hasNext()) {
						top[i].current = top[i].iterator.next();
						queue.add(top[i]);
					} else {

						top[i].current = null;
					}
				}
				numTop = 0;
			}
		};
	}

	private static class SubIterator {
		Iterator<Term> iterator;
		Term current;
		int index;
	}

	private static class TermMergeQueue extends PriorityQueue<SubIterator> {
		TermMergeQueue(int size) {
			initialize(size);
		}

		@Override
		protected boolean lessThan(SubIterator a, SubIterator b) {
			final int cmp = a.current.compareTo(b.current);
			if (cmp != 0) {
				return cmp < 0;
			} else {
				return a.index < b.index;
			}
		}
	}
}
