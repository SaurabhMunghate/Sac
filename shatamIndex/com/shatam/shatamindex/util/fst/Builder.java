/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.BytesRef;
import com.shatam.shatamindex.util.IntsRef;
import com.shatam.shatamindex.util.RamUsageEstimator;
import com.shatam.shatamindex.util.fst.FST.INPUT_TYPE;

import java.io.IOException;

public class Builder<T> {
	private final NodeHash<T> dedupHash;
	private final FST<T> fst;
	private final T NO_OUTPUT;

	private final int minSuffixCount1;

	private final int minSuffixCount2;

	private final boolean doShareNonSingletonNodes;
	private final int shareMaxTailLength;

	private final IntsRef lastInput = new IntsRef();

	private UnCompiledNode<T>[] frontier;

	public Builder(FST.INPUT_TYPE inputType, Outputs<T> outputs) {
		this(inputType, 0, 0, true, true, Integer.MAX_VALUE, outputs);
	}

	public Builder(FST.INPUT_TYPE inputType, int minSuffixCount1,
			int minSuffixCount2, boolean doShareSuffix,
			boolean doShareNonSingletonNodes, int shareMaxTailLength,
			Outputs<T> outputs) {
		this.minSuffixCount1 = minSuffixCount1;
		this.minSuffixCount2 = minSuffixCount2;
		this.doShareNonSingletonNodes = doShareNonSingletonNodes;
		this.shareMaxTailLength = shareMaxTailLength;
		fst = new FST<T>(inputType, outputs);
		if (doShareSuffix) {
			dedupHash = new NodeHash<T>(fst);
		} else {
			dedupHash = null;
		}
		NO_OUTPUT = outputs.getNoOutput();

		@SuppressWarnings("unchecked")
		final UnCompiledNode<T>[] f = (UnCompiledNode<T>[]) new UnCompiledNode[10];
		frontier = f;
		for (int idx = 0; idx < frontier.length; idx++) {
			frontier[idx] = new UnCompiledNode<T>(this, idx);
		}
	}

	public int getTotStateCount() {
		return fst.nodeCount;
	}

	public long getTermCount() {
		return frontier[0].inputCount;
	}

	public int getMappedStateCount() {
		return dedupHash == null ? 0 : fst.nodeCount;
	}

	private CompiledNode compileNode(UnCompiledNode<T> n, int tailLength)
			throws IOException {
		final int address;
		if (dedupHash != null && (doShareNonSingletonNodes || n.numArcs <= 1)
				&& tailLength <= shareMaxTailLength) {
			if (n.numArcs == 0) {
				address = fst.addNode(n);
			} else {
				address = dedupHash.add(n);
			}
		} else {
			address = fst.addNode(n);
		}
		assert address != -2;

		n.clear();

		final CompiledNode fn = new CompiledNode();
		fn.address = address;
		return fn;
	}

