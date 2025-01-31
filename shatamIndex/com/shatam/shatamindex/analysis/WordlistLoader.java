/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import com.shatam.shatamindex.util.IOUtils;
import com.shatam.shatamindex.util.Version;

public class WordlistLoader {

	private static final int INITITAL_CAPACITY = 16;

	public static CharArraySet getWordSet(Reader reader, CharArraySet result)
			throws IOException {
		BufferedReader br = null;
		try {
			br = getBufferedReader(reader);
			String word = null;
			while ((word = br.readLine()) != null) {
				result.add(word.trim());
			}
		} finally {
			IOUtils.close(br);
		}
		return result;
	}

	public static CharArraySet getWordSet(Reader reader, Version matchVersion)
			throws IOException {
		return getWordSet(reader, new CharArraySet(matchVersion,
				INITITAL_CAPACITY, false));
	}

	public static CharArraySet getWordSet(Reader reader, String comment,
			Version matchVersion) throws IOException {
		return getWordSet(reader, comment, new CharArraySet(matchVersion,
				INITITAL_CAPACITY, false));
	}

	public static CharArraySet getWordSet(Reader reader, String comment,
			CharArraySet result) throws IOException {
		BufferedReader br = null;
		try {
			br = getBufferedReader(reader);
			String word = null;
			while ((word = br.readLine()) != null) {
				if (word.startsWith(comment) == false) {
					result.add(word.trim());
				}
			}
		} finally {
			IOUtils.close(br);
		}
		return result;
	}

	public static CharArraySet getSnowballWordSet(Reader reader,
			CharArraySet result) throws IOException {
		BufferedReader br = null;
		try {
			br = getBufferedReader(reader);
			String line = null;
			while ((line = br.readLine()) != null) {
				int comment = line.indexOf('|');
				if (comment >= 0)
					line = line.substring(0, comment);
				String words[] = line.split("\\s+");
				for (int i = 0; i < words.length; i++)
					if (words[i].length() > 0)
						result.add(words[i]);
			}
		} finally {
			IOUtils.close(br);
		}
		return result;
	}

	public static CharArraySet getSnowballWordSet(Reader reader,
			Version matchVersion) throws IOException {
		return getSnowballWordSet(reader, new CharArraySet(matchVersion,
				INITITAL_CAPACITY, false));
	}

	public static CharArrayMap<String> getStemDict(Reader reader,
			CharArrayMap<String> result) throws IOException {
		BufferedReader br = null;
		try {
			br = getBufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null) {
				String[] wordstem = line.split("\t", 2);
				result.put(wordstem[0], wordstem[1]);
			}
		} finally {
			IOUtils.close(br);
		}
		return result;
	}

	private static BufferedReader getBufferedReader(Reader reader) {
		return (reader instanceof BufferedReader) ? (BufferedReader) reader
				: new BufferedReader(reader);
	}

}
