/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.search.BooleanClause;
import com.shatam.shatamindex.search.BooleanQuery;
import com.shatam.shatamindex.search.MultiPhraseQuery;
import com.shatam.shatamindex.search.PhraseQuery;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.Version;

public class MultiFieldQueryParser extends QueryParser {
	protected String[] fields;
	protected Map<String, Float> boosts;

	public MultiFieldQueryParser(Version matchVersion, String[] fields,
			Analyzer analyzer, Map<String, Float> boosts) {
		this(matchVersion, fields, analyzer);
		this.boosts = boosts;
	}

	public MultiFieldQueryParser(Version matchVersion, String[] fields,
			Analyzer analyzer) {
		super(matchVersion, null, analyzer);
		this.fields = fields;
	}

	@Override
	protected Query getFieldQuery(String field, String queryText, int slop)
			throws ParseException {
		if (field == null) {
			List<BooleanClause> clauses = new ArrayList<BooleanClause>();
			for (int i = 0; i < fields.length; i++) {
				Query q = super.getFieldQuery(fields[i], queryText, true);
				if (q != null) {

					if (boosts != null) {

						Float boost = boosts.get(fields[i]);
						if (boost != null) {
							q.setBoost(boost.floatValue());
						}
					}
					applySlop(q, slop);
					clauses.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
				}
			}
			if (clauses.size() == 0)
				return null;
			return getBooleanQuery(clauses, true);
		}
		Query q = super.getFieldQuery(field, queryText, true);
		applySlop(q, slop);
		return q;
	}

	private void applySlop(Query q, int slop) {
		if (q instanceof PhraseQuery) {
			((PhraseQuery) q).setSlop(slop);
		} else if (q instanceof MultiPhraseQuery) {
			((MultiPhraseQuery) q).setSlop(slop);
		}
	}

	@Override
	protected Query getFieldQuery(String field, String queryText, boolean quoted)
			throws ParseException {
		if (field == null) {
			List<BooleanClause> clauses = new ArrayList<BooleanClause>();
			for (int i = 0; i < fields.length; i++) {
				Query q = super.getFieldQuery(fields[i], queryText, quoted);
				if (q != null) {

					if (boosts != null) {

						Float boost = boosts.get(fields[i]);
						if (boost != null) {
							q.setBoost(boost.floatValue());
						}
					}
					clauses.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
				}
			}
			if (clauses.size() == 0)
				return null;
			return getBooleanQuery(clauses, true);
		}
		Query q = super.getFieldQuery(field, queryText, quoted);
		return q;
	}

	@Override
	protected Query getFuzzyQuery(String field, String termStr,
			float minSimilarity) throws ParseException {
		if (field == null) {
			List<BooleanClause> clauses = new ArrayList<BooleanClause>();
			for (int i = 0; i < fields.length; i++) {
				clauses.add(new BooleanClause(getFuzzyQuery(fields[i], termStr,
						minSimilarity), BooleanClause.Occur.SHOULD));
			}
			return getBooleanQuery(clauses, true);
		}
		return super.getFuzzyQuery(field, termStr, minSimilarity);
	}

	@Override
	protected Query getPrefixQuery(String field, String termStr)
			throws ParseException {
		if (field == null) {
			List<BooleanClause> clauses = new ArrayList<BooleanClause>();
			for (int i = 0; i < fields.length; i++) {
				clauses.add(new BooleanClause(
						getPrefixQuery(fields[i], termStr),
						BooleanClause.Occur.SHOULD));
			}
			return getBooleanQuery(clauses, true);
		}
		return super.getPrefixQuery(field, termStr);
	}

	@Override
	protected Query getWildcardQuery(String field, String termStr)
			throws ParseException {
		if (field == null) {
			List<BooleanClause> clauses = new ArrayList<BooleanClause>();
			for (int i = 0; i < fields.length; i++) {
				clauses.add(new BooleanClause(getWildcardQuery(fields[i],
						termStr), BooleanClause.Occur.SHOULD));
			}
			return getBooleanQuery(clauses, true);
		}
		return super.getWildcardQuery(field, termStr);
	}

	@Override
	protected Query getRangeQuery(String field, String part1, String part2,
			boolean inclusive) throws ParseException {
		if (field == null) {
			List<BooleanClause> clauses = new ArrayList<BooleanClause>();
			for (int i = 0; i < fields.length; i++) {
				clauses.add(new BooleanClause(getRangeQuery(fields[i], part1,
						part2, inclusive), BooleanClause.Occur.SHOULD));
			}
			return getBooleanQuery(clauses, true);
		}
		return super.getRangeQuery(field, part1, part2, inclusive);
	}

	public static Query parse(Version matchVersion, String[] queries,
			String[] fields, Analyzer analyzer) throws ParseException {
		if (queries.length != fields.length)
			throw new IllegalArgumentException(
					"queries.length != fields.length");
		BooleanQuery bQuery = new BooleanQuery();
		for (int i = 0; i < fields.length; i++) {
			QueryParser qp = new QueryParser(matchVersion, fields[i], analyzer);
			Query q = qp.parse(queries[i]);
			if (q != null
					&& (!(q instanceof BooleanQuery) || ((BooleanQuery) q)
							.getClauses().length > 0)) {
				bQuery.add(q, BooleanClause.Occur.SHOULD);
			}
		}
		return bQuery;
	}

	public static Query parse(Version matchVersion, String query,
			String[] fields, BooleanClause.Occur[] flags, Analyzer analyzer)
			throws ParseException {
		if (fields.length != flags.length)
			throw new IllegalArgumentException("fields.length != flags.length");
		BooleanQuery bQuery = new BooleanQuery();
		for (int i = 0; i < fields.length; i++) {
			QueryParser qp = new QueryParser(matchVersion, fields[i], analyzer);
			Query q = qp.parse(query);
			if (q != null
					&& (!(q instanceof BooleanQuery) || ((BooleanQuery) q)
							.getClauses().length > 0)) {
				bQuery.add(q, flags[i]);
			}
		}
		return bQuery;
	}

	public static Query parse(Version matchVersion, String[] queries,
			String[] fields, BooleanClause.Occur[] flags, Analyzer analyzer)
			throws ParseException {
		if (!(queries.length == fields.length && queries.length == flags.length))
			throw new IllegalArgumentException(
					"queries, fields, and flags array have have different length");
		BooleanQuery bQuery = new BooleanQuery();
		for (int i = 0; i < fields.length; i++) {
			QueryParser qp = new QueryParser(matchVersion, fields[i], analyzer);
			Query q = qp.parse(queries[i]);
			if (q != null
					&& (!(q instanceof BooleanQuery) || ((BooleanQuery) q)
							.getClauses().length > 0)) {
				bQuery.add(q, flags[i]);
			}
		}
		return bQuery;
	}

}
