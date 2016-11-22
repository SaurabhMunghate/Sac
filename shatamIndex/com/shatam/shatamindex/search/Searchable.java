/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.io.Closeable;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.Term;

@Deprecated
public interface Searchable extends Closeable {

	void search(Weight weight, Filter filter, Collector collector)
			throws IOException;

	void close() throws IOException;

	int docFreq(Term term) throws IOException;

	int[] docFreqs(Term[] terms) throws IOException;

	int maxDoc() throws IOException;

	TopDocs search(Weight weight, Filter filter, int n) throws IOException;

	Document doc(int i) throws CorruptIndexException, IOException;

	Document doc(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException;

	Query rewrite(Query query) throws IOException;

	Explanation explain(Weight weight, int doc) throws IOException;

	TopFieldDocs search(Weight weight, Filter filter, int n, Sort sort)
			throws IOException;

}
