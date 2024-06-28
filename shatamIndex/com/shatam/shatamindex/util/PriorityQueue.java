/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

public abstract class PriorityQueue<T> {
	private int size;
	private int maxSize;
	private T[] heap;

	protected abstract boolean lessThan(T a, T b);

	protected T getSentinelObject() {
		return null;
	}

	@SuppressWarnings("unchecked")
	protected final void initialize(int maxSize) {
		size = 0;
		int heapSize;
		if (0 == maxSize)

			heapSize = 2;
		else {
			if (maxSize == Integer.MAX_VALUE) {

				heapSize = Integer.MAX_VALUE;
			} else {

				heapSize = maxSize + 1;
			}
		}
		heap = (T[]) new Object[heapSize];
		this.maxSize = maxSize;

		T sentinel = getSentinelObject();
		if (sentinel != null) {
			heap[1] = sentinel;
			for (int i = 2; i < heap.length; i++) {
				heap[i] = getSentinelObject();
			}
			size = maxSize;
		}
	}

	public final T add(T element) {
		size++;
		heap[size] = element;
		upHeap();
		return heap[1];
	}

	public T insertWithOverflow(T element) {
		if (size < maxSize) {
			add(element);
			return null;
		} else if (size > 0 && !lessThan(element, heap[1])) {
			T ret = heap[1];
			heap[1] = element;
			updateTop();
			return ret;
		} else {
			return element;
		}
	}

	public final T top() {

		return heap[1];
	}

	public final T pop() {
		if (size > 0) {
			T result = heap[1];
			heap[1] = heap[size];
			heap[size] = null;
			size--;
			downHeap();
			return result;
		} else
			return null;
	}

	public final T updateTop() {
		downHeap();
		return heap[1];
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
		T node = heap[i];
		int j = i >>> 1;
		while (j > 0 && lessThan(node, heap[j])) {
			heap[i] = heap[j];
			i = j;
			j = j >>> 1;
		}
		heap[i] = node;
	}

	private final void downHeap() {
		int i = 1;
		T node = heap[i];
		int j = i << 1;
		int k = j + 1;
		if (k <= size && lessThan(heap[k], heap[j])) {
			j = k;
		}
		while (j <= size && lessThan(heap[j], node)) {
			heap[i] = heap[j];
			i = j;
			j = i << 1;
			k = j + 1;
			if (k <= size && lessThan(heap[k], heap[j])) {
				j = k;
			}
		}
		heap[i] = node;
	}

	protected final Object[] getHeapArray() {
		return (Object[]) heap;
	}
}
