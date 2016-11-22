/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CloseableThreadLocal<T> implements Closeable {

	private ThreadLocal<WeakReference<T>> t = new ThreadLocal<WeakReference<T>>();

	private Map<Thread, T> hardRefs = new HashMap<Thread, T>();

	protected T initialValue() {
		return null;
	}

	public T get() {
		WeakReference<T> weakRef = t.get();
		if (weakRef == null) {
			T iv = initialValue();
			if (iv != null) {
				set(iv);
				return iv;
			} else
				return null;
		} else {
			return weakRef.get();
		}
	}

	public void set(T object) {

		t.set(new WeakReference<T>(object));

		synchronized (hardRefs) {
			hardRefs.put(Thread.currentThread(), object);

			for (Iterator<Thread> it = hardRefs.keySet().iterator(); it
					.hasNext();) {
				final Thread t = it.next();
				if (!t.isAlive())
					it.remove();
			}
		}
	}

	public void close() {

		hardRefs = null;

		if (t != null) {
			t.remove();
		}
		t = null;
	}
}
