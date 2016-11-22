/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;


import java.io.IOException;


public final class NoMergeScheduler extends MergeScheduler {

  
  public static final MergeScheduler INSTANCE = new NoMergeScheduler();

  private NoMergeScheduler() {
    
  }

  @Override
  public void close() {}

  @Override
	public void merge(IndexWriter writer) throws CorruptIndexException,
			IOException {
	}
}
