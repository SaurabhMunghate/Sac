package com.shatam.main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;

import com.shatam.io.AbstractIndexType;

public class SimpleThreadPool {

	public static void main(String[] args) {
		int n = 10000;
		int start = n / 5;
		int end = (n * 2) / 5;
		ExecutorService executor = Executors.newFixedThreadPool(30);
		// for (int i = 0; i < n; i++)
		// {

        //Runnable worker = new WorkerThread(start, end);
        //executor.execute(worker);

		// }
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");
	}

	public static void simpletrheadpoolCalling(
			ArrayList<InputJsonSchema> array, String noOfJobs) 
	/*public static JSONArray simpletrheadpoolCalling(
			ArrayList<InputJsonSchema> array,String hitScore,String maxResult, String noOfJobs)*/{
		int threadJobStart[] = new int[Integer.parseInt(noOfJobs)];
		int threadJobEnd[] = new int[Integer.parseInt(noOfJobs)];
		
		int noOfThread = 0;
		int numOfjobs = Integer.parseInt(noOfJobs);
		if (numOfjobs < 10) {
			noOfThread = 3;//Lets Have 3 As Number of default Threads 
		} 
		else 
		{
			noOfThread = numOfjobs / 2;// Integer.parseInt(noOfJobs)-10+1;
		}
		
		for (int i = 0; i < Integer.parseInt(noOfJobs); i++)
		{
			threadJobStart[i] = array.size() * i / Integer.parseInt(noOfJobs);
			threadJobEnd[i] = (array.size() * (i + 1))
					/ Integer.parseInt(noOfJobs);
		}
		
		// I need to do changes in the
		ExecutorService executor = Executors.newFixedThreadPool(5);
		int size=threadJobEnd.length;
		for (int i = 0; i < size; i++)
		{
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
