/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import java.io.IOException;

final class NodeHash<T> {

	private int[] table;
	private int count;
	private int mask;
	private final FST<T> fst;
	private final FST.Arc<T> scratchArc = new FST.Arc<T>();

	public NodeHash(FST<T> fst) {
		table = new int[16];
		mask = 15;
		this.fst = fst;
	}

	private boolean nodesEqual(Builder.UnCompiledNode<T> node, int address)
			throws IOException {
		final FST<T>.BytesReader in = fst.getBytesReader(0);
		fst.readFirstRealArc(address, scratchArc);
		if (scratchArc.bytesPerArc != 0 && node.numArcs != scratchArc.numArcs) {
			return false;
		}
		for (int arcUpto = 0; arcUpto < node.numArcs; arcUpto++) {
			final Builder.Arc<T> arc = node.arcs[arcUpto];
			if (arc.label != scratchArc.label
					|| !arc.output.equals(scratchArc.output)
					|| ((Builder.CompiledNode) arc.target).address != scratchArc.target
					|| !arc.nextFinalOutput.equals(scratchArc.nextFinalOutput)
					|| arc.isFinal != scratchArc.isFinal()) {
				return false;
			}

			if (scratchArc.isLast()) {
				if (arcUpto == node.numArcs - 1) {
					return true;
				} else {
					return false;
				}
			}
			fst.readNextRealArc(scratchArc, in);
		}

		return false;
	}

	private int hash(Builder.UnCompiledNode<T> node) {
		final int PRIME = 31;

		int h = 0;

		for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
			final Builder.Arc<T> arc = node.arcs[arcIdx];

			h = PRIME * h + arc.label;
			h = PRIME * h + ((Builder.CompiledNode) arc.target).address;
			h = PRIME * h + arc.output.hashCode();
			h = PRIME * h + arc.nextFinalOutput.hashCode();
			if (arc.isFinal) {
				h += 17;
			}
		}

		return h & Integer.MAX_VALUE;
	}

	private int hash(int node) throws IOException {
		final int PRIME = 31;
		final FST<T>.BytesReader in = fst.getBytesReader(0);

		int h = 0;
		fst.readFirstRealArc(node, scratchArc);
		while (true) {

			h = PRIME * h + scratchArc.label;
			h = PRIME * h + scratchArc.target;
			h = PRIME * h + scratchArc.output.hashCode();
			h = PRIME * h + scratchArc.nextFinalOutput.hashCode();
			if (scratchArc.isFinal()) {
				h += 17;
			}
			if (scratchArc.isLast()) {
				break;
			}
			fst.readNextRealArc(scratchArc, in);
		}

		return h & Integer.MAX_VALUE;
	}

	public int add(Builder.UnCompiledNode<T> node) throws IOException {

		final int h = hash(node);
		int pos = h & mask;
		int c = 0;
		while (true) {
			final int v = table[pos];
			if (v == 0) {

				final int address = fst.addNode(node);

				assert hash(address) == h : "frozenHash=" + hash(address)
						+ " vs h=" + h;
				count++;
				table[pos] = address;
				if (table.length < 2 * count) {
					rehash();
				}
				return address;
			} else if (nodesEqual(node, v)) {

				return v;
			}

			pos = (pos + (++c)) & mask;
		}
	}

	private void addNew(int address) throws IOException {
		int pos = hash(address) & mask;
		int c = 0;
		while (true) {
			if (table[pos] == 0) {
				table[pos] = address;
				break;
			}

			pos = (pos + (++c)) & mask;
		}
	}

	private void rehash() throws IOException {
		final int[] oldTable = table;
		table = new int[2 * table.length];
		mask = table.length - 1;
		for (int idx = 0; idx < oldTable.length; idx++) {
			final int address = oldTable[idx];
			if (address != 0) {
				addNew(address);
			}
		}
	}

	public int count() {
		return count;
	}
}
