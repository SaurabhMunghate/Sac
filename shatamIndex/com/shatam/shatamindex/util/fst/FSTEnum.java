/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.RamUsageEstimator;

import java.io.IOException;

abstract class FSTEnum<T> {
	protected final FST<T> fst;

	@SuppressWarnings("unchecked")
	protected FST.Arc<T>[] arcs = new FST.Arc[10];

	@SuppressWarnings("unchecked")
	protected T[] output = (T[]) new Object[10];

	protected final T NO_OUTPUT;
	protected final FST.Arc<T> scratchArc = new FST.Arc<T>();

	protected int upto;
	protected int targetLength;

	protected FSTEnum(FST<T> fst) {
		this.fst = fst;
		NO_OUTPUT = fst.outputs.getNoOutput();
		fst.getFirstArc(getArc(0));
		output[0] = NO_OUTPUT;
	}

	protected abstract int getTargetLabel();

	protected abstract int getCurrentLabel();

	protected abstract void setCurrentLabel(int label);

	protected abstract void grow();

	protected final void rewindPrefix() throws IOException {
		if (upto == 0) {

			upto = 1;
			fst.readFirstTargetArc(getArc(0), getArc(1));
			return;
		}

		final int currentLimit = upto;
		upto = 1;
		while (upto < currentLimit && upto <= targetLength + 1) {
			final int cmp = getCurrentLabel() - getTargetLabel();
			if (cmp < 0) {

				break;
			} else if (cmp > 0) {

				final FST.Arc<T> arc = getArc(upto);
				fst.readFirstTargetArc(getArc(upto - 1), arc);

				break;
			}
			upto++;
		}
	}

	protected void doNext() throws IOException {

		if (upto == 0) {

			upto = 1;
			fst.readFirstTargetArc(getArc(0), getArc(1));
		} else {

			while (arcs[upto].isLast()) {
				upto--;
				if (upto == 0) {

					return;
				}
			}
			fst.readNextArc(arcs[upto]);
		}

		pushFirst();
	}

	protected void doSeekCeil() throws IOException {

		rewindPrefix();

		FST.Arc<T> arc = getArc(upto);
		int targetLabel = getTargetLabel();

		while (true) {

			if (arc.bytesPerArc != 0 && arc.label != -1) {

				final FST<T>.BytesReader in = fst.getBytesReader(0);
				int low = arc.arcIdx;
				int high = arc.numArcs - 1;
				int mid = 0;

				boolean found = false;
				while (low <= high) {
					mid = (low + high) >>> 1;
					in.pos = arc.posArcsStart - arc.bytesPerArc * mid - 1;
					final int midLabel = fst.readLabel(in);
					final int cmp = midLabel - targetLabel;

					if (cmp < 0)
						low = mid + 1;
					else if (cmp > 0)
						high = mid - 1;
					else {
						found = true;
						break;
					}
				}

				if (found) {

					arc.arcIdx = mid - 1;
					fst.readNextRealArc(arc, in);
					assert arc.arcIdx == mid;
					assert arc.label == targetLabel : "arc.label=" + arc.label
							+ " vs targetLabel=" + targetLabel + " mid=" + mid;
					output[upto] = fst.outputs
							.add(output[upto - 1], arc.output);
					if (targetLabel == FST.END_LABEL) {
						return;
					}
					setCurrentLabel(arc.label);
					incr();
					arc = fst.readFirstTargetArc(arc, getArc(upto));
					targetLabel = getTargetLabel();
					continue;
				} else if (low == arc.numArcs) {

					arc.arcIdx = arc.numArcs - 2;
					fst.readNextRealArc(arc, in);
					assert arc.isLast();

					upto--;
					while (true) {
						if (upto == 0) {
							return;
						}
						final FST.Arc<T> prevArc = getArc(upto);

						if (!prevArc.isLast()) {
							fst.readNextArc(prevArc);
							pushFirst();
							return;
						}
						upto--;
					}
				} else {
					arc.arcIdx = (low > high ? low : high) - 1;
					fst.readNextRealArc(arc, in);
					assert arc.label > targetLabel;
					pushFirst();
					return;
				}
			} else {

				if (arc.label == targetLabel) {

					output[upto] = fst.outputs
							.add(output[upto - 1], arc.output);
					if (targetLabel == FST.END_LABEL) {
						return;
					}
					setCurrentLabel(arc.label);
					incr();
					arc = fst.readFirstTargetArc(arc, getArc(upto));
					targetLabel = getTargetLabel();
				} else if (arc.label > targetLabel) {
					pushFirst();
					return;
				} else if (arc.isLast()) {

					upto--;
					while (true) {
						if (upto == 0) {
							return;
						}
						final FST.Arc<T> prevArc = getArc(upto);

						if (!prevArc.isLast()) {
							fst.readNextArc(prevArc);
							pushFirst();
							return;
						}
						upto--;
					}
				} else {

					fst.readNextArc(arc);
				}
			}
		}
	}

