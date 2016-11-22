/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.util.PriorityQueue;

import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

class FieldDocSortedHitQueue extends PriorityQueue<FieldDoc> {

	volatile SortField[] fields = null;

	volatile Collator[] collators = null;

	volatile FieldComparator[] comparators = null;

	FieldDocSortedHitQueue(int size) {
		initialize(size);
	}

	void setFields(SortField[] fields) throws IOException {
		this.fields = fields;
		this.collators = hasCollators(fields);
		comparators = new FieldComparator[fields.length];
		for (int fieldIDX = 0; fieldIDX < fields.length; fieldIDX++) {
			comparators[fieldIDX] = fields[fieldIDX].getComparator(1, fieldIDX);
		}
	}

	SortField[] getFields() {
		return fields;
	}

	private Collator[] hasCollators(final SortField[] fields) {
		if (fields == null)
			return null;
		Collator[] ret = new Collator[fields.length];
		for (int i = 0; i < fields.length; ++i) {
			Locale locale = fields[i].getLocale();
			if (locale != null)
				ret[i] = Collator.getInstance(locale);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final boolean lessThan(final FieldDoc docA, final FieldDoc docB) {
		final int n = fields.length;
		int c = 0;
		for (int i = 0; i < n && c == 0; ++i) {
			final int type = fields[i].getType();
			if (type == SortField.STRING) {
				final String s1 = (String) docA.fields[i];
				final String s2 = (String) docB.fields[i];

				if (s1 == null) {
					c = (s2 == null) ? 0 : -1;
				} else if (s2 == null) {
					c = 1;
				} else if (fields[i].getLocale() == null) {
					c = s1.compareTo(s2);
				} else {
					c = collators[i].compare(s1, s2);
				}
			} else {
				c = comparators[i]
						.compareValues(docA.fields[i], docB.fields[i]);
			}

			if (fields[i].getReverse()) {
				c = -c;
			}
		}

		if (c == 0)
			return docA.doc > docB.doc;

		return c > 0;
	}
}
