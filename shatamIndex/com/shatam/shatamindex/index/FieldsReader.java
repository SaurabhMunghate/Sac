/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.zip.DataFormatException;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.document.AbstractField;
import com.shatam.shatamindex.document.CompressionTools;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.Field;
import com.shatam.shatamindex.document.FieldSelector;
import com.shatam.shatamindex.document.FieldSelectorResult;
import com.shatam.shatamindex.document.Fieldable;
import com.shatam.shatamindex.document.NumericField;
import com.shatam.shatamindex.store.AlreadyClosedException;
import com.shatam.shatamindex.store.BufferedIndexInput;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.CloseableThreadLocal;
import com.shatam.shatamindex.util.IOUtils;

final class FieldsReader implements Cloneable, Closeable {
	private final FieldInfos fieldInfos;

	private final IndexInput cloneableFieldsStream;

	private final IndexInput fieldsStream;

	private final IndexInput cloneableIndexStream;
	private final IndexInput indexStream;
	private int numTotalDocs;
	private int size;
	private boolean closed;
	private final int format;
	private final int formatSize;

	private int docStoreOffset;

	private CloseableThreadLocal<IndexInput> fieldsStreamTL = new CloseableThreadLocal<IndexInput>();
	private boolean isOriginal = false;

	@Override
	public Object clone() {
		ensureOpen();
		return new FieldsReader(fieldInfos, numTotalDocs, size, format,
				formatSize, docStoreOffset, cloneableFieldsStream,
				cloneableIndexStream);
	}

	static String detectCodeVersion(Directory dir, String segment)
			throws IOException {
		IndexInput idxStream = dir.openInput(IndexFileNames.segmentFileName(
				segment, IndexFileNames.FIELDS_INDEX_EXTENSION), 1024);
		try {
			int format = idxStream.readInt();
			if (format < FieldsWriter.FORMAT_SHATAM_3_0_NO_COMPRESSED_FIELDS) {
				return "2.x";
			} else {
				return "3.0";
			}
		} finally {
			idxStream.close();
		}
	}

	private FieldsReader(FieldInfos fieldInfos, int numTotalDocs, int size,
			int format, int formatSize, int docStoreOffset,
			IndexInput cloneableFieldsStream, IndexInput cloneableIndexStream) {
		this.fieldInfos = fieldInfos;
		this.numTotalDocs = numTotalDocs;
		this.size = size;
		this.format = format;
		this.formatSize = formatSize;
		this.docStoreOffset = docStoreOffset;
		this.cloneableFieldsStream = cloneableFieldsStream;
		this.cloneableIndexStream = cloneableIndexStream;
		fieldsStream = (IndexInput) cloneableFieldsStream.clone();
		indexStream = (IndexInput) cloneableIndexStream.clone();
	}

	FieldsReader(Directory d, String segment, FieldInfos fn) throws IOException {
		this(d, segment, fn, BufferedIndexInput.BUFFER_SIZE, -1, 0);
	}

	FieldsReader(Directory d, String segment, FieldInfos fn, int readBufferSize)
			throws IOException {
		this(d, segment, fn, readBufferSize, -1, 0);
	}

