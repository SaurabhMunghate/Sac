/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.io;

import java.util.concurrent.ThreadFactory;

public class SimpleThreadFactory implements ThreadFactory {
	   public  Thread newThread(Runnable r) {
	     return new Thread(r);
	   }
	 }