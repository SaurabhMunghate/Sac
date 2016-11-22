/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.io.IOException;
import java.util.Iterator;

import com.shatam.shatamindex.store.IndexInput;
import com.shatam.shatamindex.store.RAMFile;
import com.shatam.shatamindex.store.RAMInputStream;
import com.shatam.shatamindex.store.RAMOutputStream;
import com.shatam.shatamindex.util.BytesRef;
import com.shatam.shatamindex.util.StringHelper;

class PrefixCodedTerms implements Iterable<Term> {
	final RAMFile buffer;

	private PrefixCodedTerms(RAMFile buffer) {
		this.buffer = buffer;
	}

	public long getSizeInBytes() {
		return buffer.getSizeInBytes();
	}

	public Iterator<Term> iterator() {
		return new PrefixCodedTermsIterator();
	}

	class PrefixCodedTermsIterator implements Iterator<Term> {
		final IndexInput input;
		String field = "";
		BytesRef bytes = new BytesRef();
		Term term = new Term(field, "");

		PrefixCodedTermsIterator() {
			try {
				input = new RAMInputStream("PrefixCodedTermsIterator", buffer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean hasNext() {
			return input.getFilePointer() < input.length();
		}

		public Term next() {
			assert hasNext();
			try {
				int code = input.readVInt();
				if ((code & 1) != 0) {

					field = StringHelper.intern(input.readString());
				}
				int prefix = code >>> 1;
				int suffix = input.readVInt();
				bytes.grow(prefix + suffix);
				input.readBytes(bytes.bytes, prefix, suffix);
				bytes.length = prefix + suffix;
				term.set(field, bytes.utf8ToString());
				return term;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public static class Builder {
		private RAMFile buffer = new RAMFile();
		private RAMOutputStream output = new RAMOutputStream(buffer);
		private Term lastTerm = new Term("");
		private BytesRef lastBytes = new BytesRef();
		private BytesRef scratch = new BytesRef();

		public void add(Term term) {
			assert lastTerm.equals(new Term(""))
					|| term.compareTo(lastTerm) > 0;

			scratch.copy(term.text);
			try {
				int prefix = sharedPrefix(lastBytes, scratch);
				int suffix = scratch.length - prefix;
				if (term.field.equals(lastTerm.field)) {
					output.writeVInt(prefix << 1);
				} else {
					output.writeVInt(prefix << 1 | 1);
					output.writeString(term.field);
				}
				output.writeVInt(suffix);
				output.writeBytes(scratch.bytes, scratch.offset + prefix,
						suffix);
				lastBytes.copy(scratch);
				lastTerm.text = term.text;
				lastTerm.field = term.field;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public PrefixCodedTerms finish() {
			try {
				output.close();
				return new PrefixCodedTerms(buffer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private int sharedPrefix(BytesRef term1, BytesRef term2) {
			int pos1 = 0;
			int pos1End = pos1 + Math.min(term1.length, term2.length);
			int pos2 = 0;
			while (pos1 < pos1End) {
				if (term1.bytes[term1.offset + pos1] != term2.bytes[term2.offset
						+ pos2]) {
					return pos1;
				}
				pos1++;
				pos2++;
			}
			return pos1;
		}
	}
}
