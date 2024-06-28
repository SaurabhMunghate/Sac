/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SetOnce<T> {

	public static final class AlreadySetException extends RuntimeException {
		public AlreadySetException() {
			super("The object cannot be set twice!");
		}
	}

	private volatile T obj = null;
	private final AtomicBoolean set;

	public SetOnce() {
		set = new AtomicBoolean(false);
	}

	public SetOnce(T obj) {
		this.obj = obj;
		set = new AtomicBoolean(true);
	}

	public final void set(T obj) {
		if (set.compareAndSet(false, true)) {
			this.obj = obj;
		} else {
			throw new AlreadySetException();
		}
	}

	public final T get() {
		return obj;
	}
}
