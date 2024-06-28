/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import java.io.*;
import java.util.*;

import com.shatam.shatamindex.util.BytesRef;
import com.shatam.shatamindex.util.IntsRef;

public final class Util {
	private Util() {
	}

	public static <T> T get(FST<T> fst, IntsRef input) throws IOException {
		assert fst.inputType == FST.INPUT_TYPE.BYTE4;

		final FST.Arc<T> arc = fst.getFirstArc(new FST.Arc<T>());

		final T NO_OUTPUT = fst.outputs.getNoOutput();
		T output = NO_OUTPUT;
		for (int i = 0; i < input.length; i++) {
			if (fst.findTargetArc(input.ints[input.offset + i], arc, arc) == null) {
				return null;
			} else if (arc.output != NO_OUTPUT) {
				output = fst.outputs.add(output, arc.output);
			}
		}

		if (fst.findTargetArc(FST.END_LABEL, arc, arc) == null) {
			return null;
		} else if (arc.output != NO_OUTPUT) {
			return fst.outputs.add(output, arc.output);
		} else {
			return output;
		}
	}

	public static <T> T get(FST<T> fst, char[] input, int offset, int length)
			throws IOException {
		assert fst.inputType == FST.INPUT_TYPE.BYTE4;

		final FST.Arc<T> arc = fst.getFirstArc(new FST.Arc<T>());

		int charIdx = offset;
		final int charLimit = offset + length;

		final T NO_OUTPUT = fst.outputs.getNoOutput();
		T output = NO_OUTPUT;
		while (charIdx < charLimit) {
			final int utf32 = Character.codePointAt(input, charIdx);
			charIdx += Character.charCount(utf32);

			if (fst.findTargetArc(utf32, arc, arc) == null) {
				return null;
			} else if (arc.output != NO_OUTPUT) {
				output = fst.outputs.add(output, arc.output);
			}
		}

		if (fst.findTargetArc(FST.END_LABEL, arc, arc) == null) {
			return null;
		} else if (arc.output != NO_OUTPUT) {
			return fst.outputs.add(output, arc.output);
		} else {
			return output;
		}
	}

	public static <T> T get(FST<T> fst, CharSequence input) throws IOException {
		assert fst.inputType == FST.INPUT_TYPE.BYTE4;

		final FST.Arc<T> arc = fst.getFirstArc(new FST.Arc<T>());

		int charIdx = 0;
		final int charLimit = input.length();

		final T NO_OUTPUT = fst.outputs.getNoOutput();
		T output = NO_OUTPUT;

		while (charIdx < charLimit) {
			final int utf32 = Character.codePointAt(input, charIdx);
			charIdx += Character.charCount(utf32);

			if (fst.findTargetArc(utf32, arc, arc) == null) {
				return null;
			} else if (arc.output != NO_OUTPUT) {
				output = fst.outputs.add(output, arc.output);
			}
		}

		if (fst.findTargetArc(FST.END_LABEL, arc, arc) == null) {
			return null;
		} else if (arc.output != NO_OUTPUT) {
			return fst.outputs.add(output, arc.output);
		} else {
			return output;
		}
	}

	public static <T> T get(FST<T> fst, BytesRef input) throws IOException {
		assert fst.inputType == FST.INPUT_TYPE.BYTE1;

		final FST.Arc<T> arc = fst.getFirstArc(new FST.Arc<T>());

		final T NO_OUTPUT = fst.outputs.getNoOutput();
		T output = NO_OUTPUT;
		for (int i = 0; i < input.length; i++) {
			if (fst.findTargetArc(input.bytes[i + input.offset] & 0xFF, arc,
					arc) == null) {
				return null;
			} else if (arc.output != NO_OUTPUT) {
				output = fst.outputs.add(output, arc.output);
			}
		}

		if (fst.findTargetArc(FST.END_LABEL, arc, arc) == null) {
			return null;
		} else if (arc.output != NO_OUTPUT) {
			return fst.outputs.add(output, arc.output);
		} else {
			return output;
		}
	}