	FieldsReader(Directory d, String segment, FieldInfos fn,
			int readBufferSize, int docStoreOffset, int size)
			throws IOException {
		boolean success = false;
		isOriginal = true;
		try {
			fieldInfos = fn;

			cloneableFieldsStream = d.openInput(IndexFileNames.segmentFileName(
					segment, IndexFileNames.FIELDS_EXTENSION), readBufferSize);
			final String indexStreamFN = IndexFileNames.segmentFileName(
					segment, IndexFileNames.FIELDS_INDEX_EXTENSION);
			cloneableIndexStream = d.openInput(indexStreamFN, readBufferSize);

			int firstInt = cloneableIndexStream.readInt();
			if (firstInt == 0)
				format = 0;
			else
				format = firstInt;

			if (format > FieldsWriter.FORMAT_CURRENT)
				throw new IndexFormatTooNewException(cloneableIndexStream,
						format, 0, FieldsWriter.FORMAT_CURRENT);

			if (format > FieldsWriter.FORMAT)
				formatSize = 4;
			else
				formatSize = 0;

			if (format < FieldsWriter.FORMAT_VERSION_UTF8_LENGTH_IN_BYTES)
				cloneableFieldsStream.setModifiedUTF8StringsMode();

			fieldsStream = (IndexInput) cloneableFieldsStream.clone();

			final long indexSize = cloneableIndexStream.length() - formatSize;

			if (docStoreOffset != -1) {

				this.docStoreOffset = docStoreOffset;
				this.size = size;

				assert ((int) (indexSize / 8)) >= size + this.docStoreOffset : "indexSize="
						+ indexSize
						+ " size="
						+ size
						+ " docStoreOffset="
						+ docStoreOffset;
			} else {
				this.docStoreOffset = 0;
				this.size = (int) (indexSize >> 3);
			}

			indexStream = (IndexInput) cloneableIndexStream.clone();
			numTotalDocs = (int) (indexSize >> 3);
			success = true;
		} finally {

			if (!success) {
				close();
			}
		}
	}

	private void ensureOpen() throws AlreadyClosedException {
		if (closed) {
			throw new AlreadyClosedException("this FieldsReader is closed");
		}
	}

	public final void close() throws IOException {
		if (!closed) {
			if (isOriginal) {
				IOUtils.close(fieldsStream, indexStream, fieldsStreamTL,
						cloneableFieldsStream, cloneableIndexStream);
			} else {
				IOUtils.close(fieldsStream, indexStream, fieldsStreamTL);
			}
			closed = true;
		}
	}

	final int size() {
		return size;
	}

	private final void seekIndex(int docID) throws IOException {
		indexStream.seek(formatSize + (docID + docStoreOffset) * 8L);
	}

	boolean canReadRawDocs() {

		return format >= FieldsWriter.FORMAT_SHATAM_3_0_NO_COMPRESSED_FIELDS;
	}

	final Document doc(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		seekIndex(n);
		long position = indexStream.readLong();
		fieldsStream.seek(position);

		Document doc = new Document();
		int numFields = fieldsStream.readVInt();
		out: for (int i = 0; i < numFields; i++) {
			int fieldNumber = fieldsStream.readVInt();
			FieldInfo fi = fieldInfos.fieldInfo(fieldNumber);
			FieldSelectorResult acceptField = fieldSelector == null ? FieldSelectorResult.LOAD
					: fieldSelector.accept(fi.name);

			int bits = fieldsStream.readByte() & 0xFF;
			assert bits <= (FieldsWriter.FIELD_IS_NUMERIC_MASK
					| FieldsWriter.FIELD_IS_COMPRESSED
					| FieldsWriter.FIELD_IS_TOKENIZED | FieldsWriter.FIELD_IS_BINARY) : "bits="
					+ Integer.toHexString(bits);

			boolean compressed = (bits & FieldsWriter.FIELD_IS_COMPRESSED) != 0;
			assert (compressed ? (format < FieldsWriter.FORMAT_SHATAM_3_0_NO_COMPRESSED_FIELDS)
					: true) : "compressed fields are only allowed in indexes of version <= 2.9";
			boolean tokenize = (bits & FieldsWriter.FIELD_IS_TOKENIZED) != 0;
			boolean binary = (bits & FieldsWriter.FIELD_IS_BINARY) != 0;
			final int numeric = bits & FieldsWriter.FIELD_IS_NUMERIC_MASK;

			switch (acceptField) {
			case LOAD:
				addField(doc, fi, binary, compressed, tokenize, numeric);
				break;
			case LOAD_AND_BREAK:
				addField(doc, fi, binary, compressed, tokenize, numeric);
				break out;
			case LAZY_LOAD:
				addFieldLazy(doc, fi, binary, compressed, tokenize, true,
						numeric);
				break;
			case LATENT:
				addFieldLazy(doc, fi, binary, compressed, tokenize, false,
						numeric);
				break;
			case SIZE:
				skipFieldBytes(binary, compressed,
						addFieldSize(doc, fi, binary, compressed, numeric));
				break;
			case SIZE_AND_BREAK:
				addFieldSize(doc, fi, binary, compressed, numeric);
				break out;
			default:
				skipField(binary, compressed, numeric);
			}
		}

		return doc;
	}

