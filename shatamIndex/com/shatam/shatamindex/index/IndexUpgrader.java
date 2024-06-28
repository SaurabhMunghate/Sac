/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.store.Directory;
import com.shatam.shatamindex.store.FSDirectory;
import com.shatam.shatamindex.util.Constants;
import com.shatam.shatamindex.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

public final class IndexUpgrader {

	private static void printUsage() {
		System.err
				.println("Upgrades an index so all segments created with a previous shatam version are rewritten.");
		System.err.println("Usage:");
		System.err.println("  java " + IndexUpgrader.class.getName()
				+ " [-delete-prior-commits] [-verbose] indexDir");
		System.err
				.println("This tool keeps only the last commit in an index; for this");
		System.err
				.println("reason, if the incoming index has more than one commit, the tool");
		System.err
				.println("refuses to run by default. Specify -delete-prior-commits to override");
		System.err
				.println("this, allowing the tool to delete all but the last commit.");
		System.err.println("WARNING: This tool may reorder document IDs!");
		System.exit(1);
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {
		String dir = null;
		boolean deletePriorCommits = false;
		PrintStream out = null;
		for (String arg : args) {
			if ("-delete-prior-commits".equals(arg)) {
				deletePriorCommits = true;
			} else if ("-verbose".equals(arg)) {
				out = System.out;
			} else if (dir == null) {
				dir = arg;
			} else {
				printUsage();
			}
		}
		if (dir == null) {
			printUsage();
		}

		new IndexUpgrader(FSDirectory.open(new File(dir)),
				Version.SHATAM_CURRENT, out, deletePriorCommits).upgrade();
	}

	private final Directory dir;
	private final PrintStream infoStream;
	private final IndexWriterConfig iwc;
	private final boolean deletePriorCommits;

	public IndexUpgrader(Directory dir, Version matchVersion) {
		this(dir, new IndexWriterConfig(matchVersion, null), null, false);
	}

	public IndexUpgrader(Directory dir, Version matchVersion,
			PrintStream infoStream, boolean deletePriorCommits) {
		this(dir, new IndexWriterConfig(matchVersion, null), infoStream,
				deletePriorCommits);
	}

	public IndexUpgrader(Directory dir, IndexWriterConfig iwc,
			PrintStream infoStream, boolean deletePriorCommits) {
		this.dir = dir;
		this.iwc = iwc;
		this.infoStream = infoStream;
		this.deletePriorCommits = deletePriorCommits;
	}

	public void upgrade() throws IOException {
		if (!IndexReader.indexExists(dir)) {
			throw new IndexNotFoundException(dir.toString());
		}

		if (!deletePriorCommits) {
			final Collection<IndexCommit> commits = IndexReader
					.listCommits(dir);
			if (commits.size() > 1) {
				throw new IllegalArgumentException(
						"This tool was invoked to not delete prior commit points, but the following commits were found: "
								+ commits);
			}
		}

		final IndexWriterConfig c = (IndexWriterConfig) iwc.clone();
		c.setMergePolicy(new UpgradeIndexMergePolicy(c.getMergePolicy()));
		c.setIndexDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());

		final IndexWriter w = new IndexWriter(dir, c);
		try {
			w.setInfoStream(infoStream);
			w.message("Upgrading all pre-" + Constants.SHATAM_MAIN_VERSION
					+ " segments of index directory '" + dir + "' to version "
					+ Constants.SHATAM_MAIN_VERSION + "...");
			w.forceMerge(1);
			w.message("All segments upgraded to version "
					+ Constants.SHATAM_MAIN_VERSION);
		} finally {
			w.close();
		}
	}

}