	public static <T> void toDot(FST<T> fst, Writer out, boolean sameRank,
			boolean labelStates) throws IOException {
		final String expandedNodeColor = "blue";

		final FST.Arc<T> startArc = fst.getFirstArc(new FST.Arc<T>());

		final List<FST.Arc<T>> thisLevelQueue = new ArrayList<FST.Arc<T>>();

		final List<FST.Arc<T>> nextLevelQueue = new ArrayList<FST.Arc<T>>();
		nextLevelQueue.add(startArc);

		final List<Integer> sameLevelStates = new ArrayList<Integer>();

		final BitSet seen = new BitSet();
		seen.set(startArc.target);

		final String stateShape = "circle";

		out.write("digraph FST {\n");
		out.write("  rankdir = LR; splines=true; concentrate=true; ordering=out; ranksep=2.5; \n");

		if (!labelStates) {
			out.write("  node [shape=circle, width=.2, height=.2, style=filled]\n");
		}

		emitDotState(out, "initial", "point", "white", "");
		emitDotState(out, Integer.toString(startArc.target), stateShape,
				fst.isExpandedTarget(startArc) ? expandedNodeColor : null, "");
		out.write("  initial -> " + startArc.target + "\n");

		final T NO_OUTPUT = fst.outputs.getNoOutput();
		int level = 0;

		while (!nextLevelQueue.isEmpty()) {

			thisLevelQueue.addAll(nextLevelQueue);
			nextLevelQueue.clear();

			level++;
			out.write("\n  // Transitions and states at level: " + level + "\n");
			while (!thisLevelQueue.isEmpty()) {
				final FST.Arc<T> arc = thisLevelQueue.remove(thisLevelQueue
						.size() - 1);

				if (fst.targetHasArcs(arc)) {

					final int node = arc.target;
					fst.readFirstTargetArc(arc, arc);

					while (true) {

						if (arc.target >= 0 && !seen.get(arc.target)) {
							final boolean isExpanded = fst
									.isExpandedTarget(arc);
							emitDotState(out, Integer.toString(arc.target),
									stateShape, isExpanded ? expandedNodeColor
											: null,
									labelStates ? Integer.toString(arc.target)
											: "");
							seen.set(arc.target);
							nextLevelQueue.add(new FST.Arc<T>().copyFrom(arc));
							sameLevelStates.add(arc.target);
						}

						String outs;
						if (arc.output != NO_OUTPUT) {
							outs = "/" + fst.outputs.outputToString(arc.output);
						} else {
							outs = "";
						}

						final String cl;
						if (arc.label == FST.END_LABEL) {
							cl = "~";
						} else {
							cl = printableLabel(arc.label);
						}

						out.write("  " + node + " -> " + arc.target
								+ " [label=\"" + cl + outs + "\"]\n");

						if (arc.isLast()) {
							break;
						}
						fst.readNextArc(arc);
					}
				}
			}

			if (sameRank && sameLevelStates.size() > 1) {
				out.write("  {rank=same; ");
				for (int state : sameLevelStates) {
					out.write(state + "; ");
				}
				out.write(" }\n");
			}
			sameLevelStates.clear();
		}

		out.write("  -1 [style=filled, color=black, shape=circle, label=\"\"]\n\n");
		out.write("  {rank=sink; -1 }\n");

		out.write("}\n");
		out.flush();
	}

	private static void emitDotState(Writer out, String name, String shape,
			String color, String label) throws IOException {
		out.write("  " + name + " [" + (shape != null ? "shape=" + shape : "")
				+ " " + (color != null ? "color=" + color : "") + " "
				+ (label != null ? "label=\"" + label + "\"" : "label=\"\"")
				+ " " + "]\n");
	}

	private static String printableLabel(int label) {
		if (label >= 0x20 && label <= 0x7d) {
			return Character.toString((char) label);
		} else {
			return "0x" + Integer.toHexString(label);
		}
	}
}
