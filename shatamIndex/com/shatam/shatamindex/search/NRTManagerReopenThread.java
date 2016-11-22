/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.Closeable;
import java.io.IOException;

import com.shatam.shatamindex.util.ThreadInterruptedException;

public class NRTManagerReopenThread extends Thread implements
		NRTManager.WaitingListener, Closeable {

	private final NRTManager manager;
	private final long targetMaxStaleNS;
	private final long targetMinStaleNS;
	private boolean finish;
	private long waitingGen;
	private boolean waitingNeedsDeletes;

	public NRTManagerReopenThread(NRTManager manager, double targetMaxStaleSec,
			double targetMinStaleSec) {
		if (targetMaxStaleSec < targetMinStaleSec) {
			throw new IllegalArgumentException("targetMaxScaleSec (= "
					+ targetMaxStaleSec + ") < targetMinStaleSec (="
					+ targetMinStaleSec + ")");
		}
		this.manager = manager;
		this.targetMaxStaleNS = (long) (1000000000 * targetMaxStaleSec);
		this.targetMinStaleNS = (long) (1000000000 * targetMinStaleSec);
		manager.addWaitingListener(this);
	}

	public synchronized void close() {

		manager.removeWaitingListener(this);
		this.finish = true;
		notify();
		try {
			join();
		} catch (InterruptedException ie) {
			throw new ThreadInterruptedException(ie);
		}
	}

	public synchronized void waiting(boolean needsDeletes, long targetGen) {
		waitingNeedsDeletes |= needsDeletes;
		waitingGen = Math.max(waitingGen, targetGen);
		notify();

	}

	@Override
	public void run() {

		long lastReopenStartNS = System.nanoTime();

		try {
			while (true) {

				boolean hasWaiting = false;

				synchronized (this) {

					while (!finish) {

						hasWaiting = waitingGen > manager
								.getCurrentSearchingGen(waitingNeedsDeletes);
						final long nextReopenStartNS = lastReopenStartNS
								+ (hasWaiting ? targetMinStaleNS
										: targetMaxStaleNS);

						final long sleepNS = nextReopenStartNS
								- System.nanoTime();

						if (sleepNS > 0) {

							try {
								wait(sleepNS / 1000000,
										(int) (sleepNS % 1000000));
							} catch (InterruptedException ie) {
								Thread.currentThread().interrupt();

								finish = true;
								break;
							}
						} else {
							break;
						}
					}

					if (finish) {

						return;
					}

				}

				lastReopenStartNS = System.nanoTime();
				try {

					manager.maybeReopen(waitingNeedsDeletes);

				} catch (IOException ioe) {

					throw new RuntimeException(ioe);
				}
			}
		} catch (Throwable t) {

			throw new RuntimeException(t);
		}
	}
}
