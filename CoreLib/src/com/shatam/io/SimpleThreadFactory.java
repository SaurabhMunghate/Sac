package com.shatam.io;

import java.util.concurrent.ThreadFactory;

class SimpleThreadFactory implements ThreadFactory {
	   public  Thread newThread(Runnable r) {
	     return new Thread(r);
	   }
	 }