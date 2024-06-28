/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.util.LinkedList;

import com.shatam.shatamindex.analysis.NumericTokenStream;
import com.shatam.shatamindex.document.NumericField;
import com.shatam.shatamindex.document.NumericField.DataType;
import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermEnum;
import com.shatam.shatamindex.util.NumericUtils;
import com.shatam.shatamindex.util.StringHelper;
import com.shatam.shatamindex.util.ToStringUtils;

public final class NumericRangeQuery<T extends Number> extends MultiTermQuery {

	private NumericRangeQuery(final String field, final int precisionStep,
			final DataType dataType, T min, T max, final boolean minInclusive,
			final boolean maxInclusive) {
		if (precisionStep < 1)
			throw new IllegalArgumentException("precisionStep must be >=1");
		this.field = StringHelper.intern(field);
		this.precisionStep = precisionStep;
		this.dataType = dataType;
		this.min = min;
		this.max = max;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;

		switch (dataType) {
		case LONG:
		case DOUBLE:
			setRewriteMethod((precisionStep > 6) ? CONSTANT_SCORE_FILTER_REWRITE
					: CONSTANT_SCORE_AUTO_REWRITE_DEFAULT);
			break;
		case INT:
		case FLOAT:
			setRewriteMethod((precisionStep > 8) ? CONSTANT_SCORE_FILTER_REWRITE
					: CONSTANT_SCORE_AUTO_REWRITE_DEFAULT);
			break;
		default:

			throw new IllegalArgumentException("Invalid numeric DataType");
		}

		if (min != null && min.equals(max)) {
			setRewriteMethod(CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE);
		}
	}

