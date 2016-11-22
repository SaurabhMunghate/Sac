/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.util.ArrayUtil;

final class NormsWriterPerField extends InvertedDocEndConsumerPerField
		implements Comparable<NormsWriterPerField> {

	final NormsWriterPerThread perThread;
	final FieldInfo fieldInfo;
	final DocumentsWriter.DocState docState;

	int[] docIDs = new int[1];
	byte[] norms = new byte[1];
	int upto;

	final FieldInvertState fieldState;

	public void reset() {

		docIDs = ArrayUtil.shrink(docIDs, upto);
		norms = ArrayUtil.shrink(norms, upto);
		upto = 0;
	}

	public NormsWriterPerField(final DocInverterPerField docInverterPerField,
			final NormsWriterPerThread perThread, final FieldInfo fieldInfo) {
		this.perThread = perThread;
		this.fieldInfo = fieldInfo;
		docState = perThread.docState;
		fieldState = docInverterPerField.fieldState;
	}

	@Override
	void abort() {
		upto = 0;
	}

	public int compareTo(NormsWriterPerField other) {
		return fieldInfo.name.compareTo(other.fieldInfo.name);
	}

	@Override
	void finish() {
		if (fieldInfo.isIndexed && !fieldInfo.omitNorms) {
			if (docIDs.length <= upto) {
				assert docIDs.length == upto;
				docIDs = ArrayUtil.grow(docIDs, 1 + upto);
			}
			if (norms.length <= upto) {
				assert norms.length == upto;
				norms = ArrayUtil.grow(norms, 1 + upto);
			}
			final float norm = docState.similarity.computeNorm(fieldInfo.name,
					fieldState);
			norms[upto] = docState.similarity.encodeNormValue(norm);
			docIDs[upto] = docState.docID;
			upto++;
		}
	}
}
