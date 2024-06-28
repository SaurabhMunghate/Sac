/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Counter {

	public abstract long addAndGet(long delta);

	public abstract long get();

	public static Counter newCounter() {
		return newCounter(false);
	}

	public static Counter newCounter(boolean threadSafe) {
		return threadSafe ? new AtomicCounter() : new SerialCounter();
	}

	private final static class SerialCounter extends Counter {
		private long count = 0;

		@Override
		public long addAndGet(long delta) {
			return count += delta;
		}

		@Override
		public long get() {
			return count;
		};
	}

	private final static class AtomicCounter extends Counter {
		private final AtomicLong count = new AtomicLong();

		@Override
		public long addAndGet(long delta) {
			return count.addAndGet(delta);
		}

		@Override
		public long get() {
			return count.get();
		}

	}
}