	public static NumericRangeQuery<Long> newLongRange(final String field,
			final int precisionStep, Long min, Long max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Long>(field, precisionStep, DataType.LONG,
				min, max, minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Long> newLongRange(final String field,
			Long min, Long max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeQuery<Long>(field,
				NumericUtils.PRECISION_STEP_DEFAULT, DataType.LONG, min, max,
				minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Integer> newIntRange(final String field,
			final int precisionStep, Integer min, Integer max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Integer>(field, precisionStep,
				DataType.INT, min, max, minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Integer> newIntRange(final String field,
			Integer min, Integer max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeQuery<Integer>(field,
				NumericUtils.PRECISION_STEP_DEFAULT, DataType.INT, min, max,
				minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Double> newDoubleRange(final String field,
			final int precisionStep, Double min, Double max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Double>(field, precisionStep,
				DataType.DOUBLE, min, max, minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Double> newDoubleRange(final String field,
			Double min, Double max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeQuery<Double>(field,
				NumericUtils.PRECISION_STEP_DEFAULT, DataType.DOUBLE, min, max,
				minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Float> newFloatRange(final String field,
			final int precisionStep, Float min, Float max,
			final boolean minInclusive, final boolean maxInclusive) {
		return new NumericRangeQuery<Float>(field, precisionStep,
				DataType.FLOAT, min, max, minInclusive, maxInclusive);
	}

	public static NumericRangeQuery<Float> newFloatRange(final String field,
			Float min, Float max, final boolean minInclusive,
			final boolean maxInclusive) {
		return new NumericRangeQuery<Float>(field,
				NumericUtils.PRECISION_STEP_DEFAULT, DataType.FLOAT, min, max,
				minInclusive, maxInclusive);
	}

	@Override
	protected FilteredTermEnum getEnum(final IndexReader reader)
			throws IOException {
		return new NumericRangeTermEnum(reader);
	}

	public String getField() {
		return field;
	}

	public boolean includesMin() {
		return minInclusive;
	}

	public boolean includesMax() {
		return maxInclusive;
	}

	public T getMin() {
		return min;
	}

	public T getMax() {
		return max;
	}

	public int getPrecisionStep() {
		return precisionStep;
	}

	@Override
	public String toString(final String field) {
		final StringBuilder sb = new StringBuilder();
		if (!this.field.equals(field))
			sb.append(this.field).append(':');
		return sb.append(minInclusive ? '[' : '{')
				.append((min == null) ? "*" : min.toString()).append(" TO ")
				.append((max == null) ? "*" : max.toString())
				.append(maxInclusive ? ']' : '}')
				.append(ToStringUtils.boost(getBoost())).toString();
	}

	@Override
	public final boolean equals(final Object o) {
		if (o == this)
			return true;
		if (!super.equals(o))
			return false;
		if (o instanceof NumericRangeQuery) {
			final NumericRangeQuery q = (NumericRangeQuery) o;
			return (field == q.field
					&& (q.min == null ? min == null : q.min.equals(min))
					&& (q.max == null ? max == null : q.max.equals(max))
					&& minInclusive == q.minInclusive
					&& maxInclusive == q.maxInclusive && precisionStep == q.precisionStep);
		}
		return false;
	}

	@Override
	public final int hashCode() {
		int hash = super.hashCode();
		hash += field.hashCode() ^ 0x4565fd66 + precisionStep ^ 0x64365465;
		if (min != null)
			hash += min.hashCode() ^ 0x14fa55fb;
		if (max != null)
			hash += max.hashCode() ^ 0x733fa5fe;
		return hash + (Boolean.valueOf(minInclusive).hashCode() ^ 0x14fa55fb)
				+ (Boolean.valueOf(maxInclusive).hashCode() ^ 0x733fa5fe);
	}

	private void readObject(java.io.ObjectInputStream in)
			throws java.io.IOException, ClassNotFoundException {
		in.defaultReadObject();
		field = StringHelper.intern(field);
	}

	String field;
	final int precisionStep;
	final DataType dataType;
	final T min, max;
	final boolean minInclusive, maxInclusive;

	static final long LONG_NEGATIVE_INFINITY = NumericUtils
			.doubleToSortableLong(Double.NEGATIVE_INFINITY);
	static final long LONG_POSITIVE_INFINITY = NumericUtils
			.doubleToSortableLong(Double.POSITIVE_INFINITY);
	static final int INT_NEGATIVE_INFINITY = NumericUtils
			.floatToSortableInt(Float.NEGATIVE_INFINITY);
	static final int INT_POSITIVE_INFINITY = NumericUtils
			.floatToSortableInt(Float.POSITIVE_INFINITY);

	private final class NumericRangeTermEnum extends FilteredTermEnum {

		private final IndexReader reader;
		private final LinkedList<String> rangeBounds = new LinkedList<String>();
		private final Term termTemplate = new Term(field);
		private String currentUpperBound = null;

		NumericRangeTermEnum(final IndexReader reader) throws IOException {
			this.reader = reader;

			switch (dataType) {
			case LONG:
			case DOUBLE: {

				long minBound;
				if (dataType == DataType.LONG) {
					minBound = (min == null) ? Long.MIN_VALUE : min.longValue();
				} else {
					assert dataType == DataType.DOUBLE;
					minBound = (min == null) ? LONG_NEGATIVE_INFINITY
							: NumericUtils.doubleToSortableLong(min
									.doubleValue());
				}
				if (!minInclusive && min != null) {
					if (minBound == Long.MAX_VALUE)
						break;
					minBound++;
				}

				long maxBound;
				if (dataType == DataType.LONG) {
					maxBound = (max == null) ? Long.MAX_VALUE : max.longValue();
				} else {
					assert dataType == DataType.DOUBLE;
					maxBound = (max == null) ? LONG_POSITIVE_INFINITY
							: NumericUtils.doubleToSortableLong(max
									.doubleValue());
				}
				if (!maxInclusive && max != null) {
					if (maxBound == Long.MIN_VALUE)
						break;
					maxBound--;
				}

				NumericUtils.splitLongRange(
						new NumericUtils.LongRangeBuilder() {
							@Override
							public final void addRange(String minPrefixCoded,
									String maxPrefixCoded) {
								rangeBounds.add(minPrefixCoded);
								rangeBounds.add(maxPrefixCoded);
							}
						}, precisionStep, minBound, maxBound);
				break;
			}

			case INT:
			case FLOAT: {

				int minBound;
				if (dataType == DataType.INT) {
					minBound = (min == null) ? Integer.MIN_VALUE : min
							.intValue();
				} else {
					assert dataType == DataType.FLOAT;
					minBound = (min == null) ? INT_NEGATIVE_INFINITY
							: NumericUtils.floatToSortableInt(min.floatValue());
				}
				if (!minInclusive && min != null) {
					if (minBound == Integer.MAX_VALUE)
						break;
					minBound++;
				}

				int maxBound;
				if (dataType == DataType.INT) {
					maxBound = (max == null) ? Integer.MAX_VALUE : max
							.intValue();
				} else {
					assert dataType == DataType.FLOAT;
					maxBound = (max == null) ? INT_POSITIVE_INFINITY
							: NumericUtils.floatToSortableInt(max.floatValue());
				}
				if (!maxInclusive && max != null) {
					if (maxBound == Integer.MIN_VALUE)
						break;
					maxBound--;
				}

				NumericUtils.splitIntRange(new NumericUtils.IntRangeBuilder() {
					@Override
					public final void addRange(String minPrefixCoded,
							String maxPrefixCoded) {
						rangeBounds.add(minPrefixCoded);
						rangeBounds.add(maxPrefixCoded);
					}
				}, precisionStep, minBound, maxBound);
				break;
			}

			default:

				throw new IllegalArgumentException("Invalid numeric DataType");
			}

			next();
		}

		@Override
		public float difference() {
			return 1.0f;
		}

		@Override
		protected boolean endEnum() {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		protected void setEnum(TermEnum tenum) {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		protected boolean termCompare(Term term) {
			return (term.field() == field && term.text().compareTo(
					currentUpperBound) <= 0);
		}

		@Override
		public boolean next() throws IOException {

			if (currentTerm != null) {
				assert actualEnum != null;
				if (actualEnum.next()) {
					currentTerm = actualEnum.term();
					if (termCompare(currentTerm))
						return true;
				}
			}

			currentTerm = null;
			while (rangeBounds.size() >= 2) {
				assert rangeBounds.size() % 2 == 0;

				if (actualEnum != null) {
					actualEnum.close();
					actualEnum = null;
				}
				final String lowerBound = rangeBounds.removeFirst();
				this.currentUpperBound = rangeBounds.removeFirst();

				actualEnum = reader.terms(termTemplate.createTerm(lowerBound));
				currentTerm = actualEnum.term();
				if (currentTerm != null && termCompare(currentTerm))
					return true;

				currentTerm = null;
			}

			assert rangeBounds.size() == 0 && currentTerm == null;
			return false;
		}

		@Override
		public void close() throws IOException {
			rangeBounds.clear();
			currentUpperBound = null;
			super.close();
		}

	}

}
