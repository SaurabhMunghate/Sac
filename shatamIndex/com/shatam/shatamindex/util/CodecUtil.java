/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;

import com.shatam.shatamindex.index.CorruptIndexException;
import com.shatam.shatamindex.index.IndexFormatTooNewException;
import com.shatam.shatamindex.index.IndexFormatTooOldException;
import com.shatam.shatamindex.store.DataInput;
import com.shatam.shatamindex.store.DataOutput;

public final class CodecUtil {
	private CodecUtil() {
	}

	private final static int CODEC_MAGIC = 0x3fd76c17;

	public static DataOutput writeHeader(DataOutput out, String codec,
			int version) throws IOException {
		BytesRef bytes = new BytesRef(codec);
		if (bytes.length != codec.length() || bytes.length >= 128) {
			throw new IllegalArgumentException(
					"codec must be simple ASCII, less than 128 characters in length [got "
							+ codec + "]");
		}
		out.writeInt(CODEC_MAGIC);
		out.writeString(codec);
		out.writeInt(version);

		return out;
	}

	public static int headerLength(String codec) {
		return 9 + codec.length();
	}

	public static int checkHeader(DataInput in, String codec, int minVersion,
			int maxVersion) throws IOException {

		final int actualHeader = in.readInt();
		if (actualHeader != CODEC_MAGIC) {
			throw new CorruptIndexException(
					"codec header mismatch: actual header=" + actualHeader
							+ " vs expected header=" + CODEC_MAGIC
							+ " (resource: " + in + ")");
		}

		final String actualCodec = in.readString();
		if (!actualCodec.equals(codec)) {
			throw new CorruptIndexException("codec mismatch: actual codec="
					+ actualCodec + " vs expected codec=" + codec
					+ " (resource: " + in + ")");
		}

		final int actualVersion = in.readInt();
		if (actualVersion < minVersion) {
			throw new IndexFormatTooOldException(in, actualVersion, minVersion,
					maxVersion);
		}
		if (actualVersion > maxVersion) {
			throw new IndexFormatTooNewException(in, actualVersion, minVersion,
					maxVersion);
		}

		return actualVersion;
	}
}
