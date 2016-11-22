/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.standard;

import com.shatam.shatamindex.analysis.Tokenizer;
import com.shatam.shatamindex.analysis.standard.std31.StandardTokenizerImpl31;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.OffsetAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.PositionIncrementAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.TypeAttribute;
import com.shatam.shatamindex.util.AttributeSource;
import com.shatam.shatamindex.util.Version;

import java.io.IOException;
import java.io.Reader;

public final class StandardTokenizer extends Tokenizer {

	private StandardTokenizerInterface scanner;

	public static final int ALPHANUM = 0;

	@Deprecated
	public static final int APOSTROPHE = 1;

	@Deprecated
	public static final int ACRONYM = 2;

	@Deprecated
	public static final int COMPANY = 3;
	public static final int EMAIL = 4;

	@Deprecated
	public static final int HOST = 5;
	public static final int NUM = 6;

	@Deprecated
	public static final int CJ = 7;

	@Deprecated
	public static final int ACRONYM_DEP = 8;

	public static final int SOUTHEAST_ASIAN = 9;
	public static final int IDEOGRAPHIC = 10;
	public static final int HIRAGANA = 11;
	public static final int KATAKANA = 12;
	public static final int HANGUL = 13;

	public static final String[] TOKEN_TYPES = new String[] { "<ALPHANUM>",
			"<APOSTROPHE>", "<ACRONYM>", "<COMPANY>", "<EMAIL>", "<HOST>",
			"<NUM>", "<CJ>", "<ACRONYM_DEP>", "<SOUTHEAST_ASIAN>",
			"<IDEOGRAPHIC>", "<HIRAGANA>", "<KATAKANA>", "<HANGUL>" };

	private boolean replaceInvalidAcronym;

	private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

	public void setMaxTokenLength(int length) {
		this.maxTokenLength = length;
	}

	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	public StandardTokenizer(Version matchVersion, Reader input) {
		super();
		init(input, matchVersion);
	}

	public StandardTokenizer(Version matchVersion, AttributeSource source,
			Reader input) {
		super(source);
		init(input, matchVersion);
	}

	public StandardTokenizer(Version matchVersion, AttributeFactory factory,
			Reader input) {
		super(factory);
		init(input, matchVersion);
	}

	private final void init(Reader input, Version matchVersion) {
		if (matchVersion.onOrAfter(Version.SHATAM_34)) {
			this.scanner = new StandardTokenizerImpl(input);
		} else if (matchVersion.onOrAfter(Version.SHATAM_31)) {
			this.scanner = new StandardTokenizerImpl31(input);
		} else {
			this.scanner = new ClassicTokenizerImpl(input);
		}
		if (matchVersion.onOrAfter(Version.SHATAM_24)) {
			replaceInvalidAcronym = true;
		} else {
			replaceInvalidAcronym = false;
		}
		this.input = input;
	}

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		int posIncr = 1;

		while (true) {
			int tokenType = scanner.getNextToken();

			if (tokenType == StandardTokenizerInterface.YYEOF) {
				return false;
			}

			if (scanner.yylength() <= maxTokenLength) {
				posIncrAtt.setPositionIncrement(posIncr);
				scanner.getText(termAtt);
				final int start = scanner.yychar();
				offsetAtt.setOffset(correctOffset(start), correctOffset(start
						+ termAtt.length()));

				if (tokenType == StandardTokenizer.ACRONYM_DEP) {
					if (replaceInvalidAcronym) {
						typeAtt.setType(StandardTokenizer.TOKEN_TYPES[StandardTokenizer.HOST]);
						termAtt.setLength(termAtt.length() - 1);
					} else {
						typeAtt.setType(StandardTokenizer.TOKEN_TYPES[StandardTokenizer.ACRONYM]);
					}
				} else {
					typeAtt.setType(StandardTokenizer.TOKEN_TYPES[tokenType]);
				}
				return true;
			} else

				posIncr++;
		}
	}

	@Override
	public final void end() {

		int finalOffset = correctOffset(scanner.yychar() + scanner.yylength());
		offsetAtt.setOffset(finalOffset, finalOffset);
	}

	@Override
	public void reset(Reader reader) throws IOException {
		super.reset(reader);
		scanner.yyreset(reader);
	}

	@Deprecated
	public boolean isReplaceInvalidAcronym() {
		return replaceInvalidAcronym;
	}

	@Deprecated
	public void setReplaceInvalidAcronym(boolean replaceInvalidAcronym) {
		this.replaceInvalidAcronym = replaceInvalidAcronym;
	}
}
