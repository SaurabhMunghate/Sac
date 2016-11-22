/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

public interface TermFreqVector {

	public String getField();

	public int size();

	public String[] getTerms();

	public int[] getTermFrequencies();

	public int indexOf(String term);

	public int[] indexesOf(String[] terms, int start, int len);

}
