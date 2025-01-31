/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.shatam.shatamindex.util.Version;

public class CharArraySet extends AbstractSet<Object> {
	public static final CharArraySet EMPTY_SET = new CharArraySet(
			CharArrayMap.<Object> emptyMap());
	private static final Object PLACEHOLDER = new Object();

	private final CharArrayMap<Object> map;

	public CharArraySet(Version matchVersion, int startSize, boolean ignoreCase) {
		this(new CharArrayMap<Object>(matchVersion, startSize, ignoreCase));
	}

	public CharArraySet(Version matchVersion, Collection<?> c,
			boolean ignoreCase) {
		this(matchVersion, c.size(), ignoreCase);
		addAll(c);
	}

	@Deprecated
	public CharArraySet(int startSize, boolean ignoreCase) {
		this(Version.SHATAM_30, startSize, ignoreCase);
	}

	@Deprecated
	public CharArraySet(Collection<?> c, boolean ignoreCase) {
		this(Version.SHATAM_30, c.size(), ignoreCase);
		addAll(c);
	}

	CharArraySet(final CharArrayMap<Object> map) {
		this.map = map;
	}

	@Override
	public void clear() {
		map.clear();
	}

	public boolean contains(char[] text, int off, int len) {
		return map.containsKey(text, off, len);
	}

	public boolean contains(CharSequence cs) {
		return map.containsKey(cs);
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean add(Object o) {
		return map.put(o, PLACEHOLDER) == null;
	}

	public boolean add(CharSequence text) {
		return map.put(text, PLACEHOLDER) == null;
	}

	public boolean add(String text) {
		return map.put(text, PLACEHOLDER) == null;
	}

	public boolean add(char[] text) {
		return map.put(text, PLACEHOLDER) == null;
	}

	@Override
	public int size() {
		return map.size();
	}

	public static CharArraySet unmodifiableSet(CharArraySet set) {
		if (set == null)
			throw new NullPointerException("Given set is null");
		if (set == EMPTY_SET)
			return EMPTY_SET;
		if (set.map instanceof CharArrayMap.UnmodifiableCharArrayMap)
			return set;
		return new CharArraySet(CharArrayMap.unmodifiableMap(set.map));
	}

	@Deprecated
	public static CharArraySet copy(final Set<?> set) {
		if (set == EMPTY_SET)
			return EMPTY_SET;
		return copy(Version.SHATAM_30, set);
	}

	public static CharArraySet copy(final Version matchVersion, final Set<?> set) {
		if (set == EMPTY_SET)
			return EMPTY_SET;
		if (set instanceof CharArraySet) {
			final CharArraySet source = (CharArraySet) set;
			return new CharArraySet(CharArrayMap.copy(source.map.matchVersion,
					source.map));
		}
		return new CharArraySet(matchVersion, set, false);
	}

	@Deprecated
	public class CharArraySetIterator implements Iterator<String> {
		int pos = -1;
		char[] next;

		private CharArraySetIterator() {
			goNext();
		}

		private void goNext() {
			next = null;
			pos++;
			while (pos < map.keys.length && (next = map.keys[pos]) == null)
				pos++;
		}

		public boolean hasNext() {
			return next != null;
		}

		public char[] nextCharArray() {
			char[] ret = next;
			goNext();
			return ret;
		}

		public String next() {
			return new String(nextCharArray());
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Deprecated
	public Iterator<String> stringIterator() {
		return new CharArraySetIterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Object> iterator() {

		return map.matchVersion.onOrAfter(Version.SHATAM_31) ? map
				.originalKeySet().iterator() : (Iterator) stringIterator();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		for (Object item : this) {
			if (sb.length() > 1)
				sb.append(", ");
			if (item instanceof char[]) {
				sb.append((char[]) item);
			} else {
				sb.append(item);
			}
		}
		return sb.append(']').toString();
	}
}
