/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ChecksumIndexOutput extends IndexOutput {
	IndexOutput main;
	Checksum digest;

	public ChecksumIndexOutput(IndexOutput main) {
		this.main = main;
		digest = new CRC32();
	}

	@Override
	public void writeByte(byte b) throws IOException {
		digest.update(b);
		main.writeByte(b);
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		digest.update(b, offset, length);
		main.writeBytes(b, offset, length);
	}

	public long getChecksum() {
		return digest.getValue();
	}

	@Override
	public void flush() throws IOException {
		main.flush();
	}

	@Override
	public void close() throws IOException {
		main.close();
	}

	@Override
	public long getFilePointer() {
		return main.getFilePointer();
	}

	@Override
	public void seek(long pos) {
		throw new RuntimeException("not allowed");
	}

	public void prepareCommit() throws IOException {
		final long checksum = getChecksum();

		final long pos = main.getFilePointer();
		main.writeLong(checksum - 1);
		main.flush();
		main.seek(pos);
	}

	public void finishCommit() throws IOException {
		main.writeLong(getChecksum());
	}

	@Override
	public long length() throws IOException {
		return main.length();
	}
}
