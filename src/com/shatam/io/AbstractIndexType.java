/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.io;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.RefinedSoundex;

import com.shatam.data.USPSAliasStreetModel;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public abstract class AbstractIndexType {
	public static final AbstractIndexType NORMAL = new NormalIndexType("k1");
	public static final AbstractIndexType METAPHONE = new CodecIndexType("k2",
			new Metaphone());
	public static final AbstractIndexType SOUNDEX = new CodecIndexType("k3",
			new Soundex());
	public static final AbstractIndexType DOUBLE_METAPHONE = new CodecIndexType(
			"k4", new DoubleMetaphone());
	public static final AbstractIndexType REFINED_SOUNDEX = new CodecIndexType(
			"k5", new RefinedSoundex());

	public static AbstractIndexType[] TYPES = { NORMAL, SOUNDEX, METAPHONE,
			DOUBLE_METAPHONE, REFINED_SOUNDEX };

	public static String standardize(String v) {
		if (v.length() <= 1 && v.matches("[a-z]")) {
			v = v;
		}
		return v;

	}

	public final String encode(String v) throws Exception {
		v = standardize(v);
		return innerEncode(v);
	}

	protected abstract String innerEncode(String v) throws Exception;

	public abstract String getFieldName();

	public String buildQuery(AddressStruct struct) throws Exception {
		StringBuffer buf = new StringBuffer();
		for (AddColumns col : AddColumns.values()) {
			_app(buf, col, struct);
		}
		return buf.toString().trim();
	}

	private void _app(StringBuffer buf, AddColumns col, AddressStruct struct)
			throws Exception {
		if (!AddColumns.useInQuery(col)) {
			return;
		}

		String v = struct.get(col);
		if (!StrUtil.isEmpty(v)) {
			v = v.toLowerCase();

			if (col == AddColumns.CITY) {
				v = v.replaceAll(StrUtil.WORD_DELIMETER, "");
			}

			v = this.encode(v);

			buf.append(v);

			if (col == AddColumns.CITY || col == AddColumns.ZIP) {
				buf.append("_" + col.name());
			}

			buf.append(" ");
		}
	}

}
