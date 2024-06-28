/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util.fst;

import java.io.IOException;

import com.shatam.shatamindex.store.DataInput;
import com.shatam.shatamindex.store.DataOutput;
import com.shatam.shatamindex.util.ArrayUtil;
import com.shatam.shatamindex.util.CodecUtil;
import com.shatam.shatamindex.util.fst.Builder.UnCompiledNode;

public class FST<T> {
	public static enum INPUT_TYPE {
		BYTE1, BYTE2, BYTE4
	};

	public final INPUT_TYPE inputType;

	private final static int BIT_FINAL_ARC = 1 << 0;
	private final static int BIT_LAST_ARC = 1 << 1;
	private final static int BIT_TARGET_NEXT = 1 << 2;
	private final static int BIT_STOP_NODE = 1 << 3;
	private final static int BIT_ARC_HAS_OUTPUT = 1 << 4;
	private final static int BIT_ARC_HAS_FINAL_OUTPUT = 1 << 5;

	private final static int BIT_ARCS_AS_FIXED_ARRAY = 1 << 6;

	final static int FIXED_ARRAY_SHALLOW_DISTANCE = 3;

	final static int FIXED_ARRAY_NUM_ARCS_SHALLOW = 5;

	final static int FIXED_ARRAY_NUM_ARCS_DEEP = 10;

	private int[] bytesPerArc = new int[0];

	private final static String FILE_FORMAT_NAME = "FST";
	private final static int VERSION_START = 0;

	private final static int VERSION_INT_NUM_BYTES_PER_ARC = 1;

	private final static int VERSION_CURRENT = VERSION_INT_NUM_BYTES_PER_ARC;

	private final static int FINAL_END_NODE = -1;

	private final static int NON_FINAL_END_NODE = 0;

	T emptyOutput;
	private byte[] emptyOutputBytes;

	private byte[] bytes;
	int byteUpto = 0;

	private int startNode = -1;

	public final Outputs<T> outputs;

	private int lastFrozenNode;

	private final T NO_OUTPUT;

	public int nodeCount;
	public int arcCount;
	public int arcWithOutputCount;

	public static final int END_LABEL = -1;

	private Arc<T> cachedRootArcs[];

	public final static class Arc<T> {
		public int label;
		public T output;

		int target;

		byte flags;
		public T nextFinalOutput;
		int nextArc;

		int posArcsStart;
		int bytesPerArc;
		int arcIdx;
		int numArcs;

		public Arc<T> copyFrom(Arc<T> other) {
			label = other.label;
			target = other.target;
			flags = other.flags;
			output = other.output;
			nextFinalOutput = other.nextFinalOutput;
			nextArc = other.nextArc;
			if (other.bytesPerArc != 0) {
				bytesPerArc = other.bytesPerArc;
				posArcsStart = other.posArcsStart;
				arcIdx = other.arcIdx;
				numArcs = other.numArcs;
			} else {
				bytesPerArc = 0;
			}
			return this;
		}

		boolean flag(int flag) {
			return FST.flag(flags, flag);
		}

		public boolean isLast() {
			return flag(BIT_LAST_ARC);
		}

		public boolean isFinal() {
			return flag(BIT_FINAL_ARC);
		}
	};

	static boolean flag(int flags, int bit) {
		return (flags & bit) != 0;
	}

	private final BytesWriter writer;

	public FST(INPUT_TYPE inputType, Outputs<T> outputs) {
		this.inputType = inputType;
		this.outputs = outputs;
		bytes = new byte[128];
		NO_OUTPUT = outputs.getNoOutput();

		writer = new BytesWriter();

		emptyOutput = null;
	}

