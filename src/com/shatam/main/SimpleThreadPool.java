/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleThreadPool {

	public static void main(String[] args) {
		int n = 10000;
		int start = n / 5;
		int end = (n * 2) / 5;
		ExecutorService executor = Executors.newFixedThreadPool(30);

		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");
	}

	public static void simpletrheadpoolCalling(
			ArrayList<InputJsonSchema> array, String noOfJobs) {
		int threadJobStart[] = new int[Integer.parseInt(noOfJobs)];
		int threadJobEnd[] = new int[Integer.parseInt(noOfJobs)];
		int noOfThread = 0;
		int numOfjobs = Integer.parseInt(noOfJobs);
		if (numOfjobs < 10) {
			noOfThread = 3;
		} else {
			noOfThread = numOfjobs / 2;
		}
		for (int i = 0; i < Integer.parseInt(noOfJobs); i++) {
			threadJobStart[i] = array.size() * i / Integer.parseInt(noOfJobs);
			threadJobEnd[i] = (array.size() * (i + 1))
					/ Integer.parseInt(noOfJobs);
		}
		ExecutorService executor = Executors.newFixedThreadPool(5);
		int size = threadJobEnd.length;
		for (int i = 0; i < size; i++) {
			Runnable worker = new WorkerThread(threadJobStart[i],
					threadJobEnd[i]);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");
	}
}
