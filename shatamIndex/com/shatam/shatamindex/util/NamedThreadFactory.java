/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
	private static final AtomicInteger threadPoolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private static final String NAME_PATTERN = "%s-%d-thread";
	private final String threadNamePrefix;

	public NamedThreadFactory(String threadNamePrefix) {
		final SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
				.getThreadGroup();
		this.threadNamePrefix = String.format(NAME_PATTERN,
				checkPrefix(threadNamePrefix),
				threadPoolNumber.getAndIncrement());
	}

	private static String checkPrefix(String prefix) {
		return prefix == null || prefix.length() == 0 ? "Shatam" : prefix;
	}

	public Thread newThread(Runnable r) {
		final Thread t = new Thread(group, r, String.format("%s-%d",
				this.threadNamePrefix, threadNumber.getAndIncrement()), 0);
		t.setDaemon(false);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}

}
