/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

import com.shatam.shatamindex.index.*;

final class PhrasePositions {
	int doc;
	int position;
	int count;
	int offset;
	final int ord;
	TermPositions tp;
	PhrasePositions next;
	PhrasePositions nextRepeating;

	PhrasePositions(TermPositions t, int o, int ord) {
		tp = t;
		offset = o;
		this.ord = ord;
	}

	final boolean next() throws IOException {
		if (!tp.next()) {
			tp.close();
			doc = Integer.MAX_VALUE;
			return false;
		}
		doc = tp.doc();
		position = 0;
		return true;
	}

	final boolean skipTo(int target) throws IOException {
		if (!tp.skipTo(target)) {
			tp.close();
			doc = Integer.MAX_VALUE;
			return false;
		}
		doc = tp.doc();
		position = 0;
		return true;
	}

	final void firstPosition() throws IOException {
		count = tp.freq();
		nextPosition();
	}

	final boolean nextPosition() throws IOException {
		if (count-- > 0) {
			position = tp.nextPosition() - offset;
			return true;
		} else
			return false;
	}

	@Override
	public String toString() {
		String s = "d:" + doc + " o:" + offset + " p:" + position + " c:"
				+ count;
		if (nextRepeating != null) {
			s += " rpt[ " + nextRepeating + " ]";
		}
		return s;
	}
}
