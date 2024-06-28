/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.shatam.shatamindex.analysis.Analyzer;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.Similarity;
import com.shatam.shatamindex.store.AlreadyClosedException;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.RAMFile;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.BitVector;
import com.shatam.shatamindex.util.RamUsageEstimator;
import com.shatam.shatamindex.util.ThreadInterruptedException;

final class DocumentsWriter {
	final AtomicLong bytesUsed = new AtomicLong(0);
	IndexWriter writer;
	Directory directory;

	String segment;

	private int nextDocID;
	private int numDocs;

	private DocumentsWriterThreadState[] threadStates = new DocumentsWriterThreadState[0];
	private final HashMap<Thread, DocumentsWriterThreadState> threadBindings = new HashMap<Thread, DocumentsWriterThreadState>();

	boolean bufferIsFull;
	private boolean aborting;

	PrintStream infoStream;
	int maxFieldLength = IndexWriter.DEFAULT_MAX_FIELD_LENGTH;
	Similarity similarity;

	private final int maxThreadStates;

	private BufferedDeletes pendingDeletes = new BufferedDeletes();

	static class DocState {
		DocumentsWriter docWriter;
		Analyzer analyzer;
		int maxFieldLength;
		PrintStream infoStream;
		Similarity similarity;
		int docID;
		Document doc;
		String maxTermPrefix;

		public boolean testPoint(String name) {
			return docWriter.writer.testPoint(name);
		}

		public void clear() {

			doc = null;
			analyzer = null;
		}
	}

	abstract static class DocWriter {
		DocWriter next;
		int docID;

		abstract void finish() throws IOException;

		abstract void abort();

		abstract long sizeInBytes();

		void setNext(DocWriter next) {
			this.next = next;
		}
	}

	PerDocBuffer newPerDocBuffer() {
		return new PerDocBuffer();
	}

	class PerDocBuffer extends RAMFile {

		@Override
		protected byte[] newBuffer(int size) {
			assert size == PER_DOC_BLOCK_SIZE;
			return perDocAllocator.getByteBlock();
		}

		synchronized void recycle() {
			if (buffers.size() > 0) {
				setLength(0);

				perDocAllocator.recycleByteBlocks(buffers);
				buffers.clear();
				sizeInBytes = 0;

				assert numBuffers() == 0;
			}
		}
	}

	abstract static class IndexingChain {
		abstract DocConsumer getChain(DocumentsWriter documentsWriter);
	}

	static final IndexingChain defaultIndexingChain = new IndexingChain() {

		@Override
		DocConsumer getChain(DocumentsWriter documentsWriter) {

			final TermsHashConsumer termVectorsWriter = new TermVectorsTermsWriter(
					documentsWriter);
			final TermsHashConsumer freqProxWriter = new FreqProxTermsWriter();

			final InvertedDocConsumer termsHash = new TermsHash(
					documentsWriter, true, freqProxWriter, new TermsHash(
							documentsWriter, false, termVectorsWriter, null));
			final NormsWriter normsWriter = new NormsWriter();
			final DocInverter docInverter = new DocInverter(termsHash,
					normsWriter);
			return new DocFieldProcessor(documentsWriter, docInverter);
		}
	};

	final DocConsumer consumer;

	private final IndexWriterConfig config;

	private boolean closed;
	private final FieldInfos fieldInfos;

	private final BufferedDeletesStream bufferedDeletesStream;
	private final IndexWriter.FlushControl flushControl;

	DocumentsWriter(IndexWriterConfig config, Directory directory,
			IndexWriter writer, FieldInfos fieldInfos,
			BufferedDeletesStream bufferedDeletesStream) throws IOException {
		this.directory = directory;
		this.writer = writer;
		this.similarity = config.getSimilarity();
		this.maxThreadStates = config.getMaxThreadStates();
		this.fieldInfos = fieldInfos;
		this.bufferedDeletesStream = bufferedDeletesStream;
		flushControl = writer.flushControl;

		consumer = config.getIndexingChain().getChain(this);
		this.config = config;
	}

	synchronized void deleteDocID(int docIDUpto) {
		pendingDeletes.addDocID(docIDUpto);

	}

