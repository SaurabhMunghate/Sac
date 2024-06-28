/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;

class ReqExclScorer extends Scorer {
	private Scorer reqScorer;
	private DocIdSetIterator exclDisi;
	private int doc = -1;

	public ReqExclScorer(Scorer reqScorer, DocIdSetIterator exclDisi) {
		super(reqScorer.weight);
		this.reqScorer = reqScorer;
		this.exclDisi = exclDisi;
	}

	@Override
	public int nextDoc() throws IOException {
		if (reqScorer == null) {
			return doc;
		}
		doc = reqScorer.nextDoc();
		if (doc == NO_MORE_DOCS) {
			reqScorer = null;
			return doc;
		}
		if (exclDisi == null) {
			return doc;
		}
		return doc = toNonExcluded();
	}

	private int toNonExcluded() throws IOException {
		int exclDoc = exclDisi.docID();
		int reqDoc = reqScorer.docID();
		do {
			if (reqDoc < exclDoc) {
				return reqDoc;
			} else if (reqDoc > exclDoc) {
				exclDoc = exclDisi.advance(reqDoc);
				if (exclDoc == NO_MORE_DOCS) {
					exclDisi = null;
					return reqDoc;
				}
				if (exclDoc > reqDoc) {
					return reqDoc;
				}
			}
		} while ((reqDoc = reqScorer.nextDoc()) != NO_MORE_DOCS);
		reqScorer = null;
		return NO_MORE_DOCS;
	}

	@Override
	public int docID() {
		return doc;
	}

	@Override
	public float score() throws IOException {
		return reqScorer.score();
	}

	@Override
	public int advance(int target) throws IOException {
		if (reqScorer == null) {
			return doc = NO_MORE_DOCS;
		}
		if (exclDisi == null) {
			return doc = reqScorer.advance(target);
		}
		if (reqScorer.advance(target) == NO_MORE_DOCS) {
			reqScorer = null;
			return doc = NO_MORE_DOCS;
		}
		return doc = toNonExcluded();
	}
}