	final IndexInput rawDocs(int[] lengths, int startDocID, int numDocs)
			throws IOException {
		seekIndex(startDocID);
		long startOffset = indexStream.readLong();
		long lastOffset = startOffset;
		int count = 0;
		while (count < numDocs) {
			final long offset;
			final int docID = docStoreOffset + startDocID + count + 1;
			assert docID <= numTotalDocs;
			if (docID < numTotalDocs)
				offset = indexStream.readLong();
			else
				offset = fieldsStream.length();
			lengths[count++] = (int) (offset - lastOffset);
			lastOffset = offset;
		}

		fieldsStream.seek(startOffset);

		return fieldsStream;
	}

	private void skipField(boolean binary, boolean compressed, int numeric)
			throws IOException {
		final int numBytes;
		switch (numeric) {
		case 0:
			numBytes = fieldsStream.readVInt();
			break;
		case FieldsWriter.FIELD_IS_NUMERIC_INT:
		case FieldsWriter.FIELD_IS_NUMERIC_FLOAT:
			numBytes = 4;
			break;
		case FieldsWriter.FIELD_IS_NUMERIC_LONG:
		case FieldsWriter.FIELD_IS_NUMERIC_DOUBLE:
			numBytes = 8;
			break;
		default:
			throw new FieldReaderException("Invalid numeric type: "
					+ Integer.toHexString(numeric));
		}

		skipFieldBytes(binary, compressed, numBytes);
	}

	private void skipFieldBytes(boolean binary, boolean compressed, int toRead)
			throws IOException {
		if (format >= FieldsWriter.FORMAT_VERSION_UTF8_LENGTH_IN_BYTES
				|| binary || compressed) {
			fieldsStream.seek(fieldsStream.getFilePointer() + toRead);
		} else {

			fieldsStream.skipChars(toRead);
		}
	}

	private NumericField loadNumericField(FieldInfo fi, int numeric)
			throws IOException {
		assert numeric != 0;
		switch (numeric) {
		case FieldsWriter.FIELD_IS_NUMERIC_INT:
			return new NumericField(fi.name, Field.Store.YES, fi.isIndexed)
					.setIntValue(fieldsStream.readInt());
		case FieldsWriter.FIELD_IS_NUMERIC_LONG:
			return new NumericField(fi.name, Field.Store.YES, fi.isIndexed)
					.setLongValue(fieldsStream.readLong());
		case FieldsWriter.FIELD_IS_NUMERIC_FLOAT:
			return new NumericField(fi.name, Field.Store.YES, fi.isIndexed)
					.setFloatValue(Float.intBitsToFloat(fieldsStream.readInt()));
		case FieldsWriter.FIELD_IS_NUMERIC_DOUBLE:
			return new NumericField(fi.name, Field.Store.YES, fi.isIndexed)
					.setDoubleValue(Double.longBitsToDouble(fieldsStream
							.readLong()));
		default:
			throw new FieldReaderException("Invalid numeric type: "
					+ Integer.toHexString(numeric));
		}
	}

