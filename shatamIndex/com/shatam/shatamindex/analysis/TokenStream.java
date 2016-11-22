/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;
import java.io.Closeable;
import java.lang.reflect.Modifier;

import com.shatam.shatamindex.util.AttributeSource;

public abstract class TokenStream extends AttributeSource implements Closeable {

	protected TokenStream() {
		super();
		assert assertFinal();
	}

	protected TokenStream(AttributeSource input) {
		super(input);
		assert assertFinal();
	}

	protected TokenStream(AttributeFactory factory) {
		super(factory);
		assert assertFinal();
	}

	private boolean assertFinal() {
		try {
			final Class<?> clazz = getClass();
			if (!clazz.desiredAssertionStatus())
				return true;
			assert clazz.isAnonymousClass()
					|| (clazz.getModifiers() & (Modifier.FINAL | Modifier.PRIVATE)) != 0
					|| Modifier.isFinal(clazz.getMethod("incrementToken")
							.getModifiers()) : "TokenStream implementation classes or at least their incrementToken() implementation must be final";
			return true;
		} catch (NoSuchMethodException nsme) {
			return false;
		}
	}

	public abstract boolean incrementToken() throws IOException;

	public void end() throws IOException {
		
	}

	public void reset() throws IOException {
	}

	public void close() throws IOException {
	}

}
