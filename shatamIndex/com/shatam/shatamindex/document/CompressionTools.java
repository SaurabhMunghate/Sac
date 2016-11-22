/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import java.io.ByteArrayOutputStream;

import com.shatam.shatamindex.util.UnicodeUtil;

public class CompressionTools {

	private CompressionTools() {
	}

	public static byte[] compress(byte[] value, int offset, int length,
			int compressionLevel) {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);

		Deflater compressor = new Deflater();

		try {
			compressor.setLevel(compressionLevel);
			compressor.setInput(value, offset, length);
			compressor.finish();

			final byte[] buf = new byte[1024];
			while (!compressor.finished()) {
				int count = compressor.deflate(buf);
				bos.write(buf, 0, count);
			}
		} finally {
			compressor.end();
		}

		return bos.toByteArray();
	}

	public static byte[] compress(byte[] value, int offset, int length) {
		return compress(value, offset, length, Deflater.BEST_COMPRESSION);
	}

	public static byte[] compress(byte[] value) {
		return compress(value, 0, value.length, Deflater.BEST_COMPRESSION);
	}

	public static byte[] compressString(String value) {
		return compressString(value, Deflater.BEST_COMPRESSION);
	}

	public static byte[] compressString(String value, int compressionLevel) {
		UnicodeUtil.UTF8Result result = new UnicodeUtil.UTF8Result();
		UnicodeUtil.UTF16toUTF8(value, 0, value.length(), result);
		return compress(result.result, 0, result.length, compressionLevel);
	}

	public static byte[] decompress(byte[] value) throws DataFormatException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(value.length);

		Inflater decompressor = new Inflater();

		try {
			decompressor.setInput(value);

			final byte[] buf = new byte[1024];
			while (!decompressor.finished()) {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			}
		} finally {
			decompressor.end();
		}

		return bos.toByteArray();
	}

	public static String decompressString(byte[] value)
			throws DataFormatException {
		UnicodeUtil.UTF16Result result = new UnicodeUtil.UTF16Result();
		final byte[] bytes = decompress(value);
		UnicodeUtil.UTF8toUTF16(bytes, 0, bytes.length, result);
		return new String(result.result, 0, result.length);
	}
}
