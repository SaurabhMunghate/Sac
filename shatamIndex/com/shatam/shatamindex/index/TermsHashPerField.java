/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Arrays;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttribute;
import com.shatam.shatamindex.document.Fieldable;
import com.shatam.shatamindex.util.RamUsageEstimator;
import com.shatam.shatamindex.util.SorterTemplate;
import com.shatam.shatamindex.util.UnicodeUtil;

final class TermsHashPerField extends InvertedDocConsumerPerField {

	final TermsHashConsumerPerField consumer;

	final TermsHashPerField nextPerField;
	final TermsHashPerThread perThread;
	final DocumentsWriter.DocState docState;
	final FieldInvertState fieldState;
	CharTermAttribute termAtt;

	
	final CharBlockPool charPool;
	final IntBlockPool intPool;
	final ByteBlockPool bytePool;

	final int streamCount;
	final int numPostingInt;

	final FieldInfo fieldInfo;

	boolean postingsCompacted;
	int numPostings;
	private int postingsHashSize = 4;
	private int postingsHashHalfSize = postingsHashSize / 2;
	private int postingsHashMask = postingsHashSize - 1;
	private int[] postingsHash;

	ParallelPostingsArray postingsArray;

	public TermsHashPerField(DocInverterPerField docInverterPerField,
			final TermsHashPerThread perThread,
			final TermsHashPerThread nextPerThread, final FieldInfo fieldInfo) {
		this.perThread = perThread;
		intPool = perThread.intPool;
		charPool = perThread.charPool;
		bytePool = perThread.bytePool;
		docState = perThread.docState;

		postingsHash = new int[postingsHashSize];
		Arrays.fill(postingsHash, -1);
		bytesUsed(postingsHashSize * RamUsageEstimator.NUM_BYTES_INT);

		fieldState = docInverterPerField.fieldState;
		this.consumer = perThread.consumer.addField(this, fieldInfo);
		initPostingsArray();

		streamCount = consumer.getStreamCount();
		numPostingInt = 2 * streamCount;
		this.fieldInfo = fieldInfo;
		if (nextPerThread != null)
			nextPerField = (TermsHashPerField) nextPerThread.addField(
					docInverterPerField, fieldInfo);
		else
			nextPerField = null;
	}

	private void initPostingsArray() {
		postingsArray = consumer.createPostingsArray(2);
		bytesUsed(postingsArray.size * postingsArray.bytesPerPosting());
	}

	
	private void bytesUsed(long size) {
		if (perThread.termsHash.trackAllocations) {
			perThread.termsHash.docWriter.bytesUsed(size);
		}
	}

	void shrinkHash(int targetSize) {
		assert postingsCompacted || numPostings == 0;

		final int newSize = 4;
		if (newSize != postingsHash.length) {
			final long previousSize = postingsHash.length;
			postingsHash = new int[newSize];
			bytesUsed((newSize - previousSize)
					* RamUsageEstimator.NUM_BYTES_INT);
			Arrays.fill(postingsHash, -1);
			postingsHashSize = newSize;
			postingsHashHalfSize = newSize / 2;
			postingsHashMask = newSize - 1;
		}

		
		if (postingsArray != null) {
			bytesUsed(-postingsArray.bytesPerPosting() * postingsArray.size);
			postingsArray = null;
		}
	}

	public void reset() {
		if (!postingsCompacted)
			compactPostings();
		assert numPostings <= postingsHash.length;
		if (numPostings > 0) {
			Arrays.fill(postingsHash, 0, numPostings, -1);
			numPostings = 0;
		}
		postingsCompacted = false;
		if (nextPerField != null)
			nextPerField.reset();
	}

	@Override
	synchronized public void abort() {
		reset();
		if (nextPerField != null)
			nextPerField.abort();
	}

	private final void growParallelPostingsArray() {
		int oldSize = postingsArray.size;
		this.postingsArray = this.postingsArray.grow();
		bytesUsed(postingsArray.bytesPerPosting()
				* (postingsArray.size - oldSize));
	}

	public void initReader(ByteSliceReader reader, int termID, int stream) {
		assert stream < streamCount;
		int intStart = postingsArray.intStarts[termID];
		final int[] ints = intPool.buffers[intStart >> DocumentsWriter.INT_BLOCK_SHIFT];
		final int upto = intStart & DocumentsWriter.INT_BLOCK_MASK;
		reader.init(bytePool, postingsArray.byteStarts[termID] + stream
				* ByteBlockPool.FIRST_LEVEL_SIZE, ints[upto + stream]);
	}

