/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.index.TermPositions;

import java.io.IOException;
import java.util.Collections;
import java.util.Collection;

public class TermSpans extends Spans {
	protected TermPositions positions;
	protected Term term;
	protected int doc;
	protected int freq;
	protected int count;
	protected int position;

	public TermSpans(TermPositions positions, Term term) throws IOException {

		this.positions = positions;
		this.term = term;
		doc = -1;
	}

	@Override
	public boolean next() throws IOException {
		if (count == freq) {
			if (!positions.next()) {
				doc = Integer.MAX_VALUE;
				return false;
			}
			doc = positions.doc();
			freq = positions.freq();
			count = 0;
		}
		position = positions.nextPosition();
		count++;
		return true;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (!positions.skipTo(target)) {
			doc = Integer.MAX_VALUE;
			return false;
		}

		doc = positions.doc();
		freq = positions.freq();
		count = 0;

		position = positions.nextPosition();
		count++;

		return true;
	}

	@Override
	public int doc() {
		return doc;
	}

	@Override
	public int start() {
		return position;
	}

	@Override
	public int end() {
		return position + 1;
	}

	@Override
	public Collection<byte[]> getPayload() throws IOException {
		byte[] bytes = new byte[positions.getPayloadLength()];
		bytes = positions.getPayload(bytes, 0);
		return Collections.singletonList(bytes);
	}

	@Override
	public boolean isPayloadAvailable() {
		return positions.isPayloadAvailable();
	}

	@Override
	public String toString() {
		return "spans("
				+ term.toString()
				+ ")@"
				+ (doc == -1 ? "START" : (doc == Integer.MAX_VALUE) ? "END"
						: doc + "-" + position);
	}

	public TermPositions getPositions() {
		return positions;
	}
}
