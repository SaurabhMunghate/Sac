/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

public final class MapBackedSet<E> extends AbstractSet<E> implements
		Serializable {

	private static final long serialVersionUID = -6761513279741915432L;

	private final Map<E, Boolean> map;

	public MapBackedSet(Map<E, Boolean> map) {
		this.map = map;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean add(E o) {
		return map.put(o, Boolean.TRUE) == null;
	}

	@Override
	public boolean remove(Object o) {
		return map.remove(o) != null;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}
}
