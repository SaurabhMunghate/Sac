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

public final class FuzzyTermEnum extends FilteredTermEnum {

	private int[] p;
	private int[] d;

	private float similarity;
	private boolean endEnum = false;

	private Term searchTerm = null;
	private final String field;
	private final char[] text;
	private final String prefix;

	private final float minimumSimilarity;
	private final float scale_factor;

	public FuzzyTermEnum(IndexReader reader, Term term) throws IOException {
		this(reader, term, FuzzyQuery.defaultMinSimilarity,
				FuzzyQuery.defaultPrefixLength);
	}

	public FuzzyTermEnum(IndexReader reader, Term term, float minSimilarity)
			throws IOException {
		this(reader, term, minSimilarity, FuzzyQuery.defaultPrefixLength);
	}

	public FuzzyTermEnum(IndexReader reader, Term term,
			final float minSimilarity, final int prefixLength)
			throws IOException {
		super();

		if (minSimilarity >= 1.0f)
			throw new IllegalArgumentException(
					"minimumSimilarity cannot be greater than or equal to 1");
		else if (minSimilarity < 0.0f)
			throw new IllegalArgumentException(
					"minimumSimilarity cannot be less than 0");
		if (prefixLength < 0)
			throw new IllegalArgumentException(
					"prefixLength cannot be less than 0");

		this.minimumSimilarity = minSimilarity;
		this.scale_factor = 1.0f / (1.0f - minimumSimilarity);
		this.searchTerm = term;
		this.field = searchTerm.field();

		final int fullSearchTermLength = searchTerm.text().length();
		final int realPrefixLength = prefixLength > fullSearchTermLength ? fullSearchTermLength
				: prefixLength;

		this.text = searchTerm.text().substring(realPrefixLength).toCharArray();
		this.prefix = searchTerm.text().substring(0, realPrefixLength);

		this.p = new int[this.text.length + 1];
		this.d = new int[this.text.length + 1];

		setEnum(reader.terms(new Term(searchTerm.field(), prefix)));
	}

	@Override
	protected final boolean termCompare(Term term) {
		if (field == term.field() && term.text().startsWith(prefix)) {
			final String target = term.text().substring(prefix.length());
			this.similarity = similarity(target);
			return (similarity > minimumSimilarity);
		}
		endEnum = true;
		return false;
	}

	@Override
	public final float difference() {
		return (similarity - minimumSimilarity) * scale_factor;
	}

	@Override
	public final boolean endEnum() {
		return endEnum;
	}

	private float similarity(final String target) {
		final int m = target.length();
		final int n = text.length;
		if (n == 0) {

			return prefix.length() == 0 ? 0.0f : 1.0f - ((float) m / prefix
					.length());
		}
		if (m == 0) {
			return prefix.length() == 0 ? 0.0f : 1.0f - ((float) n / prefix
					.length());
		}

		final int maxDistance = calculateMaxDistance(m);

		if (maxDistance < Math.abs(m - n)) {

			return 0.0f;
		}

		for (int i = 0; i <= n; ++i) {
			p[i] = i;
		}

		for (int j = 1; j <= m; ++j) {
			int bestPossibleEditDistance = m;
			final char t_j = target.charAt(j - 1);
			d[0] = j;

			for (int i = 1; i <= n; ++i) {

				if (t_j != text[i - 1]) {
					d[i] = Math.min(Math.min(d[i - 1], p[i]), p[i - 1]) + 1;
				} else {
					d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1]);
				}
				bestPossibleEditDistance = Math.min(bestPossibleEditDistance,
						d[i]);
			}

			if (j > maxDistance && bestPossibleEditDistance > maxDistance) {

				return 0.0f;
			}

			int _d[] = p;
			p = d;
			d = _d;
		}

		return 1.0f - ((float) p[n] / (float) (prefix.length() + Math.min(n, m)));
	}

	private int calculateMaxDistance(int m) {
		return (int) ((1 - minimumSimilarity) * (Math.min(text.length, m) + prefix
				.length()));
	}

	@Override
	public void close() throws IOException {
		p = d = null;
		searchTerm = null;
		super.close();
	}

}