	public FST(DataInput in, Outputs<T> outputs) throws IOException {
		this.outputs = outputs;
		writer = null;
		CodecUtil.checkHeader(in, FILE_FORMAT_NAME,
				VERSION_INT_NUM_BYTES_PER_ARC, VERSION_INT_NUM_BYTES_PER_ARC);
		if (in.readByte() == 1) {

			int numBytes = in.readVInt();

			bytes = new byte[numBytes];
			in.readBytes(bytes, 0, numBytes);
			emptyOutput = outputs.read(getBytesReader(numBytes - 1));
		} else {
			emptyOutput = null;
		}
		final byte t = in.readByte();
		switch (t) {
		case 0:
			inputType = INPUT_TYPE.BYTE1;
			break;
		case 1:
			inputType = INPUT_TYPE.BYTE2;
			break;
		case 2:
			inputType = INPUT_TYPE.BYTE4;
			break;
		default:
			throw new IllegalStateException("invalid input type " + t);
		}
		startNode = in.readVInt();
		nodeCount = in.readVInt();
		arcCount = in.readVInt();
		arcWithOutputCount = in.readVInt();

		bytes = new byte[in.readVInt()];
		in.readBytes(bytes, 0, bytes.length);
		NO_OUTPUT = outputs.getNoOutput();

		cacheRootArcs();
	}

	public INPUT_TYPE getInputType() {
		return inputType;
	}

	public int sizeInBytes() {
		return bytes.length;
	}

	void finish(int startNode) throws IOException {
		if (startNode == FINAL_END_NODE && emptyOutput != null) {
			startNode = 0;
		}
		if (this.startNode != -1) {
			throw new IllegalStateException("already finished");
		}
		byte[] finalBytes = new byte[writer.posWrite];
		System.arraycopy(bytes, 0, finalBytes, 0, writer.posWrite);
		bytes = finalBytes;
		this.startNode = startNode;

		cacheRootArcs();
	}

	@SuppressWarnings("unchecked")
	private void cacheRootArcs() throws IOException {
		cachedRootArcs = (FST.Arc<T>[]) new FST.Arc[0x80];
		final FST.Arc<T> arc = new FST.Arc<T>();
		getFirstArc(arc);
		final BytesReader in = getBytesReader(0);
		if (targetHasArcs(arc)) {
			readFirstRealArc(arc.target, arc);
			while (true) {
				assert arc.label != END_LABEL;
				if (arc.label < cachedRootArcs.length) {
					cachedRootArcs[arc.label] = new Arc<T>().copyFrom(arc);
				} else {
					break;
				}
				if (arc.isLast()) {
					break;
				}
				readNextRealArc(arc, in);
			}
		}
	}

	void setEmptyOutput(T v) throws IOException {
		if (emptyOutput != null) {
			emptyOutput = outputs.merge(emptyOutput, v);
		} else {
			emptyOutput = v;
		}

		final int posSave = writer.posWrite;
		outputs.write(emptyOutput, writer);
		emptyOutputBytes = new byte[writer.posWrite - posSave];

		final int stopAt = (writer.posWrite - posSave) / 2;
		int upto = 0;
		while (upto < stopAt) {
			final byte b = bytes[posSave + upto];
			bytes[posSave + upto] = bytes[writer.posWrite - upto - 1];
			bytes[writer.posWrite - upto - 1] = b;
			upto++;
		}
		System.arraycopy(bytes, posSave, emptyOutputBytes, 0, writer.posWrite
				- posSave);
		writer.posWrite = posSave;
	}

	public void save(DataOutput out) throws IOException {
		if (startNode == -1) {
			throw new IllegalStateException("call finish first");
		}
		CodecUtil.writeHeader(out, FILE_FORMAT_NAME, VERSION_CURRENT);

		if (emptyOutput != null) {
			out.writeByte((byte) 1);
			out.writeVInt(emptyOutputBytes.length);
			out.writeBytes(emptyOutputBytes, 0, emptyOutputBytes.length);
		} else {
			out.writeByte((byte) 0);
		}
		final byte t;
		if (inputType == INPUT_TYPE.BYTE1) {
			t = 0;
		} else if (inputType == INPUT_TYPE.BYTE2) {
			t = 1;
		} else {
			t = 2;
		}
		out.writeByte(t);
		out.writeVInt(startNode);
		out.writeVInt(nodeCount);
		out.writeVInt(arcCount);
		out.writeVInt(arcWithOutputCount);
		out.writeVInt(bytes.length);
		out.writeBytes(bytes, 0, bytes.length);
	}