	private void compilePrevTail(int prefixLenPlus1) throws IOException {
		assert prefixLenPlus1 >= 1;

		for (int idx = lastInput.length; idx >= prefixLenPlus1; idx--) {
			boolean doPrune = false;
			boolean doCompile = false;

			final UnCompiledNode<T> node = frontier[idx];
			final UnCompiledNode<T> parent = frontier[idx - 1];

			if (node.inputCount < minSuffixCount1) {
				doPrune = true;
				doCompile = true;
			} else if (idx > prefixLenPlus1) {

				if (parent.inputCount < minSuffixCount2 || minSuffixCount2 == 1
						&& parent.inputCount == 1) {

					doPrune = true;
				} else {

					doPrune = false;
				}
				doCompile = true;
			} else {

				doCompile = minSuffixCount2 == 0;
			}

			if (node.inputCount < minSuffixCount2 || minSuffixCount2 == 1
					&& node.inputCount == 1) {

				for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
					@SuppressWarnings("unchecked")
					final UnCompiledNode<T> target = (UnCompiledNode<T>) node.arcs[arcIdx].target;
					target.clear();
				}
				node.numArcs = 0;
			}

			if (doPrune) {

				node.clear();
				parent.deleteLast(lastInput.ints[lastInput.offset + idx - 1],
						node);
			} else {

				if (minSuffixCount2 != 0) {
					compileAllTargets(node, lastInput.length - idx);
				}
				final T nextFinalOutput = node.output;

				final boolean isFinal = node.isFinal || node.numArcs == 0;

				if (doCompile) {

					parent.replaceLast(lastInput.ints[lastInput.offset + idx
							- 1],
							compileNode(node, 1 + lastInput.length - idx),
							nextFinalOutput, isFinal);
				} else {

					parent.replaceLast(lastInput.ints[lastInput.offset + idx
							- 1], node, nextFinalOutput, isFinal);

					frontier[idx] = new UnCompiledNode<T>(this, idx);
				}
			}
		}
	}

	private final IntsRef scratchIntsRef = new IntsRef(10);

	public void add(BytesRef input, T output) throws IOException {
		assert fst.getInputType() == FST.INPUT_TYPE.BYTE1;
		scratchIntsRef.grow(input.length);
		for (int i = 0; i < input.length; i++) {
			scratchIntsRef.ints[i] = input.bytes[i + input.offset] & 0xFF;
		}
		scratchIntsRef.length = input.length;
		add(scratchIntsRef, output);
	}

	public void add(char[] s, int offset, int length, T output)
			throws IOException {
		assert fst.getInputType() == FST.INPUT_TYPE.BYTE4;
		int charIdx = offset;
		int intIdx = 0;
		final int charLimit = offset + length;
		while (charIdx < charLimit) {
			scratchIntsRef.grow(intIdx + 1);
			final int utf32 = Character.codePointAt(s, charIdx);
			scratchIntsRef.ints[intIdx] = utf32;
			charIdx += Character.charCount(utf32);
			intIdx++;
		}
		scratchIntsRef.length = intIdx;
		add(scratchIntsRef, output);
	}

	public void add(CharSequence s, T output) throws IOException {
		assert fst.getInputType() == FST.INPUT_TYPE.BYTE4;
		int charIdx = 0;
		int intIdx = 0;
		final int charLimit = s.length();
		while (charIdx < charLimit) {
			scratchIntsRef.grow(intIdx + 1);
			final int utf32 = Character.codePointAt(s, charIdx);
			scratchIntsRef.ints[intIdx] = utf32;
			charIdx += Character.charCount(utf32);
			intIdx++;
		}
		scratchIntsRef.length = intIdx;
		add(scratchIntsRef, output);
	}

	public void add(IntsRef input, T output) throws IOException {

		assert lastInput.length == 0 || input.compareTo(lastInput) >= 0 : "inputs are added out of order lastInput="
				+ lastInput + " vs input=" + input;
		assert validOutput(output);

		if (input.length == 0) {

			frontier[0].inputCount++;
			frontier[0].isFinal = true;
			fst.setEmptyOutput(output);
			return;
		}

		int pos1 = 0;
		int pos2 = input.offset;
		final int pos1Stop = Math.min(lastInput.length, input.length);
		while (true) {

			frontier[pos1].inputCount++;
			if (pos1 >= pos1Stop || lastInput.ints[pos1] != input.ints[pos2]) {
				break;
			}
			pos1++;
			pos2++;
		}
		final int prefixLenPlus1 = pos1 + 1;

		if (frontier.length < input.length + 1) {
			@SuppressWarnings("unchecked")
			final UnCompiledNode<T>[] next = new UnCompiledNode[ArrayUtil
					.oversize(input.length + 1,
							RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(frontier, 0, next, 0, frontier.length);
			for (int idx = frontier.length; idx < next.length; idx++) {
				next[idx] = new UnCompiledNode<T>(this, idx);
			}
			frontier = next;
		}

		compilePrevTail(prefixLenPlus1);

		for (int idx = prefixLenPlus1; idx <= input.length; idx++) {
			frontier[idx - 1].addArc(input.ints[input.offset + idx - 1],
					frontier[idx]);

			frontier[idx].inputCount++;
		}

		final UnCompiledNode<T> lastNode = frontier[input.length];
		lastNode.isFinal = true;
		lastNode.output = NO_OUTPUT;

		for (int idx = 1; idx < prefixLenPlus1; idx++) {
			final UnCompiledNode<T> node = frontier[idx];
			final UnCompiledNode<T> parentNode = frontier[idx - 1];

			final T lastOutput = parentNode
					.getLastOutput(input.ints[input.offset + idx - 1]);
			assert validOutput(lastOutput);

			final T commonOutputPrefix;
			final T wordSuffix;

			if (lastOutput != NO_OUTPUT) {
				commonOutputPrefix = fst.outputs.common(output, lastOutput);
				assert validOutput(commonOutputPrefix);
				wordSuffix = fst.outputs.subtract(lastOutput,
						commonOutputPrefix);
				assert validOutput(wordSuffix);
				parentNode.setLastOutput(input.ints[input.offset + idx - 1],
						commonOutputPrefix);
				node.prependOutput(wordSuffix);
			} else {
				commonOutputPrefix = wordSuffix = NO_OUTPUT;
			}

			output = fst.outputs.subtract(output, commonOutputPrefix);
			assert validOutput(output);
		}

		if (lastInput.length == input.length
				&& prefixLenPlus1 == 1 + input.length) {

			lastNode.output = fst.outputs.merge(lastNode.output, output);
		} else {

			frontier[prefixLenPlus1 - 1].setLastOutput(input.ints[input.offset
					+ prefixLenPlus1 - 1], output);
		}

		lastInput.copy(input);

	}

	private boolean validOutput(T output) {
		return output == NO_OUTPUT || !output.equals(NO_OUTPUT);
	}

	public FST<T> finish() throws IOException {

		compilePrevTail(1);

		if (frontier[0].inputCount < minSuffixCount1
				|| frontier[0].inputCount < minSuffixCount2
				|| frontier[0].numArcs == 0) {
			if (fst.emptyOutput == null) {
				return null;
			} else if (minSuffixCount1 > 0 || minSuffixCount2 > 0) {

				return null;
			} else {
				fst.finish(compileNode(frontier[0], lastInput.length).address);

				return fst;
			}
		} else {
			if (minSuffixCount2 != 0) {
				compileAllTargets(frontier[0], lastInput.length);
			}

			fst.finish(compileNode(frontier[0], lastInput.length).address);
		}

		return fst;
	}

	private void compileAllTargets(UnCompiledNode<T> node, int tailLength)
			throws IOException {
		for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
			final Arc<T> arc = node.arcs[arcIdx];
			if (!arc.target.isCompiled()) {

				@SuppressWarnings("unchecked")
				final UnCompiledNode<T> n = (UnCompiledNode<T>) arc.target;
				if (n.numArcs == 0) {

					arc.isFinal = n.isFinal = true;
				}
				arc.target = compileNode(n, tailLength - 1);
			}
		}
	}

	static class Arc<T> {
		public int label;
		public Node target;
		public boolean isFinal;
		public T output;
		public T nextFinalOutput;
	}

	static interface Node {
		boolean isCompiled();
	}

	static final class CompiledNode implements Node {
		int address;

		public boolean isCompiled() {
			return true;
		}
	}

	static final class UnCompiledNode<T> implements Node {
		final Builder<T> owner;
		int numArcs;
		Arc<T>[] arcs;
		T output;
		boolean isFinal;
		long inputCount;

		final int depth;

		@SuppressWarnings("unchecked")
		public UnCompiledNode(Builder<T> owner, int depth) {
			this.owner = owner;
			arcs = (Arc<T>[]) new Arc[1];
			arcs[0] = new Arc<T>();
			output = owner.NO_OUTPUT;
			this.depth = depth;
		}

		public boolean isCompiled() {
			return false;
		}

		public void clear() {
			numArcs = 0;
			isFinal = false;
			output = owner.NO_OUTPUT;
			inputCount = 0;

		}

		public T getLastOutput(int labelToMatch) {
			assert numArcs > 0;
			assert arcs[numArcs - 1].label == labelToMatch;
			return arcs[numArcs - 1].output;
		}

		public void addArc(int label, Node target) {
			assert label >= 0;
			assert numArcs == 0 || label > arcs[numArcs - 1].label : "arc[-1].label="
					+ arcs[numArcs - 1].label
					+ " new label="
					+ label
					+ " numArcs=" + numArcs;
			if (numArcs == arcs.length) {
				@SuppressWarnings("unchecked")
				final Arc<T>[] newArcs = new Arc[ArrayUtil.oversize(
						numArcs + 1, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
				System.arraycopy(arcs, 0, newArcs, 0, arcs.length);
				for (int arcIdx = numArcs; arcIdx < newArcs.length; arcIdx++) {
					newArcs[arcIdx] = new Arc<T>();
				}
				arcs = newArcs;
			}
			final Arc<T> arc = arcs[numArcs++];
			arc.label = label;
			arc.target = target;
			arc.output = arc.nextFinalOutput = owner.NO_OUTPUT;
			arc.isFinal = false;
		}

		public void replaceLast(int labelToMatch, Node target,
				T nextFinalOutput, boolean isFinal) {
			assert numArcs > 0;
			final Arc<T> arc = arcs[numArcs - 1];
			assert arc.label == labelToMatch : "arc.label=" + arc.label
					+ " vs " + labelToMatch;
			arc.target = target;

			arc.nextFinalOutput = nextFinalOutput;
			arc.isFinal = isFinal;
		}

		public void deleteLast(int label, Node target) {
			assert numArcs > 0;
			assert label == arcs[numArcs - 1].label;
			assert target == arcs[numArcs - 1].target;
			numArcs--;
		}

		public void setLastOutput(int labelToMatch, T newOutput) {
			assert owner.validOutput(newOutput);
			assert numArcs > 0;
			final Arc<T> arc = arcs[numArcs - 1];
			assert arc.label == labelToMatch;
			arc.output = newOutput;
		}

		public void prependOutput(T outputPrefix) {
			assert owner.validOutput(outputPrefix);

			for (int arcIdx = 0; arcIdx < numArcs; arcIdx++) {
				arcs[arcIdx].output = owner.fst.outputs.add(outputPrefix,
						arcs[arcIdx].output);
				assert owner.validOutput(arcs[arcIdx].output);
			}

			if (isFinal) {
				output = owner.fst.outputs.add(outputPrefix, output);
				assert owner.validOutput(output);
			}
		}
	}
}
