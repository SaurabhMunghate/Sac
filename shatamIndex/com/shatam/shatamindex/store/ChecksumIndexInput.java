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

public class ChecksumIndexInput extends IndexInput {
	IndexInput main;
	Checksum digest;

	public ChecksumIndexInput(IndexInput main) {
		super("ChecksumIndexInput(" + main + ")");
		this.main = main;
		digest = new CRC32();
	}

	@Override
	public byte readByte() throws IOException {
		final byte b = main.readByte();
		digest.update(b);
		return b;
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		main.readBytes(b, offset, len);
		digest.update(b, offset, len);
	}

	public long getChecksum() {
		return digest.getValue();
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

	@Override
	public long length() {
		return main.length();
	}
}
