/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import com.shatam.shatamindex.util.ThreadInterruptedException;

@Deprecated
public class FilterManager {

	protected static FilterManager manager;

	protected static final int DEFAULT_CACHE_CLEAN_SIZE = 100;

	protected static final long DEFAULT_CACHE_SLEEP_TIME = 1000 * 60 * 10;

	protected Map<Integer, FilterItem> cache;

	protected int cacheCleanSize;

	protected long cleanSleepTime;

	protected FilterCleaner filterCleaner;

	public synchronized static FilterManager getInstance() {
		if (manager == null) {
			manager = new FilterManager();
		}
		return manager;
	}

	protected FilterManager() {
		cache = new HashMap<Integer, FilterItem>();
		cacheCleanSize = DEFAULT_CACHE_CLEAN_SIZE;
		cleanSleepTime = DEFAULT_CACHE_SLEEP_TIME;

		filterCleaner = new FilterCleaner();
		Thread fcThread = new Thread(filterCleaner);

		fcThread.setDaemon(true);
		fcThread.start();
	}

	public void setCacheSize(int cacheCleanSize) {
		this.cacheCleanSize = cacheCleanSize;
	}

	public void setCleanThreadSleepTime(long cleanSleepTime) {
		this.cleanSleepTime = cleanSleepTime;
	}

	public Filter getFilter(Filter filter) {
		synchronized (cache) {
			FilterItem fi = null;
			fi = cache.get(Integer.valueOf(filter.hashCode()));
			if (fi != null) {
				fi.timestamp = new Date().getTime();
				return fi.filter;
			}
			cache.put(Integer.valueOf(filter.hashCode()),
					new FilterItem(filter));
			return filter;
		}
	}

	protected class FilterItem {
		public Filter filter;
		public long timestamp;

		public FilterItem(Filter filter) {
			this.filter = filter;
			this.timestamp = new Date().getTime();
		}
	}

	protected class FilterCleaner implements Runnable {

		private boolean running = true;
		private TreeSet<Map.Entry<Integer, FilterItem>> sortedFilterItems;

		public FilterCleaner() {
			sortedFilterItems = new TreeSet<Map.Entry<Integer, FilterItem>>(
					new Comparator<Map.Entry<Integer, FilterItem>>() {
						public int compare(Map.Entry<Integer, FilterItem> a,
								Map.Entry<Integer, FilterItem> b) {
							FilterItem fia = a.getValue();
							FilterItem fib = b.getValue();
							if (fia.timestamp == fib.timestamp) {
								return 0;
							}

							if (fia.timestamp < fib.timestamp) {
								return -1;
							}

							return 1;

						}
					});
		}

		public void run() {
			while (running) {

				if (cache.size() > cacheCleanSize) {

					sortedFilterItems.clear();
					synchronized (cache) {
						sortedFilterItems.addAll(cache.entrySet());
						Iterator<Map.Entry<Integer, FilterItem>> it = sortedFilterItems
								.iterator();
						int numToDelete = (int) ((cache.size() - cacheCleanSize) * 1.5);
						int counter = 0;

						while (it.hasNext() && counter++ < numToDelete) {
							Map.Entry<Integer, FilterItem> entry = it.next();
							cache.remove(entry.getKey());
						}
					}

					sortedFilterItems.clear();
				}

				try {
					Thread.sleep(cleanSleepTime);
				} catch (InterruptedException ie) {
					throw new ThreadInterruptedException(ie);
				}
			}
		}
	}
}
