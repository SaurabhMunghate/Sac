/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.*;

public class SortedTermVectorMapper extends TermVectorMapper {

	private SortedSet<TermVectorEntry> currentSet;
	private Map<String, TermVectorEntry> termToTVE = new HashMap<String, TermVectorEntry>();
	private boolean storeOffsets;
	private boolean storePositions;

	public static final String ALL = "_ALL_";

	public SortedTermVectorMapper(Comparator<TermVectorEntry> comparator) {
		this(false, false, comparator);
	}

	public SortedTermVectorMapper(boolean ignoringPositions,
			boolean ignoringOffsets, Comparator<TermVectorEntry> comparator) {
		super(ignoringPositions, ignoringOffsets);
		currentSet = new TreeSet<TermVectorEntry>(comparator);
	}

	@Override
	public void map(String term, int frequency, TermVectorOffsetInfo[] offsets,
			int[] positions) {
		TermVectorEntry entry = termToTVE.get(term);
		if (entry == null) {
			entry = new TermVectorEntry(ALL, term, frequency,
					storeOffsets == true ? offsets : null,
					storePositions == true ? positions : null);
			termToTVE.put(term, entry);
			currentSet.add(entry);
		} else {
			entry.setFrequency(entry.getFrequency() + frequency);
			if (storeOffsets) {
				TermVectorOffsetInfo[] existingOffsets = entry.getOffsets();

				if (existingOffsets != null && offsets != null
						&& offsets.length > 0) {

					TermVectorOffsetInfo[] newOffsets = new TermVectorOffsetInfo[existingOffsets.length
							+ offsets.length];
					System.arraycopy(existingOffsets, 0, newOffsets, 0,
							existingOffsets.length);
					System.arraycopy(offsets, 0, newOffsets,
							existingOffsets.length, offsets.length);
					entry.setOffsets(newOffsets);
				} else if (existingOffsets == null && offsets != null
						&& offsets.length > 0) {
					entry.setOffsets(offsets);
				}

			}
			if (storePositions) {
				int[] existingPositions = entry.getPositions();
				if (existingPositions != null && positions != null
						&& positions.length > 0) {
					int[] newPositions = new int[existingPositions.length
							+ positions.length];
					System.arraycopy(existingPositions, 0, newPositions, 0,
							existingPositions.length);
					System.arraycopy(positions, 0, newPositions,
							existingPositions.length, positions.length);
					entry.setPositions(newPositions);
				} else if (existingPositions == null && positions != null
						&& positions.length > 0) {
					entry.setPositions(positions);
				}
			}
		}

	}

	@Override
	public void setExpectations(String field, int numTerms,
			boolean storeOffsets, boolean storePositions) {

		this.storeOffsets = storeOffsets;
		this.storePositions = storePositions;
	}

	public SortedSet<TermVectorEntry> getTermVectorEntrySet() {
		return currentSet;
	}

}