	boolean deleteQueries(Query... queries) {
		final boolean doFlush = flushControl.waitUpdate(0, queries.length);
		synchronized (this) {
			for (Query query : queries) {
				pendingDeletes.addQuery(query, numDocs);
			}
		}
		return doFlush;
	}

	boolean deleteQuery(Query query) {
		final boolean doFlush = flushControl.waitUpdate(0, 1);
		synchronized (this) {
			pendingDeletes.addQuery(query, numDocs);
		}
		return doFlush;
	}

	boolean deleteTerms(Term... terms) {
		final boolean doFlush = flushControl.waitUpdate(0, terms.length);
		synchronized (this) {
			for (Term term : terms) {
				pendingDeletes.addTerm(term, numDocs);
			}
		}
		return doFlush;
	}

	boolean deleteTerm(Term term, boolean skipWait) {
		final boolean doFlush = flushControl.waitUpdate(0, 1, skipWait);
		synchronized (this) {
			pendingDeletes.addTerm(term, numDocs);
		}
		return doFlush;
	}

	public FieldInfos getFieldInfos() {
		return fieldInfos;
	}

	synchronized void setInfoStream(PrintStream infoStream) {
		this.infoStream = infoStream;
		for (int i = 0; i < threadStates.length; i++) {
			threadStates[i].docState.infoStream = infoStream;
		}
	}

	synchronized void setMaxFieldLength(int maxFieldLength) {
		this.maxFieldLength = maxFieldLength;
		for (int i = 0; i < threadStates.length; i++) {
			threadStates[i].docState.maxFieldLength = maxFieldLength;
		}
	}

