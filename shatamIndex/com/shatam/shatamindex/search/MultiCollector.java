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
import com.shatam.shatamindex.search.Collector;
import com.shatam.shatamindex.search.Scorer;

public class MultiCollector extends Collector {

	public static Collector wrap(Collector... collectors) {

		int n = 0;
		for (Collector c : collectors) {
			if (c != null) {
				n++;
			}
		}

		if (n == 0) {
			throw new IllegalArgumentException(
					"At least 1 collector must not be null");
		} else if (n == 1) {

			Collector col = null;
			for (Collector c : collectors) {
				if (c != null) {
					col = c;
					break;
				}
			}
			return col;
		} else if (n == collectors.length) {
			return new MultiCollector(collectors);
		} else {
			Collector[] colls = new Collector[n];
			n = 0;
			for (Collector c : collectors) {
				if (c != null) {
					colls[n++] = c;
				}
			}
			return new MultiCollector(colls);
		}
	}

	private final Collector[] collectors;

	private MultiCollector(Collector... collectors) {
		this.collectors = collectors;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		for (Collector c : collectors) {
			if (!c.acceptsDocsOutOfOrder()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void collect(int doc) throws IOException {
		for (Collector c : collectors) {
			c.collect(doc);
		}
	}

	@Override
	public void setNextReader(IndexReader reader, int o) throws IOException {
		for (Collector c : collectors) {
			c.setNextReader(reader, o);
		}
	}

	@Override
	public void setScorer(Scorer s) throws IOException {
		for (Collector c : collectors) {
			c.setScorer(s);
		}
	}

}
