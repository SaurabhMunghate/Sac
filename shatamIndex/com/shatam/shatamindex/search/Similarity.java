/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.index.FieldInvertState;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.Explanation.IDFExplanation;
import com.shatam.shatamindex.util.SmallFloat;
import com.shatam.shatamindex.util.VirtualMethod;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public abstract class Similarity implements Serializable {

	private static final VirtualMethod<Similarity> withoutDocFreqMethod = new VirtualMethod<Similarity>(
			Similarity.class, "idfExplain", Term.class, Searcher.class);
	private static final VirtualMethod<Similarity> withDocFreqMethod = new VirtualMethod<Similarity>(
			Similarity.class, "idfExplain", Term.class, Searcher.class,
			int.class);

	private final boolean hasIDFExplainWithDocFreqAPI = VirtualMethod
			.compareImplementationDistance(getClass(), withDocFreqMethod,
					withoutDocFreqMethod) >= 0;

	private static Similarity defaultImpl = new DefaultSimilarity();

	public static final int NO_DOC_ID_PROVIDED = -1;

	public static void setDefault(Similarity similarity) {
		Similarity.defaultImpl = similarity;
	}

	public static Similarity getDefault() {
		return Similarity.defaultImpl;
	}

	private static final float[] NORM_TABLE = new float[256];

	static {
		for (int i = 0; i < 256; i++)
			NORM_TABLE[i] = SmallFloat.byte315ToFloat((byte) i);
	}

	@Deprecated
	public static float decodeNorm(byte b) {
		return NORM_TABLE[b & 0xFF];
	}

	public float decodeNormValue(byte b) {
		return NORM_TABLE[b & 0xFF];
	}

	@Deprecated
	public static float[] getNormDecoder() {
		return NORM_TABLE;
	}

	public abstract float computeNorm(String field, FieldInvertState state);

	@Deprecated
	public final float lengthNorm(String fieldName, int numTokens) {
		throw new UnsupportedOperationException(
				"please use computeNorm instead");
	}

	public abstract float queryNorm(float sumOfSquaredWeights);

	public byte encodeNormValue(float f) {
		return SmallFloat.floatToByte315(f);
	}

	@Deprecated
	public static byte encodeNorm(float f) {
		return SmallFloat.floatToByte315(f);
	}

	public float tf(int freq) {
		return tf((float) freq);
	}

	public abstract float sloppyFreq(int distance);

	public abstract float tf(float freq);

	public IDFExplanation idfExplain(final Term term, final Searcher searcher,
			int docFreq) throws IOException {

		if (!hasIDFExplainWithDocFreqAPI) {

			return idfExplain(term, searcher);
		}
		final int df = docFreq;
		final int max = searcher.maxDoc();
		final float idf = idf(df, max);
		return new IDFExplanation() {
			@Override
			public String explain() {
				return "idf(docFreq=" + df + ", maxDocs=" + max + ")";
			}

			@Override
			public float getIdf() {
				return idf;
			}
		};
	}

	public IDFExplanation idfExplain(final Term term, final Searcher searcher)
			throws IOException {
		return idfExplain(term, searcher, searcher.docFreq(term));
	}

	public IDFExplanation idfExplain(Collection<Term> terms, Searcher searcher)
			throws IOException {
		final int max = searcher.maxDoc();
		float idf = 0.0f;
		final StringBuilder exp = new StringBuilder();
		for (final Term term : terms) {
			final int df = searcher.docFreq(term);
			idf += idf(df, max);
			exp.append(" ");
			exp.append(term.text());
			exp.append("=");
			exp.append(df);
		}
		final float fIdf = idf;
		return new IDFExplanation() {
			@Override
			public float getIdf() {
				return fIdf;
			}

			@Override
			public String explain() {
				return exp.toString();
			}
		};
	}

	public abstract float idf(int docFreq, int numDocs);

	public abstract float coord(int overlap, int maxOverlap);

	public float scorePayload(int docId, String fieldName, int start, int end,
			byte[] payload, int offset, int length) {
		return 1;
	}

}