	protected void doSeekFloor() throws IOException {

		rewindPrefix();

		FST.Arc<T> arc = getArc(upto);
		int targetLabel = getTargetLabel();

		while (true) {

			if (arc.bytesPerArc != 0 && arc.label != FST.END_LABEL) {

				final FST<T>.BytesReader in = fst.getBytesReader(0);
				int low = arc.arcIdx;
				int high = arc.numArcs - 1;
				int mid = 0;

				boolean found = false;
				while (low <= high) {
					mid = (low + high) >>> 1;
					in.pos = arc.posArcsStart - arc.bytesPerArc * mid - 1;
					final int midLabel = fst.readLabel(in);
					final int cmp = midLabel - targetLabel;

					if (cmp < 0)
						low = mid + 1;
					else if (cmp > 0)
						high = mid - 1;
					else {
						found = true;
						break;
					}
				}

				if (found) {

					arc.arcIdx = mid - 1;
					fst.readNextRealArc(arc, in);
					assert arc.arcIdx == mid;
					assert arc.label == targetLabel : "arc.label=" + arc.label
							+ " vs targetLabel=" + targetLabel + " mid=" + mid;
					output[upto] = fst.outputs
							.add(output[upto - 1], arc.output);
					if (targetLabel == FST.END_LABEL) {
						return;
					}
					setCurrentLabel(arc.label);
					incr();
					arc = fst.readFirstTargetArc(arc, getArc(upto));
					targetLabel = getTargetLabel();
					continue;
				} else if (high == -1) {

					while (true) {

						fst.readFirstTargetArc(getArc(upto - 1), arc);
						if (arc.label < targetLabel) {

							while (!arc.isLast()
									&& fst.readNextArcLabel(arc) < targetLabel) {
								fst.readNextArc(arc);
							}
							pushLast();
							return;
						}
						upto--;
						if (upto == 0) {
							return;
						}
						targetLabel = getTargetLabel();
						arc = getArc(upto);
					}
				} else {

					arc.arcIdx = (low > high ? high : low) - 1;

					fst.readNextRealArc(arc, in);
					assert arc.isLast()
							|| fst.readNextArcLabel(arc) > targetLabel;
					assert arc.label < targetLabel;
					pushLast();
					return;
				}
			} else {

				if (arc.label == targetLabel) {

					output[upto] = fst.outputs
							.add(output[upto - 1], arc.output);
					if (targetLabel == FST.END_LABEL) {
						return;
					}
					setCurrentLabel(arc.label);
					incr();
					arc = fst.readFirstTargetArc(arc, getArc(upto));
					targetLabel = getTargetLabel();
				} else if (arc.label > targetLabel) {

					while (true) {

						fst.readFirstTargetArc(getArc(upto - 1), arc);
						if (arc.label < targetLabel) {

							while (!arc.isLast()
									&& fst.readNextArcLabel(arc) < targetLabel) {
								fst.readNextArc(arc);
							}
							pushLast();
							return;
						}
						upto--;
						if (upto == 0) {
							return;
						}
						targetLabel = getTargetLabel();
						arc = getArc(upto);
					}
				} else if (!arc.isLast()) {

					if (fst.readNextArcLabel(arc) > targetLabel) {
						pushLast();
						return;
					} else {

						fst.readNextArc(arc);
					}
				} else {
					pushLast();
					return;
				}
			}
		}
	}

	private void incr() {
		upto++;
		grow();
		if (arcs.length <= upto) {
			@SuppressWarnings("unchecked")
			final FST.Arc<T>[] newArcs = new FST.Arc[ArrayUtil.oversize(
					1 + upto, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(arcs, 0, newArcs, 0, arcs.length);
			arcs = newArcs;
		}
		if (output.length <= upto) {
			@SuppressWarnings("unchecked")
			final T[] newOutput = (T[]) new Object[ArrayUtil.oversize(1 + upto,
					RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
			System.arraycopy(output, 0, newOutput, 0, output.length);
			output = newOutput;
		}
	}

	private void pushFirst() throws IOException {

		FST.Arc<T> arc = arcs[upto];
		assert arc != null;

		while (true) {
			output[upto] = fst.outputs.add(output[upto - 1], arc.output);
			if (arc.label == FST.END_LABEL) {

				break;
			}

			setCurrentLabel(arc.label);
			incr();

			final FST.Arc<T> nextArc = getArc(upto);
			fst.readFirstTargetArc(arc, nextArc);
			arc = nextArc;
		}
	}

	private void pushLast() throws IOException {

		FST.Arc<T> arc = arcs[upto];
		assert arc != null;

		while (true) {
			setCurrentLabel(arc.label);
			output[upto] = fst.outputs.add(output[upto - 1], arc.output);
			if (arc.label == FST.END_LABEL) {

				break;
			}
			incr();

			arc = fst.readLastTargetArc(arc, getArc(upto));
		}
	}

	private FST.Arc<T> getArc(int idx) {
		if (arcs[idx] == null) {
			arcs[idx] = new FST.Arc<T>();
		}
		return arcs[idx];
	}
}