	synchronized void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
		for (int i = 0; i < threadStates.length; i++) {
			threadStates[i].docState.similarity = similarity;
		}
	}

	synchronized String getSegment() {
		return segment;
	}

	synchronized int getNumDocs() {
		return numDocs;
	}

	void message(String message) {
		if (infoStream != null) {
			writer.message("DW: " + message);
		}
	}

	synchronized void setAborting() {
		if (infoStream != null) {
			message("setAborting");
		}
		aborting = true;
	}

	synchronized void abort() throws IOException {
		if (infoStream != null) {
			message("docWriter: abort");
		}

		boolean success = false;

		try {

			try {
				waitQueue.abort();
			} catch (Throwable t) {
			}

			try {
				waitIdle();
			} finally {
				if (infoStream != null) {
					message("docWriter: abort waitIdle done");
				}

				assert 0 == waitQueue.numWaiting : "waitQueue.numWaiting="
						+ waitQueue.numWaiting;
				waitQueue.waitingBytes = 0;

				pendingDeletes.clear();

				for (DocumentsWriterThreadState threadState : threadStates) {
					try {
						threadState.consumer.abort();
					} catch (Throwable t) {
					}
				}

				try {
					consumer.abort();
				} catch (Throwable t) {
				}

				doAfterFlush();
			}

			success = true;
		} finally {
			aborting = false;
			notifyAll();
			if (infoStream != null) {
				message("docWriter: done abort; success=" + success);
			}
		}
	}

	private void doAfterFlush() throws IOException {

		assert allThreadsIdle();
		threadBindings.clear();
		waitQueue.reset();
		segment = null;
		numDocs = 0;
		nextDocID = 0;
		bufferIsFull = false;
		for (int i = 0; i < threadStates.length; i++) {
			threadStates[i].doAfterFlush();
		}
	}

	private synchronized boolean allThreadsIdle() {
		for (int i = 0; i < threadStates.length; i++) {
			if (!threadStates[i].isIdle) {
				return false;
			}
		}
		return true;
	}

	synchronized boolean anyChanges() {
		return numDocs != 0 || pendingDeletes.any();
	}

	public BufferedDeletes getPendingDeletes() {
		return pendingDeletes;
	}

	private void pushDeletes(SegmentInfo newSegment, SegmentInfos segmentInfos) {

		final long delGen = bufferedDeletesStream.getNextGen();
		if (pendingDeletes.any()) {
			if (segmentInfos.size() > 0 || newSegment != null) {
				final FrozenBufferedDeletes packet = new FrozenBufferedDeletes(
						pendingDeletes, delGen);
				if (infoStream != null) {
					message("flush: push buffered deletes startSize="
							+ pendingDeletes.bytesUsed.get() + " frozenSize="
							+ packet.bytesUsed);
				}
				bufferedDeletesStream.push(packet);
				if (infoStream != null) {
					message("flush: delGen=" + packet.gen);
				}
				if (newSegment != null) {
					newSegment.setBufferedDeletesGen(packet.gen);
				}
			} else {
				if (infoStream != null) {
					message("flush: drop buffered deletes: no segments");
				}

			}
			pendingDeletes.clear();
		} else if (newSegment != null) {
			newSegment.setBufferedDeletesGen(delGen);
		}
	}

	public boolean anyDeletions() {
		return pendingDeletes.any();
	}

	synchronized SegmentInfo flush(IndexWriter writer,
			IndexFileDeleter deleter, MergePolicy mergePolicy,
			SegmentInfos segmentInfos) throws IOException {

		final long startTime = System.currentTimeMillis();

		assert Thread.holdsLock(writer);

		waitIdle();

		if (numDocs == 0) {

			if (infoStream != null) {
				message("flush: no docs; skipping");
			}

			pushDeletes(null, segmentInfos);
			return null;
		}

		if (aborting) {
			if (infoStream != null) {
				message("flush: skip because aborting is set");
			}
			return null;
		}

		boolean success = false;

		SegmentInfo newSegment;

		try {

			assert nextDocID == numDocs : "nextDocID=" + nextDocID
					+ " numDocs=" + numDocs;
			assert waitQueue.numWaiting == 0 : "numWaiting="
					+ waitQueue.numWaiting;
			assert waitQueue.waitingBytes == 0;

			if (infoStream != null) {
				message("flush postings as segment " + segment + " numDocs="
						+ numDocs);
			}

			final SegmentWriteState flushState = new SegmentWriteState(
					infoStream, directory, segment, fieldInfos, numDocs, writer
							.getConfig().getTermIndexInterval(), pendingDeletes);

			if (pendingDeletes.docIDs.size() > 0) {
				flushState.deletedDocs = new BitVector(numDocs);
				for (int delDocID : pendingDeletes.docIDs) {
					flushState.deletedDocs.set(delDocID);
				}
				pendingDeletes.bytesUsed.addAndGet(-pendingDeletes.docIDs
						.size() * BufferedDeletes.BYTES_PER_DEL_DOCID);
				pendingDeletes.docIDs.clear();
			}

			newSegment = new SegmentInfo(segment, numDocs, directory, false,
					true, fieldInfos.hasProx(), false);

			Collection<DocConsumerPerThread> threads = new HashSet<DocConsumerPerThread>();
			for (DocumentsWriterThreadState threadState : threadStates) {
				threads.add(threadState.consumer);
			}

			double startMBUsed = bytesUsed() / 1024. / 1024.;

			consumer.flush(threads, flushState);

			newSegment.setHasVectors(flushState.hasVectors);

			if (infoStream != null) {
				message("new segment has "
						+ (flushState.hasVectors ? "vectors" : "no vectors"));
				if (flushState.deletedDocs != null) {
					message("new segment has " + flushState.deletedDocs.count()
							+ " deleted docs");
				}
				message("flushedFiles=" + newSegment.files());
			}

			if (mergePolicy.useCompoundFile(segmentInfos, newSegment)) {
				final String cfsFileName = IndexFileNames.segmentFileName(
						segment, IndexFileNames.COMPOUND_FILE_EXTENSION);

				if (infoStream != null) {
					message("flush: create compound file \"" + cfsFileName
							+ "\"");
				}

				CompoundFileWriter cfsWriter = new CompoundFileWriter(
						directory, cfsFileName);
				for (String fileName : newSegment.files()) {
					cfsWriter.addFile(fileName);
				}
				cfsWriter.close();
				deleter.deleteNewFiles(newSegment.files());
				newSegment.setUseCompoundFile(true);
			}

			if (flushState.deletedDocs != null) {
				final int delCount = flushState.deletedDocs.count();
				assert delCount > 0;
				newSegment.setDelCount(delCount);
				newSegment.advanceDelGen();
				final String delFileName = newSegment.getDelFileName();
				if (infoStream != null) {
					message("flush: write " + delCount + " deletes to "
							+ delFileName);
				}
				boolean success2 = false;
				try {

					flushState.deletedDocs.write(directory, delFileName);
					success2 = true;
				} finally {
					if (!success2) {
						try {
							directory.deleteFile(delFileName);
						} catch (Throwable t) {

						}
					}
				}
			}

			if (infoStream != null) {
				message("flush: segment=" + newSegment);
				final double newSegmentSizeNoStore = newSegment
						.sizeInBytes(false) / 1024. / 1024.;
				final double newSegmentSize = newSegment.sizeInBytes(true) / 1024. / 1024.;
				message("  ramUsed="
						+ nf.format(startMBUsed)
						+ " MB"
						+ " newFlushedSize="
						+ nf.format(newSegmentSize)
						+ " MB"
						+ " ("
						+ nf.format(newSegmentSizeNoStore)
						+ " MB w/o doc stores)"
						+ " docs/MB="
						+ nf.format(numDocs / newSegmentSize)
						+ " new/old="
						+ nf.format(100.0 * newSegmentSizeNoStore / startMBUsed)
						+ "%");
			}

			success = true;
		} finally {
			notifyAll();
			if (!success) {
				if (segment != null) {
					deleter.refresh(segment);
				}
				abort();
			}
		}

		doAfterFlush();

		pushDeletes(newSegment, segmentInfos);
		if (infoStream != null) {
			message("flush time " + (System.currentTimeMillis() - startTime)
					+ " msec");
		}

		return newSegment;
	}

	synchronized void close() {
		closed = true;
		notifyAll();
	}

	synchronized DocumentsWriterThreadState getThreadState(Term delTerm,
			int docCount) throws IOException {

		final Thread currentThread = Thread.currentThread();
		assert !Thread.holdsLock(writer);

		DocumentsWriterThreadState state = threadBindings.get(currentThread);
		if (state == null) {

			DocumentsWriterThreadState minThreadState = null;
			for (int i = 0; i < threadStates.length; i++) {
				DocumentsWriterThreadState ts = threadStates[i];
				if (minThreadState == null
						|| ts.numThreads < minThreadState.numThreads) {
					minThreadState = ts;
				}
			}
			if (minThreadState != null
					&& (minThreadState.numThreads == 0 || threadStates.length >= maxThreadStates)) {
				state = minThreadState;
				state.numThreads++;
			} else {

				DocumentsWriterThreadState[] newArray = new DocumentsWriterThreadState[1 + threadStates.length];
				if (threadStates.length > 0) {
					System.arraycopy(threadStates, 0, newArray, 0,
							threadStates.length);
				}
				state = newArray[threadStates.length] = new DocumentsWriterThreadState(
						this);
				threadStates = newArray;
			}
			threadBindings.put(currentThread, state);
		}

		waitReady(state);

		if (segment == null) {
			segment = writer.newSegmentName();
			assert numDocs == 0;
		}

		state.docState.docID = nextDocID;
		nextDocID += docCount;

		if (delTerm != null) {
			pendingDeletes.addTerm(delTerm, state.docState.docID);
		}

		numDocs += docCount;
		state.isIdle = false;
		return state;
	}

	boolean addDocument(Document doc, Analyzer analyzer)
			throws CorruptIndexException, IOException {
		return updateDocument(doc, analyzer, null);
	}

	boolean updateDocument(Document doc, Analyzer analyzer, Term delTerm)
			throws CorruptIndexException, IOException {

		boolean doFlush = flushControl.waitUpdate(1, delTerm != null ? 1 : 0);

		final DocumentsWriterThreadState state = getThreadState(delTerm, 1);

		final DocState docState = state.docState;
		docState.doc = doc;
		docState.analyzer = analyzer;

		boolean success = false;
		try {

			final DocWriter perDoc;
			try {
				perDoc = state.consumer.processDocument();
			} finally {
				docState.clear();
			}

			finishDocument(state, perDoc);

			success = true;
		} finally {
			if (!success) {

				if (doFlush) {
					flushControl.clearFlushPending();
				}

				if (infoStream != null) {
					message("exception in updateDocument aborting=" + aborting);
				}

				synchronized (this) {

					state.isIdle = true;
					notifyAll();

					if (aborting) {
						abort();
					} else {
						skipDocWriter.docID = docState.docID;
						boolean success2 = false;
						try {
							waitQueue.add(skipDocWriter);
							success2 = true;
						} finally {
							if (!success2) {
								abort();
								return false;
							}
						}

						deleteDocID(state.docState.docID);
					}
				}
			}
		}

		doFlush |= flushControl.flushByRAMUsage("new document");

		return doFlush;
	}

	boolean updateDocuments(Collection<Document> docs, Analyzer analyzer,
			Term delTerm) throws CorruptIndexException, IOException {

		boolean doFlush = flushControl.waitUpdate(docs.size(),
				delTerm != null ? 1 : 0);

		final int docCount = docs.size();

		final DocumentsWriterThreadState state = getThreadState(null, docCount);
		final DocState docState = state.docState;

		final int startDocID = docState.docID;
		int docID = startDocID;

		for (Document doc : docs) {
			docState.doc = doc;
			docState.analyzer = analyzer;

			docState.docID = docID++;

			boolean success = false;
			try {

				final DocWriter perDoc;
				try {
					perDoc = state.consumer.processDocument();
				} finally {
					docState.clear();
				}

				balanceRAM();

				synchronized (this) {
					if (aborting) {
						break;
					}
					assert perDoc == null || perDoc.docID == docState.docID;
					final boolean doPause;
					if (perDoc != null) {
						waitQueue.add(perDoc);
					} else {
						skipDocWriter.docID = docState.docID;
						waitQueue.add(skipDocWriter);
					}
				}

				success = true;
			} finally {
				if (!success) {

					if (doFlush) {
						message("clearFlushPending!");
						flushControl.clearFlushPending();
					}

					if (infoStream != null) {
						message("exception in updateDocuments aborting="
								+ aborting);
					}

					synchronized (this) {

						state.isIdle = true;
						notifyAll();

						if (aborting) {
							abort();
						} else {

							final int endDocID = startDocID + docCount;
							docID = docState.docID;
							while (docID < endDocID) {
								skipDocWriter.docID = docID++;
								boolean success2 = false;
								try {
									waitQueue.add(skipDocWriter);
									success2 = true;
								} finally {
									if (!success2) {
										abort();
										return false;
									}
								}
							}

							docID = startDocID;
							while (docID < startDocID + docs.size()) {
								deleteDocID(docID++);
							}
						}
					}
				}
			}
		}

		synchronized (this) {

			if (waitQueue.doPause()) {
				waitForWaitQueue();
			}

			if (aborting) {

				state.isIdle = true;

				notifyAll();

				abort();

				if (doFlush) {
					message("clearFlushPending!");
					flushControl.clearFlushPending();
				}

				return false;
			}

			if (delTerm != null) {
				pendingDeletes.addTerm(delTerm, startDocID);
			}

			state.isIdle = true;

			notifyAll();
		}

		doFlush |= flushControl.flushByRAMUsage("new document");

		return doFlush;
	}

	public synchronized void waitIdle() {
		while (!allThreadsIdle()) {
			try {
				wait();
			} catch (InterruptedException ie) {
				throw new ThreadInterruptedException(ie);
			}
		}
	}

	synchronized void waitReady(DocumentsWriterThreadState state) {
		while (!closed && (!state.isIdle || aborting)) {
			try {
				wait();
			} catch (InterruptedException ie) {
				throw new ThreadInterruptedException(ie);
			}
		}

		if (closed) {
			throw new AlreadyClosedException("this IndexWriter is closed");
		}
	}

	private void finishDocument(DocumentsWriterThreadState perThread,
			DocWriter docWriter) throws IOException {

		balanceRAM();

		synchronized (this) {

			assert docWriter == null
					|| docWriter.docID == perThread.docState.docID;

			if (aborting) {

				if (docWriter != null) {
					try {
						docWriter.abort();
					} catch (Throwable t) {
					}
				}

				perThread.isIdle = true;

				notifyAll();

				return;
			}

			final boolean doPause;

			if (docWriter != null) {
				doPause = waitQueue.add(docWriter);
			} else {
				skipDocWriter.docID = perThread.docState.docID;
				doPause = waitQueue.add(skipDocWriter);
			}

			if (doPause) {
				waitForWaitQueue();
			}

			perThread.isIdle = true;

			notifyAll();
		}
	}

	synchronized void waitForWaitQueue() {
		do {
			try {
				wait();
			} catch (InterruptedException ie) {
				throw new ThreadInterruptedException(ie);
			}
		} while (!waitQueue.doResume());
	}

	private static class SkipDocWriter extends DocWriter {
		@Override
		void finish() {
		}

		@Override
		void abort() {
		}

		@Override
		long sizeInBytes() {
			return 0;
		}
	}

	final SkipDocWriter skipDocWriter = new SkipDocWriter();

	NumberFormat nf = NumberFormat.getInstance();

	final static int BYTE_BLOCK_SHIFT = 15;
	final static int BYTE_BLOCK_SIZE = 1 << BYTE_BLOCK_SHIFT;
	final static int BYTE_BLOCK_MASK = BYTE_BLOCK_SIZE - 1;
	final static int BYTE_BLOCK_NOT_MASK = ~BYTE_BLOCK_MASK;

	private class ByteBlockAllocator extends ByteBlockPool.Allocator {
		final int blockSize;

		ByteBlockAllocator(int blockSize) {
			this.blockSize = blockSize;
		}

		ArrayList<byte[]> freeByteBlocks = new ArrayList<byte[]>();

		@Override
		byte[] getByteBlock() {
			synchronized (DocumentsWriter.this) {
				final int size = freeByteBlocks.size();
				final byte[] b;
				if (0 == size) {
					b = new byte[blockSize];
					bytesUsed.addAndGet(blockSize);
				} else
					b = freeByteBlocks.remove(size - 1);
				return b;
			}
		}

		@Override
		void recycleByteBlocks(byte[][] blocks, int start, int end) {
			synchronized (DocumentsWriter.this) {
				for (int i = start; i < end; i++) {
					freeByteBlocks.add(blocks[i]);
					blocks[i] = null;
				}
			}
		}

		@Override
		void recycleByteBlocks(List<byte[]> blocks) {
			synchronized (DocumentsWriter.this) {
				final int size = blocks.size();
				for (int i = 0; i < size; i++) {
					freeByteBlocks.add(blocks.get(i));
					blocks.set(i, null);
				}
			}
		}
	}

	final static int INT_BLOCK_SHIFT = 13;
	final static int INT_BLOCK_SIZE = 1 << INT_BLOCK_SHIFT;
	final static int INT_BLOCK_MASK = INT_BLOCK_SIZE - 1;

	private List<int[]> freeIntBlocks = new ArrayList<int[]>();

	synchronized int[] getIntBlock() {
		final int size = freeIntBlocks.size();
		final int[] b;
		if (0 == size) {
			b = new int[INT_BLOCK_SIZE];
			bytesUsed.addAndGet(INT_BLOCK_SIZE
					* RamUsageEstimator.NUM_BYTES_INT);
		} else {
			b = freeIntBlocks.remove(size - 1);
		}
		return b;
	}

	synchronized void bytesUsed(long numBytes) {
		bytesUsed.addAndGet(numBytes);
	}

	long bytesUsed() {
		return bytesUsed.get() + pendingDeletes.bytesUsed.get();
	}

	synchronized void recycleIntBlocks(int[][] blocks, int start, int end) {
		for (int i = start; i < end; i++) {
			freeIntBlocks.add(blocks[i]);
			blocks[i] = null;
		}
	}

	ByteBlockAllocator byteBlockAllocator = new ByteBlockAllocator(
			BYTE_BLOCK_SIZE);

	final static int PER_DOC_BLOCK_SIZE = 1024;

	final ByteBlockAllocator perDocAllocator = new ByteBlockAllocator(
			PER_DOC_BLOCK_SIZE);

	final static int CHAR_BLOCK_SHIFT = 14;
	final static int CHAR_BLOCK_SIZE = 1 << CHAR_BLOCK_SHIFT;
	final static int CHAR_BLOCK_MASK = CHAR_BLOCK_SIZE - 1;

	final static int MAX_TERM_LENGTH = CHAR_BLOCK_SIZE - 1;

	private ArrayList<char[]> freeCharBlocks = new ArrayList<char[]>();

	synchronized char[] getCharBlock() {
		final int size = freeCharBlocks.size();
		final char[] c;
		if (0 == size) {
			bytesUsed.addAndGet(CHAR_BLOCK_SIZE
					* RamUsageEstimator.NUM_BYTES_CHAR);
			c = new char[CHAR_BLOCK_SIZE];
		} else
			c = freeCharBlocks.remove(size - 1);

		return c;
	}

	synchronized void recycleCharBlocks(char[][] blocks, int numBlocks) {
		for (int i = 0; i < numBlocks; i++) {
			freeCharBlocks.add(blocks[i]);
			blocks[i] = null;
		}
	}

	String toMB(long v) {
		return nf.format(v / 1024. / 1024.);
	}

	void balanceRAM() {

		final boolean doBalance;
		final long deletesRAMUsed;

		deletesRAMUsed = bufferedDeletesStream.bytesUsed();

		final long ramBufferSize;
		final double mb = config.getRAMBufferSizeMB();
		if (mb == IndexWriterConfig.DISABLE_AUTO_FLUSH) {
			ramBufferSize = IndexWriterConfig.DISABLE_AUTO_FLUSH;
		} else {
			ramBufferSize = (long) (mb * 1024 * 1024);
		}

		synchronized (this) {
			if (ramBufferSize == IndexWriterConfig.DISABLE_AUTO_FLUSH
					|| bufferIsFull) {
				return;
			}

			doBalance = bytesUsed() + deletesRAMUsed >= ramBufferSize;
		}

		if (doBalance) {

			if (infoStream != null) {
				message("  RAM: balance allocations: usedMB="
						+ toMB(bytesUsed())
						+ " vs trigger="
						+ toMB(ramBufferSize)
						+ " deletesMB="
						+ toMB(deletesRAMUsed)
						+ " byteBlockFree="
						+ toMB(byteBlockAllocator.freeByteBlocks.size()
								* BYTE_BLOCK_SIZE)
						+ " perDocFree="
						+ toMB(perDocAllocator.freeByteBlocks.size()
								* PER_DOC_BLOCK_SIZE)
						+ " charBlockFree="
						+ toMB(freeCharBlocks.size() * CHAR_BLOCK_SIZE
								* RamUsageEstimator.NUM_BYTES_CHAR));
			}

			final long startBytesUsed = bytesUsed() + deletesRAMUsed;

			int iter = 0;

			boolean any = true;

			final long freeLevel = (long) (0.95 * ramBufferSize);

			while (bytesUsed() + deletesRAMUsed > freeLevel) {

				synchronized (this) {
					if (0 == perDocAllocator.freeByteBlocks.size()
							&& 0 == byteBlockAllocator.freeByteBlocks.size()
							&& 0 == freeCharBlocks.size()
							&& 0 == freeIntBlocks.size() && !any) {

						bufferIsFull = bytesUsed() + deletesRAMUsed > ramBufferSize;
						if (infoStream != null) {
							if (bytesUsed() + deletesRAMUsed > ramBufferSize) {
								message("    nothing to free; set bufferIsFull");
							} else {
								message("    nothing to free");
							}
						}
						break;
					}

					if ((0 == iter % 5)
							&& byteBlockAllocator.freeByteBlocks.size() > 0) {
						byteBlockAllocator.freeByteBlocks
								.remove(byteBlockAllocator.freeByteBlocks
										.size() - 1);
						bytesUsed.addAndGet(-BYTE_BLOCK_SIZE);
					}

					if ((1 == iter % 5) && freeCharBlocks.size() > 0) {
						freeCharBlocks.remove(freeCharBlocks.size() - 1);
						bytesUsed.addAndGet(-CHAR_BLOCK_SIZE
								* RamUsageEstimator.NUM_BYTES_CHAR);
					}

					if ((2 == iter % 5) && freeIntBlocks.size() > 0) {
						freeIntBlocks.remove(freeIntBlocks.size() - 1);
						bytesUsed.addAndGet(-INT_BLOCK_SIZE
								* RamUsageEstimator.NUM_BYTES_INT);
					}

					if ((3 == iter % 5)
							&& perDocAllocator.freeByteBlocks.size() > 0) {

						for (int i = 0; i < 32; ++i) {
							perDocAllocator.freeByteBlocks
									.remove(perDocAllocator.freeByteBlocks
											.size() - 1);
							bytesUsed.addAndGet(-PER_DOC_BLOCK_SIZE);
							if (perDocAllocator.freeByteBlocks.size() == 0) {
								break;
							}
						}
					}
				}

				if ((4 == iter % 5) && any) {

					any = consumer.freeRAM();
				}

				iter++;
			}

			if (infoStream != null) {
				message("    after free: freedMB="
						+ nf.format((startBytesUsed - bytesUsed() - deletesRAMUsed) / 1024. / 1024.)
						+ " usedMB="
						+ nf.format((bytesUsed() + deletesRAMUsed) / 1024. / 1024.));
			}
		}
	}

	final WaitQueue waitQueue = new WaitQueue();

	private class WaitQueue {
		DocWriter[] waiting;
		int nextWriteDocID;
		int nextWriteLoc;
		int numWaiting;
		long waitingBytes;

		public WaitQueue() {
			waiting = new DocWriter[10];
		}

		synchronized void reset() {

			assert numWaiting == 0;
			assert waitingBytes == 0;
			nextWriteDocID = 0;
		}

		synchronized boolean doResume() {
			final double mb = config.getRAMBufferSizeMB();
			final long waitQueueResumeBytes;
			if (mb == IndexWriterConfig.DISABLE_AUTO_FLUSH) {
				waitQueueResumeBytes = 2 * 1024 * 1024;
			} else {
				waitQueueResumeBytes = (long) (mb * 1024 * 1024 * 0.05);
			}
			return waitingBytes <= waitQueueResumeBytes;
		}

		synchronized boolean doPause() {
			final double mb = config.getRAMBufferSizeMB();
			final long waitQueuePauseBytes;
			if (mb == IndexWriterConfig.DISABLE_AUTO_FLUSH) {
				waitQueuePauseBytes = 4 * 1024 * 1024;
			} else {
				waitQueuePauseBytes = (long) (mb * 1024 * 1024 * 0.1);
			}
			return waitingBytes > waitQueuePauseBytes;
		}

		synchronized void abort() {
			int count = 0;
			for (int i = 0; i < waiting.length; i++) {
				final DocWriter doc = waiting[i];
				if (doc != null) {
					doc.abort();
					waiting[i] = null;
					count++;
				}
			}
			waitingBytes = 0;
			assert count == numWaiting;
			numWaiting = 0;
		}

		private void writeDocument(DocWriter doc) throws IOException {
			assert doc == skipDocWriter || nextWriteDocID == doc.docID;
			boolean success = false;
			try {
				doc.finish();
				nextWriteDocID++;
				nextWriteLoc++;
				assert nextWriteLoc <= waiting.length;
				if (nextWriteLoc == waiting.length) {
					nextWriteLoc = 0;
				}
				success = true;
			} finally {
				if (!success) {
					setAborting();
				}
			}
		}

		synchronized public boolean add(DocWriter doc) throws IOException {

			assert doc.docID >= nextWriteDocID;

			if (doc.docID == nextWriteDocID) {
				writeDocument(doc);
				while (true) {
					doc = waiting[nextWriteLoc];
					if (doc != null) {
						numWaiting--;
						waiting[nextWriteLoc] = null;
						waitingBytes -= doc.sizeInBytes();
						writeDocument(doc);
					} else {
						break;
					}
				}
			} else {

				int gap = doc.docID - nextWriteDocID;
				if (gap >= waiting.length) {

					DocWriter[] newArray = new DocWriter[ArrayUtil.oversize(
							gap, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
					assert nextWriteLoc >= 0;
					System.arraycopy(waiting, nextWriteLoc, newArray, 0,
							waiting.length - nextWriteLoc);
					System.arraycopy(waiting, 0, newArray, waiting.length
							- nextWriteLoc, nextWriteLoc);
					nextWriteLoc = 0;
					waiting = newArray;
					gap = doc.docID - nextWriteDocID;
				}

				int loc = nextWriteLoc + gap;
				if (loc >= waiting.length) {
					loc -= waiting.length;
				}

				assert loc < waiting.length;

				assert waiting[loc] == null;
				waiting[loc] = doc;
				numWaiting++;
				waitingBytes += doc.sizeInBytes();
			}

			return doPause();
		}
	}
}