	private void compactPostings() {
		int upto = 0;
		for (int i = 0; i < postingsHashSize; i++) {
			if (postingsHash[i] != -1) {
				if (upto < i) {
					postingsHash[upto] = postingsHash[i];
					postingsHash[i] = -1;
				}
				upto++;
			}
		}

		assert upto == numPostings : "upto=" + upto + " numPostings="
				+ numPostings;
		postingsCompacted = true;
	}

	public int[] sortPostings() {
		compactPostings();
		final int[] postingsHash = this.postingsHash;
		new SorterTemplate() {
			@Override
			protected void swap(int i, int j) {
				final int o = postingsHash[i];
				postingsHash[i] = postingsHash[j];
				postingsHash[j] = o;
			}

			@Override
			protected int compare(int i, int j) {
				final int term1 = postingsHash[i], term2 = postingsHash[j];
				if (term1 == term2)
					return 0;
				final int textStart1 = postingsArray.textStarts[term1], textStart2 = postingsArray.textStarts[term2];
				final char[] text1 = charPool.buffers[textStart1 >> DocumentsWriter.CHAR_BLOCK_SHIFT];
				final int pos1 = textStart1 & DocumentsWriter.CHAR_BLOCK_MASK;
				final char[] text2 = charPool.buffers[textStart2 >> DocumentsWriter.CHAR_BLOCK_SHIFT];
				final int pos2 = textStart2 & DocumentsWriter.CHAR_BLOCK_MASK;
				return comparePostings(text1, pos1, text2, pos2);
			}

			@Override
			protected void setPivot(int i) {
				pivotTerm = postingsHash[i];
				final int textStart = postingsArray.textStarts[pivotTerm];
				pivotBuf = charPool.buffers[textStart >> DocumentsWriter.CHAR_BLOCK_SHIFT];
				pivotBufPos = textStart & DocumentsWriter.CHAR_BLOCK_MASK;
			}

			@Override
			protected int comparePivot(int j) {
				final int term = postingsHash[j];
				if (pivotTerm == term)
					return 0;
				final int textStart = postingsArray.textStarts[term];
				final char[] text = charPool.buffers[textStart >> DocumentsWriter.CHAR_BLOCK_SHIFT];
				final int pos = textStart & DocumentsWriter.CHAR_BLOCK_MASK;
				return comparePostings(pivotBuf, pivotBufPos, text, pos);
			}

			private int pivotTerm, pivotBufPos;
			private char[] pivotBuf;

			private int comparePostings(final char[] text1, int pos1,
					final char[] text2, int pos2) {
				assert text1 != text2 || pos1 != pos2;

				while (true) {
					final char c1 = text1[pos1++];
					final char c2 = text2[pos2++];
					if (c1 != c2) {
						if (0xffff == c2)
							return 1;
						else if (0xffff == c1)
							return -1;
						else
							return c1 - c2;
					} else
						
						
						assert c1 != 0xffff;
				}
			}
		}.quickSort(0, numPostings - 1);
		return postingsHash;
	}

	private boolean postingEquals(final int termID, final char[] tokenText,
			final int tokenTextLen) {
		final int textStart = postingsArray.textStarts[termID];

		final char[] text = perThread.charPool.buffers[textStart >> DocumentsWriter.CHAR_BLOCK_SHIFT];
		assert text != null;
		int pos = textStart & DocumentsWriter.CHAR_BLOCK_MASK;

		int tokenPos = 0;
		for (; tokenPos < tokenTextLen; pos++, tokenPos++)
			if (tokenText[tokenPos] != text[pos])
				return false;
		return 0xffff == text[pos];
	}

	private boolean doCall;
	private boolean doNextCall;

	@Override
	void start(Fieldable f) {
		termAtt = fieldState.attributeSource
				.addAttribute(CharTermAttribute.class);
		consumer.start(f);
		if (nextPerField != null) {
			nextPerField.start(f);
		}
	}

