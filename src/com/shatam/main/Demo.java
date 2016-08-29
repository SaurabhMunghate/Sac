package com.shatam.main;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import com.shatam.util.U;

public class Demo {
	
	static int a=1;
	static int b=2;
	static ArrayList<demo1>demoobj=new ArrayList<>();
 public static void main(String args[]) throws IOException, InterruptedException{
	 Runtime rt = Runtime.getRuntime();
     long totalMem = rt.totalMemory();
     long maxMem = rt.maxMemory();
     long freeMem = rt.freeMemory();
     double megs = 1048576.0;

     System.out.println ("Total Memory: " + totalMem + " (" + (totalMem/megs) + " MiB)");
     System.out.println ("Max Memory:   " + maxMem + " (" + (maxMem/megs) + " MiB)");
     System.out.println ("Free Memory:  " + freeMem + " (" + (freeMem/megs) + " MiB)");
	/* ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
	 ThreadGroup parentGroup;
	 while ( ( parentGroup = rootGroup.getParent() ) != null ) {
	     rootGroup = parentGroup;
	 }
	 Thread[] threads = new Thread[ rootGroup.activeCount() ];
	 while ( rootGroup.enumerate( threads, true ) == threads.length ) {
	     threads = new Thread[ threads.length * 2 ];
	 }
	 for(int i=0;i<threads.length;i++){
		U.log("::::"+threads[i].getName());
		U.log("***"+threads[i].currentThread().getName());
		
	 }*/
	/* int corePoolSize = 4;
		int maximumPoolSize = 8;
		int keepAliveTime = 5000;
		BlockingQueue<Runnable> threadPool = new LinkedBlockingQueue<Runnable>();

		ThreadPoolExecutor tpExecutor = new ThreadPoolExecutor(20, 2000, 0L, TimeUnit.MILLISECONDS, threadPool);

		tpExecutor.prestartAllCoreThreads();
		long s=System.currentTimeMillis();
		for(int i=0;i<=10000;i++){
		 
			  
			tpExecutor.submit(new Runnable() {
				public void run() {
			   
			    	 System.out.println(Thread.currentThread().getName());
			    	 int c=a+b;
			    	 int d=a-b;
			    	System.out.println("Kirti");
			    	
			    }
			    
			    
	 });

		 }
		 tpExecutor.shutdown();
		
	       tpExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		 long e=System.currentTimeMillis();
		System.out.println("time="+(e-s));*/
		
	 //createobkj();
	 /* for(int i=0;i<2;i++){
		 demo1 obj=demoobj.get(i);
		 System.out.println(demoobj.get(i));
		 System.out.println(obj.getA());
	 }*/
 }
 public static  void createobkj() throws IOException{
	 /* long s=System.currentTimeMillis();
		for(int i=0;i<=10000;i++){
		 
			  
			((ThreadPoolExecutor) executorService).setThreadFactory(new ThreadFactory() {
        		public Thread newThread(Runnable runnable) {
			    public void run() {
			    	 System.out.println(Thread.currentThread().getName());
			    	 int c=a+b;
			    	 int d=a-b;
			    	System.out.println("Kirti");
			    }
			    
			    
	 });

		 }
		 executorService.shutdownNow();
		 long e=System.currentTimeMillis();
		System.out.println("time="+(e-s));*/
		
	}
//	    System.out.println(System.getProperty("user.dir"));
//	    File file = new File("Kirti.text");
//	    BufferedWriter output = new BufferedWriter(new FileWriter(file));
//	    output.write("Kirti");
//	    output.close();
	/* for(int i=0;i<2;i++){
			
		 
		 demo1 demo=new demo1();
		 if(i==0)
		 demo.setA("Kirti");
	    
		
		if(i==1)
		 demo.setA("Tanu");
		 demoobj.add(demo);
		 
	 }*/
 //}

 }  
 
	
 
	 
 
 