	private void writeLabel(int v) throws IOException {
		assert v >= 0 : "v=" + v;
		if (inputType == INPUT_TYPE.BYTE1) {
			assert v <= 255 : "v=" + v;
			writer.writeByte((byte) v);
		} else if (inputType == INPUT_TYPE.BYTE2) {
			assert v <= 65535 : "v=" + v;
			writer.writeVInt(v);
		} else {

			writer.writeVInt(v);
		}
	}

	int readLabel(DataInput in) throws IOException {
		final int v;
		if (inputType == INPUT_TYPE.BYTE1) {
			v = in.readByte() & 0xFF;
		} else {
			v = in.readVInt();
		}
		return v;
	}

	public boolean targetHasArcs(Arc<T> arc) {
		return arc.target > 0;
	}

	int addNode(Builder.UnCompiledNode<T> node) throws IOException {

		if (node.numArcs == 0) {
			if (node.isFinal) {
				return FINAL_END_NODE;
			} else {
				return NON_FINAL_END_NODE;
			}
		}

		int startAddress = writer.posWrite;

		final boolean doFixedArray = shouldExpand(node);
		final int fixedArrayStart;
		if (doFixedArray) {
			if (bytesPerArc.length < node.numArcs) {
				bytesPerArc = new int[ArrayUtil.oversize(node.numArcs, 1)];
			}

			writer.writeByte((byte) BIT_ARCS_AS_FIXED_ARRAY);
			writer.writeVInt(node.numArcs);

			writer.writeInt(0);
			fixedArrayStart = writer.posWrite;

		} else {
			fixedArrayStart = 0;
		}

		nodeCount++;
		arcCount += node.numArcs;

		final int lastArc = node.numArcs - 1;

		int lastArcStart = writer.posWrite;
		int maxBytesPerArc = 0;
		for (int arcIdx = 0; arcIdx < node.numArcs; arcIdx++) {
			final Builder.Arc<T> arc = node.arcs[arcIdx];
			final Builder.CompiledNode target = (Builder.CompiledNode) arc.target;
			int flags = 0;

			if (arcIdx == lastArc) {
				flags += BIT_LAST_ARC;
			}

			if (lastFrozenNode == target.address && !doFixedArray) {
				flags += BIT_TARGET_NEXT;
			}

			if (arc.isFinal) {
				flags += BIT_FINAL_ARC;
				if (arc.nextFinalOutput != NO_OUTPUT) {
					flags += BIT_ARC_HAS_FINAL_OUTPUT;
				}
			} else {
				assert arc.nextFinalOutput == NO_OUTPUT;
			}

			boolean targetHasArcs = target.address > 0;

			if (!targetHasArcs) {
				flags += BIT_STOP_NODE;
			}

			if (arc.output != NO_OUTPUT) {
				flags += BIT_ARC_HAS_OUTPUT;
			}

			writer.writeByte((byte) flags);
			writeLabel(arc.label);

			if (arc.output != NO_OUTPUT) {
				outputs.write(arc.output, writer);
				arcWithOutputCount++;
			}
			if (arc.nextFinalOutput != NO_OUTPUT) {
				outputs.write(arc.nextFinalOutput, writer);
			}

			if (targetHasArcs
					&& (doFixedArray || lastFrozenNode != target.address)) {
				assert target.address > 0;
				writer.writeInt(target.address);
			}

			if (doFixedArray) {
				bytesPerArc[arcIdx] = writer.posWrite - lastArcStart;
				lastArcStart = writer.posWrite;
				maxBytesPerArc = Math.max(maxBytesPerArc, bytesPerArc[arcIdx]);

			}
		}

		if (doFixedArray) {
			assert maxBytesPerArc > 0;

			final int sizeNeeded = fixedArrayStart + node.numArcs
					* maxBytesPerArc;
			bytes = ArrayUtil.grow(bytes, sizeNeeded);

			bytes[fixedArrayStart - 4] = (byte) (maxBytesPerArc >> 24);
			bytes[fixedArrayStart - 3] = (byte) (maxBytesPerArc >> 16);
			bytes[fixedArrayStart - 2] = (byte) (maxBytesPerArc >> 8);
			bytes[fixedArrayStart - 1] = (byte) maxBytesPerArc;

			int srcPos = writer.posWrite;
			int destPos = fixedArrayStart + node.numArcs * maxBytesPerArc;
			writer.posWrite = destPos;
			for (int arcIdx = node.numArcs - 1; arcIdx >= 0; arcIdx--) {

				destPos -= maxBytesPerArc;
				srcPos -= bytesPerArc[arcIdx];
				if (srcPos != destPos) {
					assert destPos > srcPos;
					System.arraycopy(bytes, srcPos, bytes, destPos,
							bytesPerArc[arcIdx]);
				}
			}
		}

		final int endAddress = lastFrozenNode = writer.posWrite - 1;

		int left = startAddress;
		int right = endAddress;
		while (left < right) {
			final byte b = bytes[left];
			bytes[left++] = bytes[right];
			bytes[right--] = b;
		}

		return endAddress;
	}

