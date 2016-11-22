/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.util.PriorityQueue;

final class HitQueue extends PriorityQueue<ScoreDoc> {

	private boolean prePopulate;

	HitQueue(int size, boolean prePopulate) {
		this.prePopulate = prePopulate;
		initialize(size);
	}

	@Override
	protected ScoreDoc getSentinelObject() {

		return !prePopulate ? null : new ScoreDoc(Integer.MAX_VALUE,
				Float.NEGATIVE_INFINITY);
	}

	@Override
	protected final boolean lessThan(ScoreDoc hitA, ScoreDoc hitB) {
		if (hitA.score == hitB.score)
			return hitA.doc > hitB.doc;
		else
			return hitA.score < hitB.score;
	}
}
