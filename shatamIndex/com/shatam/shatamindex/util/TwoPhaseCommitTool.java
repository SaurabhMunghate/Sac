/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.IOException;

import java.util.Map;

public final class TwoPhaseCommitTool {

	public static final class TwoPhaseCommitWrapper implements TwoPhaseCommit {

		private final TwoPhaseCommit tpc;
		private final Map<String, String> commitData;

		public TwoPhaseCommitWrapper(TwoPhaseCommit tpc,
				Map<String, String> commitData) {
			this.tpc = tpc;
			this.commitData = commitData;
		}

		public void prepareCommit() throws IOException {
			prepareCommit(commitData);
		}

		public void prepareCommit(Map<String, String> commitData)
				throws IOException {
			tpc.prepareCommit(this.commitData);
		}

		public void commit() throws IOException {
			commit(commitData);
		}

		public void commit(Map<String, String> commitData) throws IOException {
			tpc.commit(this.commitData);
		}

		public void rollback() throws IOException {
			tpc.rollback();
		}
	}

	public static class PrepareCommitFailException extends IOException {

		public PrepareCommitFailException(Throwable cause, TwoPhaseCommit obj) {
			super("prepareCommit() failed on " + obj);
			initCause(cause);
		}

	}

	public static class CommitFailException extends IOException {

		public CommitFailException(Throwable cause, TwoPhaseCommit obj) {
			super("commit() failed on " + obj);
			initCause(cause);
		}

	}

	private static void rollback(TwoPhaseCommit... objects) {
		for (TwoPhaseCommit tpc : objects) {

			if (tpc != null) {
				try {
					tpc.rollback();
				} catch (Throwable t) {
				}
			}
		}
	}

	public static void execute(TwoPhaseCommit... objects)
			throws PrepareCommitFailException, CommitFailException {
		TwoPhaseCommit tpc = null;
		try {

			for (int i = 0; i < objects.length; i++) {
				tpc = objects[i];
				if (tpc != null) {
					tpc.prepareCommit();
				}
			}
		} catch (Throwable t) {

			rollback(objects);
			throw new PrepareCommitFailException(t, tpc);
		}

		try {
			for (int i = 0; i < objects.length; i++) {
				tpc = objects[i];
				if (tpc != null) {
					tpc.commit();
				}
			}
		} catch (Throwable t) {

			rollback(objects);
			throw new CommitFailException(t, tpc);
		}
	}

}