	private void addFieldLazy(Document doc, FieldInfo fi, boolean binary,
			boolean compressed, boolean tokenize, boolean cacheResult,
			int numeric) throws IOException {
		final AbstractField f;
		if (binary) {
			int toRead = fieldsStream.readVInt();
			long pointer = fieldsStream.getFilePointer();
			f = new LazyField(fi.name, Field.Store.YES, toRead, pointer,
					binary, compressed, cacheResult);

			fieldsStream.seek(pointer + toRead);
		} else if (numeric != 0) {
			f = loadNumericField(fi, numeric);
		} else {
			Field.Store store = Field.Store.YES;
			Field.Index index = Field.Index.toIndex(fi.isIndexed, tokenize);
			Field.TermVector termVector = Field.TermVector.toTermVector(
					fi.storeTermVector, fi.storeOffsetWithTermVector,
					fi.storePositionWithTermVector);

			if (compressed) {
				int toRead = fieldsStream.readVInt();
				long pointer = fieldsStream.getFilePointer();
				f = new LazyField(fi.name, store, toRead, pointer, binary,
						compressed, cacheResult);

				fieldsStream.seek(pointer + toRead);
			} else {
				int length = fieldsStream.readVInt();
				long pointer = fieldsStream.getFilePointer();

				if (format >= FieldsWriter.FORMAT_VERSION_UTF8_LENGTH_IN_BYTES) {
					fieldsStream.seek(pointer + length);
				} else {
					fieldsStream.skipChars(length);
				}
				f = new LazyField(fi.name, store, index, termVector, length,
						pointer, binary, compressed, cacheResult);
			}
		}

		f.setOmitNorms(fi.omitNorms);
		f.setIndexOptions(fi.indexOptions);
		doc.add(f);
	}

	private void addField(Document doc, FieldInfo fi, boolean binary,
			boolean compressed, boolean tokenize, int numeric)
			throws CorruptIndexException, IOException {
		final AbstractField f;

		if (binary) {
			int toRead = fieldsStream.readVInt();
			final byte[] b = new byte[toRead];
			fieldsStream.readBytes(b, 0, b.length);
			if (compressed) {
				f = new Field(fi.name, uncompress(b));
			} else {
				f = new Field(fi.name, b);
			}
		} else if (numeric != 0) {
			f = loadNumericField(fi, numeric);
		} else {
			Field.Store store = Field.Store.YES;
			Field.Index index = Field.Index.toIndex(fi.isIndexed, tokenize);
			Field.TermVector termVector = Field.TermVector.toTermVector(
					fi.storeTermVector, fi.storeOffsetWithTermVector,
					fi.storePositionWithTermVector);
			if (compressed) {
				int toRead = fieldsStream.readVInt();
				final byte[] b = new byte[toRead];
				fieldsStream.readBytes(b, 0, b.length);
				f = new Field(fi.name, false,
						new String(uncompress(b), "UTF-8"), store, index,
						termVector);
			} else {
				f = new Field(fi.name, false, fieldsStream.readString(), store,
						index, termVector);
			}
		}

		f.setIndexOptions(fi.indexOptions);
		f.setOmitNorms(fi.omitNorms);
		doc.add(f);
	}

	private int addFieldSize(Document doc, FieldInfo fi, boolean binary,
			boolean compressed, int numeric) throws IOException {
		final int bytesize, size;
		switch (numeric) {
		case 0:
			size = fieldsStream.readVInt();
			bytesize = (binary || compressed) ? size : 2 * size;
			break;
		case FieldsWriter.FIELD_IS_NUMERIC_INT:
		case FieldsWriter.FIELD_IS_NUMERIC_FLOAT:
			size = bytesize = 4;
			break;
		case FieldsWriter.FIELD_IS_NUMERIC_LONG:
		case FieldsWriter.FIELD_IS_NUMERIC_DOUBLE:
			size = bytesize = 8;
			break;
		default:
			throw new FieldReaderException("Invalid numeric type: "
					+ Integer.toHexString(numeric));
		}
		byte[] sizebytes = new byte[4];
		sizebytes[0] = (byte) (bytesize >>> 24);
		sizebytes[1] = (byte) (bytesize >>> 16);
		sizebytes[2] = (byte) (bytesize >>> 8);
		sizebytes[3] = (byte) bytesize;
		doc.add(new Field(fi.name, sizebytes));
		return size;
	}

	private class LazyField extends AbstractField implements Fieldable {
		private int toRead;
		private long pointer;

		@Deprecated
		private boolean isCompressed;
		private boolean cacheResult;

