/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.Field;
import com.shatam.shatamindex.document.Fieldable;
import com.shatam.shatamindex.document.Field.Index;
import com.shatam.shatamindex.document.Field.Store;
import com.shatam.shatamindex.index.IndexWriterConfig.OpenMode;
import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.LockObtainFailedException;
import com.shatam.shatamindex.util.Version;

public class PersistentSnapshotDeletionPolicy extends SnapshotDeletionPolicy {

	private static final String SNAPSHOTS_ID = "$SNAPSHOTS_DOC$";

	private final IndexWriter writer;

	public static Map<String, String> readSnapshotsInfo(Directory dir)
			throws IOException {
		IndexReader r = IndexReader.open(dir, true);
		Map<String, String> snapshots = new HashMap<String, String>();
		try {
			int numDocs = r.numDocs();

			if (numDocs == 1) {
				Document doc = r.document(r.maxDoc() - 1);
				Field sid = doc.getField(SNAPSHOTS_ID);
				if (sid == null) {
					throw new IllegalStateException(
							"directory is not a valid snapshots store!");
				}
				doc.removeField(SNAPSHOTS_ID);
				for (Fieldable f : doc.getFields()) {
					snapshots.put(f.name(), f.stringValue());
				}
			} else if (numDocs != 0) {
				throw new IllegalStateException(
						"should be at most 1 document in the snapshots directory: "
								+ numDocs);
			}
		} finally {
			r.close();
		}
		return snapshots;
	}

	public PersistentSnapshotDeletionPolicy(IndexDeletionPolicy primary,
			Directory dir, OpenMode mode, Version matchVersion)
			throws CorruptIndexException, LockObtainFailedException,
			IOException {
		super(primary, null);

		writer = new IndexWriter(dir,
				new IndexWriterConfig(matchVersion, null).setOpenMode(mode));
		if (mode != OpenMode.APPEND) {

			writer.commit();
		}

		try {

			for (Entry<String, String> e : readSnapshotsInfo(dir).entrySet()) {
				registerSnapshotInfo(e.getKey(), e.getValue(), null);
			}
		} catch (RuntimeException e) {
			writer.close();
			throw e;
		} catch (IOException e) {
			writer.close();
			throw e;
		}
	}

	@Override
	public synchronized void onInit(List<? extends IndexCommit> commits)
			throws IOException {

		super.onInit(commits);
		persistSnapshotInfos(null, null);
	}

	@Override
	public synchronized IndexCommit snapshot(String id) throws IOException {
		checkSnapshotted(id);
		if (SNAPSHOTS_ID.equals(id)) {
			throw new IllegalArgumentException(id
					+ " is reserved and cannot be used as a snapshot id");
		}
		persistSnapshotInfos(id, lastCommit.getSegmentsFileName());
		return super.snapshot(id);
	}

	@Override
	public synchronized void release(String id) throws IOException {
		super.release(id);
		persistSnapshotInfos(null, null);
	}

	public void close() throws CorruptIndexException, IOException {
		writer.close();
	}

	private void persistSnapshotInfos(String id, String segment)
			throws IOException {
		writer.deleteAll();
		Document d = new Document();
		d.add(new Field(SNAPSHOTS_ID, "", Store.YES, Index.NO));
		for (Entry<String, String> e : super.getSnapshots().entrySet()) {
			d.add(new Field(e.getKey(), e.getValue(), Store.YES, Index.NO));
		}
		if (id != null) {
			d.add(new Field(id, segment, Store.YES, Index.NO));
		}
		writer.addDocument(d);
		writer.commit();
	}

}
