/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import java.io.Reader;

import com.shatam.shatamindex.analysis.NumericTokenStream;
import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.search.FieldCache;
import com.shatam.shatamindex.search.NumericRangeFilter;
import com.shatam.shatamindex.search.NumericRangeQuery;
import com.shatam.shatamindex.search.SortField;
import com.shatam.shatamindex.util.NumericUtils;

public final class NumericField extends AbstractField {

	public static enum DataType {
		INT, LONG, FLOAT, DOUBLE
	}

	private transient NumericTokenStream numericTS;
	private DataType type;
	private final int precisionStep;

	public NumericField(String name) {
		this(name, NumericUtils.PRECISION_STEP_DEFAULT, Field.Store.NO, true);
	}

	public NumericField(String name, Field.Store store, boolean index) {
		this(name, NumericUtils.PRECISION_STEP_DEFAULT, store, index);
	}

	public NumericField(String name, int precisionStep) {
		this(name, precisionStep, Field.Store.NO, true);
	}

	public NumericField(String name, int precisionStep, Field.Store store,
			boolean index) {
		super(name, store, index ? Field.Index.ANALYZED_NO_NORMS
				: Field.Index.NO, Field.TermVector.NO);
		this.precisionStep = precisionStep;
		setIndexOptions(IndexOptions.DOCS_ONLY);
	}

	public TokenStream tokenStreamValue() {
		if (!isIndexed())
			return null;
		if (numericTS == null) {

			numericTS = new NumericTokenStream(precisionStep);

			if (fieldsData != null) {
				assert type != null;
				final Number val = (Number) fieldsData;
				switch (type) {
				case INT:
					numericTS.setIntValue(val.intValue());
					break;
				case LONG:
					numericTS.setLongValue(val.longValue());
					break;
				case FLOAT:
					numericTS.setFloatValue(val.floatValue());
					break;
				case DOUBLE:
					numericTS.setDoubleValue(val.doubleValue());
					break;
				default:
					assert false : "Should never get here";
				}
			}
		}
		return numericTS;
	}

	@Override
	public byte[] getBinaryValue(byte[] result) {
		return null;
	}

	public Reader readerValue() {
		return null;
	}

	public String stringValue() {
		return (fieldsData == null) ? null : fieldsData.toString();
	}

	public Number getNumericValue() {
		return (Number) fieldsData;
	}

	public int getPrecisionStep() {
		return precisionStep;
	}

	public DataType getDataType() {
		return type;
	}

	public NumericField setLongValue(final long value) {
		if (numericTS != null)
			numericTS.setLongValue(value);
		fieldsData = Long.valueOf(value);
		type = DataType.LONG;
		return this;
	}

	public NumericField setIntValue(final int value) {
		if (numericTS != null)
			numericTS.setIntValue(value);
		fieldsData = Integer.valueOf(value);
		type = DataType.INT;
		return this;
	}

	public NumericField setDoubleValue(final double value) {
		if (numericTS != null)
			numericTS.setDoubleValue(value);
		fieldsData = Double.valueOf(value);
		type = DataType.DOUBLE;
		return this;
	}

	public NumericField setFloatValue(final float value) {
		if (numericTS != null)
			numericTS.setFloatValue(value);
		fieldsData = Float.valueOf(value);
		type = DataType.FLOAT;
		return this;
	}

}