	@Override
	boolean start(Fieldable[] fields, int count) throws IOException {
		doCall = consumer.start(fields, count);
		if (postingsArray == null) {
			initPostingsArray();
		}

		if (nextPerField != null)
			doNextCall = nextPerField.start(fields, count);
		return doCall || doNextCall;
	}

	
	
	
	public void add(int textStart) throws IOException {
		int code = textStart;

		int hashPos = code & postingsHashMask;

		assert !postingsCompacted;

		
		int termID = postingsHash[hashPos];

		if (termID != -1 && postingsArray.textStarts[termID] != textStart) {
			
			
			final int inc = ((code >> 8) + code) | 1;
			do {
				code += inc;
				hashPos = code & postingsHashMask;
				termID = postingsHash[hashPos];
			} while (termID != -1
					&& postingsArray.textStarts[termID] != textStart);
		}

		if (termID == -1) {

			
			

			
			termID = numPostings++;
			if (termID >= postingsArray.size) {
				growParallelPostingsArray();
			}

			assert termID >= 0;

			postingsArray.textStarts[termID] = textStart;

			assert postingsHash[hashPos] == -1;
			postingsHash[hashPos] = termID;

			if (numPostings == postingsHashHalfSize)
				rehashPostings(2 * postingsHashSize);

			
			if (numPostingInt + intPool.intUpto > DocumentsWriter.INT_BLOCK_SIZE)
				intPool.nextBuffer();

			if (DocumentsWriter.BYTE_BLOCK_SIZE - bytePool.byteUpto < numPostingInt
					* ByteBlockPool.FIRST_LEVEL_SIZE)
				bytePool.nextBuffer();

			intUptos = intPool.buffer;
			intUptoStart = intPool.intUpto;
			intPool.intUpto += streamCount;

			postingsArray.intStarts[termID] = intUptoStart + intPool.intOffset;

			for (int i = 0; i < streamCount; i++) {
				final int upto = bytePool
						.newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
				intUptos[intUptoStart + i] = upto + bytePool.byteOffset;
			}
			postingsArray.byteStarts[termID] = intUptos[intUptoStart];

			consumer.newTerm(termID);

		} else {
			int intStart = postingsArray.intStarts[termID];
			intUptos = intPool.buffers[intStart >> DocumentsWriter.INT_BLOCK_SHIFT];
			intUptoStart = intStart & DocumentsWriter.INT_BLOCK_MASK;
			consumer.addTerm(termID);
		}
	}

	
	@Override
	void add() throws IOException {

		assert !postingsCompacted;

		
		

		
		final char[] tokenText = termAtt.buffer();
		final int tokenTextLen = termAtt.length();

		
		int downto = tokenTextLen;
		int code = 0;
		while (downto > 0) {
			char ch = tokenText[--downto];

			if (ch >= UnicodeUtil.UNI_SUR_LOW_START
					&& ch <= UnicodeUtil.UNI_SUR_LOW_END) {
				if (0 == downto) {
					
					ch = tokenText[downto] = UnicodeUtil.UNI_REPLACEMENT_CHAR;
				} else {
					final char ch2 = tokenText[downto - 1];
					if (ch2 >= UnicodeUtil.UNI_SUR_HIGH_START
							&& ch2 <= UnicodeUtil.UNI_SUR_HIGH_END) {
						
						
						code = ((code * 31) + ch) * 31 + ch2;
						downto--;
						continue;
					} else {
						
						ch = tokenText[downto] = UnicodeUtil.UNI_REPLACEMENT_CHAR;
					}
				}
			} else if (ch >= UnicodeUtil.UNI_SUR_HIGH_START
					&& (ch <= UnicodeUtil.UNI_SUR_HIGH_END || ch == 0xffff)) {
				
				ch = tokenText[downto] = UnicodeUtil.UNI_REPLACEMENT_CHAR;
			}

			code = (code * 31) + ch;
		}

		int hashPos = code & postingsHashMask;

		
		int termID = postingsHash[hashPos];

		if (termID != -1 && !postingEquals(termID, tokenText, tokenTextLen)) {
			
			
			final int inc = ((code >> 8) + code) | 1;
			do {
				code += inc;
				hashPos = code & postingsHashMask;
				termID = postingsHash[hashPos];
			} while (termID != -1
					&& !postingEquals(termID, tokenText, tokenTextLen));
		}

		if (termID == -1) {

			
			
			final int textLen1 = 1 + tokenTextLen;
			if (textLen1 + charPool.charUpto > DocumentsWriter.CHAR_BLOCK_SIZE) {
				if (textLen1 > DocumentsWriter.CHAR_BLOCK_SIZE) {
					
					
					
					
					

					if (docState.maxTermPrefix == null)
						docState.maxTermPrefix = new String(tokenText, 0, 30);

					consumer.skippingLongTerm();
					return;
				}
				charPool.nextBuffer();
			}

			
			termID = numPostings++;
			if (termID >= postingsArray.size) {
				growParallelPostingsArray();
			}

			assert termID != -1;

			final char[] text = charPool.buffer;
			final int textUpto = charPool.charUpto;
			postingsArray.textStarts[termID] = textUpto + charPool.charOffset;
			charPool.charUpto += textLen1;
			System.arraycopy(tokenText, 0, text, textUpto, tokenTextLen);
			text[textUpto + tokenTextLen] = 0xffff;

			assert postingsHash[hashPos] == -1;
			postingsHash[hashPos] = termID;

			if (numPostings == postingsHashHalfSize) {
				rehashPostings(2 * postingsHashSize);
				bytesUsed(2 * numPostings * RamUsageEstimator.NUM_BYTES_INT);
			}

			
			if (numPostingInt + intPool.intUpto > DocumentsWriter.INT_BLOCK_SIZE)
				intPool.nextBuffer();

			if (DocumentsWriter.BYTE_BLOCK_SIZE - bytePool.byteUpto < numPostingInt
					* ByteBlockPool.FIRST_LEVEL_SIZE)
				bytePool.nextBuffer();

			intUptos = intPool.buffer;
			intUptoStart = intPool.intUpto;
			intPool.intUpto += streamCount;

			postingsArray.intStarts[termID] = intUptoStart + intPool.intOffset;

			for (int i = 0; i < streamCount; i++) {
				final int upto = bytePool
						.newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
				intUptos[intUptoStart + i] = upto + bytePool.byteOffset;
			}
			postingsArray.byteStarts[termID] = intUptos[intUptoStart];

			consumer.newTerm(termID);

		} else {
			final int intStart = postingsArray.intStarts[termID];
			intUptos = intPool.buffers[intStart >> DocumentsWriter.INT_BLOCK_SHIFT];
			intUptoStart = intStart & DocumentsWriter.INT_BLOCK_MASK;
			consumer.addTerm(termID);
		}

		if (doNextCall)
			nextPerField.add(postingsArray.textStarts[termID]);
	}

