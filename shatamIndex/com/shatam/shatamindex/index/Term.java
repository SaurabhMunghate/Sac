/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import com.shatam.shatamindex.util.StringHelper;

public final class Term implements Comparable<Term>, java.io.Serializable {
	String field;
	String text;

	public Term(String fld, String txt) {
		field = StringHelper.intern(fld);
		text = txt;
	}

	public Term(String fld) {
		this(fld, "", true);
	}

	Term(String fld, String txt, boolean intern) {
		field = intern ? StringHelper.intern(fld) : fld;
		text = txt;
	}

	public final String field() {
		return field;
	}

	public final String text() {
		return text;
	}

	public Term createTerm(String text) {
		return new Term(field, text, false);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Term other = (Term) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (field != other.field)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	public final int compareTo(Term other) {
		if (field == other.field)
			return text.compareTo(other.text);
		else
			return field.compareTo(other.field);
	}

	final void set(String fld, String txt) {
		field = fld;
		text = txt;
	}

	@Override
	public final String toString() {
		return field + ":" + text;
	}

	private void readObject(java.io.ObjectInputStream in)
			throws java.io.IOException, ClassNotFoundException {
		in.defaultReadObject();
		field = StringHelper.intern(field);
	}
}
