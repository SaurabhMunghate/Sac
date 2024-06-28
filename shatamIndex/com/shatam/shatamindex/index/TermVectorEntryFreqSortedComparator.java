/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.Comparator;

public class TermVectorEntryFreqSortedComparator implements
		Comparator<TermVectorEntry> {
	public int compare(TermVectorEntry entry, TermVectorEntry entry1) {
		int result = 0;
		result = entry1.getFrequency() - entry.getFrequency();
		if (result == 0) {
			result = entry.getTerm().compareTo(entry1.getTerm());
			if (result == 0) {
				result = entry.getField().compareTo(entry1.getField());
			}
		}
		return result;
	}
}
