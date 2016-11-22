/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;

final class SegmentNorms implements Cloneable {

	static final byte[] NORMS_HEADER = new byte[] { 'N', 'R', 'M', -1 };

	int refCount = 1;

	private SegmentNorms origNorm;

	private IndexInput in;
	private long normSeek;

	private AtomicInteger bytesRef;
	private byte[] bytes;
	private int number;

	boolean dirty;
	boolean rollbackDirty;

	private final SegmentReader owner;

	public SegmentNorms(IndexInput in, int number, long normSeek,
			SegmentReader owner) {
		this.in = in;
		this.number = number;
		this.normSeek = normSeek;
		this.owner = owner;
	}

	public synchronized void incRef() {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
		refCount++;
	}

	private void closeInput() throws IOException {
		if (in != null) {
			if (in != owner.singleNormStream) {

				in.close();
			} else {

				if (owner.singleNormRef.decrementAndGet() == 0) {
					owner.singleNormStream.close();
					owner.singleNormStream = null;
				}
			}

			in = null;
		}
	}

	public synchronized void decRef() throws IOException {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);

		if (--refCount == 0) {
			if (origNorm != null) {
				origNorm.decRef();
				origNorm = null;
			} else {
				closeInput();
			}

			if (bytes != null) {
				assert bytesRef != null;
				bytesRef.decrementAndGet();
				bytes = null;
				bytesRef = null;
			} else {
				assert bytesRef == null;
			}
		}
	}

	public synchronized void bytes(byte[] bytesOut, int offset, int len)
			throws IOException {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
		if (bytes != null) {

			assert len <= owner.maxDoc();
			System.arraycopy(bytes, 0, bytesOut, offset, len);
		} else {

			if (origNorm != null) {

				origNorm.bytes(bytesOut, offset, len);
			} else {

				synchronized (in) {
					in.seek(normSeek);
					in.readBytes(bytesOut, offset, len, false);
				}
			}
		}
	}

	public synchronized byte[] bytes() throws IOException {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
		if (bytes == null) {
			assert bytesRef == null;
			if (origNorm != null) {

				bytes = origNorm.bytes();
				bytesRef = origNorm.bytesRef;
				bytesRef.incrementAndGet();

				origNorm.decRef();
				origNorm = null;

			} else {

				final int count = owner.maxDoc();
				bytes = new byte[count];

				assert in != null;

				synchronized (in) {
					in.seek(normSeek);
					in.readBytes(bytes, 0, count, false);
				}

				bytesRef = new AtomicInteger(1);
				closeInput();
			}
		}

		return bytes;
	}

	AtomicInteger bytesRef() {
		return bytesRef;
	}

	public synchronized byte[] copyOnWrite() throws IOException {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);
		bytes();
		assert bytes != null;
		assert bytesRef != null;
		if (bytesRef.get() > 1) {

			assert refCount == 1;
			final AtomicInteger oldRef = bytesRef;
			bytes = owner.cloneNormBytes(bytes);
			bytesRef = new AtomicInteger(1);
			oldRef.decrementAndGet();
		}
		dirty = true;
		return bytes;
	}

	@Override
	public synchronized Object clone() {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0);

		SegmentNorms clone;
		try {
			clone = (SegmentNorms) super.clone();
		} catch (CloneNotSupportedException cnse) {

			throw new RuntimeException("unexpected CloneNotSupportedException",
					cnse);
		}
		clone.refCount = 1;

		if (bytes != null) {
			assert bytesRef != null;
			assert origNorm == null;

			clone.bytesRef.incrementAndGet();
		} else {
			assert bytesRef == null;
			if (origNorm == null) {

				clone.origNorm = this;
			}
			clone.origNorm.incRef();
		}

		clone.in = null;

		return clone;
	}

	public void reWrite(SegmentInfo si) throws IOException {
		assert refCount > 0 && (origNorm == null || origNorm.refCount > 0) : "refCount="
				+ refCount + " origNorm=" + origNorm;

		si.advanceNormGen(this.number);
		final String normFileName = si.getNormFileName(this.number);
		IndexOutput out = owner.directory().createOutput(normFileName);
		boolean success = false;
		try {
			try {
				out.writeBytes(SegmentNorms.NORMS_HEADER, 0,
						SegmentNorms.NORMS_HEADER.length);
				out.writeBytes(bytes, owner.maxDoc());
			} finally {
				out.close();
			}
			success = true;
		} finally {
			if (!success) {
				try {
					owner.directory().deleteFile(normFileName);
				} catch (Throwable t) {

				}
			}
		}
		this.dirty = false;
	}
}