	public Arc<T> getFirstArc(Arc<T> arc) {
		if (emptyOutput != null) {
			arc.flags = BIT_FINAL_ARC | BIT_LAST_ARC;
			arc.nextFinalOutput = emptyOutput;
		} else {
			arc.flags = BIT_LAST_ARC;
			arc.nextFinalOutput = NO_OUTPUT;
		}
		arc.output = NO_OUTPUT;

		arc.target = startNode;
		return arc;
	}

	public Arc<T> readLastTargetArc(Arc<T> follow, Arc<T> arc)
			throws IOException {

		if (!targetHasArcs(follow)) {

			assert follow.isFinal();
			arc.label = END_LABEL;
			arc.output = follow.nextFinalOutput;
			arc.flags = BIT_LAST_ARC;
			return arc;
		} else {
			final BytesReader in = getBytesReader(follow.target);
			arc.flags = in.readByte();
			if (arc.flag(BIT_ARCS_AS_FIXED_ARRAY)) {

				arc.numArcs = in.readVInt();
				arc.bytesPerArc = in.readInt();

				arc.posArcsStart = in.pos;
				arc.arcIdx = arc.numArcs - 2;
			} else {

				arc.bytesPerArc = 0;

				while (!arc.isLast()) {

					readLabel(in);
					if (arc.flag(BIT_ARC_HAS_OUTPUT)) {
						outputs.read(in);
					}
					if (arc.flag(BIT_ARC_HAS_FINAL_OUTPUT)) {
						outputs.read(in);
					}
					if (arc.flag(BIT_STOP_NODE)) {
					} else if (arc.flag(BIT_TARGET_NEXT)) {
					} else {
						in.pos -= 4;
					}
					arc.flags = in.readByte();
				}
				arc.nextArc = in.pos + 1;
			}
			readNextRealArc(arc, in);
			assert arc.isLast();
			return arc;
		}
	}

	public Arc<T> readFirstTargetArc(Arc<T> follow, Arc<T> arc)
			throws IOException {

		if (follow.isFinal()) {

			arc.label = END_LABEL;
			arc.output = follow.nextFinalOutput;
			if (follow.target <= 0) {
				arc.flags = BIT_LAST_ARC;
			} else {
				arc.flags = 0;
				arc.nextArc = follow.target;
			}

			return arc;
		} else {
			return readFirstRealArc(follow.target, arc);
		}
	}

