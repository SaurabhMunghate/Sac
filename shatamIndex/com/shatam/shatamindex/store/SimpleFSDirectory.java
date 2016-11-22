/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SimpleFSDirectory extends FSDirectory {

	public SimpleFSDirectory(File path, LockFactory lockFactory)
			throws IOException {
		super(path, lockFactory);
	}

	public SimpleFSDirectory(File path) throws IOException {
		super(path, null);
	}

	@Override
	public IndexInput openInput(String name, int bufferSize) throws IOException {
		ensureOpen();
		final File path = new File(directory, name);
		return new SimpleFSIndexInput("SimpleFSIndexInput(path=\""
				+ path.getPath() + "\")", path, bufferSize, getReadChunkSize());
	}

	protected static class SimpleFSIndexInput extends BufferedIndexInput {

		protected static class Descriptor extends RandomAccessFile {

			protected volatile boolean isOpen;
			long position;
			final long length;

			public Descriptor(File file, String mode) throws IOException {
				super(file, mode);
				isOpen = true;
				length = length();
			}

			@Override
			public void close() throws IOException {
				if (isOpen) {
					isOpen = false;
					super.close();
				}
			}
		}

		protected final Descriptor file;
		boolean isClone;

		protected final int chunkSize;

		@Deprecated
		public SimpleFSIndexInput(File path, int bufferSize, int chunkSize)
				throws IOException {
			this("anonymous SimpleFSIndexInput", path, bufferSize, chunkSize);
		}

		public SimpleFSIndexInput(String resourceDesc, File path,
				int bufferSize, int chunkSize) throws IOException {
			super(resourceDesc, bufferSize);
			file = new Descriptor(path, "r");
			this.chunkSize = chunkSize;
		}

		@Override
		protected void readInternal(byte[] b, int offset, int len)
				throws IOException {
			synchronized (file) {
				long position = getFilePointer();
				if (position != file.position) {
					file.seek(position);
					file.position = position;
				}
				int total = 0;

				try {
					do {
						final int readLength;
						if (total + chunkSize > len) {
							readLength = len - total;
						} else {
							//
							readLength = chunkSize;
						}
						final int i = file.read(b, offset + total, readLength);
						if (i == -1) {
							throw new IOException("read past EOF");
						}
						file.position += i;
						total += i;
					} while (total < len);
				} catch (OutOfMemoryError e) {

					final OutOfMemoryError outOfMemoryError = new OutOfMemoryError(
							"OutOfMemoryError likely caused by the Sun VM Bug described in "
									+ " try calling FSDirectory.setReadChunkSize "
									+ "with a value smaller than the current chunk size ("
									+ chunkSize + ")");
					outOfMemoryError.initCause(e);
					throw outOfMemoryError;
				} catch (IOException ioe) {
					IOException newIOE = new IOException(ioe.getMessage()
							+ ": " + this);
					newIOE.initCause(ioe);
					throw newIOE;
				}
			}
		}

		@Override
		public void close() throws IOException {

			if (!isClone)
				file.close();
		}

		@Override
		protected void seekInternal(long position) {
		}

		@Override
		public long length() {
			return file.length;
		}

		@Override
		public Object clone() {
			SimpleFSIndexInput clone = (SimpleFSIndexInput) super.clone();
			clone.isClone = true;
			return clone;
		}

		boolean isFDValid() throws IOException {
			return file.getFD().valid();
		}

		@Override
		public void copyBytes(IndexOutput out, long numBytes)
				throws IOException {
			numBytes -= flushBuffer(out, numBytes);

			out.copyBytes(this, numBytes);
		}
	}
}
