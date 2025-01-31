/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public final class IOUtils {

	public static final String UTF_8 = "UTF-8";

	public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

	private IOUtils() {
	}

	public static <E extends Exception> void closeWhileHandlingException(
			E priorException, Closeable... objects) throws E, IOException {
		Throwable th = null;

		for (Closeable object : objects) {
			try {
				if (object != null) {
					object.close();
				}
			} catch (Throwable t) {
				addSuppressed((priorException == null) ? th : priorException, t);
				if (th == null) {
					th = t;
				}
			}
		}

		if (priorException != null) {
			throw priorException;
		} else if (th != null) {
			if (th instanceof IOException)
				throw (IOException) th;
			if (th instanceof RuntimeException)
				throw (RuntimeException) th;
			if (th instanceof Error)
				throw (Error) th;
			throw new RuntimeException(th);
		}
	}

	public static <E extends Exception> void closeWhileHandlingException(
			E priorException, Iterable<Closeable> objects) throws E,
			IOException {
		Throwable th = null;

		for (Closeable object : objects) {
			try {
				if (object != null) {
					object.close();
				}
			} catch (Throwable t) {
				addSuppressed((priorException == null) ? th : priorException, t);
				if (th == null) {
					th = t;
				}
			}
		}

		if (priorException != null) {
			throw priorException;
		} else if (th != null) {
			if (th instanceof IOException)
				throw (IOException) th;
			if (th instanceof RuntimeException)
				throw (RuntimeException) th;
			if (th instanceof Error)
				throw (Error) th;
			throw new RuntimeException(th);
		}
	}

	public static void close(Closeable... objects) throws IOException {
		Throwable th = null;

		for (Closeable object : objects) {
			try {
				if (object != null) {
					object.close();
				}
			} catch (Throwable t) {
				addSuppressed(th, t);
				if (th == null) {
					th = t;
				}
			}
		}

		if (th != null) {
			if (th instanceof IOException)
				throw (IOException) th;
			if (th instanceof RuntimeException)
				throw (RuntimeException) th;
			if (th instanceof Error)
				throw (Error) th;
			throw new RuntimeException(th);
		}
	}

	public static void close(Iterable<? extends Closeable> objects)
			throws IOException {
		Throwable th = null;

		for (Closeable object : objects) {
			try {
				if (object != null) {
					object.close();
				}
			} catch (Throwable t) {
				addSuppressed(th, t);
				if (th == null) {
					th = t;
				}
			}
		}

		if (th != null) {
			if (th instanceof IOException)
				throw (IOException) th;
			if (th instanceof RuntimeException)
				throw (RuntimeException) th;
			if (th instanceof Error)
				throw (Error) th;
			throw new RuntimeException(th);
		}
	}

	public static void closeWhileHandlingException(Closeable... objects)
			throws IOException {
		for (Closeable object : objects) {
			try {
				if (object != null) {
					object.close();
				}
			} catch (Throwable t) {
			}
		}
	}

	public static void closeWhileHandlingException(
			Iterable<? extends Closeable> objects) throws IOException {
		for (Closeable object : objects) {
			try {
				if (object != null) {
					object.close();
				}
			} catch (Throwable t) {
			}
		}
	}

	private static final Method SUPPRESS_METHOD;
	static {
		Method m;
		try {
			m = Throwable.class.getMethod("addSuppressed", Throwable.class);
		} catch (Exception e) {
			m = null;
		}
		SUPPRESS_METHOD = m;
	}

	private static final void addSuppressed(Throwable exception,
			Throwable suppressed) {
		if (SUPPRESS_METHOD != null && exception != null && suppressed != null) {
			try {
				SUPPRESS_METHOD.invoke(exception, suppressed);
			} catch (Exception e) {

			}
		}
	}

	public static Reader getDecodingReader(InputStream stream, Charset charSet) {
		final CharsetDecoder charSetDecoder = charSet.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		return new BufferedReader(new InputStreamReader(stream, charSetDecoder));
	}

	public static Reader getDecodingReader(File file, Charset charSet)
			throws IOException {
		FileInputStream stream = null;
		boolean success = false;
		try {
			stream = new FileInputStream(file);
			final Reader reader = getDecodingReader(stream, charSet);
			success = true;
			return reader;

		} finally {
			if (!success) {
				IOUtils.close(stream);
			}
		}
	}

	public static Reader getDecodingReader(Class<?> clazz, String resource,
			Charset charSet) throws IOException {
		InputStream stream = null;
		boolean success = false;
		try {
			stream = clazz.getResourceAsStream(resource);
			final Reader reader = getDecodingReader(stream, charSet);
			success = true;
			return reader;
		} finally {
			if (!success) {
				IOUtils.close(stream);
			}
		}
	}

}
