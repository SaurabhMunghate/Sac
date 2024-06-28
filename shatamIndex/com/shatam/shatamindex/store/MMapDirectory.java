/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.store;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.lang.reflect.Method;

import com.shatam.shatamindex.util.Constants;

public class MMapDirectory extends FSDirectory {
	private boolean useUnmapHack = UNMAP_SUPPORTED;
	public static final int DEFAULT_MAX_BUFF = Constants.JRE_IS_64BIT ? (1 << 30)
			: (1 << 28);
	private int chunkSizePower;

	public MMapDirectory(File path, LockFactory lockFactory) throws IOException {
		super(path, lockFactory);
		setMaxChunkSize(DEFAULT_MAX_BUFF);
	}

	public MMapDirectory(File path) throws IOException {
		super(path, null);
		setMaxChunkSize(DEFAULT_MAX_BUFF);
	}

	public static final boolean UNMAP_SUPPORTED;
	static {
		boolean v;
		try {
			Class.forName("sun.misc.Cleaner");
			Class.forName("java.nio.DirectByteBuffer").getMethod("cleaner");
			v = true;
		} catch (Exception e) {
			v = false;
		}
		UNMAP_SUPPORTED = v;
	}

	public void setUseUnmap(final boolean useUnmapHack) {
		if (useUnmapHack && !UNMAP_SUPPORTED)
			throw new IllegalArgumentException(
					"Unmap hack not supported on this platform!");
		this.useUnmapHack = useUnmapHack;
	}

	public boolean getUseUnmap() {
		return useUnmapHack;
	}

	final void cleanMapping(final ByteBuffer buffer) throws IOException {
		if (useUnmapHack) {
			try {
				AccessController
						.doPrivileged(new PrivilegedExceptionAction<Object>() {
							public Object run() throws Exception {
								final Method getCleanerMethod = buffer
										.getClass().getMethod("cleaner");
								getCleanerMethod.setAccessible(true);
								final Object cleaner = getCleanerMethod
										.invoke(buffer);
								if (cleaner != null) {
									cleaner.getClass().getMethod("clean")
											.invoke(cleaner);
								}
								return null;
							}
						});
			} catch (PrivilegedActionException e) {
				final IOException ioe = new IOException(
						"unable to unmap the mapped buffer");
				ioe.initCause(e.getCause());
				throw ioe;
			}
		}
	}

	public final void setMaxChunkSize(final int maxChunkSize) {
		if (maxChunkSize <= 0)
			throw new IllegalArgumentException(
					"Maximum chunk size for mmap must be >0");

		this.chunkSizePower = 31 - Integer.numberOfLeadingZeros(maxChunkSize);
		assert this.chunkSizePower >= 0 && this.chunkSizePower <= 30;

	}

	public final int getMaxChunkSize() {
		return 1 << chunkSizePower;
	}

	@Override
	public IndexInput openInput(String name, int bufferSize) throws IOException {
		ensureOpen();
		File f = new File(getDirectory(), name);
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		try {
			return new MMapIndexInput("MMapIndexInput(path=\"" + f + "\")",
					raf, chunkSizePower);
		} finally {
			raf.close();
		}
	}

	private final class MMapIndexInput extends IndexInput {

		private ByteBuffer[] buffers;

		private final long length, chunkSizeMask, chunkSize;
		private final int chunkSizePower;

		private int curBufIndex;

		private ByteBuffer curBuf;

		private boolean isClone = false;

