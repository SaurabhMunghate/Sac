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

import java.io.Reader;
import java.io.Serializable;

public interface Fieldable extends Serializable {

	void setBoost(float boost);

	float getBoost();

	String name();

	public String stringValue();

	public Reader readerValue();

	public TokenStream tokenStreamValue();

	boolean isStored();

	boolean isIndexed();

	boolean isTokenized();

	boolean isTermVectorStored();

	boolean isStoreOffsetWithTermVector();

	boolean isStorePositionWithTermVector();

	boolean isBinary();

	boolean getOmitNorms();

	void setOmitNorms(boolean omitNorms);

	boolean isLazy();

	abstract int getBinaryOffset();

	abstract int getBinaryLength();

	abstract byte[] getBinaryValue();

	abstract byte[] getBinaryValue(byte[] result);

	IndexOptions getIndexOptions();

	void setIndexOptions(IndexOptions indexOptions);
}
