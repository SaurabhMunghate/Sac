
/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import java.util.HashSet;

import java.util.Set;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;

public abstract class Query implements java.io.Serializable, Cloneable {
	private float boost = 1.0f;

	public void setBoost(float b) {
		boost = b;
	}

	public float getBoost() {
		return boost;
	}

	public abstract String toString(String field);

	@Override
	public String toString() {
		return toString("");
	}

	public Weight createWeight(Searcher searcher) throws IOException {
		throw new UnsupportedOperationException("Query " + this
				+ " does not implement createWeight");
	}

	@Deprecated
	public final Weight weight(Searcher searcher) throws IOException {
		return searcher.createNormalizedWeight(this);
	}

	public Query rewrite(IndexReader reader) throws IOException {
		return this;
	}

	public Query combine(Query[] queries) {
		HashSet<Query> uniques = new HashSet<Query>();
		for (int i = 0; i < queries.length; i++) {
			Query query = queries[i];
			BooleanClause[] clauses = null;

			boolean splittable = (query instanceof BooleanQuery);
			if (splittable) {
				BooleanQuery bq = (BooleanQuery) query;
				splittable = bq.isCoordDisabled();
				clauses = bq.getClauses();
				for (int j = 0; splittable && j < clauses.length; j++) {
					splittable = (clauses[j].getOccur() == BooleanClause.Occur.SHOULD);
				}
			}
			if (splittable) {
				for (int j = 0; j < clauses.length; j++) {
					uniques.add(clauses[j].getQuery());
				}
			} else {
				uniques.add(query);
			}
		}

		if (uniques.size() == 1) {
			return uniques.iterator().next();
		}
		BooleanQuery result = new BooleanQuery(true);
		for (final Query query : uniques)
			result.add(query, BooleanClause.Occur.SHOULD);
		return result;
	}

	public void extractTerms(Set<Term> terms) {

		throw new UnsupportedOperationException();
	}

	public static Query mergeBooleanQueries(BooleanQuery... queries) {
		HashSet<BooleanClause> allClauses = new HashSet<BooleanClause>();
		for (BooleanQuery booleanQuery : queries) {
			for (BooleanClause clause : booleanQuery) {
				allClauses.add(clause);
			}
		}

		boolean coordDisabled = queries.length == 0 ? false : queries[0]
				.isCoordDisabled();
		BooleanQuery result = new BooleanQuery(coordDisabled);
		for (BooleanClause clause2 : allClauses) {
			result.add(clause2);
		}
		return result;
	}

	@Deprecated
	public Similarity getSimilarity(Searcher searcher) {
		return searcher.getSimilarity();
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Clone not supported: " + e.getMessage());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(boost);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Query other = (Query) obj;
		if (Float.floatToIntBits(boost) != Float.floatToIntBits(other.boost))
			return false;
		return true;
	}
}