	Arc<T> readFirstRealArc(int address, Arc<T> arc) throws IOException {

		final BytesReader in = getBytesReader(address);

		arc.flags = in.readByte();

		if (arc.flag(BIT_ARCS_AS_FIXED_ARRAY)) {

			arc.numArcs = in.readVInt();
			arc.bytesPerArc = in.readInt();
			arc.arcIdx = -1;
			arc.nextArc = arc.posArcsStart = in.pos;

		} else {
			arc.nextArc = address;
			arc.bytesPerArc = 0;
		}
		return readNextRealArc(arc, in);
	}

	boolean isExpandedTarget(Arc<T> follow) throws IOException {
		if (!targetHasArcs(follow)) {
			return false;
		} else {
			final BytesReader in = getBytesReader(follow.target);
			final byte b = in.readByte();
			return (b & BIT_ARCS_AS_FIXED_ARRAY) != 0;
		}
	}

	public Arc<T> readNextArc(Arc<T> arc) throws IOException {
		if (arc.label == END_LABEL) {

			if (arc.nextArc <= 0) {

				return null;
			}
			return readFirstRealArc(arc.nextArc, arc);
		} else {
			return readNextRealArc(arc, getBytesReader(0));
		}
	}

	public int readNextArcLabel(Arc<T> arc) throws IOException {
		assert !arc.isLast();

		final BytesReader in;
		if (arc.label == END_LABEL) {

			in = getBytesReader(arc.nextArc);
			byte flags = bytes[in.pos];
			if (flag(flags, BIT_ARCS_AS_FIXED_ARRAY)) {

				in.pos--;
				in.readVInt();
				in.readInt();
			}
		} else {
			if (arc.bytesPerArc != 0) {

				in = getBytesReader(arc.posArcsStart - (1 + arc.arcIdx)
						* arc.bytesPerArc);
			} else {

				in = getBytesReader(arc.nextArc);
			}
		}

		in.readByte();
		return readLabel(in);
	}

	Arc<T> readNextRealArc(Arc<T> arc, final BytesReader in) throws IOException {

		if (arc.bytesPerArc != 0) {

			arc.arcIdx++;
			assert arc.arcIdx < arc.numArcs;
			in.pos = arc.posArcsStart - arc.arcIdx * arc.bytesPerArc;
		} else {

			in.pos = arc.nextArc;
		}
		arc.flags = in.readByte();
		arc.label = readLabel(in);

		if (arc.flag(BIT_ARC_HAS_OUTPUT)) {
			arc.output = outputs.read(in);
		} else {
			arc.output = outputs.getNoOutput();
		}

		if (arc.flag(BIT_ARC_HAS_FINAL_OUTPUT)) {
			arc.nextFinalOutput = outputs.read(in);
		} else {
			arc.nextFinalOutput = outputs.getNoOutput();
		}

		if (arc.flag(BIT_STOP_NODE)) {
			if (arc.flag(BIT_FINAL_ARC)) {
				arc.target = FINAL_END_NODE;
			} else {
				arc.target = NON_FINAL_END_NODE;
			}
			arc.nextArc = in.pos;
		} else if (arc.flag(BIT_TARGET_NEXT)) {
			arc.nextArc = in.pos;
			if (!arc.flag(BIT_LAST_ARC)) {
				if (arc.bytesPerArc == 0) {

					seekToNextNode(in);
				} else {
					in.pos = arc.posArcsStart - arc.bytesPerArc * arc.numArcs;
				}
			}
			arc.target = in.pos;
		} else {
			arc.target = in.readInt();
			arc.nextArc = in.pos;
		}

		return arc;
	}

