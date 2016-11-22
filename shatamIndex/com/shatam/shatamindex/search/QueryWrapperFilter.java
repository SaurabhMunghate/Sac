/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;

public class QueryWrapperFilter extends Filter {
	private Query query;

	public QueryWrapperFilter(Query query) {
		this.query = query;
	}

	@Override
	public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
		final Weight weight = new IndexSearcher(reader)
				.createNormalizedWeight(query);
		return new DocIdSet() {
			@Override
			public DocIdSetIterator iterator() throws IOException {
				return weight.scorer(reader, true, false);
			}

			@Override
			public boolean isCacheable() {
				return false;
			}
		};
	}

	@Override
	public String toString() {
		return "QueryWrapperFilter(" + query + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof QueryWrapperFilter))
			return false;
		return this.query.equals(((QueryWrapperFilter) o).query);
	}

	@Override
	public int hashCode() {
		return query.hashCode() ^ 0x923F64B9;
	}
}
