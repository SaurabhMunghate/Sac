/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.Reader;
import java.io.IOException;
import java.io.Closeable;
import java.lang.reflect.Modifier;

import com.shatam.shatamindex.document.Fieldable;
import com.shatam.shatamindex.store.AlreadyClosedException;
import com.shatam.shatamindex.util.CloseableThreadLocal;

public abstract class Analyzer implements Closeable {

	protected Analyzer() {
		super();
		assert assertFinal();
	}

	private boolean assertFinal() {
		try {
			final Class<?> clazz = getClass();
			if (!clazz.desiredAssertionStatus())
				return true;
			assert clazz.isAnonymousClass()
					|| (clazz.getModifiers() & (Modifier.FINAL | Modifier.PRIVATE)) != 0
					|| (Modifier.isFinal(clazz.getMethod("tokenStream",
							String.class, Reader.class).getModifiers()) && Modifier
							.isFinal(clazz.getMethod("reusableTokenStream",
									String.class, Reader.class).getModifiers())) : "Analyzer implementation classes or at least their tokenStream() and reusableTokenStream() implementations must be final";
			return true;
		} catch (NoSuchMethodException nsme) {
			return false;
		}
	}

	public abstract TokenStream tokenStream(String fieldName, Reader reader);

	public TokenStream reusableTokenStream(String fieldName, Reader reader)
			throws IOException {
		return tokenStream(fieldName, reader);
	}

	private CloseableThreadLocal<Object> tokenStreams = new CloseableThreadLocal<Object>();

	protected Object getPreviousTokenStream() {
		try {
			return tokenStreams.get();
		} catch (NullPointerException npe) {
			if (tokenStreams == null) {
				throw new AlreadyClosedException("this Analyzer is closed");
			} else {
				throw npe;
			}
		}
	}

	protected void setPreviousTokenStream(Object obj) {
		try {
			tokenStreams.set(obj);
		} catch (NullPointerException npe) {
			if (tokenStreams == null) {
				throw new AlreadyClosedException("this Analyzer is closed");
			} else {
				throw npe;
			}
		}
	}

	public int getPositionIncrementGap(String fieldName) {
		return 0;
	}

	public int getOffsetGap(Fieldable field) {
		if (field.isTokenized())
			return 1;
		else
			return 0;
	}

	public void close() {
		tokenStreams.close();
		tokenStreams = null;
	}
}
