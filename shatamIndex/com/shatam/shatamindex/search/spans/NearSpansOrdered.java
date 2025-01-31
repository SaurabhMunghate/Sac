/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.ArrayUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Collection;
import java.util.Set;

public class NearSpansOrdered extends Spans {
	private final int allowedSlop;
	private boolean firstTime = true;
	private boolean more = false;

	private final Spans[] subSpans;

	private boolean inSameDoc = false;

	private int matchDoc = -1;
	private int matchStart = -1;
	private int matchEnd = -1;
	private List<byte[]> matchPayload;

	private final Spans[] subSpansByDoc;
	private final Comparator<Spans> spanDocComparator = new Comparator<Spans>() {
		public int compare(Spans o1, Spans o2) {
			return o1.doc() - o2.doc();
		}
	};

	private SpanNearQuery query;
	private boolean collectPayloads = true;

	public NearSpansOrdered(SpanNearQuery spanNearQuery, IndexReader reader)
			throws IOException {
		this(spanNearQuery, reader, true);
	}

	public NearSpansOrdered(SpanNearQuery spanNearQuery, IndexReader reader,
			boolean collectPayloads) throws IOException {
		if (spanNearQuery.getClauses().length < 2) {
			throw new IllegalArgumentException("Less than 2 clauses: "
					+ spanNearQuery);
		}
		this.collectPayloads = collectPayloads;
		allowedSlop = spanNearQuery.getSlop();
		SpanQuery[] clauses = spanNearQuery.getClauses();
		subSpans = new Spans[clauses.length];
		matchPayload = new LinkedList<byte[]>();
		subSpansByDoc = new Spans[clauses.length];
		for (int i = 0; i < clauses.length; i++) {
			subSpans[i] = clauses[i].getSpans(reader);
			subSpansByDoc[i] = subSpans[i];
		}
		query = spanNearQuery;
	}

	@Override
	public int doc() {
		return matchDoc;
	}

	@Override
	public int start() {
		return matchStart;
	}

	@Override
	public int end() {
		return matchEnd;
	}

	public Spans[] getSubSpans() {
		return subSpans;
	}

	@Override
	public Collection<byte[]> getPayload() throws IOException {
		return matchPayload;
	}

	@Override
	public boolean isPayloadAvailable() {
		return matchPayload.isEmpty() == false;
	}

	@Override
	public boolean next() throws IOException {
		if (firstTime) {
			firstTime = false;
			for (int i = 0; i < subSpans.length; i++) {
				if (!subSpans[i].next()) {
					more = false;
					return false;
				}
			}
			more = true;
		}
		if (collectPayloads) {
			matchPayload.clear();
		}
		return advanceAfterOrdered();
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (firstTime) {
			firstTime = false;
			for (int i = 0; i < subSpans.length; i++) {
				if (!subSpans[i].skipTo(target)) {
					more = false;
					return false;
				}
			}
			more = true;
		} else if (more && (subSpans[0].doc() < target)) {
			if (subSpans[0].skipTo(target)) {
				inSameDoc = false;
			} else {
				more = false;
				return false;
			}
		}
		if (collectPayloads) {
			matchPayload.clear();
		}
		return advanceAfterOrdered();
	}

	private boolean advanceAfterOrdered() throws IOException {
		while (more && (inSameDoc || toSameDoc())) {
			if (stretchToOrder() && shrinkToAfterShortestMatch()) {
				return true;
			}
		}
		return false;
	}

	private boolean toSameDoc() throws IOException {
		ArrayUtil.mergeSort(subSpansByDoc, spanDocComparator);
		int firstIndex = 0;
		int maxDoc = subSpansByDoc[subSpansByDoc.length - 1].doc();
		while (subSpansByDoc[firstIndex].doc() != maxDoc) {
			if (!subSpansByDoc[firstIndex].skipTo(maxDoc)) {
				more = false;
				inSameDoc = false;
				return false;
			}
			maxDoc = subSpansByDoc[firstIndex].doc();
			if (++firstIndex == subSpansByDoc.length) {
				firstIndex = 0;
			}
		}
		for (int i = 0; i < subSpansByDoc.length; i++) {
			assert (subSpansByDoc[i].doc() == maxDoc) : " NearSpansOrdered.toSameDoc() spans "
					+ subSpansByDoc[0]
					+ "\n at doc "
					+ subSpansByDoc[i].doc()
					+ ", but should be at " + maxDoc;
		}
		inSameDoc = true;
		return true;
	}

