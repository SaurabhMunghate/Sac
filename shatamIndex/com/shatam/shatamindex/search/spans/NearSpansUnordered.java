/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.PriorityQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class NearSpansUnordered extends Spans {
	private SpanNearQuery query;

	private List<SpansCell> ordered = new ArrayList<SpansCell>();
	private Spans[] subSpans;
	private int slop;

	private SpansCell first;
	private SpansCell last;

	private int totalLength;

	private CellQueue queue;
	private SpansCell max;

	private boolean more = true;
	private boolean firstTime = true;

	private class CellQueue extends PriorityQueue<SpansCell> {
		public CellQueue(int size) {
			initialize(size);
		}

		@Override
		protected final boolean lessThan(SpansCell spans1, SpansCell spans2) {
			if (spans1.doc() == spans2.doc()) {
				return NearSpansOrdered.docSpansOrdered(spans1, spans2);
			} else {
				return spans1.doc() < spans2.doc();
			}
		}
	}

	private class SpansCell extends Spans {
		private Spans spans;
		private SpansCell next;
		private int length = -1;
		private int index;

		public SpansCell(Spans spans, int index) {
			this.spans = spans;
			this.index = index;
		}

		@Override
		public boolean next() throws IOException {
			return adjust(spans.next());
		}

		@Override
		public boolean skipTo(int target) throws IOException {
			return adjust(spans.skipTo(target));
		}

		private boolean adjust(boolean condition) {
			if (length != -1) {
				totalLength -= length;
			}
			if (condition) {
				length = end() - start();
				totalLength += length;

				if (max == null || doc() > max.doc() || (doc() == max.doc())
						&& (end() > max.end())) {
					max = this;
				}
			}
			more = condition;
			return condition;
		}

		@Override
		public int doc() {
			return spans.doc();
		}

		@Override
		public int start() {
			return spans.start();
		}

		@Override
		public int end() {
			return spans.end();
		}

		@Override
		public Collection<byte[]> getPayload() throws IOException {
			return new ArrayList<byte[]>(spans.getPayload());
		}

		@Override
		public boolean isPayloadAvailable() {
			return spans.isPayloadAvailable();
		}

		@Override
		public String toString() {
			return spans.toString() + "#" + index;
		}
	}

	public NearSpansUnordered(SpanNearQuery query, IndexReader reader)
			throws IOException {
		this.query = query;
		this.slop = query.getSlop();

		SpanQuery[] clauses = query.getClauses();
		queue = new CellQueue(clauses.length);
		subSpans = new Spans[clauses.length];
		for (int i = 0; i < clauses.length; i++) {
			SpansCell cell = new SpansCell(clauses[i].getSpans(reader), i);
			ordered.add(cell);
			subSpans[i] = cell.spans;
		}
	}

	public Spans[] getSubSpans() {
		return subSpans;
	}

	@Override
	public boolean next() throws IOException {
		if (firstTime) {
			initList(true);
			listToQueue();
			firstTime = false;
		} else if (more) {
			if (min().next()) {
				queue.updateTop();
			} else {
				more = false;
			}
		}

		while (more) {

			boolean queueStale = false;

			if (min().doc() != max.doc()) {
				queueToList();
				queueStale = true;
			}

			while (more && first.doc() < last.doc()) {
				more = first.skipTo(last.doc());
				firstToLast();
				queueStale = true;
			}

			if (!more)
				return false;

			if (queueStale) {
				listToQueue();
				queueStale = false;
			}

			if (atMatch()) {
				return true;
			}

			more = min().next();
			if (more) {
				queue.updateTop();
			}
		}
		return false;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (firstTime) {
			initList(false);
			for (SpansCell cell = first; more && cell != null; cell = cell.next) {
				more = cell.skipTo(target);
			}
			if (more) {
				listToQueue();
			}
			firstTime = false;
		} else {
			while (more && min().doc() < target) {
				if (min().skipTo(target)) {
					queue.updateTop();
				} else {
					more = false;
				}
			}
		}
		return more && (atMatch() || next());
	}

	private SpansCell min() {
		return queue.top();
	}

	@Override
	public int doc() {
		return min().doc();
	}

	@Override
	public int start() {
		return min().start();
	}

	@Override
	public int end() {
		return max.end();
	}

	@Override
	public Collection<byte[]> getPayload() throws IOException {
		Set<byte[]> matchPayload = new HashSet<byte[]>();
		for (SpansCell cell = first; cell != null; cell = cell.next) {
			if (cell.isPayloadAvailable()) {
				matchPayload.addAll(cell.getPayload());
			}
		}
		return matchPayload;
	}

	@Override
	public boolean isPayloadAvailable() {
		SpansCell pointer = min();
		while (pointer != null) {
			if (pointer.isPayloadAvailable()) {
				return true;
			}
			pointer = pointer.next;
		}

		return false;
	}

	@Override
	public String toString() {
		return getClass().getName()
				+ "("
				+ query.toString()
				+ ")@"
				+ (firstTime ? "START"
						: (more ? (doc() + ":" + start() + "-" + end()) : "END"));
	}

	private void initList(boolean next) throws IOException {
		for (int i = 0; more && i < ordered.size(); i++) {
			SpansCell cell = ordered.get(i);
			if (next)
				more = cell.next();
			if (more) {
				addToList(cell);
			}
		}
	}

	private void addToList(SpansCell cell) throws IOException {
		if (last != null) {
			last.next = cell;
		} else
			first = cell;
		last = cell;
		cell.next = null;
	}

	private void firstToLast() {
		last.next = first;
		last = first;
		first = first.next;
		last.next = null;
	}

	private void queueToList() throws IOException {
		last = first = null;
		while (queue.top() != null) {
			addToList(queue.pop());
		}
	}

	private void listToQueue() {
		queue.clear();
		for (SpansCell cell = first; cell != null; cell = cell.next) {
			queue.add(cell);
		}
	}

	private boolean atMatch() {
		return (min().doc() == max.doc())
				&& ((max.end() - min().start() - totalLength) <= slop);
	}
}