	public Arc<T> findTargetArc(int labelToMatch, Arc<T> follow, Arc<T> arc)
			throws IOException {
		assert cachedRootArcs != null;

		if (follow.target == startNode && labelToMatch != END_LABEL
				&& labelToMatch < cachedRootArcs.length) {
			final Arc<T> result = cachedRootArcs[labelToMatch];
			if (result == null) {
				return result;
			} else {
				arc.copyFrom(result);
				return arc;
			}
		}

		if (labelToMatch == END_LABEL) {
			if (follow.isFinal()) {
				arc.output = follow.nextFinalOutput;
				arc.label = END_LABEL;
				return arc;
			} else {
				return null;
			}
		}

		if (!targetHasArcs(follow)) {
			return null;
		}

		final BytesReader in = getBytesReader(follow.target);

		if ((in.readByte() & BIT_ARCS_AS_FIXED_ARRAY) != 0) {

			arc.numArcs = in.readVInt();

			arc.bytesPerArc = in.readInt();
			arc.posArcsStart = in.pos;
			int low = 0;
			int high = arc.numArcs - 1;
			while (low <= high) {

				int mid = (low + high) >>> 1;
				in.pos = arc.posArcsStart - arc.bytesPerArc * mid - 1;
				int midLabel = readLabel(in);
				final int cmp = midLabel - labelToMatch;
				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else {
					arc.arcIdx = mid - 1;

					return readNextRealArc(arc, in);
				}
			}

			return null;
		}

		readFirstTargetArc(follow, arc);
		while (true) {

			if (arc.label == labelToMatch) {

				return arc;
			} else if (arc.label > labelToMatch) {
				return null;
			} else if (arc.isLast()) {
				return null;
			} else {
				readNextArc(arc);
			}
		}
	}

	private void seekToNextNode(BytesReader in) throws IOException {

		while (true) {

			final int flags = in.readByte();
			readLabel(in);

			if (flag(flags, BIT_ARC_HAS_OUTPUT)) {
				outputs.read(in);
			}

			if (flag(flags, BIT_ARC_HAS_FINAL_OUTPUT)) {
				outputs.read(in);
			}

			if (!flag(flags, BIT_STOP_NODE) && !flag(flags, BIT_TARGET_NEXT)) {
				in.readInt();
			}

			if (flag(flags, BIT_LAST_ARC)) {
				return;
			}
		}
	}

	public int getNodeCount() {

		return 1 + nodeCount;
	}

	public int getArcCount() {
		return arcCount;
	}

	public int getArcWithOutputCount() {
		return arcWithOutputCount;
	}

	private boolean shouldExpand(UnCompiledNode<T> node) {
		return (node.depth <= FIXED_ARRAY_SHALLOW_DISTANCE && node.numArcs >= FIXED_ARRAY_NUM_ARCS_SHALLOW)
				|| node.numArcs >= FIXED_ARRAY_NUM_ARCS_DEEP;
	}

	class BytesWriter extends DataOutput {
		int posWrite;

		public BytesWriter() {

			posWrite = 1;
		}

		@Override
		public void writeByte(byte b) {
			if (bytes.length == posWrite) {
				bytes = ArrayUtil.grow(bytes);
			}
			assert posWrite < bytes.length : "posWrite=" + posWrite
					+ " bytes.length=" + bytes.length;
			bytes[posWrite++] = b;
		}

		@Override
		public void writeBytes(byte[] b, int offset, int length) {
			final int size = posWrite + length;
			bytes = ArrayUtil.grow(bytes, size);
			System.arraycopy(b, offset, bytes, posWrite, length);
			posWrite += length;
		}
	}

	final BytesReader getBytesReader(int pos) {

		return new BytesReader(pos);
	}

	final class BytesReader extends DataInput {
		int pos;

		public BytesReader(int pos) {
			this.pos = pos;
		}

		@Override
		public byte readByte() {
			return bytes[pos--];
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) {
			for (int i = 0; i < len; i++) {
				b[offset + i] = bytes[pos--];
			}
		}
	}
}
