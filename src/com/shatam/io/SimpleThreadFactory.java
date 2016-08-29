package com.shatam.io;

import java.util.concurrent.ThreadFactory;

public class SimpleThreadFactory implements ThreadFactory {
	   public  Thread newThread(Runnable r) {
	     return new Thread(r);
	   }
	 }