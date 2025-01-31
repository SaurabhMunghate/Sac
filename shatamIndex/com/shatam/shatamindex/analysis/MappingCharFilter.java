/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

public class MappingCharFilter extends BaseCharFilter {

	private final NormalizeCharMap normMap;
	private LinkedList<Character> buffer;
	private String replacement;
	private int charPointer;
	private int nextCharCounter;

	public MappingCharFilter(NormalizeCharMap normMap, CharStream in) {
		super(in);
		this.normMap = normMap;
	}

	public MappingCharFilter(NormalizeCharMap normMap, Reader in) {
		super(CharReader.get(in));
		this.normMap = normMap;
	}

	@Override
	public int read() throws IOException {
		while (true) {
			if (replacement != null && charPointer < replacement.length()) {
				return replacement.charAt(charPointer++);
			}

			int firstChar = nextChar();
			if (firstChar == -1)
				return -1;
			NormalizeCharMap nm = normMap.submap != null ? normMap.submap
					.get(Character.valueOf((char) firstChar)) : null;
			if (nm == null)
				return firstChar;
			NormalizeCharMap result = match(nm);
			if (result == null)
				return firstChar;
			replacement = result.normStr;
			charPointer = 0;
			if (result.diff != 0) {
				int prevCumulativeDiff = getLastCumulativeDiff();
				if (result.diff < 0) {
					for (int i = 0; i < -result.diff; i++)
						addOffCorrectMap(nextCharCounter + i
								- prevCumulativeDiff, prevCumulativeDiff - 1
								- i);
				} else {
					addOffCorrectMap(nextCharCounter - result.diff
							- prevCumulativeDiff, prevCumulativeDiff
							+ result.diff);
				}
			}
		}
	}

	private int nextChar() throws IOException {
		nextCharCounter++;
		if (buffer != null && !buffer.isEmpty()) {
			return buffer.removeFirst().charValue();
		}
		return input.read();
	}

	private void pushChar(int c) {
		nextCharCounter--;
		if (buffer == null)
			buffer = new LinkedList<Character>();
		buffer.addFirst(Character.valueOf((char) c));
	}

	private void pushLastChar(int c) {
		if (buffer == null) {
			buffer = new LinkedList<Character>();
		}
		buffer.addLast(Character.valueOf((char) c));
	}

	private NormalizeCharMap match(NormalizeCharMap map) throws IOException {
		NormalizeCharMap result = null;
		if (map.submap != null) {
			int chr = nextChar();
			if (chr != -1) {
				NormalizeCharMap subMap = map.submap.get(Character
						.valueOf((char) chr));
				if (subMap != null) {
					result = match(subMap);
				}
				if (result == null) {
					pushChar(chr);
				}
			}
		}
		if (result == null && map.normStr != null) {
			result = map;
		}
		return result;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		char[] tmp = new char[len];
		int l = input.read(tmp, 0, len);
		if (l != -1) {
			for (int i = 0; i < l; i++)
				pushLastChar(tmp[i]);
		}
		l = 0;
		for (int i = off; i < off + len; i++) {
			int c = read();
			if (c == -1)
				break;
			cbuf[i] = (char) c;
			l++;
		}
		return l == 0 ? -1 : l;
	}
}
