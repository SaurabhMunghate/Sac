/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.*;

public class FieldSortedTermVectorMapper extends TermVectorMapper {
	private Map<String, SortedSet<TermVectorEntry>> fieldToTerms = new HashMap<String, SortedSet<TermVectorEntry>>();
	private SortedSet<TermVectorEntry> currentSet;
	private String currentField;
	private Comparator<TermVectorEntry> comparator;

	public FieldSortedTermVectorMapper(Comparator<TermVectorEntry> comparator) {
		this(false, false, comparator);
	}

	public FieldSortedTermVectorMapper(boolean ignoringPositions,
			boolean ignoringOffsets, Comparator<TermVectorEntry> comparator) {
		super(ignoringPositions, ignoringOffsets);
		this.comparator = comparator;
	}

	@Override
	public void map(String term, int frequency, TermVectorOffsetInfo[] offsets,
			int[] positions) {
		TermVectorEntry entry = new TermVectorEntry(currentField, term,
				frequency, offsets, positions);
		currentSet.add(entry);
	}

	@Override
	public void setExpectations(String field, int numTerms,
			boolean storeOffsets, boolean storePositions) {
		currentSet = new TreeSet<TermVectorEntry>(comparator);
		currentField = field;
		fieldToTerms.put(field, currentSet);
	}

	public Map<String, SortedSet<TermVectorEntry>> getFieldToTerms() {
		return fieldToTerms;
	}

	public Comparator<TermVectorEntry> getComparator() {
		return comparator;
	}
}
