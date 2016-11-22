/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.Fieldable;
import com.shatam.shatamindex.index.FieldInfo.IndexOptions;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.StringHelper;

import java.io.IOException;
import java.util.*;

final class FieldInfos {

	public static final int FORMAT_PRE = -1;

	public static final int FORMAT_START = -2;

	public static final int FORMAT_OMIT_POSITIONS = -3;

	static final int CURRENT_FORMAT = FORMAT_OMIT_POSITIONS;

	static final byte IS_INDEXED = 0x1;
	static final byte STORE_TERMVECTOR = 0x2;
	static final byte STORE_POSITIONS_WITH_TERMVECTOR = 0x4;
	static final byte STORE_OFFSET_WITH_TERMVECTOR = 0x8;
	static final byte OMIT_NORMS = 0x10;
	static final byte STORE_PAYLOADS = 0x20;
	static final byte OMIT_TERM_FREQ_AND_POSITIONS = 0x40;
	static final byte OMIT_POSITIONS = -128;

	private final ArrayList<FieldInfo> byNumber = new ArrayList<FieldInfo>();
	private final HashMap<String, FieldInfo> byName = new HashMap<String, FieldInfo>();
	private int format;

	FieldInfos() {
	}

	FieldInfos(Directory d, String name) throws IOException {
		IndexInput input = d.openInput(name);
		try {
			try {
				read(input, name);
			} catch (IOException ioe) {
				if (format == FORMAT_PRE) {

					input.seek(0);
					input.setModifiedUTF8StringsMode();
					byNumber.clear();
					byName.clear();
					try {
						read(input, name);
					} catch (Throwable t) {

						throw ioe;
					}
				} else {

					throw ioe;
				}
			}
		} finally {
			input.close();
		}
	}

	@Override
	synchronized public Object clone() {
		FieldInfos fis = new FieldInfos();
		final int numField = byNumber.size();
		for (int i = 0; i < numField; i++) {
			FieldInfo fi = (FieldInfo) (byNumber.get(i)).clone();
			fis.byNumber.add(fi);
			fis.byName.put(fi.name, fi);
		}
		return fis;
	}

	synchronized public void add(Document doc) {
		List<Fieldable> fields = doc.getFields();
		for (Fieldable field : fields) {
			add(field.name(), field.isIndexed(), field.isTermVectorStored(),
					field.isStorePositionWithTermVector(),
					field.isStoreOffsetWithTermVector(), field.getOmitNorms(),
					false, field.getIndexOptions());
		}
	}

	boolean hasProx() {
		final int numFields = byNumber.size();
		for (int i = 0; i < numFields; i++) {
			final FieldInfo fi = fieldInfo(i);
			if (fi.isIndexed
					&& fi.indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
				return true;
			}
		}
		return false;
	}

	synchronized public void addIndexed(Collection<String> names,
			boolean storeTermVectors, boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector) {
		for (String name : names) {
			add(name, true, storeTermVectors, storePositionWithTermVector,
					storeOffsetWithTermVector);
		}
	}

	synchronized public void add(Collection<String> names, boolean isIndexed) {
		for (String name : names) {
			add(name, isIndexed);
		}
	}

	synchronized public void add(String name, boolean isIndexed) {
		add(name, isIndexed, false, false, false, false);
	}

	synchronized public void add(String name, boolean isIndexed,
			boolean storeTermVector) {
		add(name, isIndexed, storeTermVector, false, false, false);
	}

	synchronized public void add(String name, boolean isIndexed,
			boolean storeTermVector, boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector) {

		add(name, isIndexed, storeTermVector, storePositionWithTermVector,
				storeOffsetWithTermVector, false);
	}

