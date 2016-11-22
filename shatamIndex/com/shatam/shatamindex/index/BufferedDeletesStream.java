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
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.shatam.shatamindex.search.DocIdSet;
import com.shatam.shatamindex.search.DocIdSetIterator;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.search.QueryWrapperFilter;
import com.shatam.shatamindex.util.BytesRef;

class BufferedDeletesStream {

	private final List<FrozenBufferedDeletes> deletes = new ArrayList<FrozenBufferedDeletes>();

	private long nextGen = 1;

	private Term lastDeleteTerm;

	private PrintStream infoStream;
	private final AtomicLong bytesUsed = new AtomicLong();
	private final AtomicInteger numTerms = new AtomicInteger();
	private final int messageID;

	public BufferedDeletesStream(int messageID) {
		this.messageID = messageID;
	}

	private synchronized void message(String message) {
		if (infoStream != null) {
			infoStream.println("BD " + messageID + " [" + new Date() + "; "
					+ Thread.currentThread().getName() + "]: " + message);
		}
	}

	public synchronized void setInfoStream(PrintStream infoStream) {
		this.infoStream = infoStream;
	}

	public synchronized void push(FrozenBufferedDeletes packet) {
		assert packet.any();
		assert checkDeleteStats();
		assert packet.gen < nextGen;
		deletes.add(packet);
		numTerms.addAndGet(packet.numTermDeletes);
		bytesUsed.addAndGet(packet.bytesUsed);
		if (infoStream != null) {
			message("push deletes " + packet + " delGen=" + packet.gen
					+ " packetCount=" + deletes.size());
		}
		assert checkDeleteStats();
	}

	public synchronized void clear() {
		deletes.clear();
		nextGen = 1;
		numTerms.set(0);
		bytesUsed.set(0);
	}

	public boolean any() {
		return bytesUsed.get() != 0;
	}

	public int numTerms() {
		return numTerms.get();
	}

	public long bytesUsed() {
		return bytesUsed.get();
	}

	public static class ApplyDeletesResult {

		public final boolean anyDeletes;

		public final long gen;

		public final List<SegmentInfo> allDeleted;

		ApplyDeletesResult(boolean anyDeletes, long gen,
				List<SegmentInfo> allDeleted) {
			this.anyDeletes = anyDeletes;
			this.gen = gen;
			this.allDeleted = allDeleted;
		}
	}

	private static final Comparator<SegmentInfo> sortByDelGen = new Comparator<SegmentInfo>() {

		public int compare(SegmentInfo si1, SegmentInfo si2) {
			final long cmp = si1.getBufferedDeletesGen()
					- si2.getBufferedDeletesGen();
			if (cmp > 0) {
				return 1;
			} else if (cmp < 0) {
				return -1;
			} else {
				return 0;
			}
		}
	};

	public synchronized ApplyDeletesResult applyDeletes(
			IndexWriter.ReaderPool readerPool, List<SegmentInfo> infos)
			throws IOException {
		final long t0 = System.currentTimeMillis();

		if (infos.size() == 0) {
			return new ApplyDeletesResult(false, nextGen++, null);
		}

		assert checkDeleteStats();

		if (!any()) {
			message("applyDeletes: no deletes; skipping");
			return new ApplyDeletesResult(false, nextGen++, null);
		}

		if (infoStream != null) {
			message("applyDeletes: infos=" + infos + " packetCount="
					+ deletes.size());
		}

		List<SegmentInfo> infos2 = new ArrayList<SegmentInfo>();
		infos2.addAll(infos);
		Collections.sort(infos2, sortByDelGen);

		CoalescedDeletes coalescedDeletes = null;
		boolean anyNewDeletes = false;

		int infosIDX = infos2.size() - 1;
		int delIDX = deletes.size() - 1;

		List<SegmentInfo> allDeleted = null;

		while (infosIDX >= 0) {

			final FrozenBufferedDeletes packet = delIDX >= 0 ? deletes
					.get(delIDX) : null;
			final SegmentInfo info = infos2.get(infosIDX);
			final long segGen = info.getBufferedDeletesGen();

			if (packet != null && segGen < packet.gen) {

				if (coalescedDeletes == null) {
					coalescedDeletes = new CoalescedDeletes();
				}
				coalescedDeletes.update(packet);
				delIDX--;
			} else if (packet != null && segGen == packet.gen) {

				assert readerPool.infoIsLive(info);
				SegmentReader reader = readerPool.get(info, false);
				int delCount = 0;
				final boolean segAllDeletes;
				try {
					if (coalescedDeletes != null) {

						delCount += applyTermDeletes(
								coalescedDeletes.termsIterable(), reader);
						delCount += applyQueryDeletes(
								coalescedDeletes.queriesIterable(), reader);
					}

					delCount += applyQueryDeletes(packet.queriesIterable(),
							reader);
					segAllDeletes = reader.numDocs() == 0;
				} finally {
					readerPool.release(reader);
				}
				anyNewDeletes |= delCount > 0;

				if (segAllDeletes) {
					if (allDeleted == null) {
						allDeleted = new ArrayList<SegmentInfo>();
					}
					allDeleted.add(info);
				}

				if (infoStream != null) {
					message("seg="
							+ info
							+ " segGen="
							+ segGen
							+ " segDeletes=["
							+ packet
							+ "]; coalesced deletes=["
							+ (coalescedDeletes == null ? "null"
									: coalescedDeletes) + "] delCount="
							+ delCount + (segAllDeletes ? " 100% deleted" : ""));
				}

				if (coalescedDeletes == null) {
					coalescedDeletes = new CoalescedDeletes();
				}
				coalescedDeletes.update(packet);
				delIDX--;
				infosIDX--;
				info.setBufferedDeletesGen(nextGen);

			} else {

				if (coalescedDeletes != null) {

					assert readerPool.infoIsLive(info);
					SegmentReader reader = readerPool.get(info, false);
					int delCount = 0;
					final boolean segAllDeletes;
					try {
						delCount += applyTermDeletes(
								coalescedDeletes.termsIterable(), reader);
						delCount += applyQueryDeletes(
								coalescedDeletes.queriesIterable(), reader);
						segAllDeletes = reader.numDocs() == 0;
					} finally {
						readerPool.release(reader);
					}
					anyNewDeletes |= delCount > 0;

					if (segAllDeletes) {
						if (allDeleted == null) {
							allDeleted = new ArrayList<SegmentInfo>();
						}
						allDeleted.add(info);
					}

					if (infoStream != null) {
						message("seg="
								+ info
								+ " segGen="
								+ segGen
								+ " coalesced deletes=["
								+ (coalescedDeletes == null ? "null"
										: coalescedDeletes) + "] delCount="
								+ delCount
								+ (segAllDeletes ? " 100% deleted" : ""));
					}
				}
				info.setBufferedDeletesGen(nextGen);

				infosIDX--;
			}
		}

		assert checkDeleteStats();
		if (infoStream != null) {
			message("applyDeletes took " + (System.currentTimeMillis() - t0)
					+ " msec");
		}

		return new ApplyDeletesResult(anyNewDeletes, nextGen++, allDeleted);
	}

