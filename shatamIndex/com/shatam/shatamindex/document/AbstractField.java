/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.index.FieldInvertState;
import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.search.PhraseQuery;
import com.shatam.shatamindex.search.spans.SpanQuery;
import com.shatam.shatamindex.util.StringHelper;

public abstract class AbstractField implements Fieldable {

	protected String name = "body";
	protected boolean storeTermVector = false;
	protected boolean storeOffsetWithTermVector = false;
	protected boolean storePositionWithTermVector = false;
	protected boolean omitNorms = false;
	protected boolean isStored = false;
	protected boolean isIndexed = true;
	protected boolean isTokenized = true;
	protected boolean isBinary = false;
	protected boolean lazy = false;
	protected IndexOptions indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
	protected float boost = 1.0f;

	protected Object fieldsData = null;

	protected TokenStream tokenStream;

	protected int binaryLength;
	protected int binaryOffset;

	protected AbstractField() {
	}

	protected AbstractField(String name, Field.Store store, Field.Index index,
			Field.TermVector termVector) {
		if (name == null)
			throw new NullPointerException("name cannot be null");
		this.name = StringHelper.intern(name);

		this.isStored = store.isStored();
		this.isIndexed = index.isIndexed();
		this.isTokenized = index.isAnalyzed();
		this.omitNorms = index.omitNorms();

		this.isBinary = false;

		setStoreTermVector(termVector);
	}

	public void setBoost(float boost) {
		this.boost = boost;
	}

	public float getBoost() {
		return boost;
	}

	public String name() {
		return name;
	}

	protected void setStoreTermVector(Field.TermVector termVector) {
		this.storeTermVector = termVector.isStored();
		this.storePositionWithTermVector = termVector.withPositions();
		this.storeOffsetWithTermVector = termVector.withOffsets();
	}

	public final boolean isStored() {
		return isStored;
	}

	public final boolean isIndexed() {
		return isIndexed;
	}

	public final boolean isTokenized() {
		return isTokenized;
	}

	public final boolean isTermVectorStored() {
		return storeTermVector;
	}

	public boolean isStoreOffsetWithTermVector() {
		return storeOffsetWithTermVector;
	}

	public boolean isStorePositionWithTermVector() {
		return storePositionWithTermVector;
	}

	public final boolean isBinary() {
		return isBinary;
	}

	public byte[] getBinaryValue() {
		return getBinaryValue(null);
	}

	public byte[] getBinaryValue(byte[] result) {
		if (isBinary || fieldsData instanceof byte[])
			return (byte[]) fieldsData;
		else
			return null;
	}

	public int getBinaryLength() {
		if (isBinary) {
			return binaryLength;
		} else if (fieldsData instanceof byte[])
			return ((byte[]) fieldsData).length;
		else
			return 0;
	}

	public int getBinaryOffset() {
		return binaryOffset;
	}

	public boolean getOmitNorms() {
		return omitNorms;
	}

	@Deprecated
	public boolean getOmitTermFreqAndPositions() {
		return indexOptions == IndexOptions.DOCS_ONLY;
	}

	public IndexOptions getIndexOptions() {
		return indexOptions;
	}

	public void setOmitNorms(boolean omitNorms) {
		this.omitNorms = omitNorms;
	}

	@Deprecated
	public void setOmitTermFreqAndPositions(boolean omitTermFreqAndPositions) {
		if (omitTermFreqAndPositions) {
			indexOptions = IndexOptions.DOCS_ONLY;
		} else {
			indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
		}
	}

	public void setIndexOptions(IndexOptions indexOptions) {
		this.indexOptions = indexOptions;
	}

	public boolean isLazy() {
		return lazy;
	}

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder();
		if (isStored) {
			result.append("stored");
		}
		if (isIndexed) {
			if (result.length() > 0)
				result.append(",");
			result.append("indexed");
		}
		if (isTokenized) {
			if (result.length() > 0)
				result.append(",");
			result.append("tokenized");
		}
		if (storeTermVector) {
			if (result.length() > 0)
				result.append(",");
			result.append("termVector");
		}
		if (storeOffsetWithTermVector) {
			if (result.length() > 0)
				result.append(",");
			result.append("termVectorOffsets");
		}
		if (storePositionWithTermVector) {
			if (result.length() > 0)
				result.append(",");
			result.append("termVectorPosition");
		}
		if (isBinary) {
			if (result.length() > 0)
				result.append(",");
			result.append("binary");
		}
		if (omitNorms) {
			result.append(",omitNorms");
		}
		if (indexOptions != IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
			result.append(",indexOptions=");
			result.append(indexOptions);
		}
		if (lazy) {
			result.append(",lazy");
		}
		result.append('<');
		result.append(name);
		result.append(':');

		if (fieldsData != null && lazy == false) {
			result.append(fieldsData);
		}

		result.append('>');
		return result.toString();
	}
}
