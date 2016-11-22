/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;
import java.io.Reader;

public abstract class CharacterUtils {
	private static final Java4CharacterUtils JAVA_4 = new Java4CharacterUtils();
	private static final Java5CharacterUtils JAVA_5 = new Java5CharacterUtils();

	public static CharacterUtils getInstance(final Version matchVersion) {
		return matchVersion.onOrAfter(Version.SHATAM_31) ? JAVA_5 : JAVA_4;
	}

	public abstract int codePointAt(final char[] chars, final int offset);

	public abstract int codePointAt(final CharSequence seq, final int offset);

	public abstract int codePointAt(final char[] chars, final int offset,
			final int limit);

	public static CharacterBuffer newCharacterBuffer(final int bufferSize) {
		if (bufferSize < 2)
			throw new IllegalArgumentException("buffersize must be >= 2");
		return new CharacterBuffer(new char[bufferSize], 0, 0);
	}

	public abstract boolean fill(CharacterBuffer buffer, Reader reader)
			throws IOException;

	private static final class Java5CharacterUtils extends CharacterUtils {
		Java5CharacterUtils() {
		}

		@Override
		public final int codePointAt(final char[] chars, final int offset) {
			return Character.codePointAt(chars, offset);
		}

		@Override
		public int codePointAt(final CharSequence seq, final int offset) {
			return Character.codePointAt(seq, offset);
		}

		@Override
		public int codePointAt(final char[] chars, final int offset,
				final int limit) {
			return Character.codePointAt(chars, offset, limit);
		}

		@Override
		public boolean fill(final CharacterBuffer buffer, final Reader reader)
				throws IOException {
			final char[] charBuffer = buffer.buffer;
			buffer.offset = 0;
			charBuffer[0] = buffer.lastTrailingHighSurrogate;
			final int offset = buffer.lastTrailingHighSurrogate == 0 ? 0 : 1;
			buffer.lastTrailingHighSurrogate = 0;
			final int read = reader.read(charBuffer, offset, charBuffer.length
					- offset);
			if (read == -1) {
				buffer.length = offset;
				return offset != 0;
			}
			buffer.length = read + offset;

			if (buffer.length > 1
					&& Character.isHighSurrogate(charBuffer[buffer.length - 1])) {
				buffer.lastTrailingHighSurrogate = charBuffer[--buffer.length];
			}
			return true;
		}
	}

	private static final class Java4CharacterUtils extends CharacterUtils {
		Java4CharacterUtils() {
		}

		@Override
		public final int codePointAt(final char[] chars, final int offset) {
			return chars[offset];
		}

		@Override
		public int codePointAt(final CharSequence seq, final int offset) {
			return seq.charAt(offset);
		}

		@Override
		public int codePointAt(final char[] chars, final int offset,
				final int limit) {
			if (offset >= limit)
				throw new IndexOutOfBoundsException(
						"offset must be less than limit");
			return chars[offset];
		}

		@Override
		public boolean fill(final CharacterBuffer buffer, final Reader reader)
				throws IOException {
			buffer.offset = 0;
			final int read = reader.read(buffer.buffer);
			if (read == -1)
				return false;
			buffer.length = read;
			return true;
		}

	}

	public static final class CharacterBuffer {

		private final char[] buffer;
		private int offset;
		private int length;
		private char lastTrailingHighSurrogate = 0;

		CharacterBuffer(char[] buffer, int offset, int length) {
			this.buffer = buffer;
			this.offset = offset;
			this.length = length;
		}

		public char[] getBuffer() {
			return buffer;
		}

		public int getOffset() {
			return offset;
		}

		public int getLength() {
			return length;
		}

		public void reset() {
			offset = 0;
			length = 0;
			lastTrailingHighSurrogate = 0;
		}
	}

}