	public synchronized long getNextGen() {
		return nextGen++;
	}

	public synchronized void prune(SegmentInfos segmentInfos) {
		assert checkDeleteStats();
		long minGen = Long.MAX_VALUE;
		for (SegmentInfo info : segmentInfos) {
			minGen = Math.min(info.getBufferedDeletesGen(), minGen);
		}

		if (infoStream != null) {
			message("prune sis=" + segmentInfos + " minGen=" + minGen
					+ " packetCount=" + deletes.size());
		}

		final int limit = deletes.size();
		for (int delIDX = 0; delIDX < limit; delIDX++) {
			if (deletes.get(delIDX).gen >= minGen) {
				prune(delIDX);
				assert checkDeleteStats();
				return;
			}
		}

		prune(limit);
		assert !any();
		assert checkDeleteStats();
	}

	private synchronized void prune(int count) {
		if (count > 0) {
			if (infoStream != null) {
				message("pruneDeletes: prune " + count + " packets; "
						+ (deletes.size() - count) + " packets remain");
			}
			for (int delIDX = 0; delIDX < count; delIDX++) {
				final FrozenBufferedDeletes packet = deletes.get(delIDX);
				numTerms.addAndGet(-packet.numTermDeletes);
				assert numTerms.get() >= 0;
				bytesUsed.addAndGet(-packet.bytesUsed);
				assert bytesUsed.get() >= 0;
			}
			deletes.subList(0, count).clear();
		}
	}

	private synchronized long applyTermDeletes(Iterable<Term> termsIter,
			SegmentReader reader) throws IOException {
		long delCount = 0;

		assert checkDeleteTerm(null);

		final TermDocs docs = reader.termDocs();

		for (Term term : termsIter) {

			assert checkDeleteTerm(term);
			docs.seek(term);

			while (docs.next()) {
				final int docID = docs.doc();
				reader.deleteDocument(docID);

				delCount++;
			}
		}

		return delCount;
	}

	public static class QueryAndLimit {
		public final Query query;
		public final int limit;

		public QueryAndLimit(Query query, int limit) {
			this.query = query;
			this.limit = limit;
		}
	}

	private synchronized long applyQueryDeletes(
			Iterable<QueryAndLimit> queriesIter, SegmentReader reader)
			throws IOException {
		long delCount = 0;

		for (QueryAndLimit ent : queriesIter) {
			Query query = ent.query;
			int limit = ent.limit;
			final DocIdSet docs = new QueryWrapperFilter(query)
					.getDocIdSet(reader);
			if (docs != null) {
				final DocIdSetIterator it = docs.iterator();
				if (it != null) {
					while (true) {
						int doc = it.nextDoc();
						if (doc >= limit)
							break;

						reader.deleteDocument(doc);

						delCount++;
					}
				}
			}
		}

		return delCount;
	}

	private boolean checkDeleteTerm(Term term) {
		if (term != null) {
			assert lastDeleteTerm == null || term.compareTo(lastDeleteTerm) > 0 : "lastTerm="
					+ lastDeleteTerm + " vs term=" + term;
		}

		lastDeleteTerm = term == null ? null : new Term(term.field(),
				term.text());
		return true;
	}

	private boolean checkDeleteStats() {
		int numTerms2 = 0;
		long bytesUsed2 = 0;
		for (FrozenBufferedDeletes packet : deletes) {
			numTerms2 += packet.numTermDeletes;
			bytesUsed2 += packet.bytesUsed;
		}
		assert numTerms2 == numTerms.get() : "numTerms2=" + numTerms2 + " vs "
				+ numTerms.get();
		assert bytesUsed2 == bytesUsed.get() : "bytesUsed2=" + bytesUsed2
				+ " vs " + bytesUsed;
		return true;
	}
}