	int[] intUptos;
	int intUptoStart;

	void writeByte(int stream, byte b) {
		int upto = intUptos[intUptoStart + stream];
		byte[] bytes = bytePool.buffers[upto >> DocumentsWriter.BYTE_BLOCK_SHIFT];
		assert bytes != null;
		int offset = upto & DocumentsWriter.BYTE_BLOCK_MASK;
		if (bytes[offset] != 0) {
			
			offset = bytePool.allocSlice(bytes, offset);
			bytes = bytePool.buffer;
			intUptos[intUptoStart + stream] = offset + bytePool.byteOffset;
		}
		bytes[offset] = b;
		(intUptos[intUptoStart + stream])++;
	}

	public void writeBytes(int stream, byte[] b, int offset, int len) {
		
		final int end = offset + len;
		for (int i = offset; i < end; i++)
			writeByte(stream, b[i]);
	}

	void writeVInt(int stream, int i) {
		assert stream < streamCount;
		while ((i & ~0x7F) != 0) {
			writeByte(stream, (byte) ((i & 0x7f) | 0x80));
			i >>>= 7;
		}
		writeByte(stream, (byte) i);
	}

	@Override
	void finish() throws IOException {
		try {
			consumer.finish();
		} finally {
			if (nextPerField != null) {
				nextPerField.finish();
			}
		}
	}

	void rehashPostings(final int newSize) {

		final int newMask = newSize - 1;

		int[] newHash = new int[newSize];
		Arrays.fill(newHash, -1);
		for (int i = 0; i < postingsHashSize; i++) {
			int termID = postingsHash[i];
			if (termID != -1) {
				int code;
				if (perThread.primary) {
					final int textStart = postingsArray.textStarts[termID];
					final int start = textStart
							& DocumentsWriter.CHAR_BLOCK_MASK;
					final char[] text = charPool.buffers[textStart >> DocumentsWriter.CHAR_BLOCK_SHIFT];
					int pos = start;
					while (text[pos] != 0xffff)
						pos++;
					code = 0;
					while (pos > start)
						code = (code * 31) + text[--pos];
				} else
					code = postingsArray.textStarts[termID];

				int hashPos = code & newMask;
				assert hashPos >= 0;
				if (newHash[hashPos] != -1) {
					final int inc = ((code >> 8) + code) | 1;
					do {
						code += inc;
						hashPos = code & newMask;
					} while (newHash[hashPos] != -1);
				}
				newHash[hashPos] = termID;
			}
		}

		postingsHashMask = newMask;
		postingsHash = newHash;

		postingsHashSize = newSize;
		postingsHashHalfSize = newSize >> 1;
	}
}
