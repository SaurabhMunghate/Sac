/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

class TermInfo {

	int docFreq = 0;

	long freqPointer = 0;
	long proxPointer = 0;
	int skipOffset;

	TermInfo() {
	}

	TermInfo(int df, long fp, long pp) {
		docFreq = df;
		freqPointer = fp;
		proxPointer = pp;
	}

	TermInfo(TermInfo ti) {
		docFreq = ti.docFreq;
		freqPointer = ti.freqPointer;
		proxPointer = ti.proxPointer;
		skipOffset = ti.skipOffset;
	}

	final void set(int docFreq, long freqPointer, long proxPointer,
			int skipOffset) {
		this.docFreq = docFreq;
		this.freqPointer = freqPointer;
		this.proxPointer = proxPointer;
		this.skipOffset = skipOffset;
	}

	final void set(TermInfo ti) {
		docFreq = ti.docFreq;
		freqPointer = ti.freqPointer;
		proxPointer = ti.proxPointer;
		skipOffset = ti.skipOffset;
	}
}
