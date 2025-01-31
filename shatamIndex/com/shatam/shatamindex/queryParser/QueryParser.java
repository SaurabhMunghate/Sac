/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.queryParser;

import java.io.IOException;
import java.io.StringReader;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.analysis.CachingTokenFilter;
import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.PositionIncrementAttribute;
import com.shatam.shatamindex.document.DateField;
import com.shatam.shatamindex.document.DateTools;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.BooleanClause;
import com.shatam.shatamindex.search.BooleanQuery;
import com.shatam.shatamindex.search.FuzzyQuery;
import com.shatam.shatamindex.search.MatchAllDocsQuery;
import com.shatam.shatamindex.search.MultiPhraseQuery;
import com.shatam.shatamindex.search.MultiTermQuery;
import com.shatam.shatamindex.search.PhraseQuery;
import com.shatam.shatamindex.search.PrefixQuery;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.TermQuery;
import com.shatam.shatamindex.search.TermRangeQuery;
import com.shatam.shatamindex.search.WildcardQuery;
import com.shatam.shatamindex.util.Version;
import com.shatam.shatamindex.util.VirtualMethod;

public class QueryParser implements QueryParserConstants {

	private static final int CONJ_NONE = 0;
	private static final int CONJ_AND = 1;
	private static final int CONJ_OR = 2;

	private static final int MOD_NONE = 0;
	private static final int MOD_NOT = 10;
	private static final int MOD_REQ = 11;

	public static final Operator AND_OPERATOR = Operator.AND;

	public static final Operator OR_OPERATOR = Operator.OR;

	private Operator operator = OR_OPERATOR;

	boolean lowercaseExpandedTerms = true;
	MultiTermQuery.RewriteMethod multiTermRewriteMethod = MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT;
	boolean allowLeadingWildcard = false;
	boolean enablePositionIncrements = true;

	Analyzer analyzer;
	String field;
	int phraseSlop = 0;
	float fuzzyMinSim = FuzzyQuery.defaultMinSimilarity;
	int fuzzyPrefixLength = FuzzyQuery.defaultPrefixLength;
	Locale locale = Locale.getDefault();

	DateTools.Resolution dateResolution = null;

	Map<String, DateTools.Resolution> fieldToDateResolution = null;

	Collator rangeCollator = null;

	@Deprecated
	private static final VirtualMethod<QueryParser> getFieldQueryMethod = new VirtualMethod<QueryParser>(
			QueryParser.class, "getFieldQuery", String.class, String.class);

	@Deprecated
	private static final VirtualMethod<QueryParser> getFieldQueryWithQuotedMethod = new VirtualMethod<QueryParser>(
			QueryParser.class, "getFieldQuery", String.class, String.class,
			boolean.class);

	@Deprecated
	private final boolean hasNewAPI = VirtualMethod
			.compareImplementationDistance(getClass(),
					getFieldQueryWithQuotedMethod, getFieldQueryMethod) >= 0;

	private boolean autoGeneratePhraseQueries;

	static public enum Operator {
		OR, AND
	}

	public QueryParser(Version matchVersion, String f, Analyzer a) {
		this(new FastCharStream(new StringReader("")));
		analyzer = a;
		field = f;
		if (matchVersion.onOrAfter(Version.SHATAM_29)) {
			enablePositionIncrements = true;
		} else {
			enablePositionIncrements = false;
		}
		if (matchVersion.onOrAfter(Version.SHATAM_31)) {
			setAutoGeneratePhraseQueries(false);
		} else {
			setAutoGeneratePhraseQueries(true);
		}
	}

	public Query parse(String query) throws ParseException {
		ReInit(new FastCharStream(new StringReader(query)));
		try {

			Query res = TopLevelQuery(field);
			return res != null ? res : newBooleanQuery(false);
		} catch (ParseException tme) {

			ParseException e = new ParseException("Cannot parse '" + query
					+ "': " + tme.getMessage());
			e.initCause(tme);
			throw e;
		} catch (TokenMgrError tme) {
			ParseException e = new ParseException("Cannot parse '" + query
					+ "': " + tme.getMessage());
			e.initCause(tme);
			throw e;
		} catch (BooleanQuery.TooManyClauses tmc) {
			ParseException e = new ParseException("Cannot parse '" + query
					+ "': too many boolean clauses");
			e.initCause(tmc);
			throw e;
		}
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public String getField() {
		return field;
	}

	public final boolean getAutoGeneratePhraseQueries() {
		return autoGeneratePhraseQueries;
	}

	public final void setAutoGeneratePhraseQueries(boolean value) {
		if (value == false && !hasNewAPI)
			throw new IllegalArgumentException(
					"You must implement the new API: getFieldQuery(String,String,boolean)"
							+ " to use setAutoGeneratePhraseQueries(false)");
		this.autoGeneratePhraseQueries = value;
	}

	public float getFuzzyMinSim() {
		return fuzzyMinSim;
	}

	public void setFuzzyMinSim(float fuzzyMinSim) {
		this.fuzzyMinSim = fuzzyMinSim;
	}

	public int getFuzzyPrefixLength() {
		return fuzzyPrefixLength;
	}

	public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
		this.fuzzyPrefixLength = fuzzyPrefixLength;
	}

