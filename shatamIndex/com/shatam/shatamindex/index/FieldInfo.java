/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

public final class FieldInfo {
	final String name;
	final int number;

	boolean isIndexed;

	boolean storeTermVector;
	boolean storeOffsetWithTermVector;
	boolean storePositionWithTermVector;

	public boolean omitNorms;
	public IndexOptions indexOptions;

	boolean storePayloads;

	public static enum IndexOptions {

		DOCS_ONLY,

		DOCS_AND_FREQS,

		DOCS_AND_FREQS_AND_POSITIONS
	};

	FieldInfo(String na, boolean tk, int nu, boolean storeTermVector,
			boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector, boolean omitNorms,
			boolean storePayloads, IndexOptions indexOptions) {
		name = na;
		isIndexed = tk;
		number = nu;
		if (isIndexed) {
			this.storeTermVector = storeTermVector;
			this.storeOffsetWithTermVector = storeOffsetWithTermVector;
			this.storePositionWithTermVector = storePositionWithTermVector;
			this.storePayloads = storePayloads;
			this.omitNorms = omitNorms;
			this.indexOptions = indexOptions;
		} else {
			this.storeTermVector = false;
			this.storeOffsetWithTermVector = false;
			this.storePositionWithTermVector = false;
			this.storePayloads = false;
			this.omitNorms = true;
			this.indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
		}
		assert indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS
				|| !storePayloads;
	}

	@Override
	public Object clone() {
		return new FieldInfo(name, isIndexed, number, storeTermVector,
				storePositionWithTermVector, storeOffsetWithTermVector,
				omitNorms, storePayloads, indexOptions);
	}

	void update(boolean isIndexed, boolean storeTermVector,
			boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector, boolean omitNorms,
			boolean storePayloads, IndexOptions indexOptions) {

		if (this.isIndexed != isIndexed) {
			this.isIndexed = true;
		}
		if (isIndexed) {
			if (this.storeTermVector != storeTermVector) {
				this.storeTermVector = true;
			}
			if (this.storePositionWithTermVector != storePositionWithTermVector) {
				this.storePositionWithTermVector = true;
			}
			if (this.storeOffsetWithTermVector != storeOffsetWithTermVector) {
				this.storeOffsetWithTermVector = true;
			}
			if (this.storePayloads != storePayloads) {
				this.storePayloads = true;
			}
			if (this.omitNorms != omitNorms) {
				this.omitNorms = false;
			}
			if (this.indexOptions != indexOptions) {

				this.indexOptions = this.indexOptions.compareTo(indexOptions) < 0 ? this.indexOptions
						: indexOptions;
				this.storePayloads = false;
			}
		}
		assert this.indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS
				|| !this.storePayloads;
	}
}
