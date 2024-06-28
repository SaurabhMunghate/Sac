/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import com.shatam.shatamindex.util.PriorityQueue;

final class PhraseQueue extends PriorityQueue<PhrasePositions> {
	PhraseQueue(int size) {
		initialize(size);
	}

	@Override
	protected final boolean lessThan(PhrasePositions pp1, PhrasePositions pp2) {
		if (pp1.doc == pp2.doc)
			if (pp1.position == pp2.position)

				if (pp1.offset == pp2.offset) {
					return pp1.ord < pp2.ord;
				} else {
					return pp1.offset < pp2.offset;
				}
			else {
				return pp1.position < pp2.position;
			}
		else {
			return pp1.doc < pp2.doc;
		}
	}
}
