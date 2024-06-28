/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.util.Counter;
import com.shatam.shatamindex.util.ThreadInterruptedException;

public class TimeLimitingCollector extends Collector {

	@SuppressWarnings("serial")
	public static class TimeExceededException extends RuntimeException {
		private long timeAllowed;
		private long timeElapsed;
		private int lastDocCollected;

		private TimeExceededException(long timeAllowed, long timeElapsed,
				int lastDocCollected) {
			super("Elapsed time: " + timeElapsed
					+ "Exceeded allowed search time: " + timeAllowed + " ms.");
			this.timeAllowed = timeAllowed;
			this.timeElapsed = timeElapsed;
			this.lastDocCollected = lastDocCollected;
		}

		public long getTimeAllowed() {
			return timeAllowed;
		}

		public long getTimeElapsed() {
			return timeElapsed;
		}

		public int getLastDocCollected() {
			return lastDocCollected;
		}
	}

	private long t0 = Long.MIN_VALUE;
	private long timeout = Long.MIN_VALUE;
	private final Collector collector;
	private final Counter clock;
	private final long ticksAllowed;
	private boolean greedy = false;
	private int docBase;

	public TimeLimitingCollector(final Collector collector, Counter clock,
			final long ticksAllowed) {
		this.collector = collector;
		this.clock = clock;
		this.ticksAllowed = ticksAllowed;
	}

	public void setBaseline(long clockTime) {
		t0 = clockTime;
		timeout = t0 + ticksAllowed;
	}

	public void setBaseline() {
		setBaseline(clock.get());
	}

	public boolean isGreedy() {
		return greedy;
	}

	public void setGreedy(boolean greedy) {
		this.greedy = greedy;
	}

	@Override
	public void collect(final int doc) throws IOException {
		final long time = clock.get();
		if (timeout < time) {
			if (greedy) {

				collector.collect(doc);
			}

			throw new TimeExceededException(timeout - t0, time - t0, docBase
					+ doc);
		}

		collector.collect(doc);
	}

	@Override
	public void setNextReader(IndexReader reader, int base) throws IOException {
		collector.setNextReader(reader, base);
		this.docBase = base;
		if (Long.MIN_VALUE == t0) {
			setBaseline();
		}
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		collector.setScorer(scorer);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return collector.acceptsDocsOutOfOrder();
	}

	public static Counter getGlobalCounter() {
		return TimerThreadHolder.THREAD.counter;
	}

	public static TimerThread getGlobalTimerThread() {
		return TimerThreadHolder.THREAD;
	}

	private static final class TimerThreadHolder {
		static final TimerThread THREAD;
		static {
			THREAD = new TimerThread(Counter.newCounter(true));
			THREAD.start();
		}
	}

	public static final class TimerThread extends Thread {

		public static final String THREAD_NAME = "TimeLimitedCollector timer thread";
		public static final int DEFAULT_RESOLUTION = 20;

		private volatile long time = 0;
		private volatile boolean stop = false;
		private volatile long resolution;
		final Counter counter;

		public TimerThread(long resolution, Counter counter) {
			super(THREAD_NAME);
			this.resolution = resolution;
			this.counter = counter;
			this.setDaemon(true);
		}

		public TimerThread(Counter counter) {
			this(DEFAULT_RESOLUTION, counter);
		}

		@Override
		public void run() {
			while (!stop) {

				counter.addAndGet(resolution);
				try {
					Thread.sleep(resolution);
				} catch (InterruptedException ie) {
					throw new ThreadInterruptedException(ie);
				}
			}
		}

		public long getMilliseconds() {
			return time;
		}

		public void stopTimer() {
			stop = true;
		}

		public long getResolution() {
			return resolution;
		}

		public void setResolution(long resolution) {
			this.resolution = Math.max(resolution, 5);
		}
	}

}
