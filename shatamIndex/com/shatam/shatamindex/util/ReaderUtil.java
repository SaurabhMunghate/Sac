/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.shatam.shatamindex.index.IndexReader;

public final class ReaderUtil {

	private ReaderUtil() {
	}

	public static void gatherSubReaders(List<IndexReader> allSubReaders,
			IndexReader reader) {
		IndexReader[] subReaders = reader.getSequentialSubReaders();
		if (subReaders == null) {

			allSubReaders.add(reader);
		} else {
			for (int i = 0; i < subReaders.length; i++) {
				gatherSubReaders(allSubReaders, subReaders[i]);
			}
		}
	}

	public static abstract class Gather {
		private final IndexReader topReader;

		public Gather(IndexReader r) {
			topReader = r;
		}

		public int run() throws IOException {
			return run(0, topReader);
		}

		public int run(int docBase) throws IOException {
			return run(docBase, topReader);
		}

		private int run(int base, IndexReader reader) throws IOException {
			IndexReader[] subReaders = reader.getSequentialSubReaders();
			if (subReaders == null) {

				add(base, reader);
				base += reader.maxDoc();
			} else {

				for (int i = 0; i < subReaders.length; i++) {
					base = run(base, subReaders[i]);
				}
			}

			return base;
		}

		protected abstract void add(int base, IndexReader r) throws IOException;
	}

	public static IndexReader subReader(int doc, IndexReader reader) {
		List<IndexReader> subReadersList = new ArrayList<IndexReader>();
		ReaderUtil.gatherSubReaders(subReadersList, reader);
		IndexReader[] subReaders = subReadersList
				.toArray(new IndexReader[subReadersList.size()]);
		int[] docStarts = new int[subReaders.length];
		int maxDoc = 0;
		for (int i = 0; i < subReaders.length; i++) {
			docStarts[i] = maxDoc;
			maxDoc += subReaders[i].maxDoc();
		}
		return subReaders[ReaderUtil.subIndex(doc, docStarts)];
	}

	public static IndexReader subReader(IndexReader reader, int subIndex) {
		List<IndexReader> subReadersList = new ArrayList<IndexReader>();
		ReaderUtil.gatherSubReaders(subReadersList, reader);
		IndexReader[] subReaders = subReadersList
				.toArray(new IndexReader[subReadersList.size()]);
		return subReaders[subIndex];
	}

	public static int subIndex(int n, int[] docStarts) {

		int size = docStarts.length;
		int lo = 0;
		int hi = size - 1;
		while (hi >= lo) {
			int mid = (lo + hi) >>> 1;
			int midValue = docStarts[mid];
			if (n < midValue)
				hi = mid - 1;
			else if (n > midValue)
				lo = mid + 1;
			else {
				while (mid + 1 < size && docStarts[mid + 1] == midValue) {
					mid++;
				}
				return mid;
			}
		}
		return hi;
	}
}
