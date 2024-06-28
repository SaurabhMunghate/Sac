/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.standard;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.shatam.shatamindex.analysis.Tokenizer;
import com.shatam.shatamindex.analysis.standard.std31.StandardTokenizerImpl31;
import com.shatam.shatamindex.analysis.standard.std31.UAX29URLEmailTokenizerImpl31;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.OffsetAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.PositionIncrementAttribute;
import com.shatam.shatamindex.analysis.tokenattributes.TypeAttribute;
import com.shatam.shatamindex.util.AttributeSource;
import com.shatam.shatamindex.util.Version;
import com.shatam.shatamindex.util.AttributeSource.AttributeFactory;

public final class UAX29URLEmailTokenizer extends Tokenizer {

	private final StandardTokenizerInterface scanner;

	public static final int ALPHANUM = 0;
	public static final int NUM = 1;
	public static final int SOUTHEAST_ASIAN = 2;
	public static final int IDEOGRAPHIC = 3;
	public static final int HIRAGANA = 4;
	public static final int KATAKANA = 5;
	public static final int HANGUL = 6;
	public static final int URL = 7;
	public static final int EMAIL = 8;

	public static final String[] TOKEN_TYPES = new String[] {
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.ALPHANUM],
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.NUM],
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.SOUTHEAST_ASIAN],
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.IDEOGRAPHIC],
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.HIRAGANA],
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.KATAKANA],
			StandardTokenizer.TOKEN_TYPES[StandardTokenizer.HANGUL], "<URL>",
			"<EMAIL>", };

	@Deprecated
	public static final String WORD_TYPE = TOKEN_TYPES[ALPHANUM];

	@Deprecated
	public static final String NUMERIC_TYPE = TOKEN_TYPES[NUM];

	@Deprecated
	public static final String URL_TYPE = TOKEN_TYPES[URL];

	@Deprecated
	public static final String EMAIL_TYPE = TOKEN_TYPES[EMAIL];

	@Deprecated
	public static final String SOUTH_EAST_ASIAN_TYPE = TOKEN_TYPES[SOUTHEAST_ASIAN];

	@Deprecated
	public static final String IDEOGRAPHIC_TYPE = TOKEN_TYPES[IDEOGRAPHIC];

	@Deprecated
	public static final String HIRAGANA_TYPE = TOKEN_TYPES[HIRAGANA];

	@Deprecated
	public static final String KATAKANA_TYPE = TOKEN_TYPES[KATAKANA];

	@Deprecated
	public static final String HANGUL_TYPE = TOKEN_TYPES[HANGUL];

	private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

	public void setMaxTokenLength(int length) {
		this.maxTokenLength = length;
	}

	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	@Deprecated
	public UAX29URLEmailTokenizer(Reader input) {
		this(Version.SHATAM_31, input);
	}

	@Deprecated
	public UAX29URLEmailTokenizer(InputStream input) {
		this(Version.SHATAM_31, new InputStreamReader(input));
	}

	@Deprecated
	public UAX29URLEmailTokenizer(AttributeSource source, Reader input) {
		this(Version.SHATAM_31, source, input);
	}

	@Deprecated
	public UAX29URLEmailTokenizer(AttributeFactory factory, Reader input) {
		this(Version.SHATAM_31, factory, input);
	}

	public UAX29URLEmailTokenizer(Version matchVersion, Reader input) {
		super(input);
		this.scanner = getScannerFor(matchVersion, input);
	}

	public UAX29URLEmailTokenizer(Version matchVersion, AttributeSource source,
			Reader input) {
		super(source, input);
		this.scanner = getScannerFor(matchVersion, input);
	}

	public UAX29URLEmailTokenizer(Version matchVersion,
			AttributeFactory factory, Reader input) {
		super(factory, input);
		this.scanner = getScannerFor(matchVersion, input);
	}

	private static StandardTokenizerInterface getScannerFor(
			Version matchVersion, Reader input) {
		if (matchVersion.onOrAfter(Version.SHATAM_34)) {
			return new UAX29URLEmailTokenizerImpl(input);
		} else {
			return new UAX29URLEmailTokenizerImpl31(input);
		}
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
				typeAtt.setType(TOKEN_TYPES[tokenType]);
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
}
