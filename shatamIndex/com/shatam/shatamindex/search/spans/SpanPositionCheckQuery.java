/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;

import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public abstract class SpanPositionCheckQuery extends SpanQuery implements
		Cloneable {
	protected SpanQuery match;

	public SpanPositionCheckQuery(SpanQuery match) {
		this.match = match;
	}

	public SpanQuery getMatch() {
		return match;
	}

	@Override
	public String getField() {
		return match.getField();
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		match.extractTerms(terms);
	}

	protected static enum AcceptStatus {
		YES, NO, NO_AND_ADVANCE
	};

	protected abstract AcceptStatus acceptPosition(Spans spans)
			throws IOException;

	@Override
	public Spans getSpans(final IndexReader reader) throws IOException {
		return new PositionCheckSpan(reader);
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		SpanPositionCheckQuery clone = null;

		SpanQuery rewritten = (SpanQuery) match.rewrite(reader);
		if (rewritten != match) {
			clone = (SpanPositionCheckQuery) this.clone();
			clone.match = rewritten;
		}

		if (clone != null) {
			return clone;
		} else {
			return this;
		}
	}

	protected class PositionCheckSpan extends Spans {
		private Spans spans;

		public PositionCheckSpan(IndexReader reader) throws IOException {
			spans = match.getSpans(reader);
		}

		@Override
		public boolean next() throws IOException {
			if (!spans.next())
				return false;

			return doNext();
		}

		@Override
		public boolean skipTo(int target) throws IOException {
			if (!spans.skipTo(target))
				return false;

			return doNext();
		}

		protected boolean doNext() throws IOException {
			for (;;) {
				switch (acceptPosition(this)) {
				case YES:
					return true;
				case NO:
					if (!spans.next())
						return false;
					break;
				case NO_AND_ADVANCE:
					if (!spans.skipTo(spans.doc() + 1))
						return false;
					break;
				}
			}
		}

		@Override
		public int doc() {
			return spans.doc();
		}

		@Override
		public int start() {
			return spans.start();
		}

		@Override
		public int end() {
			return spans.end();
		}

		@Override
		public Collection<byte[]> getPayload() throws IOException {
			ArrayList<byte[]> result = null;
			if (spans.isPayloadAvailable()) {
				result = new ArrayList<byte[]>(spans.getPayload());
			}
			return result;
		}

		@Override
		public boolean isPayloadAvailable() {
			return spans.isPayloadAvailable();
		}

		@Override
		public String toString() {
			return "spans(" + SpanPositionCheckQuery.this.toString() + ")";
		}

	}
}