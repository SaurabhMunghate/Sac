/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;

import com.shatam.shatamindex.search.DocIdSetIterator;
import com.shatam.shatamindex.search.Scorer;

public class ScorerDocQueue {

	private final HeapedScorerDoc[] heap;
	private final int maxSize;
	private int size;

	private class HeapedScorerDoc {
		Scorer scorer;
		int doc;

		HeapedScorerDoc(Scorer s) {
			this(s, s.docID());
		}

		HeapedScorerDoc(Scorer scorer, int doc) {
			this.scorer = scorer;
			this.doc = doc;
		}

		void adjust() {
			doc = scorer.docID();
		}
	}

	private HeapedScorerDoc topHSD;

	public ScorerDocQueue(int maxSize) {

		size = 0;
		int heapSize = maxSize + 1;
		heap = new HeapedScorerDoc[heapSize];
		this.maxSize = maxSize;
		topHSD = heap[1];
	}

	public final void put(Scorer scorer) {
		size++;
		heap[size] = new HeapedScorerDoc(scorer);
		upHeap();
	}

	public boolean insert(Scorer scorer) {
		if (size < maxSize) {
			put(scorer);
			return true;
		} else {
			int docNr = scorer.docID();
			if ((size > 0) && (!(docNr < topHSD.doc))) {
				heap[1] = new HeapedScorerDoc(scorer, docNr);
				downHeap();
				return true;
			} else {
				return false;
			}
		}
	}

	public final Scorer top() {

		return topHSD.scorer;
	}

	public final int topDoc() {

		return topHSD.doc;
	}

	public final float topScore() throws IOException {

		return topHSD.scorer.score();
	}

	public final boolean topNextAndAdjustElsePop() throws IOException {
		return checkAdjustElsePop(topHSD.scorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
	}

	public final boolean topSkipToAndAdjustElsePop(int target)
			throws IOException {
		return checkAdjustElsePop(topHSD.scorer.advance(target) != DocIdSetIterator.NO_MORE_DOCS);
	}

	private boolean checkAdjustElsePop(boolean cond) {
		if (cond) {
			topHSD.doc = topHSD.scorer.docID();
		} else {
			heap[1] = heap[size];
			heap[size] = null;
			size--;
		}
		downHeap();
		return cond;
	}

	public final Scorer pop() {

		Scorer result = topHSD.scorer;
		popNoResult();
		return result;
	}

	private final void popNoResult() {
		heap[1] = heap[size];
		heap[size] = null;
		size--;
		downHeap();
	}

	public final void adjustTop() {

		topHSD.adjust();
		downHeap();
	}

	public final int size() {
		return size;
	}

	public final void clear() {
		for (int i = 0; i <= size; i++) {
			heap[i] = null;
		}
		size = 0;
	}

	private final void upHeap() {
		int i = size;
		HeapedScorerDoc node = heap[i];
		int j = i >>> 1;
		while ((j > 0) && (node.doc < heap[j].doc)) {
			heap[i] = heap[j];
			i = j;
			j = j >>> 1;
		}
		heap[i] = node;
		topHSD = heap[1];
	}

	private final void downHeap() {
		int i = 1;
		HeapedScorerDoc node = heap[i];
		int j = i << 1;
		int k = j + 1;
		if ((k <= size) && (heap[k].doc < heap[j].doc)) {
			j = k;
		}
		while ((j <= size) && (heap[j].doc < node.doc)) {
			heap[i] = heap[j];
			i = j;
			j = i << 1;
			k = j + 1;
			if (k <= size && (heap[k].doc < heap[j].doc)) {
				j = k;
			}
		}
		heap[i] = node;
		topHSD = heap[1];
	}
}
