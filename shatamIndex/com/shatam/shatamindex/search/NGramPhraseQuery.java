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

public class NGramPhraseQuery extends PhraseQuery {
	private final int n;

	public NGramPhraseQuery(int n) {
		super();
		this.n = n;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if (getSlop() != 0)
			return super.rewrite(reader);

		if (n < 2 || getTerms().length < 3)
			return super.rewrite(reader);

		int[] positions = getPositions();
		Term[] terms = getTerms();
		int prevPosition = positions[0];
		for (int i = 1; i < positions.length; i++) {
			int pos = positions[i];
			if (prevPosition + 1 != pos)
				return super.rewrite(reader);
			prevPosition = pos;
		}

		PhraseQuery optimized = new PhraseQuery();
		int pos = 0;
		final int lastPos = terms.length - 1;
		for (int i = 0; i < terms.length; i++) {
			if (pos % n == 0 || pos >= lastPos) {
				optimized.add(terms[i], positions[i]);
			}
			pos++;
		}

		return optimized;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NGramPhraseQuery))
			return false;
		NGramPhraseQuery other = (NGramPhraseQuery) o;
		if (this.n != other.n)
			return false;
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost()) ^ getSlop()
				^ getTerms().hashCode() ^ getPositions().hashCode() ^ n;
	}
}