	synchronized public void add(String name, boolean isIndexed,
			boolean storeTermVector, boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector, boolean omitNorms) {
		add(name, isIndexed, storeTermVector, storePositionWithTermVector,
				storeOffsetWithTermVector, omitNorms, false,
				IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
	}

	synchronized public FieldInfo add(String name, boolean isIndexed,
			boolean storeTermVector, boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector, boolean omitNorms,
			boolean storePayloads, IndexOptions indexOptions) {
		FieldInfo fi = fieldInfo(name);
		if (fi == null) {
			return addInternal(name, isIndexed, storeTermVector,
					storePositionWithTermVector, storeOffsetWithTermVector,
					omitNorms, storePayloads, indexOptions);
		} else {
			fi.update(isIndexed, storeTermVector, storePositionWithTermVector,
					storeOffsetWithTermVector, omitNorms, storePayloads,
					indexOptions);
		}
		assert fi.indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS
				|| !fi.storePayloads;
		return fi;
	}

	synchronized public FieldInfo add(FieldInfo fi) {
		return add(fi.name, fi.isIndexed, fi.storeTermVector,
				fi.storePositionWithTermVector, fi.storeOffsetWithTermVector,
				fi.omitNorms, fi.storePayloads, fi.indexOptions);
	}

	private FieldInfo addInternal(String name, boolean isIndexed,
			boolean storeTermVector, boolean storePositionWithTermVector,
			boolean storeOffsetWithTermVector, boolean omitNorms,
			boolean storePayloads, IndexOptions indexOptions) {
		name = StringHelper.intern(name);
		FieldInfo fi = new FieldInfo(name, isIndexed, byNumber.size(),
				storeTermVector, storePositionWithTermVector,
				storeOffsetWithTermVector, omitNorms, storePayloads,
				indexOptions);
		byNumber.add(fi);
		byName.put(name, fi);
		return fi;
	}

	public int fieldNumber(String fieldName) {
		FieldInfo fi = fieldInfo(fieldName);
		return (fi != null) ? fi.number : -1;
	}

	public FieldInfo fieldInfo(String fieldName) {
		return byName.get(fieldName);
	}

	public String fieldName(int fieldNumber) {
		FieldInfo fi = fieldInfo(fieldNumber);
		return (fi != null) ? fi.name : "";
	}

	public FieldInfo fieldInfo(int fieldNumber) {
		return (fieldNumber >= 0) ? byNumber.get(fieldNumber) : null;
	}

	public int size() {
		return byNumber.size();
	}

	public boolean hasVectors() {
		boolean hasVectors = false;
		for (int i = 0; i < size(); i++) {
			if (fieldInfo(i).storeTermVector) {
				hasVectors = true;
				break;
			}
		}
		return hasVectors;
	}

	public void write(Directory d, String name) throws IOException {
		IndexOutput output = d.createOutput(name);
		try {
			write(output);
		} finally {
			output.close();
		}
	}

	public void write(IndexOutput output) throws IOException {
		output.writeVInt(CURRENT_FORMAT);
		output.writeVInt(size());
		for (int i = 0; i < size(); i++) {
			FieldInfo fi = fieldInfo(i);
			assert fi.indexOptions == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS
					|| !fi.storePayloads;
			byte bits = 0x0;
			if (fi.isIndexed)
				bits |= IS_INDEXED;
			if (fi.storeTermVector)
				bits |= STORE_TERMVECTOR;
			if (fi.storePositionWithTermVector)
				bits |= STORE_POSITIONS_WITH_TERMVECTOR;
			if (fi.storeOffsetWithTermVector)
				bits |= STORE_OFFSET_WITH_TERMVECTOR;
			if (fi.omitNorms)
				bits |= OMIT_NORMS;
			if (fi.storePayloads)
				bits |= STORE_PAYLOADS;
			if (fi.indexOptions == IndexOptions.DOCS_ONLY)
				bits |= OMIT_TERM_FREQ_AND_POSITIONS;
			else if (fi.indexOptions == IndexOptions.DOCS_AND_FREQS)
				bits |= OMIT_POSITIONS;

			output.writeString(fi.name);
			output.writeByte(bits);
		}
	}

	private void read(IndexInput input, String fileName) throws IOException {
		int firstInt = input.readVInt();

		if (firstInt < 0) {

			format = firstInt;
		} else {
			format = FORMAT_PRE;
		}

		if (format != FORMAT_PRE && format != FORMAT_START
				&& format != FORMAT_OMIT_POSITIONS) {
			throw new CorruptIndexException("unrecognized format " + format
					+ " in file \"" + fileName + "\"");
		}

		int size;
		if (format == FORMAT_PRE) {
			size = firstInt;
		} else {
			size = input.readVInt();
		}

		for (int i = 0; i < size; i++) {
			String name = StringHelper.intern(input.readString());
			byte bits = input.readByte();
			boolean isIndexed = (bits & IS_INDEXED) != 0;
			boolean storeTermVector = (bits & STORE_TERMVECTOR) != 0;
			boolean storePositionsWithTermVector = (bits & STORE_POSITIONS_WITH_TERMVECTOR) != 0;
			boolean storeOffsetWithTermVector = (bits & STORE_OFFSET_WITH_TERMVECTOR) != 0;
			boolean omitNorms = (bits & OMIT_NORMS) != 0;
			boolean storePayloads = (bits & STORE_PAYLOADS) != 0;
			final IndexOptions indexOptions;
			if ((bits & OMIT_TERM_FREQ_AND_POSITIONS) != 0) {
				indexOptions = IndexOptions.DOCS_ONLY;
			} else if ((bits & OMIT_POSITIONS) != 0) {
				if (format <= FORMAT_OMIT_POSITIONS) {
					indexOptions = IndexOptions.DOCS_AND_FREQS;
				} else {
					throw new CorruptIndexException(
							"Corrupt fieldinfos, OMIT_POSITIONS set but format="
									+ format + " (resource: " + input + ")");
				}
			} else {
				indexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
			}

			if (indexOptions != IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
				storePayloads = false;
			}

			addInternal(name, isIndexed, storeTermVector,
					storePositionsWithTermVector, storeOffsetWithTermVector,
					omitNorms, storePayloads, indexOptions);
		}

		if (input.getFilePointer() != input.length()) {
			throw new CorruptIndexException(
					"did not read all bytes from file \"" + fileName
							+ "\": read " + input.getFilePointer()
							+ " vs size " + input.length() + " (resource: "
							+ input + ")");
		}
	}

}
