/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;


public class WorkerThread implements Runnable {

	private int start;
	private int end;

	ThreadedSAC threadedSAC = new ThreadedSAC();

	public WorkerThread(int s, int e) {
		this.start = s;
		this.end = e;

	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName()
				+ " Start. Command = " + end);

		System.out.println(Thread.currentThread().getName() + " End.");
	}

	private void processCommand() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "end";
	}
}