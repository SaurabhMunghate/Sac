/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.function;

import com.shatam.shatamindex.search.Explanation;

public abstract class DocValues {

	public abstract float floatVal(int doc);

	public int intVal(int doc) {
		return (int) floatVal(doc);
	}

	public long longVal(int doc) {
		return (long) floatVal(doc);
	}

	public double doubleVal(int doc) {
		return floatVal(doc);
	}

	public String strVal(int doc) {
		return Float.toString(floatVal(doc));
	}

	public abstract String toString(int doc);

	public Explanation explain(int doc) {
		return new Explanation(floatVal(doc), toString(doc));
	}

	Object getInnerArray() {
		throw new UnsupportedOperationException(
				"this optional method is for test purposes only");
	}

	private float minVal = Float.NaN;
	private float maxVal = Float.NaN;
	private float avgVal = Float.NaN;
	private boolean computed = false;

	private void compute() {
		if (computed) {
			return;
		}
		float sum = 0;
		int n = 0;
		while (true) {
			float val;
			try {
				val = floatVal(n);
			} catch (ArrayIndexOutOfBoundsException e) {
				break;
			}
			sum += val;
			minVal = Float.isNaN(minVal) ? val : Math.min(minVal, val);
			maxVal = Float.isNaN(maxVal) ? val : Math.max(maxVal, val);
			++n;
		}

		avgVal = n == 0 ? Float.NaN : sum / n;
		computed = true;
	}

	public float getMinValue() {
		compute();
		return minVal;
	}

	public float getMaxValue() {
		compute();
		return maxVal;
	}

	public float getAverageValue() {
		compute();
		return avgVal;
	}

}
