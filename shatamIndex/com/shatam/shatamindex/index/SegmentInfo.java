/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.IndexOutput;
import com.shatam.shatamindex.util.BitVector;
import com.shatam.shatamindex.util.Constants;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public final class SegmentInfo implements Cloneable {

	static final int NO = -1;
	static final int YES = 1;
	static final int CHECK_DIR = 0;
	static final int WITHOUT_GEN = 0;

	public String name;
	public int docCount;
	public Directory dir;

	private boolean preLockless;

	private long delGen;

	private long[] normGen;

	private byte isCompoundFile;

	private boolean hasSingleNormFile;

	private volatile List<String> files;

	private volatile long sizeInBytesNoStore = -1;
	private volatile long sizeInBytesWithStore = -1;

	private int docStoreOffset;

	private String docStoreSegment;

	private boolean docStoreIsCompoundFile;

	private int delCount;

	private boolean hasProx;

	private boolean hasVectors;

	private Map<String, String> diagnostics;

	private String version;

	private long bufferedDeletesGen;

	public SegmentInfo(String name, int docCount, Directory dir,
			boolean isCompoundFile, boolean hasSingleNormFile, boolean hasProx,
			boolean hasVectors) {
		this.name = name;
		this.docCount = docCount;
		this.dir = dir;
		delGen = NO;
		this.isCompoundFile = (byte) (isCompoundFile ? YES : NO);
		preLockless = false;
		this.hasSingleNormFile = hasSingleNormFile;
		this.docStoreOffset = -1;
		delCount = 0;
		this.hasProx = hasProx;
		this.hasVectors = hasVectors;
		this.version = Constants.SHATAM_MAIN_VERSION;
	}

	void reset(SegmentInfo src) {
		clearFiles();
		version = src.version;
		name = src.name;
		docCount = src.docCount;
		dir = src.dir;
		preLockless = src.preLockless;
		delGen = src.delGen;
		docStoreOffset = src.docStoreOffset;
		docStoreIsCompoundFile = src.docStoreIsCompoundFile;
		hasVectors = src.hasVectors;
		hasProx = src.hasProx;
		if (src.normGen == null) {
			normGen = null;
		} else {
			normGen = new long[src.normGen.length];
			System.arraycopy(src.normGen, 0, normGen, 0, src.normGen.length);
		}
		isCompoundFile = src.isCompoundFile;
		hasSingleNormFile = src.hasSingleNormFile;
		delCount = src.delCount;
	}

	void setDiagnostics(Map<String, String> diagnostics) {
		this.diagnostics = diagnostics;
	}

	public Map<String, String> getDiagnostics() {
		return diagnostics;
	}

	SegmentInfo(Directory dir, int format, IndexInput input) throws IOException {
		this.dir = dir;
		if (format <= SegmentInfos.FORMAT_3_1) {
			version = input.readString();
		}
		name = input.readString();
		docCount = input.readInt();
		if (format <= SegmentInfos.FORMAT_LOCKLESS) {
			delGen = input.readLong();
			if (format <= SegmentInfos.FORMAT_SHARED_DOC_STORE) {
				docStoreOffset = input.readInt();
				if (docStoreOffset != -1) {
					docStoreSegment = input.readString();
					docStoreIsCompoundFile = (1 == input.readByte());
				} else {
					docStoreSegment = name;
					docStoreIsCompoundFile = false;
				}
			} else {
				docStoreOffset = -1;
				docStoreSegment = name;
				docStoreIsCompoundFile = false;
			}
			if (format <= SegmentInfos.FORMAT_SINGLE_NORM_FILE) {
				hasSingleNormFile = (1 == input.readByte());
			} else {
				hasSingleNormFile = false;
			}
			int numNormGen = input.readInt();
			if (numNormGen == NO) {
				normGen = null;
			} else {
				normGen = new long[numNormGen];
				for (int j = 0; j < numNormGen; j++) {
					normGen[j] = input.readLong();
				}
			}
			isCompoundFile = input.readByte();
			preLockless = (isCompoundFile == CHECK_DIR);
			if (format <= SegmentInfos.FORMAT_DEL_COUNT) {
				delCount = input.readInt();
				assert delCount <= docCount;
			} else
				delCount = -1;
			if (format <= SegmentInfos.FORMAT_HAS_PROX)
				hasProx = input.readByte() == 1;
			else
				hasProx = true;

			if (format <= SegmentInfos.FORMAT_DIAGNOSTICS) {
				diagnostics = input.readStringStringMap();
			} else {
				diagnostics = Collections.<String, String> emptyMap();
			}

			if (format <= SegmentInfos.FORMAT_HAS_VECTORS) {
				hasVectors = input.readByte() == 1;
			} else {
				final String storesSegment;
				final String ext;
				final boolean isCompoundFile;
				if (docStoreOffset != -1) {
					storesSegment = docStoreSegment;
					isCompoundFile = docStoreIsCompoundFile;
					ext = IndexFileNames.COMPOUND_FILE_STORE_EXTENSION;
				} else {
					storesSegment = name;
					isCompoundFile = getUseCompoundFile();
					ext = IndexFileNames.COMPOUND_FILE_EXTENSION;
				}
				final Directory dirToTest;
				if (isCompoundFile) {
					dirToTest = new CompoundFileReader(dir,
							IndexFileNames.segmentFileName(storesSegment, ext));
				} else {
					dirToTest = dir;
				}
				try {
					hasVectors = dirToTest.fileExists(IndexFileNames
							.segmentFileName(storesSegment,
									IndexFileNames.VECTORS_INDEX_EXTENSION));
				} finally {
					if (isCompoundFile) {
						dirToTest.close();
					}
				}
			}
		} else {
			delGen = CHECK_DIR;
			normGen = null;
			isCompoundFile = CHECK_DIR;
			preLockless = true;
			hasSingleNormFile = false;
			docStoreOffset = -1;
			docStoreIsCompoundFile = false;
			docStoreSegment = null;
			delCount = -1;
			hasProx = true;
			diagnostics = Collections.<String, String> emptyMap();
		}
	}

	void setNumFields(int numFields) {
		if (normGen == null) {

			normGen = new long[numFields];

			if (preLockless) {

			} else {

				for (int i = 0; i < numFields; i++) {
					normGen[i] = NO;
				}
			}
		}
	}

	public long sizeInBytes(boolean includeDocStores) throws IOException {
		if (includeDocStores) {
			if (sizeInBytesWithStore != -1) {
				return sizeInBytesWithStore;
			}
			long sum = 0;
			for (final String fileName : files()) {

				if (docStoreOffset == -1
						|| !IndexFileNames.isDocStoreFile(fileName)) {
					sum += dir.fileLength(fileName);
				}
			}
			sizeInBytesWithStore = sum;
			return sizeInBytesWithStore;
		} else {
			if (sizeInBytesNoStore != -1) {
				return sizeInBytesNoStore;
			}
			long sum = 0;
			for (final String fileName : files()) {
				if (IndexFileNames.isDocStoreFile(fileName)) {
					continue;
				}
				sum += dir.fileLength(fileName);
			}
			sizeInBytesNoStore = sum;
			return sizeInBytesNoStore;
		}
	}

	public boolean getHasVectors() throws IOException {
		return hasVectors;
	}

	public void setHasVectors(boolean v) {
		hasVectors = v;
		clearFiles();
	}

	public boolean hasDeletions() throws IOException {

		if (delGen == NO) {
			return false;
		} else if (delGen >= YES) {
			return true;
		} else {
			return dir.fileExists(getDelFileName());
		}
	}

	void advanceDelGen() {

		if (delGen == NO) {
			delGen = YES;
		} else {
			delGen++;
		}
		clearFiles();
	}

	void clearDelGen() {
		delGen = NO;
		clearFiles();
	}

	@Override
	public Object clone() {
		SegmentInfo si = new SegmentInfo(name, docCount, dir, false,
				hasSingleNormFile, hasProx, hasVectors);
		si.docStoreOffset = docStoreOffset;
		si.docStoreSegment = docStoreSegment;
		si.docStoreIsCompoundFile = docStoreIsCompoundFile;
		si.delGen = delGen;
		si.delCount = delCount;
		si.preLockless = preLockless;
		si.isCompoundFile = isCompoundFile;
		si.diagnostics = new HashMap<String, String>(diagnostics);
		if (normGen != null) {
			si.normGen = normGen.clone();
		}
		si.version = version;
		return si;
	}

	public String getDelFileName() {
		if (delGen == NO) {

			return null;
		} else {

			return IndexFileNames.fileNameFromGeneration(name,
					IndexFileNames.DELETES_EXTENSION, delGen);
		}
	}

	public boolean hasSeparateNorms(int fieldNumber) throws IOException {
		if ((normGen == null && preLockless)
				|| (normGen != null && normGen[fieldNumber] == CHECK_DIR)) {

			String fileName = name + ".s" + fieldNumber;
			return dir.fileExists(fileName);
		} else if (normGen == null || normGen[fieldNumber] == NO) {
			return false;
		} else {
			return true;
		}
	}

	public boolean hasSeparateNorms() throws IOException {
		if (normGen == null) {
			if (!preLockless) {

				return false;
			} else {

				String[] result = dir.listAll();
				if (result == null)
					throw new IOException("cannot read directory " + dir
							+ ": listAll() returned null");

				final IndexFileNameFilter filter = IndexFileNameFilter
						.getFilter();
				String pattern;
				pattern = name + ".s";
				int patternLength = pattern.length();
				for (int i = 0; i < result.length; i++) {
					String fileName = result[i];
					if (filter.accept(null, fileName)
							&& fileName.startsWith(pattern)
							&& Character
									.isDigit(fileName.charAt(patternLength)))
						return true;
				}
				return false;
			}
		} else {

			for (int i = 0; i < normGen.length; i++) {
				if (normGen[i] >= YES) {
					return true;
				}
			}

			for (int i = 0; i < normGen.length; i++) {
				if (normGen[i] == CHECK_DIR) {
					if (hasSeparateNorms(i)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	void advanceNormGen(int fieldIndex) {
		if (normGen[fieldIndex] == NO) {
			normGen[fieldIndex] = YES;
		} else {
			normGen[fieldIndex]++;
		}
		clearFiles();
	}

	public String getNormFileName(int number) throws IOException {
		long gen;
		if (normGen == null) {
			gen = CHECK_DIR;
		} else {
			gen = normGen[number];
		}

		if (hasSeparateNorms(number)) {

			return IndexFileNames.fileNameFromGeneration(name, "s" + number,
					gen);
		}

		if (hasSingleNormFile) {

			return IndexFileNames.fileNameFromGeneration(name,
					IndexFileNames.NORMS_EXTENSION, WITHOUT_GEN);
		}

		return IndexFileNames.fileNameFromGeneration(name, "f" + number,
				WITHOUT_GEN);
	}

	void setUseCompoundFile(boolean isCompoundFile) {
		if (isCompoundFile) {
			this.isCompoundFile = YES;
		} else {
			this.isCompoundFile = NO;
		}
		clearFiles();
	}

	public boolean getUseCompoundFile() throws IOException {
		if (isCompoundFile == NO) {
			return false;
		} else if (isCompoundFile == YES) {
			return true;
		} else {
			return dir.fileExists(IndexFileNames.segmentFileName(name,
					IndexFileNames.COMPOUND_FILE_EXTENSION));
		}
	}

	public int getDelCount() throws IOException {
		if (delCount == -1) {
			if (hasDeletions()) {
				final String delFileName = getDelFileName();
				delCount = new BitVector(dir, delFileName).count();
			} else
				delCount = 0;
		}
		assert delCount <= docCount;
		return delCount;
	}

	void setDelCount(int delCount) {
		this.delCount = delCount;
		assert delCount <= docCount;
	}

	public int getDocStoreOffset() {
		return docStoreOffset;
	}

	public boolean getDocStoreIsCompoundFile() {
		return docStoreIsCompoundFile;
	}

	void setDocStoreIsCompoundFile(boolean v) {
		docStoreIsCompoundFile = v;
		clearFiles();
	}

	public String getDocStoreSegment() {
		return docStoreSegment;
	}

	public void setDocStoreSegment(String segment) {
		docStoreSegment = segment;
	}

	void setDocStoreOffset(int offset) {
		docStoreOffset = offset;
		clearFiles();
	}

	void setDocStore(int offset, String segment, boolean isCompoundFile) {
		docStoreOffset = offset;
		docStoreSegment = segment;
		docStoreIsCompoundFile = isCompoundFile;
		clearFiles();
	}

	void write(IndexOutput output) throws IOException {
		assert delCount <= docCount : "delCount=" + delCount + " docCount="
				+ docCount + " segment=" + name;

		output.writeString(version);
		output.writeString(name);
		output.writeInt(docCount);
		output.writeLong(delGen);
		output.writeInt(docStoreOffset);
		if (docStoreOffset != -1) {
			output.writeString(docStoreSegment);
			output.writeByte((byte) (docStoreIsCompoundFile ? 1 : 0));
		}

		output.writeByte((byte) (hasSingleNormFile ? 1 : 0));
		if (normGen == null) {
			output.writeInt(NO);
		} else {
			output.writeInt(normGen.length);
			for (int j = 0; j < normGen.length; j++) {
				output.writeLong(normGen[j]);
			}
		}
		output.writeByte(isCompoundFile);
		output.writeInt(delCount);
		output.writeByte((byte) (hasProx ? 1 : 0));
		output.writeStringStringMap(diagnostics);
		output.writeByte((byte) (hasVectors ? 1 : 0));
	}

	void setHasProx(boolean hasProx) {
		this.hasProx = hasProx;
		clearFiles();
	}

	public boolean getHasProx() {
		return hasProx;
	}

	private void addIfExists(Set<String> files, String fileName)
			throws IOException {
		if (dir.fileExists(fileName))
			files.add(fileName);
	}

	public List<String> files() throws IOException {

		if (files != null) {

			return files;
		}

		HashSet<String> filesSet = new HashSet<String>();

		boolean useCompoundFile = getUseCompoundFile();

		if (useCompoundFile) {
			filesSet.add(IndexFileNames.segmentFileName(name,
					IndexFileNames.COMPOUND_FILE_EXTENSION));
		} else {
			for (String ext : IndexFileNames.NON_STORE_INDEX_EXTENSIONS)
				addIfExists(filesSet, IndexFileNames.segmentFileName(name, ext));
		}

		if (docStoreOffset != -1) {

			assert docStoreSegment != null;
			if (docStoreIsCompoundFile) {
				filesSet.add(IndexFileNames.segmentFileName(docStoreSegment,
						IndexFileNames.COMPOUND_FILE_STORE_EXTENSION));
			} else {
				filesSet.add(IndexFileNames.segmentFileName(docStoreSegment,
						IndexFileNames.FIELDS_INDEX_EXTENSION));
				filesSet.add(IndexFileNames.segmentFileName(docStoreSegment,
						IndexFileNames.FIELDS_EXTENSION));
				if (hasVectors) {
					filesSet.add(IndexFileNames.segmentFileName(
							docStoreSegment,
							IndexFileNames.VECTORS_INDEX_EXTENSION));
					filesSet.add(IndexFileNames.segmentFileName(
							docStoreSegment,
							IndexFileNames.VECTORS_DOCUMENTS_EXTENSION));
					filesSet.add(IndexFileNames.segmentFileName(
							docStoreSegment,
							IndexFileNames.VECTORS_FIELDS_EXTENSION));
				}
			}
		} else if (!useCompoundFile) {
			filesSet.add(IndexFileNames.segmentFileName(name,
					IndexFileNames.FIELDS_INDEX_EXTENSION));
			filesSet.add(IndexFileNames.segmentFileName(name,
					IndexFileNames.FIELDS_EXTENSION));
			if (hasVectors) {
				filesSet.add(IndexFileNames.segmentFileName(name,
						IndexFileNames.VECTORS_INDEX_EXTENSION));
				filesSet.add(IndexFileNames.segmentFileName(name,
						IndexFileNames.VECTORS_DOCUMENTS_EXTENSION));
				filesSet.add(IndexFileNames.segmentFileName(name,
						IndexFileNames.VECTORS_FIELDS_EXTENSION));
			}
		}

		String delFileName = IndexFileNames.fileNameFromGeneration(name,
				IndexFileNames.DELETES_EXTENSION, delGen);
		if (delFileName != null
				&& (delGen >= YES || dir.fileExists(delFileName))) {
			filesSet.add(delFileName);
		}

		if (normGen != null) {
			for (int i = 0; i < normGen.length; i++) {
				long gen = normGen[i];
				if (gen >= YES) {

					filesSet.add(IndexFileNames.fileNameFromGeneration(name,
							IndexFileNames.SEPARATE_NORMS_EXTENSION + i, gen));
				} else if (NO == gen) {

					if (!hasSingleNormFile && !useCompoundFile) {
						String fileName = IndexFileNames.segmentFileName(name,
								IndexFileNames.PLAIN_NORMS_EXTENSION + i);
						if (dir.fileExists(fileName)) {
							filesSet.add(fileName);
						}
					}
				} else if (CHECK_DIR == gen) {

					String fileName = null;
					if (useCompoundFile) {
						fileName = IndexFileNames.segmentFileName(name,
								IndexFileNames.SEPARATE_NORMS_EXTENSION + i);
					} else if (!hasSingleNormFile) {
						fileName = IndexFileNames.segmentFileName(name,
								IndexFileNames.PLAIN_NORMS_EXTENSION + i);
					}
					if (fileName != null && dir.fileExists(fileName)) {
						filesSet.add(fileName);
					}
				}
			}
		} else if (preLockless || (!hasSingleNormFile && !useCompoundFile)) {

			String prefix;
			if (useCompoundFile)
				prefix = IndexFileNames.segmentFileName(name,
						IndexFileNames.SEPARATE_NORMS_EXTENSION);
			else
				prefix = IndexFileNames.segmentFileName(name,
						IndexFileNames.PLAIN_NORMS_EXTENSION);
			int prefixLength = prefix.length();
			String[] allFiles = dir.listAll();
			final IndexFileNameFilter filter = IndexFileNameFilter.getFilter();
			for (int i = 0; i < allFiles.length; i++) {
				String fileName = allFiles[i];
				if (filter.accept(null, fileName)
						&& fileName.length() > prefixLength
						&& Character.isDigit(fileName.charAt(prefixLength))
						&& fileName.startsWith(prefix)) {
					filesSet.add(fileName);
				}
			}
		}
		return files = new ArrayList<String>(filesSet);
	}

	private void clearFiles() {
		files = null;
		sizeInBytesNoStore = -1;
		sizeInBytesWithStore = -1;
	}

	@Override
	public String toString() {
		return toString(dir, 0);
	}

	public String toString(Directory dir, int pendingDelCount) {

		StringBuilder s = new StringBuilder();
		s.append(name).append('(').append(version == null ? "?" : version)
				.append(')').append(':');

		char cfs;
		try {
			if (getUseCompoundFile()) {
				cfs = 'c';
			} else {
				cfs = 'C';
			}
		} catch (IOException ioe) {
			cfs = '?';
		}
		s.append(cfs);

		if (this.dir != dir) {
			s.append('x');
		}
		if (hasVectors) {
			s.append('v');
		}
		s.append(docCount);

		int delCount;
		try {
			delCount = getDelCount();
		} catch (IOException ioe) {
			delCount = -1;
		}
		if (delCount != -1) {
			delCount += pendingDelCount;
		}
		if (delCount != 0) {
			s.append('/');
			if (delCount == -1) {
				s.append('?');
			} else {
				s.append(delCount);
			}
		}

		if (docStoreOffset != -1) {
			s.append("->").append(docStoreSegment);
			if (docStoreIsCompoundFile) {
				s.append('c');
			} else {
				s.append('C');
			}
			s.append('+').append(docStoreOffset);
		}

		return s.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof SegmentInfo) {
			final SegmentInfo other = (SegmentInfo) obj;
			return other.dir == dir && other.name.equals(name);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return dir.hashCode() + name.hashCode();
	}

	void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	long getBufferedDeletesGen() {
		return bufferedDeletesGen;
	}

	void setBufferedDeletesGen(long v) {
		bufferedDeletesGen = v;
	}
}