	static final boolean docSpansOrdered(Spans spans1, Spans spans2) {
		assert spans1.doc() == spans2.doc() : "doc1 " + spans1.doc()
				+ " != doc2 " + spans2.doc();
		int start1 = spans1.start();
		int start2 = spans2.start();

		return (start1 == start2) ? (spans1.end() < spans2.end())
				: (start1 < start2);
	}

	private static final boolean docSpansOrdered(int start1, int end1,
			int start2, int end2) {
		return (start1 == start2) ? (end1 < end2) : (start1 < start2);
	}

	private boolean stretchToOrder() throws IOException {
		matchDoc = subSpans[0].doc();
		for (int i = 1; inSameDoc && (i < subSpans.length); i++) {
			while (!docSpansOrdered(subSpans[i - 1], subSpans[i])) {
				if (!subSpans[i].next()) {
					inSameDoc = false;
					more = false;
					break;
				} else if (matchDoc != subSpans[i].doc()) {
					inSameDoc = false;
					break;
				}
			}
		}
		return inSameDoc;
	}

	private boolean shrinkToAfterShortestMatch() throws IOException {
		matchStart = subSpans[subSpans.length - 1].start();
		matchEnd = subSpans[subSpans.length - 1].end();
		Set<byte[]> possibleMatchPayloads = new HashSet<byte[]>();
		if (subSpans[subSpans.length - 1].isPayloadAvailable()) {
			possibleMatchPayloads.addAll(subSpans[subSpans.length - 1]
					.getPayload());
		}

		Collection<byte[]> possiblePayload = null;

		int matchSlop = 0;
		int lastStart = matchStart;
		int lastEnd = matchEnd;
		for (int i = subSpans.length - 2; i >= 0; i--) {
			Spans prevSpans = subSpans[i];
			if (collectPayloads && prevSpans.isPayloadAvailable()) {
				Collection<byte[]> payload = prevSpans.getPayload();
				possiblePayload = new ArrayList<byte[]>(payload.size());
				possiblePayload.addAll(payload);
			}

			int prevStart = prevSpans.start();
			int prevEnd = prevSpans.end();
			while (true) {
				if (!prevSpans.next()) {
					inSameDoc = false;
					more = false;
					break;
				} else if (matchDoc != prevSpans.doc()) {
					inSameDoc = false;
					break;
				} else {
					int ppStart = prevSpans.start();
					int ppEnd = prevSpans.end();
					if (!docSpansOrdered(ppStart, ppEnd, lastStart, lastEnd)) {
						break;
					} else {
						prevStart = ppStart;
						prevEnd = ppEnd;
						if (collectPayloads && prevSpans.isPayloadAvailable()) {
							Collection<byte[]> payload = prevSpans.getPayload();
							possiblePayload = new ArrayList<byte[]>(
									payload.size());
							possiblePayload.addAll(payload);
						}
					}
				}
			}

			if (collectPayloads && possiblePayload != null) {
				possibleMatchPayloads.addAll(possiblePayload);
			}

			assert prevStart <= matchStart;
			if (matchStart > prevEnd) {
				matchSlop += (matchStart - prevEnd);
			}

			matchStart = prevStart;
			lastStart = prevStart;
			lastEnd = prevEnd;
		}

		boolean match = matchSlop <= allowedSlop;

		if (collectPayloads && match && possibleMatchPayloads.size() > 0) {
			matchPayload.addAll(possibleMatchPayloads);
		}

		return match;
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
}
