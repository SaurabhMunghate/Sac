/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.analysis.NumericTokenStream;
import com.shatam.shatamindex.document.NumericField;
import com.shatam.shatamindex.util.NumericUtils;

public final class NumericRangeFilter<T extends Number> extends
		MultiTermQueryWrapperFilter<NumericRangeQuery<T>> {

	private NumericRangeFilter(final NumericRangeQuery<T> query) {
		super(query);
	}

	public static NumericRangeFilter<Long> newLongRange(final String field,
			final int precisionStep, Long min, Long max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeFilter<Long>(NumericRangeQuery.newLongRange(
				field, precisionStep, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Long> newLongRange(final String field,
			Long min, Long max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeFilter<Long>(NumericRangeQuery.newLongRange(
				field, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Integer> newIntRange(final String field,
			final int precisionStep, Integer min, Integer max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeFilter<Integer>(NumericRangeQuery.newIntRange(
				field, precisionStep, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Integer> newIntRange(final String field,
			Integer min, Integer max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeFilter<Integer>(NumericRangeQuery.newIntRange(
				field, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Double> newDoubleRange(final String field,
			final int precisionStep, Double min, Double max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeFilter<Double>(NumericRangeQuery.newDoubleRange(
				field, precisionStep, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Double> newDoubleRange(final String field,
			Double min, Double max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeFilter<Double>(NumericRangeQuery.newDoubleRange(
				field, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Float> newFloatRange(final String field,
			final int precisionStep, Float min, Float max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeFilter<Float>(NumericRangeQuery.newFloatRange(
				field, precisionStep, min, max, minInclusive, maxInclusive));
	}

	public static NumericRangeFilter<Float> newFloatRange(final String field,
			Float min, Float max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeFilter<Float>(NumericRangeQuery.newFloatRange(
				field, min, max, minInclusive, maxInclusive));
	}

	public String getField() {
		return query.getField();
	}

	public boolean includesMin() {
		return query.includesMin();
	}

	public boolean includesMax() {
		return query.includesMax();
	}

	public T getMin() {
		return query.getMin();
	}

	public T getMax() {
		return query.getMax();
	}

	public int getPrecisionStep() {
		return query.getPrecisionStep();
	}

}
