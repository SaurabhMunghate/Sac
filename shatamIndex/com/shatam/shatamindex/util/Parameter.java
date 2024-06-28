/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Map;

@Deprecated
@SuppressWarnings("serial")
public abstract class Parameter implements Serializable {
	static Map<String, Parameter> allParameters = new HashMap<String, Parameter>();

	private String name;

	protected Parameter(String name) {

		this.name = name;
		String key = makeKey(name);

		if (allParameters.containsKey(key))
			throw new IllegalArgumentException("Parameter name " + key
					+ " already used!");

		allParameters.put(key, this);
	}

	private String makeKey(String name) {
		return getClass() + " " + name;
	}

	@Override
	public String toString() {
		return name;
	}

	protected Object readResolve() throws ObjectStreamException {
		Object par = allParameters.get(makeKey(name));

		if (par == null)
			throw new StreamCorruptedException("Unknown parameter value: "
					+ name);

		return par;
	}

}