		public LazyField(String name, Field.Store store, int toRead,
				long pointer, boolean isBinary, boolean isCompressed,
				boolean cacheResult) {
			super(name, store, Field.Index.NO, Field.TermVector.NO);
			this.toRead = toRead;
			this.pointer = pointer;
			this.isBinary = isBinary;
			this.cacheResult = cacheResult;
			if (isBinary)
				binaryLength = toRead;
			lazy = true;
			this.isCompressed = isCompressed;
		}

		public LazyField(String name, Field.Store store, Field.Index index,
				Field.TermVector termVector, int toRead, long pointer,
				boolean isBinary, boolean isCompressed, boolean cacheResult) {
			super(name, store, index, termVector);
			this.toRead = toRead;
			this.pointer = pointer;
			this.isBinary = isBinary;
			this.cacheResult = cacheResult;
			if (isBinary)
				binaryLength = toRead;
			lazy = true;
			this.isCompressed = isCompressed;
		}

		private IndexInput getFieldStream() {
			IndexInput localFieldsStream = fieldsStreamTL.get();
			if (localFieldsStream == null) {
				localFieldsStream = (IndexInput) cloneableFieldsStream.clone();
				fieldsStreamTL.set(localFieldsStream);
			}
			return localFieldsStream;
		}

		public Reader readerValue() {
			ensureOpen();
			return null;
		}

		public TokenStream tokenStreamValue() {
			ensureOpen();
			return null;
		}

		public String stringValue() {
			ensureOpen();
			if (isBinary)
				return null;
			else {
				if (fieldsData == null) {
					IndexInput localFieldsStream = getFieldStream();
					String value;
					try {
						localFieldsStream.seek(pointer);
						if (isCompressed) {
							final byte[] b = new byte[toRead];
							localFieldsStream.readBytes(b, 0, b.length);
							value = new String(uncompress(b), "UTF-8");
						} else {
							if (format >= FieldsWriter.FORMAT_VERSION_UTF8_LENGTH_IN_BYTES) {
								byte[] bytes = new byte[toRead];
								localFieldsStream.readBytes(bytes, 0, toRead);
								value = new String(bytes, "UTF-8");
							} else {

								char[] chars = new char[toRead];
								localFieldsStream.readChars(chars, 0, toRead);
								value = new String(chars);
							}
						}
					} catch (IOException e) {
						throw new FieldReaderException(e);
					}
					if (cacheResult) {
						fieldsData = value;
					}
					return value;
				} else {
					return (String) fieldsData;
				}

			}
		}

		public long getPointer() {
			ensureOpen();
			return pointer;
		}

		public void setPointer(long pointer) {
			ensureOpen();
			this.pointer = pointer;
		}

		public int getToRead() {
			ensureOpen();
			return toRead;
		}

		public void setToRead(int toRead) {
			ensureOpen();
			this.toRead = toRead;
		}

		@Override
		public byte[] getBinaryValue(byte[] result) {
			ensureOpen();

			if (isBinary) {
				if (fieldsData == null) {

					final byte[] b;
					byte[] value;
					if (result == null || result.length < toRead)
						b = new byte[toRead];
					else
						b = result;

					IndexInput localFieldsStream = getFieldStream();

					try {
						localFieldsStream.seek(pointer);
						localFieldsStream.readBytes(b, 0, toRead);
						if (isCompressed == true) {
							value = uncompress(b);
						} else {
							value = b;
						}
					} catch (IOException e) {
						throw new FieldReaderException(e);
					}

					binaryOffset = 0;
					binaryLength = toRead;
					if (cacheResult == true) {
						fieldsData = value;
					}
					return value;
				} else {
					return (byte[]) fieldsData;
				}
			} else {
				return null;
			}
		}
	}

	private byte[] uncompress(byte[] b) throws CorruptIndexException {
		try {
			return CompressionTools.decompress(b);
		} catch (DataFormatException e) {

			CorruptIndexException newException = new CorruptIndexException(
					"field data are in wrong format: " + e.toString());
			newException.initCause(e);
			throw newException;
		}
	}
}