	public void setPhraseSlop(int phraseSlop) {
		this.phraseSlop = phraseSlop;
	}

	public int getPhraseSlop() {
		return phraseSlop;
	}

	public void setAllowLeadingWildcard(boolean allowLeadingWildcard) {
		this.allowLeadingWildcard = allowLeadingWildcard;
	}

	public boolean getAllowLeadingWildcard() {
		return allowLeadingWildcard;
	}

	public void setEnablePositionIncrements(boolean enable) {
		this.enablePositionIncrements = enable;
	}

	public boolean getEnablePositionIncrements() {
		return enablePositionIncrements;
	}

	public void setDefaultOperator(Operator op) {
		this.operator = op;
	}

	public Operator getDefaultOperator() {
		return operator;
	}

	public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
		this.lowercaseExpandedTerms = lowercaseExpandedTerms;
	}

	public boolean getLowercaseExpandedTerms() {
		return lowercaseExpandedTerms;
	}

	public void setMultiTermRewriteMethod(MultiTermQuery.RewriteMethod method) {
		multiTermRewriteMethod = method;
	}

	public MultiTermQuery.RewriteMethod getMultiTermRewriteMethod() {
		return multiTermRewriteMethod;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setDateResolution(DateTools.Resolution dateResolution) {
		this.dateResolution = dateResolution;
	}

	public void setDateResolution(String fieldName,
			DateTools.Resolution dateResolution) {
		if (fieldName == null) {
			throw new IllegalArgumentException("Field cannot be null.");
		}

		if (fieldToDateResolution == null) {

			fieldToDateResolution = new HashMap<String, DateTools.Resolution>();
		}

		fieldToDateResolution.put(fieldName, dateResolution);
	}

	public DateTools.Resolution getDateResolution(String fieldName) {
		if (fieldName == null) {
			throw new IllegalArgumentException("Field cannot be null.");
		}

		if (fieldToDateResolution == null) {

			return this.dateResolution;
		}

		DateTools.Resolution resolution = fieldToDateResolution.get(fieldName);
		if (resolution == null) {

			resolution = this.dateResolution;
		}

		return resolution;
	}

	public void setRangeCollator(Collator rc) {
		rangeCollator = rc;
	}

	public Collator getRangeCollator() {
		return rangeCollator;
	}

	protected void addClause(List<BooleanClause> clauses, int conj, int mods,
			Query q) {
		boolean required, prohibited;

		if (clauses.size() > 0 && conj == CONJ_AND) {
			BooleanClause c = clauses.get(clauses.size() - 1);
			if (!c.isProhibited())
				c.setOccur(BooleanClause.Occur.MUST);
		}

		if (clauses.size() > 0 && operator == AND_OPERATOR && conj == CONJ_OR) {

			BooleanClause c = clauses.get(clauses.size() - 1);
			if (!c.isProhibited())
				c.setOccur(BooleanClause.Occur.SHOULD);
		}

		if (q == null)
			return;

		if (operator == OR_OPERATOR) {

			prohibited = (mods == MOD_NOT);
			required = (mods == MOD_REQ);
			if (conj == CONJ_AND && !prohibited) {
				required = true;
			}
		} else {

			prohibited = (mods == MOD_NOT);
			required = (!prohibited && conj != CONJ_OR);
		}
		if (required && !prohibited)
			clauses.add(newBooleanClause(q, BooleanClause.Occur.MUST));
		else if (!required && !prohibited)
			clauses.add(newBooleanClause(q, BooleanClause.Occur.SHOULD));
		else if (!required && prohibited)
			clauses.add(newBooleanClause(q, BooleanClause.Occur.MUST_NOT));
		else
			throw new RuntimeException(
					"Clause cannot be both required and prohibited");
	}

	@Deprecated
	protected Query getFieldQuery(String field, String queryText)
			throws ParseException {

		return getFieldQuery(field, queryText, true);
	}

	protected Query getFieldQuery(String field, String queryText, boolean quoted)
			throws ParseException {

		TokenStream source;
		try {
			source = analyzer.reusableTokenStream(field, new StringReader(
					queryText));
			source.reset();
		} catch (IOException e) {
			source = analyzer.tokenStream(field, new StringReader(queryText));
		}
		CachingTokenFilter buffer = new CachingTokenFilter(source);
		CharTermAttribute termAtt = null;
		PositionIncrementAttribute posIncrAtt = null;
		int numTokens = 0;

		boolean success = false;
		try {
			buffer.reset();
			success = true;
		} catch (IOException e) {

		}
		if (success) {
			if (buffer.hasAttribute(CharTermAttribute.class)) {
				termAtt = buffer.getAttribute(CharTermAttribute.class);
			}
			if (buffer.hasAttribute(PositionIncrementAttribute.class)) {
				posIncrAtt = buffer
						.getAttribute(PositionIncrementAttribute.class);
			}
		}

		int positionCount = 0;
		boolean severalTokensAtSamePosition = false;

		boolean hasMoreTokens = false;
		if (termAtt != null) {
			try {
				hasMoreTokens = buffer.incrementToken();
				while (hasMoreTokens) {
					numTokens++;
					int positionIncrement = (posIncrAtt != null) ? posIncrAtt
							.getPositionIncrement() : 1;
					if (positionIncrement != 0) {
						positionCount += positionIncrement;
					} else {
						severalTokensAtSamePosition = true;
					}
					hasMoreTokens = buffer.incrementToken();
				}
			} catch (IOException e) {

			}
		}
		try {

			buffer.reset();

			source.close();
		} catch (IOException e) {

		}

		if (numTokens == 0)
			return null;
		else if (numTokens == 1) {
			String term = null;
			try {
				boolean hasNext = buffer.incrementToken();
				assert hasNext == true;
				term = termAtt.toString();
			} catch (IOException e) {

			}
			return newTermQuery(new Term(field, term));
		} else {
			if (severalTokensAtSamePosition
					|| (!quoted && !autoGeneratePhraseQueries)) {
				if (positionCount == 1
						|| (!quoted && !autoGeneratePhraseQueries)) {

					BooleanQuery q = newBooleanQuery(positionCount == 1);

					BooleanClause.Occur occur = positionCount > 1
							&& operator == AND_OPERATOR ? BooleanClause.Occur.MUST
							: BooleanClause.Occur.SHOULD;

					for (int i = 0; i < numTokens; i++) {
						String term = null;
						try {
							boolean hasNext = buffer.incrementToken();
							assert hasNext == true;
							term = termAtt.toString();
						} catch (IOException e) {

						}

						Query currentQuery = newTermQuery(new Term(field, term));
						q.add(currentQuery, occur);
					}
					return q;
				} else {

					MultiPhraseQuery mpq = newMultiPhraseQuery();
					mpq.setSlop(phraseSlop);
					List<Term> multiTerms = new ArrayList<Term>();
					int position = -1;
					for (int i = 0; i < numTokens; i++) {
						String term = null;
						int positionIncrement = 1;
						try {
							boolean hasNext = buffer.incrementToken();
							assert hasNext == true;
							term = termAtt.toString();
							if (posIncrAtt != null) {
								positionIncrement = posIncrAtt
										.getPositionIncrement();
							}
						} catch (IOException e) {

						}

						if (positionIncrement > 0 && multiTerms.size() > 0) {
							if (enablePositionIncrements) {
								mpq.add(multiTerms.toArray(new Term[0]),
										position);
							} else {
								mpq.add(multiTerms.toArray(new Term[0]));
							}
							multiTerms.clear();
						}
						position += positionIncrement;
						multiTerms.add(new Term(field, term));
					}
					if (enablePositionIncrements) {
						mpq.add(multiTerms.toArray(new Term[0]), position);
					} else {
						mpq.add(multiTerms.toArray(new Term[0]));
					}
					return mpq;
				}
			} else {
				PhraseQuery pq = newPhraseQuery();
				pq.setSlop(phraseSlop);
				int position = -1;

				for (int i = 0; i < numTokens; i++) {
					String term = null;
					int positionIncrement = 1;

					try {
						boolean hasNext = buffer.incrementToken();
						assert hasNext == true;
						term = termAtt.toString();
						if (posIncrAtt != null) {
							positionIncrement = posIncrAtt
									.getPositionIncrement();
						}
					} catch (IOException e) {

					}

					if (enablePositionIncrements) {
						position += positionIncrement;
						pq.add(new Term(field, term), position);
					} else {
						pq.add(new Term(field, term));
					}
				}
				return pq;
			}
		}
	}

	protected Query getFieldQuery(String field, String queryText, int slop)
			throws ParseException {
		Query query = hasNewAPI ? getFieldQuery(field, queryText, true)
				: getFieldQuery(field, queryText);

		if (query instanceof PhraseQuery) {
			((PhraseQuery) query).setSlop(slop);
		}
		if (query instanceof MultiPhraseQuery) {
			((MultiPhraseQuery) query).setSlop(slop);
		}

		return query;
	}

	protected Query getRangeQuery(String field, String part1, String part2,
			boolean inclusive) throws ParseException {
		if (lowercaseExpandedTerms) {
			part1 = part1.toLowerCase();
			part2 = part2.toLowerCase();
		}
		try {
			DateFormat df = DateFormat
					.getDateInstance(DateFormat.SHORT, locale);
			df.setLenient(true);
			Date d1 = df.parse(part1);
			Date d2 = df.parse(part2);
			if (inclusive) {

				Calendar cal = Calendar.getInstance(locale);
				cal.setTime(d2);
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				cal.set(Calendar.MILLISECOND, 999);
				d2 = cal.getTime();
			}
			DateTools.Resolution resolution = getDateResolution(field);
			if (resolution == null) {

				part1 = DateField.dateToString(d1);
				part2 = DateField.dateToString(d2);
			} else {
				part1 = DateTools.dateToString(d1, resolution);
				part2 = DateTools.dateToString(d2, resolution);
			}
		} catch (Exception e) {
		}

		return newRangeQuery(field, part1, part2, inclusive);
	}

	protected BooleanQuery newBooleanQuery(boolean disableCoord) {
		return new BooleanQuery(disableCoord);
	}

	protected BooleanClause newBooleanClause(Query q, BooleanClause.Occur occur) {
		return new BooleanClause(q, occur);
	}

	protected Query newTermQuery(Term term) {
		return new TermQuery(term);
	}

	protected PhraseQuery newPhraseQuery() {
		return new PhraseQuery();
	}

	protected MultiPhraseQuery newMultiPhraseQuery() {
		return new MultiPhraseQuery();
	}

	protected Query newPrefixQuery(Term prefix) {
		PrefixQuery query = new PrefixQuery(prefix);
		query.setRewriteMethod(multiTermRewriteMethod);
		return query;
	}

	protected Query newFuzzyQuery(Term term, float minimumSimilarity,
			int prefixLength) {

		return new FuzzyQuery(term, minimumSimilarity, prefixLength);
	}

	protected Query newRangeQuery(String field, String part1, String part2,
			boolean inclusive) {
		final TermRangeQuery query = new TermRangeQuery(field, part1, part2,
				inclusive, inclusive, rangeCollator);
		query.setRewriteMethod(multiTermRewriteMethod);
		return query;
	}

	protected Query newMatchAllDocsQuery() {
		return new MatchAllDocsQuery();
	}

	protected Query newWildcardQuery(Term t) {
		WildcardQuery query = new WildcardQuery(t);
		query.setRewriteMethod(multiTermRewriteMethod);
		return query;
	}

	protected Query getBooleanQuery(List<BooleanClause> clauses)
			throws ParseException {
		return getBooleanQuery(clauses, false);
	}

	protected Query getBooleanQuery(List<BooleanClause> clauses,
			boolean disableCoord) throws ParseException {
		if (clauses.size() == 0) {
			return null;
		}
		BooleanQuery query = newBooleanQuery(disableCoord);
		for (final BooleanClause clause : clauses) {
			query.add(clause);
		}
		return query;
	}

	protected Query getWildcardQuery(String field, String termStr)
			throws ParseException {
		if ("*".equals(field)) {
			if ("*".equals(termStr))
				return newMatchAllDocsQuery();
		}
		if (!allowLeadingWildcard
				&& (termStr.startsWith("*") || termStr.startsWith("?")))
			throw new ParseException(
					"'*' or '?' not allowed as first character in WildcardQuery");
		if (lowercaseExpandedTerms) {
			termStr = termStr.toLowerCase();
		}
		Term t = new Term(field, termStr);
		return newWildcardQuery(t);
	}

	protected Query getPrefixQuery(String field, String termStr)
			throws ParseException {
		if (!allowLeadingWildcard && termStr.startsWith("*"))
			throw new ParseException(
					"'*' not allowed as first character in PrefixQuery");
		if (lowercaseExpandedTerms) {
			termStr = termStr.toLowerCase();
		}
		Term t = new Term(field, termStr);
		return newPrefixQuery(t);
	}

	protected Query getFuzzyQuery(String field, String termStr,
			float minSimilarity) throws ParseException {
		if (lowercaseExpandedTerms) {
			termStr = termStr.toLowerCase();
		}
		Term t = new Term(field, termStr);
		return newFuzzyQuery(t, minSimilarity, fuzzyPrefixLength);
	}

	private String discardEscapeChar(String input) throws ParseException {

		char[] output = new char[input.length()];

		int length = 0;

		boolean lastCharWasEscapeChar = false;

		int codePointMultiplier = 0;

		int codePoint = 0;

		for (int i = 0; i < input.length(); i++) {
			char curChar = input.charAt(i);
			if (codePointMultiplier > 0) {
				codePoint += hexToInt(curChar) * codePointMultiplier;
				codePointMultiplier >>>= 4;
				if (codePointMultiplier == 0) {
					output[length++] = (char) codePoint;
					codePoint = 0;
				}
			} else if (lastCharWasEscapeChar) {
				if (curChar == 'u') {

					codePointMultiplier = 16 * 16 * 16;
				} else {

					output[length] = curChar;
					length++;
				}
				lastCharWasEscapeChar = false;
			} else {
				if (curChar == '\\') {
					lastCharWasEscapeChar = true;
				} else {
					output[length] = curChar;
					length++;
				}
			}
		}

		if (codePointMultiplier > 0) {
			throw new ParseException("Truncated unicode escape sequence.");
		}

		if (lastCharWasEscapeChar) {
			throw new ParseException("Term can not end with escape character.");
		}

		return new String(output, 0, length);
	}

	private static final int hexToInt(char c) throws ParseException {
		if ('0' <= c && c <= '9') {
			return c - '0';
		} else if ('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		} else if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		} else {
			throw new ParseException(
					"None-hex character in unicode escape sequence: " + c);
		}
	}

	public static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '('
					|| c == ')' || c == ':' || c == '^' || c == '[' || c == ']'
					|| c == '\"' || c == '{' || c == '}' || c == '~'
					|| c == '*' || c == '?' || c == '|' || c == '&') {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out
					.println("Usage: java org.shatam.shatamindex.queryParser.QueryParser <input>");
			System.exit(0);
		}
		QueryParser qp = new QueryParser(Version.SHATAM_CURRENT, "field",
				new com.shatam.shatamindex.analysis.SimpleAnalyzer());
		Query q = qp.parse(args[0]);
		System.out.println(q.toString("field"));
	}

	final public int Conjunction() throws ParseException {
		int ret = CONJ_NONE;
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case AND:
		case OR:
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case AND:
				jj_consume_token(AND);
				ret = CONJ_AND;
				break;
			case OR:
				jj_consume_token(OR);
				ret = CONJ_OR;
				break;
			default:
				jj_la1[0] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			break;
		default:
			jj_la1[1] = jj_gen;
			;
		}
		{
			if (true)
				return ret;
		}
		throw new Error("Missing return statement in function");
	}

	final public int Modifiers() throws ParseException {
		int ret = MOD_NONE;
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case NOT:
		case PLUS:
		case MINUS:
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case PLUS:
				jj_consume_token(PLUS);
				ret = MOD_REQ;
				break;
			case MINUS:
				jj_consume_token(MINUS);
				ret = MOD_NOT;
				break;
			case NOT:
				jj_consume_token(NOT);
				ret = MOD_NOT;
				break;
			default:
				jj_la1[2] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			break;
		default:
			jj_la1[3] = jj_gen;
			;
		}
		{
			if (true)
				return ret;
		}
		throw new Error("Missing return statement in function");
	}

	final public Query TopLevelQuery(String field) throws ParseException {
		Query q;

		q = Query(field);
		jj_consume_token(0);
		{
			if (true)
				return q;
		}
		throw new Error("Missing return statement in function");
	}

	final public Query Query(String field) throws ParseException {
		List<BooleanClause> clauses = new ArrayList<BooleanClause>();
		Query q, firstQuery = null;
		int conj, mods;
		mods = Modifiers();
		q = Clause(field);
		addClause(clauses, CONJ_NONE, mods, q);
		if (mods == MOD_NONE)
			firstQuery = q;
		label_1: while (true) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case AND:
			case OR:
			case NOT:
			case PLUS:
			case MINUS:
			case LPAREN:
			case STAR:
			case QUOTED:
			case TERM:
			case PREFIXTERM:
			case WILDTERM:
			case RANGEIN_START:
			case RANGEEX_START:
			case NUMBER:
				;
				break;
			default:
				jj_la1[4] = jj_gen;
				break label_1;
			}
			conj = Conjunction();
			mods = Modifiers();
			q = Clause(field);
			addClause(clauses, conj, mods, q);
		}
		if (clauses.size() == 1 && firstQuery != null) {
			if (true)
				return firstQuery;
		} else {
			{
				if (true)
					return getBooleanQuery(clauses);
			}
		}
		throw new Error("Missing return statement in function");
	}

	final public Query Clause(String field) throws ParseException {
		Query q;
		Token fieldToken = null, boost = null;
		if (jj_2_1(2)) {
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case TERM:
				fieldToken = jj_consume_token(TERM);
				jj_consume_token(COLON);
				field = discardEscapeChar(fieldToken.image);
				break;
			case STAR:
				jj_consume_token(STAR);
				jj_consume_token(COLON);
				field = "*";
				break;
			default:
				jj_la1[5] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
		} else {
			;
		}
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case STAR:
		case QUOTED:
		case TERM:
		case PREFIXTERM:
		case WILDTERM:
		case RANGEIN_START:
		case RANGEEX_START:
		case NUMBER:
			q = Term(field);
			break;
		case LPAREN:
			jj_consume_token(LPAREN);
			q = Query(field);
			jj_consume_token(RPAREN);
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case CARAT:
				jj_consume_token(CARAT);
				boost = jj_consume_token(NUMBER);
				break;
			default:
				jj_la1[6] = jj_gen;
				;
			}
			break;
		default:
			jj_la1[7] = jj_gen;
			jj_consume_token(-1);
			throw new ParseException();
		}
		if (boost != null) {
			float f = (float) 1.0;
			try {
				f = Float.valueOf(boost.image).floatValue();
				q.setBoost(f);
			} catch (Exception ignored) {
			}
		}
		{
			if (true)
				return q;
		}
		throw new Error("Missing return statement in function");
	}

	final public Query Term(String field) throws ParseException {
		Token term, boost = null, fuzzySlop = null, goop1, goop2;
		boolean prefix = false;
		boolean wildcard = false;
		boolean fuzzy = false;
		Query q;
		switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
		case STAR:
		case TERM:
		case PREFIXTERM:
		case WILDTERM:
		case NUMBER:
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case TERM:
				term = jj_consume_token(TERM);
				break;
			case STAR:
				term = jj_consume_token(STAR);
				wildcard = true;
				break;
			case PREFIXTERM:
				term = jj_consume_token(PREFIXTERM);
				prefix = true;
				break;
			case WILDTERM:
				term = jj_consume_token(WILDTERM);
				wildcard = true;
				break;
			case NUMBER:
				term = jj_consume_token(NUMBER);
				break;
			default:
				jj_la1[8] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case FUZZY_SLOP:
				fuzzySlop = jj_consume_token(FUZZY_SLOP);
				fuzzy = true;
				break;
			default:
				jj_la1[9] = jj_gen;
				;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case CARAT:
				jj_consume_token(CARAT);
				boost = jj_consume_token(NUMBER);
				switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
				case FUZZY_SLOP:
					fuzzySlop = jj_consume_token(FUZZY_SLOP);
					fuzzy = true;
					break;
				default:
					jj_la1[10] = jj_gen;
					;
				}
				break;
			default:
				jj_la1[11] = jj_gen;
				;
			}
			String termImage = discardEscapeChar(term.image);
			if (wildcard) {
				q = getWildcardQuery(field, termImage);
			} else if (prefix) {
				q = getPrefixQuery(
						field,
						discardEscapeChar(term.image.substring(0,
								term.image.length() - 1)));
			} else if (fuzzy) {
				float fms = fuzzyMinSim;
				try {
					fms = Float.valueOf(fuzzySlop.image.substring(1))
							.floatValue();
				} catch (Exception ignored) {
				}
				if (fms < 0.0f || fms > 1.0f) {
					{
						if (true)
							throw new ParseException(
									"Minimum similarity for a FuzzyQuery has to be between 0.0f and 1.0f !");
					}
				}
				q = getFuzzyQuery(field, termImage, fms);
			} else {
				q = hasNewAPI ? getFieldQuery(field, termImage, false)
						: getFieldQuery(field, termImage);
			}
			break;
		case RANGEIN_START:
			jj_consume_token(RANGEIN_START);
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case RANGEIN_GOOP:
				goop1 = jj_consume_token(RANGEIN_GOOP);
				break;
			case RANGEIN_QUOTED:
				goop1 = jj_consume_token(RANGEIN_QUOTED);
				break;
			default:
				jj_la1[12] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case RANGEIN_TO:
				jj_consume_token(RANGEIN_TO);
				break;
			default:
				jj_la1[13] = jj_gen;
				;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case RANGEIN_GOOP:
				goop2 = jj_consume_token(RANGEIN_GOOP);
				break;
			case RANGEIN_QUOTED:
				goop2 = jj_consume_token(RANGEIN_QUOTED);
				break;
			default:
				jj_la1[14] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			jj_consume_token(RANGEIN_END);
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case CARAT:
				jj_consume_token(CARAT);
				boost = jj_consume_token(NUMBER);
				break;
			default:
				jj_la1[15] = jj_gen;
				;
			}
			if (goop1.kind == RANGEIN_QUOTED) {
				goop1.image = goop1.image
						.substring(1, goop1.image.length() - 1);
			}
			if (goop2.kind == RANGEIN_QUOTED) {
				goop2.image = goop2.image
						.substring(1, goop2.image.length() - 1);
			}
			q = getRangeQuery(field, discardEscapeChar(goop1.image),
					discardEscapeChar(goop2.image), true);
			break;
		case RANGEEX_START:
			jj_consume_token(RANGEEX_START);
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case RANGEEX_GOOP:
				goop1 = jj_consume_token(RANGEEX_GOOP);
				break;
			case RANGEEX_QUOTED:
				goop1 = jj_consume_token(RANGEEX_QUOTED);
				break;
			default:
				jj_la1[16] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case RANGEEX_TO:
				jj_consume_token(RANGEEX_TO);
				break;
			default:
				jj_la1[17] = jj_gen;
				;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case RANGEEX_GOOP:
				goop2 = jj_consume_token(RANGEEX_GOOP);
				break;
			case RANGEEX_QUOTED:
				goop2 = jj_consume_token(RANGEEX_QUOTED);
				break;
			default:
				jj_la1[18] = jj_gen;
				jj_consume_token(-1);
				throw new ParseException();
			}
			jj_consume_token(RANGEEX_END);
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case CARAT:
				jj_consume_token(CARAT);
				boost = jj_consume_token(NUMBER);
				break;
			default:
				jj_la1[19] = jj_gen;
				;
			}
			if (goop1.kind == RANGEEX_QUOTED) {
				goop1.image = goop1.image
						.substring(1, goop1.image.length() - 1);
			}
			if (goop2.kind == RANGEEX_QUOTED) {
				goop2.image = goop2.image
						.substring(1, goop2.image.length() - 1);
			}

			q = getRangeQuery(field, discardEscapeChar(goop1.image),
					discardEscapeChar(goop2.image), false);
			break;
		case QUOTED:
			term = jj_consume_token(QUOTED);
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case FUZZY_SLOP:
				fuzzySlop = jj_consume_token(FUZZY_SLOP);
				break;
			default:
				jj_la1[20] = jj_gen;
				;
			}
			switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
			case CARAT:
				jj_consume_token(CARAT);
				boost = jj_consume_token(NUMBER);
				break;
			default:
				jj_la1[21] = jj_gen;
				;
			}
			int s = phraseSlop;

			if (fuzzySlop != null) {
				try {
					s = Float.valueOf(fuzzySlop.image.substring(1)).intValue();
				} catch (Exception ignored) {
				}
			}
			q = getFieldQuery(
					field,
					discardEscapeChar(term.image.substring(1,
							term.image.length() - 1)), s);
			break;
		default:
			jj_la1[22] = jj_gen;
			jj_consume_token(-1);
			throw new ParseException();
		}
		if (boost != null) {
			float f = (float) 1.0;
			try {
				f = Float.valueOf(boost.image).floatValue();
			} catch (Exception ignored) {

			}

			if (q != null) {
				q.setBoost(f);
			}
		}
		{
			if (true)
				return q;
		}
		throw new Error("Missing return statement in function");
	}

	private boolean jj_2_1(int xla) {
		jj_la = xla;
		jj_lastpos = jj_scanpos = token;
		try {
			return !jj_3_1();
		} catch (LookaheadSuccess ls) {
			return true;
		} finally {
			jj_save(0, xla);
		}
	}

	private boolean jj_3R_3() {
		if (jj_scan_token(STAR))
			return true;
		if (jj_scan_token(COLON))
			return true;
		return false;
	}

	private boolean jj_3R_2() {
		if (jj_scan_token(TERM))
			return true;
		if (jj_scan_token(COLON))
			return true;
		return false;
	}

	private boolean jj_3_1() {
		Token xsp;
		xsp = jj_scanpos;
		if (jj_3R_2()) {
			jj_scanpos = xsp;
			if (jj_3R_3())
				return true;
		}
		return false;
	}

	public QueryParserTokenManager token_source;

	public Token token;

	public Token jj_nt;
	private int jj_ntk;
	private Token jj_scanpos, jj_lastpos;
	private int jj_la;
	private int jj_gen;
	final private int[] jj_la1 = new int[23];
	static private int[] jj_la1_0;
	static private int[] jj_la1_1;
	static {
		jj_la1_init_0();
		jj_la1_init_1();
	}

	private static void jj_la1_init_0() {
		jj_la1_0 = new int[] { 0x300, 0x300, 0x1c00, 0x1c00, 0x3ed3f00,
				0x90000, 0x20000, 0x3ed2000, 0x2690000, 0x100000, 0x100000,
				0x20000, 0x30000000, 0x4000000, 0x30000000, 0x20000, 0x0,
				0x40000000, 0x0, 0x20000, 0x100000, 0x20000, 0x3ed0000, };
	}

	private static void jj_la1_init_1() {
		jj_la1_1 = new int[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
				0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x3, 0x0, 0x3, 0x0, 0x0,
				0x0, 0x0, };
	}

	final private JJCalls[] jj_2_rtns = new JJCalls[1];
	private boolean jj_rescan = false;
	private int jj_gc = 0;

	protected QueryParser(CharStream stream) {
		token_source = new QueryParserTokenManager(stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 23; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public void ReInit(CharStream stream) {
		token_source.ReInit(stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 23; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	protected QueryParser(QueryParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 23; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	public void ReInit(QueryParserTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 23; i++)
			jj_la1[i] = -1;
		for (int i = 0; i < jj_2_rtns.length; i++)
			jj_2_rtns[i] = new JJCalls();
	}

	private Token jj_consume_token(int kind) throws ParseException {
		Token oldToken;
		if ((oldToken = token).next != null)
			token = token.next;
		else
			token = token.next = token_source.getNextToken();
		jj_ntk = -1;
		if (token.kind == kind) {
			jj_gen++;
			if (++jj_gc > 100) {
				jj_gc = 0;
				for (int i = 0; i < jj_2_rtns.length; i++) {
					JJCalls c = jj_2_rtns[i];
					while (c != null) {
						if (c.gen < jj_gen)
							c.first = null;
						c = c.next;
					}
				}
			}
			return token;
		}
		token = oldToken;
		jj_kind = kind;
		throw generateParseException();
	}

	static private final class LookaheadSuccess extends java.lang.Error {
	}

	final private LookaheadSuccess jj_ls = new LookaheadSuccess();

	private boolean jj_scan_token(int kind) {
		if (jj_scanpos == jj_lastpos) {
			jj_la--;
			if (jj_scanpos.next == null) {
				jj_lastpos = jj_scanpos = jj_scanpos.next = token_source
						.getNextToken();
			} else {
				jj_lastpos = jj_scanpos = jj_scanpos.next;
			}
		} else {
			jj_scanpos = jj_scanpos.next;
		}
		if (jj_rescan) {
			int i = 0;
			Token tok = token;
			while (tok != null && tok != jj_scanpos) {
				i++;
				tok = tok.next;
			}
			if (tok != null)
				jj_add_error_token(kind, i);
		}
		if (jj_scanpos.kind != kind)
			return true;
		if (jj_la == 0 && jj_scanpos == jj_lastpos)
			throw jj_ls;
		return false;
	}

	final public Token getNextToken() {
		if (token.next != null)
			token = token.next;
		else
			token = token.next = token_source.getNextToken();
		jj_ntk = -1;
		jj_gen++;
		return token;
	}

	final public Token getToken(int index) {
		Token t = token;
		for (int i = 0; i < index; i++) {
			if (t.next != null)
				t = t.next;
			else
				t = t.next = token_source.getNextToken();
		}
		return t;
	}

	private int jj_ntk() {
		if ((jj_nt = token.next) == null)
			return (jj_ntk = (token.next = token_source.getNextToken()).kind);
		else
			return (jj_ntk = jj_nt.kind);
	}

	private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
	private int[] jj_expentry;
	private int jj_kind = -1;
	private int[] jj_lasttokens = new int[100];
	private int jj_endpos;

	private void jj_add_error_token(int kind, int pos) {
		if (pos >= 100)
			return;
		if (pos == jj_endpos + 1) {
			jj_lasttokens[jj_endpos++] = kind;
		} else if (jj_endpos != 0) {
			jj_expentry = new int[jj_endpos];
			for (int i = 0; i < jj_endpos; i++) {
				jj_expentry[i] = jj_lasttokens[i];
			}
			jj_entries_loop: for (java.util.Iterator it = jj_expentries
					.iterator(); it.hasNext();) {
				int[] oldentry = (int[]) (it.next());
				if (oldentry.length == jj_expentry.length) {
					for (int i = 0; i < jj_expentry.length; i++) {
						if (oldentry[i] != jj_expentry[i]) {
							continue jj_entries_loop;
						}
					}
					jj_expentries.add(jj_expentry);
					break jj_entries_loop;
				}
			}
			if (pos != 0)
				jj_lasttokens[(jj_endpos = pos) - 1] = kind;
		}
	}

	public ParseException generateParseException() {
		jj_expentries.clear();
		boolean[] la1tokens = new boolean[34];
		if (jj_kind >= 0) {
			la1tokens[jj_kind] = true;
			jj_kind = -1;
		}
		for (int i = 0; i < 23; i++) {
			if (jj_la1[i] == jj_gen) {
				for (int j = 0; j < 32; j++) {
					if ((jj_la1_0[i] & (1 << j)) != 0) {
						la1tokens[j] = true;
					}
					if ((jj_la1_1[i] & (1 << j)) != 0) {
						la1tokens[32 + j] = true;
					}
				}
			}
		}
		for (int i = 0; i < 34; i++) {
			if (la1tokens[i]) {
				jj_expentry = new int[1];
				jj_expentry[0] = i;
				jj_expentries.add(jj_expentry);
			}
		}
		jj_endpos = 0;
		jj_rescan_token();
		jj_add_error_token(0, 0);
		int[][] exptokseq = new int[jj_expentries.size()][];
		for (int i = 0; i < jj_expentries.size(); i++) {
			exptokseq[i] = jj_expentries.get(i);
		}
		return new ParseException(token, exptokseq, tokenImage);
	}

	final public void enable_tracing() {
	}

	final public void disable_tracing() {
	}

	private void jj_rescan_token() {
		jj_rescan = true;
		for (int i = 0; i < 1; i++) {
			try {
				JJCalls p = jj_2_rtns[i];
				do {
					if (p.gen > jj_gen) {
						jj_la = p.arg;
						jj_lastpos = jj_scanpos = p.first;
						switch (i) {
						case 0:
							jj_3_1();
							break;
						}
					}
					p = p.next;
				} while (p != null);
			} catch (LookaheadSuccess ls) {
			}
		}
		jj_rescan = false;
	}

	private void jj_save(int index, int xla) {
		JJCalls p = jj_2_rtns[index];
		while (p.gen > jj_gen) {
			if (p.next == null) {
				p = p.next = new JJCalls();
				break;
			}
			p = p.next;
		}
		p.gen = jj_gen + xla - jj_la;
		p.first = token;
		p.arg = xla;
	}

	static final class JJCalls {
		int gen;
		Token first;
		int arg;
		JJCalls next;
	}

}