		MMapIndexInput(String resourceDescription, RandomAccessFile raf,
				int chunkSizePower) throws IOException {
			super(resourceDescription);
			this.length = raf.length();
			this.chunkSizePower = chunkSizePower;
			this.chunkSize = 1L << chunkSizePower;
			this.chunkSizeMask = chunkSize - 1L;

			if (chunkSizePower < 0 || chunkSizePower > 30)
				throw new IllegalArgumentException(
						"Invalid chunkSizePower used for ByteBuffer size: "
								+ chunkSizePower);

			if ((length >>> chunkSizePower) >= Integer.MAX_VALUE)
				throw new IllegalArgumentException(
						"RandomAccessFile too big for chunk size: "
								+ raf.toString());

			final int nrBuffers = (int) (length >>> chunkSizePower) + 1;

			this.buffers = new ByteBuffer[nrBuffers];

			long bufferStart = 0L;
			FileChannel rafc = raf.getChannel();
			for (int bufNr = 0; bufNr < nrBuffers; bufNr++) {
				int bufSize = (int) ((length > (bufferStart + chunkSize)) ? chunkSize
						: (length - bufferStart));
				this.buffers[bufNr] = rafc.map(MapMode.READ_ONLY, bufferStart,
						bufSize);
				bufferStart += bufSize;
			}
			seek(0L);
		}

		@Override
		public byte readByte() throws IOException {
			try {
				return curBuf.get();
			} catch (BufferUnderflowException e) {
				do {
					curBufIndex++;
					if (curBufIndex >= buffers.length) {
						throw new IOException("read past EOF: " + this);
					}
					curBuf = buffers[curBufIndex];
					curBuf.position(0);
				} while (!curBuf.hasRemaining());
				return curBuf.get();
			}
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) throws IOException {
			try {
				curBuf.get(b, offset, len);
			} catch (BufferUnderflowException e) {
				int curAvail = curBuf.remaining();
				while (len > curAvail) {
					curBuf.get(b, offset, curAvail);
					len -= curAvail;
					offset += curAvail;
					curBufIndex++;
					if (curBufIndex >= buffers.length) {
						throw new IOException("read past EOF: " + this);
					}
					curBuf = buffers[curBufIndex];
					curBuf.position(0);
					curAvail = curBuf.remaining();
				}
				curBuf.get(b, offset, len);
			}
		}

		@Override
		public int readInt() throws IOException {
			try {
				return curBuf.getInt();
			} catch (BufferUnderflowException e) {
				return super.readInt();
			}
		}

		@Override
		public long readLong() throws IOException {
			try {
				return curBuf.getLong();
			} catch (BufferUnderflowException e) {
				return super.readLong();
			}
		}

		@Override
		public long getFilePointer() {
			return (((long) curBufIndex) << chunkSizePower) + curBuf.position();
		}

		@Override
		public void seek(long pos) throws IOException {

			final int bi = (int) (pos >> chunkSizePower);
			try {
				final ByteBuffer b = buffers[bi];
				b.position((int) (pos & chunkSizeMask));

				this.curBufIndex = bi;
				this.curBuf = b;
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				if (pos < 0L) {
					throw new IllegalArgumentException(
							"Seeking to negative position: " + this);
				}
				throw new IOException("seek past EOF");
			} catch (IllegalArgumentException iae) {
				if (pos < 0L) {
					throw new IllegalArgumentException(
							"Seeking to negative position: " + this);
				}
				throw new IOException("seek past EOF: " + this);
			}
		}

		@Override
		public long length() {
			return length;
		}

		@Override
		public Object clone() {
			if (buffers == null) {
				throw new AlreadyClosedException(
						"MMapIndexInput already closed: " + this);
			}
			final MMapIndexInput clone = (MMapIndexInput) super.clone();
			clone.isClone = true;
			clone.buffers = new ByteBuffer[buffers.length];

			for (int bufNr = 0; bufNr < buffers.length; bufNr++) {
				clone.buffers[bufNr] = buffers[bufNr].duplicate();
			}
			try {
				clone.seek(getFilePointer());
			} catch (IOException ioe) {
				throw new RuntimeException("Should never happen: " + this, ioe);
			}
			return clone;
		}

		@Override
		public void close() throws IOException {
			try {
				if (isClone || buffers == null)
					return;
				for (int bufNr = 0; bufNr < buffers.length; bufNr++) {

					try {
						cleanMapping(buffers[bufNr]);
					} finally {
						buffers[bufNr] = null;
					}
				}
			} finally {
				buffers = null;
			}
		}
	}
}
