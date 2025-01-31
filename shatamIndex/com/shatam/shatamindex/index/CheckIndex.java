/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.document.AbstractField;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.search.IndexSearcher;
import com.shatam.shatamindex.search.TermQuery;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.FSDirectory;
import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.util.StringHelper;

import java.text.NumberFormat;
import java.io.PrintStream;
import java.io.IOException;
import java.io.File;
import java.util.Collection;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class CheckIndex {

	private PrintStream infoStream;
	private Directory dir;

	public static class Status {

		public boolean clean;

		public boolean missingSegments;

		public boolean cantOpenSegments;

		public boolean missingSegmentVersion;

		public String segmentsFileName;

		public int numSegments;

		public String segmentFormat;

		public List<String> segmentsChecked = new ArrayList<String>();

		public boolean toolOutOfDate;

		public List<SegmentInfoStatus> segmentInfos = new ArrayList<SegmentInfoStatus>();

		public Directory dir;

		SegmentInfos newSegments;

		public int totLoseDocCount;

		public int numBadSegments;

		public boolean partial;

		public int maxSegmentName;

		public boolean validCounter;

		public Map<String, String> userData;

		public static class SegmentInfoStatus {

			public String name;

			public int docCount;

			public boolean compound;

			public int numFiles;

			public double sizeMB;

			public int docStoreOffset = -1;

			public String docStoreSegment;

			public boolean docStoreCompoundFile;

			public boolean hasDeletions;

			public String deletionsFileName;

			public int numDeleted;

			public boolean openReaderPassed;

			int numFields;

			public boolean hasProx;

			public Map<String, String> diagnostics;

			public FieldNormStatus fieldNormStatus;

			public TermIndexStatus termIndexStatus;

			public StoredFieldStatus storedFieldStatus;

			public TermVectorStatus termVectorStatus;
		}

		public static final class FieldNormStatus {

			public long totFields = 0L;

			public Throwable error = null;
		}

		public static final class TermIndexStatus {

			public long termCount = 0L;

			public long totFreq = 0L;

			public long totPos = 0L;

			public Throwable error = null;
		}

		public static final class StoredFieldStatus {

			public int docCount = 0;

			public long totFields = 0;

			public Throwable error = null;
		}

		public static final class TermVectorStatus {

			public int docCount = 0;

			public long totVectors = 0;

			public Throwable error = null;
		}
	}

	public CheckIndex(Directory dir) {
		this.dir = dir;
		infoStream = null;
	}

	public void setInfoStream(PrintStream out) {
		infoStream = out;
	}

	private void msg(String msg) {
		if (infoStream != null)
			infoStream.println(msg);
	}

	private static class MySegmentTermDocs extends SegmentTermDocs {

		int delCount;

		MySegmentTermDocs(SegmentReader p) {
			super(p);
		}

		@Override
		public void seek(Term term) throws IOException {
			super.seek(term);
			delCount = 0;
		}

		@Override
		protected void skippingDoc() throws IOException {
			delCount++;
		}
	}

	public Status checkIndex() throws IOException {
		return checkIndex(null);
	}

	public Status checkIndex(List<String> onlySegments) throws IOException {
		NumberFormat nf = NumberFormat.getInstance();
		SegmentInfos sis = new SegmentInfos();
		Status result = new Status();
		result.dir = dir;
		try {
			sis.read(dir);
		} catch (Throwable t) {
			msg("ERROR: could not read any segments file in directory");
			result.missingSegments = true;
			if (infoStream != null)
				t.printStackTrace(infoStream);
			return result;
		}

		String oldest = Integer.toString(Integer.MAX_VALUE), newest = Integer
				.toString(Integer.MIN_VALUE);
		String oldSegs = null;
		boolean foundNonNullVersion = false;
		Comparator<String> versionComparator = StringHelper
				.getVersionComparator();
		for (SegmentInfo si : sis) {
			String version = si.getVersion();
			if (version == null) {

				oldSegs = "pre-3.1";
			} else if (version.equals("2.x")) {

				oldSegs = "2.x";
			} else {
				foundNonNullVersion = true;
				if (versionComparator.compare(version, oldest) < 0) {
					oldest = version;
				}
				if (versionComparator.compare(version, newest) > 0) {
					newest = version;
				}
			}
		}

		final int numSegments = sis.size();
		final String segmentsFileName = sis.getCurrentSegmentFileName();
		IndexInput input = null;
		try {
			input = dir.openInput(segmentsFileName);
		} catch (Throwable t) {
			msg("ERROR: could not open segments file in directory");
			if (infoStream != null)
				t.printStackTrace(infoStream);
			result.cantOpenSegments = true;
			return result;
		}
		int format = 0;
		try {
			format = input.readInt();
		} catch (Throwable t) {
			msg("ERROR: could not read segment file version in directory");
			if (infoStream != null)
				t.printStackTrace(infoStream);
			result.missingSegmentVersion = true;
			return result;
		} finally {
			if (input != null)
				input.close();
		}

		String sFormat = "";
		boolean skip = false;

		if (format == SegmentInfos.FORMAT)
			sFormat = "FORMAT [shatam Pre-2.1]";
		if (format == SegmentInfos.FORMAT_LOCKLESS)
			sFormat = "FORMAT_LOCKLESS [shatam 2.1]";
		else if (format == SegmentInfos.FORMAT_SINGLE_NORM_FILE)
			sFormat = "FORMAT_SINGLE_NORM_FILE [shatam 2.2]";
		else if (format == SegmentInfos.FORMAT_SHARED_DOC_STORE)
			sFormat = "FORMAT_SHARED_DOC_STORE [shatam 2.3]";
		else {
			if (format == SegmentInfos.FORMAT_CHECKSUM)
				sFormat = "FORMAT_CHECKSUM [shatam 2.4]";
			else if (format == SegmentInfos.FORMAT_DEL_COUNT)
				sFormat = "FORMAT_DEL_COUNT [shatam 2.4]";
			else if (format == SegmentInfos.FORMAT_HAS_PROX)
				sFormat = "FORMAT_HAS_PROX [shatam 2.4]";
			else if (format == SegmentInfos.FORMAT_USER_DATA)
				sFormat = "FORMAT_USER_DATA [shatam 2.9]";
			else if (format == SegmentInfos.FORMAT_DIAGNOSTICS)
				sFormat = "FORMAT_DIAGNOSTICS [shatam 2.9]";
			else if (format == SegmentInfos.FORMAT_HAS_VECTORS)
				sFormat = "FORMAT_HAS_VECTORS [shatam 3.1]";
			else if (format == SegmentInfos.FORMAT_3_1)
				sFormat = "FORMAT_3_1 [shatam 3.1+]";
			else if (format == SegmentInfos.CURRENT_FORMAT)
				throw new RuntimeException("BUG: You should update this tool!");
			else if (format < SegmentInfos.CURRENT_FORMAT) {
				sFormat = "int=" + format
						+ " [newer version of shatam than this tool]";
				skip = true;
			} else {
				sFormat = format + " [shatam 1.3 or prior]";
			}
		}

		result.segmentsFileName = segmentsFileName;
		result.numSegments = numSegments;
		result.segmentFormat = sFormat;
		result.userData = sis.getUserData();
		String userDataString;
		if (sis.getUserData().size() > 0) {
			userDataString = " userData=" + sis.getUserData();
		} else {
			userDataString = "";
		}

		String versionString = null;
		if (oldSegs != null) {
			if (foundNonNullVersion) {
				versionString = "versions=[" + oldSegs + " .. " + newest + "]";
			} else {
				versionString = "version=" + oldSegs;
			}
		} else {
			versionString = oldest.equals(newest) ? ("version=" + oldest)
					: ("versions=[" + oldest + " .. " + newest + "]");
		}

		msg("Segments file=" + segmentsFileName + " numSegments=" + numSegments
				+ " " + versionString + " format=" + sFormat + userDataString);

		if (onlySegments != null) {
			result.partial = true;
			if (infoStream != null)
				infoStream.print("\nChecking only these segments:");
			for (String s : onlySegments) {
				if (infoStream != null)
					infoStream.print(" " + s);
			}
			result.segmentsChecked.addAll(onlySegments);
			msg(":");
		}

		if (skip) {
			msg("\nERROR: this index appears to be created by a newer version of shatam than this tool was compiled on; please re-compile this tool on the matching version of shatam; exiting");
			result.toolOutOfDate = true;
			return result;
		}

		result.newSegments = (SegmentInfos) sis.clone();
		result.newSegments.clear();
		result.maxSegmentName = -1;

		for (int i = 0; i < numSegments; i++) {
			final SegmentInfo info = sis.info(i);
			int segmentName = Integer.parseInt(info.name.substring(1),
					Character.MAX_RADIX);
			if (segmentName > result.maxSegmentName) {
				result.maxSegmentName = segmentName;
			}
			if (onlySegments != null && !onlySegments.contains(info.name))
				continue;
			Status.SegmentInfoStatus segInfoStat = new Status.SegmentInfoStatus();
			result.segmentInfos.add(segInfoStat);
			msg("  " + (1 + i) + " of " + numSegments + ": name=" + info.name
					+ " docCount=" + info.docCount);
			segInfoStat.name = info.name;
			segInfoStat.docCount = info.docCount;

			int toLoseDocCount = info.docCount;

			SegmentReader reader = null;

			try {
				msg("    compound=" + info.getUseCompoundFile());
				segInfoStat.compound = info.getUseCompoundFile();
				msg("    hasProx=" + info.getHasProx());
				segInfoStat.hasProx = info.getHasProx();
				msg("    numFiles=" + info.files().size());
				segInfoStat.numFiles = info.files().size();
				segInfoStat.sizeMB = info.sizeInBytes(true) / (1024. * 1024.);
				msg("    size (MB)=" + nf.format(segInfoStat.sizeMB));
				Map<String, String> diagnostics = info.getDiagnostics();
				segInfoStat.diagnostics = diagnostics;
				if (diagnostics.size() > 0) {
					msg("    diagnostics = " + diagnostics);
				}

				final int docStoreOffset = info.getDocStoreOffset();
				if (docStoreOffset != -1) {
					msg("    docStoreOffset=" + docStoreOffset);
					segInfoStat.docStoreOffset = docStoreOffset;
					msg("    docStoreSegment=" + info.getDocStoreSegment());
					segInfoStat.docStoreSegment = info.getDocStoreSegment();
					msg("    docStoreIsCompoundFile="
							+ info.getDocStoreIsCompoundFile());
					segInfoStat.docStoreCompoundFile = info
							.getDocStoreIsCompoundFile();
				}
				final String delFileName = info.getDelFileName();
				if (delFileName == null) {
					msg("    no deletions");
					segInfoStat.hasDeletions = false;
				} else {
					msg("    has deletions [delFileName=" + delFileName + "]");
					segInfoStat.hasDeletions = true;
					segInfoStat.deletionsFileName = delFileName;
				}
				if (infoStream != null)
					infoStream.print("    test: open reader.........");
				reader = SegmentReader.get(true, info,
						IndexReader.DEFAULT_TERMS_INDEX_DIVISOR);

				segInfoStat.openReaderPassed = true;

				final int numDocs = reader.numDocs();
				toLoseDocCount = numDocs;
				if (reader.hasDeletions()) {
					if (reader.deletedDocs.count() != info.getDelCount()) {
						throw new RuntimeException(
								"delete count mismatch: info="
										+ info.getDelCount()
										+ " vs deletedDocs.count()="
										+ reader.deletedDocs.count());
					}
					if (reader.deletedDocs.count() > reader.maxDoc()) {
						throw new RuntimeException(
								"too many deleted docs: maxDoc()="
										+ reader.maxDoc()
										+ " vs deletedDocs.count()="
										+ reader.deletedDocs.count());
					}
					if (info.docCount - numDocs != info.getDelCount()) {
						throw new RuntimeException(
								"delete count mismatch: info="
										+ info.getDelCount() + " vs reader="
										+ (info.docCount - numDocs));
					}
					segInfoStat.numDeleted = info.docCount - numDocs;
					msg("OK [" + (segInfoStat.numDeleted) + " deleted docs]");
				} else {
					if (info.getDelCount() != 0) {
						throw new RuntimeException(
								"delete count mismatch: info="
										+ info.getDelCount() + " vs reader="
										+ (info.docCount - numDocs));
					}
					msg("OK");
				}
				if (reader.maxDoc() != info.docCount)
					throw new RuntimeException("SegmentReader.maxDoc() "
							+ reader.maxDoc() + " != SegmentInfos.docCount "
							+ info.docCount);

				if (infoStream != null) {
					infoStream.print("    test: fields..............");
				}
				Collection<String> fieldNames = reader
						.getFieldNames(IndexReader.FieldOption.ALL);
				msg("OK [" + fieldNames.size() + " fields]");
				segInfoStat.numFields = fieldNames.size();

				segInfoStat.fieldNormStatus = testFieldNorms(fieldNames, reader);

				segInfoStat.termIndexStatus = testTermIndex(info, reader);

				segInfoStat.storedFieldStatus = testStoredFields(info, reader,
						nf);

				segInfoStat.termVectorStatus = testTermVectors(info, reader, nf);

				if (segInfoStat.fieldNormStatus.error != null) {
					throw new RuntimeException("Field Norm test failed");
				} else if (segInfoStat.termIndexStatus.error != null) {
					throw new RuntimeException("Term Index test failed");
				} else if (segInfoStat.storedFieldStatus.error != null) {
					throw new RuntimeException("Stored Field test failed");
				} else if (segInfoStat.termVectorStatus.error != null) {
					throw new RuntimeException("Term Vector test failed");
				}

				msg("");

			} catch (Throwable t) {
				msg("FAILED");
				String comment;
				comment = "fixIndex() would remove reference to this segment";
				msg("    WARNING: " + comment + "; full exception:");
				if (infoStream != null)
					t.printStackTrace(infoStream);
				msg("");
				result.totLoseDocCount += toLoseDocCount;
				result.numBadSegments++;
				continue;
			} finally {
				if (reader != null)
					reader.close();
			}

			result.newSegments.add((SegmentInfo) info.clone());
		}

		if (0 == result.numBadSegments) {
			result.clean = true;
		} else
			msg("WARNING: " + result.numBadSegments
					+ " broken segments (containing " + result.totLoseDocCount
					+ " documents) detected");

		if (!(result.validCounter = (result.maxSegmentName < sis.counter))) {
			result.clean = false;
			result.newSegments.counter = result.maxSegmentName + 1;
			msg("ERROR: Next segment name counter " + sis.counter
					+ " is not greater than max segment name "
					+ result.maxSegmentName);
		}

		if (result.clean) {
			msg("No problems were detected with this index.\n");
		}

		return result;
	}

	private Status.FieldNormStatus testFieldNorms(
			Collection<String> fieldNames, SegmentReader reader) {
		final Status.FieldNormStatus status = new Status.FieldNormStatus();

		try {

			if (infoStream != null) {
				infoStream.print("    test: field norms.........");
			}
			final byte[] b = new byte[reader.maxDoc()];
			for (final String fieldName : fieldNames) {
				if (reader.hasNorms(fieldName)) {
					reader.norms(fieldName, b, 0);
					++status.totFields;
				}
			}

			msg("OK [" + status.totFields + " fields]");
		} catch (Throwable e) {
			msg("ERROR [" + String.valueOf(e.getMessage()) + "]");
			status.error = e;
			if (infoStream != null) {
				e.printStackTrace(infoStream);
			}
		}

		return status;
	}

	private Status.TermIndexStatus testTermIndex(SegmentInfo info,
			SegmentReader reader) {
		final Status.TermIndexStatus status = new Status.TermIndexStatus();

		final IndexSearcher is = new IndexSearcher(reader);

		try {
			if (infoStream != null) {
				infoStream.print("    test: terms, freq, prox...");
			}

			final TermEnum termEnum = reader.terms();
			final TermPositions termPositions = reader.termPositions();

			final MySegmentTermDocs myTermDocs = new MySegmentTermDocs(reader);

			final int maxDoc = reader.maxDoc();
			Term lastTerm = null;
			while (termEnum.next()) {
				status.termCount++;
				final Term term = termEnum.term();
				lastTerm = term;

				final int docFreq = termEnum.docFreq();
				if (docFreq <= 0) {
					throw new RuntimeException("docfreq: " + docFreq
							+ " is out of bounds");
				}
				termPositions.seek(term);
				int lastDoc = -1;
				int freq0 = 0;
				status.totFreq += docFreq;
				while (termPositions.next()) {
					freq0++;
					final int doc = termPositions.doc();
					final int freq = termPositions.freq();
					if (doc <= lastDoc)
						throw new RuntimeException("term " + term + ": doc "
								+ doc + " <= lastDoc " + lastDoc);
					if (doc >= maxDoc)
						throw new RuntimeException("term " + term + ": doc "
								+ doc + " >= maxDoc " + maxDoc);

					lastDoc = doc;
					if (freq <= 0)
						throw new RuntimeException("term " + term + ": doc "
								+ doc + ": freq " + freq + " is out of bounds");

					int lastPos = -1;
					status.totPos += freq;
					for (int j = 0; j < freq; j++) {
						final int pos = termPositions.nextPosition();
						if (pos < -1)
							throw new RuntimeException("term " + term
									+ ": doc " + doc + ": pos " + pos
									+ " is out of bounds");
						if (pos < lastPos)
							throw new RuntimeException("term " + term
									+ ": doc " + doc + ": pos " + pos
									+ " < lastPos " + lastPos);
						lastPos = pos;
					}
				}

				for (int idx = 0; idx < 7; idx++) {
					final int skipDocID = (int) (((idx + 1) * (long) maxDoc) / 8);
					termPositions.seek(term);
					if (!termPositions.skipTo(skipDocID)) {
						break;
					} else {

						final int docID = termPositions.doc();
						if (docID < skipDocID) {
							throw new RuntimeException("term " + term
									+ ": skipTo(docID=" + skipDocID
									+ ") returned docID=" + docID);
						}
						final int freq = termPositions.freq();
						if (freq <= 0) {
							throw new RuntimeException("termFreq " + freq
									+ " is out of bounds");
						}
						int lastPosition = -1;
						for (int posUpto = 0; posUpto < freq; posUpto++) {
							final int pos = termPositions.nextPosition();
							if (pos < 0) {
								throw new RuntimeException("position " + pos
										+ " is out of bounds");
							}

							if (pos < lastPosition) {
								throw new RuntimeException("position " + pos
										+ " is < lastPosition " + lastPosition);
							}
							lastPosition = pos;
						}

						if (!termPositions.next()) {
							break;
						}
						final int nextDocID = termPositions.doc();
						if (nextDocID <= docID) {
							throw new RuntimeException("term " + term
									+ ": skipTo(docID=" + skipDocID
									+ "), then .next() returned docID="
									+ nextDocID + " vs prev docID=" + docID);
						}
					}
				}

				final int delCount;
				if (reader.hasDeletions()) {
					myTermDocs.seek(term);
					while (myTermDocs.next()) {
					}
					delCount = myTermDocs.delCount;
				} else {
					delCount = 0;
				}

				if (freq0 + delCount != docFreq) {
					throw new RuntimeException("term " + term + " docFreq="
							+ docFreq + " != num docs seen " + freq0
							+ " + num docs deleted " + delCount);
				}
			}

			if (lastTerm != null) {
				is.search(new TermQuery(lastTerm), 1);
			}

			try {
				long uniqueTermCountAllFields = reader.getUniqueTermCount();
				if (status.termCount != uniqueTermCountAllFields) {
					throw new RuntimeException("termCount mismatch "
							+ uniqueTermCountAllFields + " vs "
							+ (status.termCount));
				}
			} catch (UnsupportedOperationException ex) {

			}

			msg("OK [" + status.termCount + " terms; " + status.totFreq
					+ " terms/docs pairs; " + status.totPos + " tokens]");

		} catch (Throwable e) {
			msg("ERROR [" + String.valueOf(e.getMessage()) + "]");
			status.error = e;
			if (infoStream != null) {
				e.printStackTrace(infoStream);
			}
		}

		return status;
	}

	private Status.StoredFieldStatus testStoredFields(SegmentInfo info,
			SegmentReader reader, NumberFormat format) {
		final Status.StoredFieldStatus status = new Status.StoredFieldStatus();

		try {
			if (infoStream != null) {
				infoStream.print("    test: stored fields.......");
			}

			for (int j = 0; j < info.docCount; ++j) {
				if (!reader.isDeleted(j)) {
					status.docCount++;
					Document doc = reader.document(j);
					status.totFields += doc.getFields().size();
				}
			}

			if (status.docCount != reader.numDocs()) {
				throw new RuntimeException("docCount=" + status.docCount
						+ " but saw " + status.docCount + " undeleted docs");
			}

			msg("OK ["
					+ status.totFields
					+ " total field count; avg "
					+ format.format((((float) status.totFields) / status.docCount))
					+ " fields per doc]");
		} catch (Throwable e) {
			msg("ERROR [" + String.valueOf(e.getMessage()) + "]");
			status.error = e;
			if (infoStream != null) {
				e.printStackTrace(infoStream);
			}
		}

		return status;
	}

	private Status.TermVectorStatus testTermVectors(SegmentInfo info,
			SegmentReader reader, NumberFormat format) {
		final Status.TermVectorStatus status = new Status.TermVectorStatus();

		try {
			if (infoStream != null) {
				infoStream.print("    test: term vectors........");
			}

			for (int j = 0; j < info.docCount; ++j) {
				if (!reader.isDeleted(j)) {
					status.docCount++;
					TermFreqVector[] tfv = reader.getTermFreqVectors(j);
					if (tfv != null) {
						status.totVectors += tfv.length;
					}
				}
			}

			msg("OK ["
					+ status.totVectors
					+ " total vector count; avg "
					+ format.format((((float) status.totVectors) / status.docCount))
					+ " term/freq vector fields per doc]");
		} catch (Throwable e) {
			msg("ERROR [" + String.valueOf(e.getMessage()) + "]");
			status.error = e;
			if (infoStream != null) {
				e.printStackTrace(infoStream);
			}
		}

		return status;
	}

	public void fixIndex(Status result) throws IOException {
		if (result.partial)
			throw new IllegalArgumentException(
					"can only fix an index that was fully checked (this status checked a subset of segments)");
		result.newSegments.changed();
		result.newSegments.commit(result.dir);
	}

	private static boolean assertsOn;

	private static boolean testAsserts() {
		assertsOn = true;
		return true;
	}

	private static boolean assertsOn() {
		assert testAsserts();
		return assertsOn;
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {

		boolean doFix = false;
		List<String> onlySegments = new ArrayList<String>();
		String indexPath = null;
		int i = 0;
		while (i < args.length) {
			if (args[i].equals("-fix")) {
				doFix = true;
				i++;
			} else if (args[i].equals("-segment")) {
				if (i == args.length - 1) {
					System.out
							.println("ERROR: missing name for -segment option");
					System.exit(1);
				}
				onlySegments.add(args[i + 1]);
				i += 2;
			} else {
				if (indexPath != null) {
					System.out.println("ERROR: unexpected extra argument '"
							+ args[i] + "'");
					System.exit(1);
				}
				indexPath = args[i];
				i++;
			}
		}

		if (indexPath == null) {
			System.out.println("\nERROR: index path not specified");
			System.out
					.println("\nUsage:shatam.index.CheckIndex pathToIndex [-fix] [-segment X] [-segment Y]\n"
							+ "\n"
							+ "  -fix: actually write a new segments_N file, removing any problematic segments\n"
							+ "  -segment X: only check the specified segments.  This can be specified multiple\n"
							+ "              times, to check more than one segment, eg '-segment _2 -segment _a'.\n"
							+ "              You can't use this with the -fix option\n"
							+ "\n"
							+ "**WARNING**: -fix should only be used on an emergency basis as it will cause\n"
							+ "documents (perhaps many) to be permanently removed from the index.  Always make\n"
							+ "a backup copy of your index before running this!  Do not run this tool on an index\n"
							+ "that is actively being written to.  You have been warned!\n"
							+ "\n"
							+ "Run without -fix, this tool will open the index, report version information\n"
							+ "and report any exceptions it hits and what action it would take if -fix were\n"
							+ "specified.  With -fix, this tool will remove any segments that have issues and\n"
							+ "write a new segments_N file.  This means all documents contained in the affected\n"
							+ "segments will be removed.\n"
							+ "\n"
							+ "This tool exits with exit code 1 if the index cannot be opened or has any\n"
							+ "corruption, else 0.\n");
			System.exit(1);
		}

		if (!assertsOn())
			System.out
					.println("\nNOTE: testing will be more thorough if you run java with '-shatam...', so assertions are enabled");

		if (onlySegments.size() == 0)
			onlySegments = null;
		else if (doFix) {
			System.out.println("ERROR: cannot specify both -fix and -segment");
			System.exit(1);
		}

		System.out.println("\nOpening index @ " + indexPath + "\n");
		Directory dir = null;
		try {
			dir = FSDirectory.open(new File(indexPath));
		} catch (Throwable t) {
			System.out.println("ERROR: could not open directory \"" + indexPath
					+ "\"; exiting");
			t.printStackTrace(System.out);
			System.exit(1);
		}

		CheckIndex checker = new CheckIndex(dir);
		checker.setInfoStream(System.out);

		Status result = checker.checkIndex(onlySegments);
		if (result.missingSegments) {
			System.exit(1);
		}

		if (!result.clean) {
			if (!doFix) {
				System.out
						.println("WARNING: would write new segments file, and "
								+ result.totLoseDocCount
								+ " documents would be lost, if -fix were specified\n");
			} else {
				System.out.println("WARNING: " + result.totLoseDocCount
						+ " documents will be lost\n");
				System.out
						.println("NOTE: will write new segments file in 5 seconds; this will remove "
								+ result.totLoseDocCount
								+ " docs from the index. THIS IS YOUR LAST CHANCE TO CTRL+C!");
				for (int s = 0; s < 5; s++) {
					Thread.sleep(1000);
					System.out.println("  " + (5 - s) + "...");
				}
				System.out.println("Writing...");
				checker.fixIndex(result);
				System.out.println("OK");
				System.out
						.println("Wrote new segments file \""
								+ result.newSegments
										.getCurrentSegmentFileName() + "\"");
			}
		}
		System.out.println("");

		final int exitCode;
		if (result.clean == true)
			exitCode = 0;
		else
			exitCode = 1;
		System.exit(exitCode);
	}
}
