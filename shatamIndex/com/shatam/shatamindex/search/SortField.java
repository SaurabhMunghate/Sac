/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import com.shatam.shatamindex.util.StringHelper;

public class SortField implements Serializable {

	public static final int SCORE = 0;

	public static final int DOC = 1;

	public static final int STRING = 3;

	public static final int INT = 4;

	public static final int FLOAT = 5;

	public static final int LONG = 6;

	public static final int DOUBLE = 7;

	public static final int SHORT = 8;

	public static final int CUSTOM = 9;

	public static final int BYTE = 10;

	public static final int STRING_VAL = 11;

	public static final SortField FIELD_SCORE = new SortField(null, SCORE);

	public static final SortField FIELD_DOC = new SortField(null, DOC);

	private String field;
	private int type;
	private Locale locale;
	boolean reverse = false;
	private FieldCache.Parser parser;

	private FieldComparatorSource comparatorSource;

	private Object missingValue;

	public SortField(String field, int type) {
		initFieldType(field, type);
	}

	public SortField(String field, int type, boolean reverse) {
		initFieldType(field, type);
		this.reverse = reverse;
	}

	public SortField(String field, FieldCache.Parser parser) {
		this(field, parser, false);
	}

	public SortField(String field, FieldCache.Parser parser, boolean reverse) {
		if (parser instanceof FieldCache.IntParser)
			initFieldType(field, INT);
		else if (parser instanceof FieldCache.FloatParser)
			initFieldType(field, FLOAT);
		else if (parser instanceof FieldCache.ShortParser)
			initFieldType(field, SHORT);
		else if (parser instanceof FieldCache.ByteParser)
			initFieldType(field, BYTE);
		else if (parser instanceof FieldCache.LongParser)
			initFieldType(field, LONG);
		else if (parser instanceof FieldCache.DoubleParser)
			initFieldType(field, DOUBLE);
		else
			throw new IllegalArgumentException(
					"Parser instance does not subclass existing numeric parser from FieldCache (got "
							+ parser + ")");

		this.reverse = reverse;
		this.parser = parser;
	}

	public SortField(String field, Locale locale) {
		initFieldType(field, STRING);
		this.locale = locale;
	}

	public SortField(String field, Locale locale, boolean reverse) {
		initFieldType(field, STRING);
		this.locale = locale;
		this.reverse = reverse;
	}

	public SortField(String field, FieldComparatorSource comparator) {
		initFieldType(field, CUSTOM);
		this.comparatorSource = comparator;
	}

	public SortField(String field, FieldComparatorSource comparator,
			boolean reverse) {
		initFieldType(field, CUSTOM);
		this.reverse = reverse;
		this.comparatorSource = comparator;
	}

	public SortField setMissingValue(Object missingValue) {
		if (type != BYTE && type != SHORT && type != INT && type != FLOAT
				&& type != LONG && type != DOUBLE) {
			throw new IllegalArgumentException(
					"Missing value only works for numeric types");
		}
		this.missingValue = missingValue;

		return this;
	}

	private void initFieldType(String field, int type) {
		this.type = type;
		if (field == null) {
			if (type != SCORE && type != DOC)
				throw new IllegalArgumentException(
						"field can only be null when type is SCORE or DOC");
		} else {
			this.field = StringHelper.intern(field);
		}
	}

	public String getField() {
		return field;
	}

	public int getType() {
		return type;
	}

	public Locale getLocale() {
		return locale;
	}

	public FieldCache.Parser getParser() {
		return parser;
	}

	public boolean getReverse() {
		return reverse;
	}

	public FieldComparatorSource getComparatorSource() {
		return comparatorSource;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		switch (type) {
		case SCORE:
			buffer.append("<score>");
			break;

		case DOC:
			buffer.append("<doc>");
			break;

		case STRING:
			buffer.append("<string: \"").append(field).append("\">");
			break;

		case STRING_VAL:
			buffer.append("<string_val: \"").append(field).append("\">");
			break;

		case BYTE:
			buffer.append("<byte: \"").append(field).append("\">");
			break;

		case SHORT:
			buffer.append("<short: \"").append(field).append("\">");
			break;

		case INT:
			buffer.append("<int: \"").append(field).append("\">");
			break;

		case LONG:
			buffer.append("<long: \"").append(field).append("\">");
			break;

		case FLOAT:
			buffer.append("<float: \"").append(field).append("\">");
			break;

		case DOUBLE:
			buffer.append("<double: \"").append(field).append("\">");
			break;

		case CUSTOM:
			buffer.append("<custom:\"").append(field).append("\": ")
					.append(comparatorSource).append('>');
			break;

		default:
			buffer.append("<???: \"").append(field).append("\">");
			break;
		}

		if (locale != null)
			buffer.append('(').append(locale).append(')');
		if (parser != null)
			buffer.append('(').append(parser).append(')');
		if (reverse)
			buffer.append('!');

		return buffer.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SortField))
			return false;
		final SortField other = (SortField) o;
		return (other.field == this.field
				&& other.type == this.type
				&& other.reverse == this.reverse
				&& (other.locale == null ? this.locale == null : other.locale
						.equals(this.locale))
				&& (other.comparatorSource == null ? this.comparatorSource == null
						: other.comparatorSource.equals(this.comparatorSource)) && (other.parser == null ? this.parser == null
					: other.parser.equals(this.parser)));
	}

	@Override
	public int hashCode() {
		int hash = type ^ 0x346565dd + Boolean.valueOf(reverse).hashCode()
				^ 0xaf5998bb;
		if (field != null)
			hash += field.hashCode() ^ 0xff5685dd;
		if (locale != null)
			hash += locale.hashCode() ^ 0x08150815;
		if (comparatorSource != null)
			hash += comparatorSource.hashCode();
		if (parser != null)
			hash += parser.hashCode() ^ 0x3aaf56ff;
		return hash;
	}

	private void readObject(java.io.ObjectInputStream in)
			throws java.io.IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (field != null)
			field = StringHelper.intern(field);
	}

	public FieldComparator getComparator(final int numHits, final int sortPos)
			throws IOException {

		if (locale != null) {

			return new FieldComparator.StringComparatorLocale(numHits, field,
					locale);
		}

		switch (type) {
		case SortField.SCORE:
			return new FieldComparator.RelevanceComparator(numHits);

		case SortField.DOC:
			return new FieldComparator.DocComparator(numHits);

		case SortField.INT:
			return new FieldComparator.IntComparator(numHits, field, parser,
					(Integer) missingValue);

		case SortField.FLOAT:
			return new FieldComparator.FloatComparator(numHits, field, parser,
					(Float) missingValue);

		case SortField.LONG:
			return new FieldComparator.LongComparator(numHits, field, parser,
					(Long) missingValue);

		case SortField.DOUBLE:
			return new FieldComparator.DoubleComparator(numHits, field, parser,
					(Double) missingValue);

		case SortField.BYTE:
			return new FieldComparator.ByteComparator(numHits, field, parser,
					(Byte) missingValue);

		case SortField.SHORT:
			return new FieldComparator.ShortComparator(numHits, field, parser,
					(Short) missingValue);

		case SortField.CUSTOM:
			assert comparatorSource != null;
			return comparatorSource.newComparator(field, numHits, sortPos,
					reverse);

		case SortField.STRING:
			return new FieldComparator.StringOrdValComparator(numHits, field,
					sortPos, reverse);

		case SortField.STRING_VAL:
			return new FieldComparator.StringValComparator(numHits, field);

		default:
			throw new IllegalStateException("Illegal sort type: " + type);
		}
	}
}
