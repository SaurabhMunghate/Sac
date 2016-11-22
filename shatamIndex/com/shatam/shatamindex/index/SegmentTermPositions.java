/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.store.IndexInput;

import java.io.IOException;

final class SegmentTermPositions extends SegmentTermDocs implements
		TermPositions {
	private IndexInput proxStream;
	private int proxCount;
	private int position;

	private int payloadLength;

	private boolean needToLoadPayload;

	private long lazySkipPointer = -1;
	private int lazySkipProxCount = 0;

	SegmentTermPositions(SegmentReader p) {
		super(p);
		this.proxStream = null;
	}

	@Override
	final void seek(TermInfo ti, Term term) throws IOException {
		super.seek(ti, term);
		if (ti != null)
			lazySkipPointer = ti.proxPointer;

		lazySkipProxCount = 0;
		proxCount = 0;
		payloadLength = 0;
		needToLoadPayload = false;
	}

	@Override
	public final void close() throws IOException {
		super.close();
		if (proxStream != null)
			proxStream.close();
	}

	public final int nextPosition() throws IOException {
		if (indexOptions != IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)

			return 0;

		lazySkip();
		proxCount--;
		return position += readDeltaPosition();
	}

	private final int readDeltaPosition() throws IOException {
		int delta = proxStream.readVInt();
		if (currentFieldStoresPayloads) {

			if ((delta & 1) != 0) {
				payloadLength = proxStream.readVInt();
			}
			delta >>>= 1;
			needToLoadPayload = true;
		}
		return delta;
	}

	@Override
	protected final void skippingDoc() throws IOException {

		lazySkipProxCount += freq;
	}

	@Override
	public final boolean next() throws IOException {

		lazySkipProxCount += proxCount;

		if (super.next()) {
			proxCount = freq;
			position = 0;
			return true;
		}
		return false;
	}

	@Override
	public final int read(final int[] docs, final int[] freqs) {
		throw new UnsupportedOperationException(
				"TermPositions does not support processing multiple documents in one call. Use TermDocs instead.");
	}

	@Override
	protected void skipProx(long proxPointer, int payloadLength)
			throws IOException {

		lazySkipPointer = proxPointer;
		lazySkipProxCount = 0;
		proxCount = 0;
		this.payloadLength = payloadLength;
		needToLoadPayload = false;
	}

	private void skipPositions(int n) throws IOException {
		assert indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
		for (int f = n; f > 0; f--) {
			readDeltaPosition();
			skipPayload();
		}
	}

	private void skipPayload() throws IOException {
		if (needToLoadPayload && payloadLength > 0) {
			proxStream.seek(proxStream.getFilePointer() + payloadLength);
		}
		needToLoadPayload = false;
	}

	private void lazySkip() throws IOException {
		if (proxStream == null) {

			proxStream = (IndexInput) parent.core.proxStream.clone();
		}

		skipPayload();

		if (lazySkipPointer != -1) {
			proxStream.seek(lazySkipPointer);
			lazySkipPointer = -1;
		}

		if (lazySkipProxCount != 0) {
			skipPositions(lazySkipProxCount);
			lazySkipProxCount = 0;
		}
	}

	public int getPayloadLength() {
		return payloadLength;
	}

	public byte[] getPayload(byte[] data, int offset) throws IOException {
		if (!needToLoadPayload) {
			throw new IOException(
					"Either no payload exists at this term position or an attempt was made to load it more than once.");
		}

		byte[] retArray;
		int retOffset;
		if (data == null || data.length - offset < payloadLength) {

			retArray = new byte[payloadLength];
			retOffset = 0;
		} else {
			retArray = data;
			retOffset = offset;
		}
		proxStream.readBytes(retArray, retOffset, payloadLength);
		needToLoadPayload = false;
		return retArray;
	}

	public boolean isPayloadAvailable() {
		return needToLoadPayload && payloadLength > 0;
	}

}
