/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.PriorityQueue;

public class MultipleTermPositions implements TermPositions {

	private static final class TermPositionsQueue extends
			PriorityQueue<TermPositions> {
		TermPositionsQueue(List<TermPositions> termPositions)
				throws IOException {
			initialize(termPositions.size());

			for (TermPositions tp : termPositions) {
				if (tp.next())
					add(tp);
			}
		}

		final TermPositions peek() {
			return top();
		}

		@Override
		public final boolean lessThan(TermPositions a, TermPositions b) {
			return a.doc() < b.doc();
		}
	}

	private static final class IntQueue {
		private int _arraySize = 16;
		private int _index = 0;
		private int _lastIndex = 0;
		private int[] _array = new int[_arraySize];

		final void add(int i) {
			if (_lastIndex == _arraySize)
				growArray();

			_array[_lastIndex++] = i;
		}

		final int next() {
			return _array[_index++];
		}

		final void sort() {
			Arrays.sort(_array, _index, _lastIndex);
		}

		final void clear() {
			_index = 0;
			_lastIndex = 0;
		}

		final int size() {
			return (_lastIndex - _index);
		}

		private void growArray() {
			_array = ArrayUtil.grow(_array, _arraySize + 1);
			_arraySize = _array.length;
		}
	}

	private int _doc;
	private int _freq;
	private TermPositionsQueue _termPositionsQueue;
	private IntQueue _posList;

	public MultipleTermPositions(IndexReader indexReader, Term[] terms)
			throws IOException {
		List<TermPositions> termPositions = new LinkedList<TermPositions>();

		for (int i = 0; i < terms.length; i++)
			termPositions.add(indexReader.termPositions(terms[i]));

		_termPositionsQueue = new TermPositionsQueue(termPositions);
		_posList = new IntQueue();
	}

	public final boolean next() throws IOException {
		if (_termPositionsQueue.size() == 0)
			return false;

		_posList.clear();
		_doc = _termPositionsQueue.peek().doc();

		TermPositions tp;
		do {
			tp = _termPositionsQueue.peek();

			for (int i = 0; i < tp.freq(); i++) {

				_posList.add(tp.nextPosition());
			}

			if (tp.next())
				_termPositionsQueue.updateTop();
			else {
				_termPositionsQueue.pop();
				tp.close();
			}
		} while (_termPositionsQueue.size() > 0
				&& _termPositionsQueue.peek().doc() == _doc);

		_posList.sort();
		_freq = _posList.size();

		return true;
	}

	public final int nextPosition() {

		return _posList.next();
	}

	public final boolean skipTo(int target) throws IOException {
		while (_termPositionsQueue.peek() != null
				&& target > _termPositionsQueue.peek().doc()) {
			TermPositions tp = _termPositionsQueue.pop();
			if (tp.skipTo(target))
				_termPositionsQueue.add(tp);
			else
				tp.close();
		}
		return next();
	}

	public final int doc() {
		return _doc;
	}

	public final int freq() {
		return _freq;
	}

	public final void close() throws IOException {
		while (_termPositionsQueue.size() > 0)
			_termPositionsQueue.pop().close();
	}

	public void seek(Term arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void seek(TermEnum termEnum) throws IOException {
		throw new UnsupportedOperationException();
	}

	public int read(int[] arg0, int[] arg1) throws IOException {
		throw new UnsupportedOperationException();
	}

	public int getPayloadLength() {
		throw new UnsupportedOperationException();
	}

	public byte[] getPayload(byte[] data, int offset) throws IOException {
		throw new UnsupportedOperationException();
	}

	public boolean isPayloadAvailable() {
		return false;
	}
}
