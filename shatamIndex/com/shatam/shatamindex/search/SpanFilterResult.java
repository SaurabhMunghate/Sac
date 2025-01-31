/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.util.ArrayList;

import java.util.List;

public class SpanFilterResult {
	private DocIdSet docIdSet;
	private List<PositionInfo> positions;

	public SpanFilterResult(DocIdSet docIdSet, List<PositionInfo> positions) {
		this.docIdSet = docIdSet;
		this.positions = positions;
	}

	public List<PositionInfo> getPositions() {
		return positions;
	}

	public DocIdSet getDocIdSet() {
		return docIdSet;
	}

	public static class PositionInfo {
		private int doc;
		private List<StartEnd> positions;

		public PositionInfo(int doc) {
			this.doc = doc;
			positions = new ArrayList<StartEnd>();
		}

		public void addPosition(int start, int end) {
			positions.add(new StartEnd(start, end));
		}

		public int getDoc() {
			return doc;
		}

		public List<StartEnd> getPositions() {
			return positions;
		}
	}

	public static class StartEnd {
		private int start;
		private int end;

		public StartEnd(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int getEnd() {
			return end;
		}

		public int getStart() {
			return start;
		}

	}
}
