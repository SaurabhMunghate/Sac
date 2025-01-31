/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.index;

import java.util.regex.Pattern;

public final class IndexFileNames {

	public static final String SEGMENTS = "segments";

	public static final String SEGMENTS_GEN = "segments.gen";

	public static final String DELETABLE = "deletable";

	public static final String NORMS_EXTENSION = "nrm";

	public static final String FREQ_EXTENSION = "frq";

	public static final String PROX_EXTENSION = "prx";

	public static final String TERMS_EXTENSION = "tis";

	public static final String TERMS_INDEX_EXTENSION = "tii";

	public static final String FIELDS_INDEX_EXTENSION = "fdx";

	public static final String FIELDS_EXTENSION = "fdt";

	public static final String VECTORS_FIELDS_EXTENSION = "tvf";

	public static final String VECTORS_DOCUMENTS_EXTENSION = "tvd";

	public static final String VECTORS_INDEX_EXTENSION = "tvx";

	public static final String COMPOUND_FILE_EXTENSION = "cfs";

	public static final String COMPOUND_FILE_STORE_EXTENSION = "cfx";

	public static final String DELETES_EXTENSION = "del";

	public static final String FIELD_INFOS_EXTENSION = "fnm";

	public static final String PLAIN_NORMS_EXTENSION = "f";

	public static final String SEPARATE_NORMS_EXTENSION = "s";

	public static final String GEN_EXTENSION = "gen";

	public static final String INDEX_EXTENSIONS[] = new String[] {
			COMPOUND_FILE_EXTENSION, FIELD_INFOS_EXTENSION,
			FIELDS_INDEX_EXTENSION, FIELDS_EXTENSION, TERMS_INDEX_EXTENSION,
			TERMS_EXTENSION, FREQ_EXTENSION, PROX_EXTENSION, DELETES_EXTENSION,
			VECTORS_INDEX_EXTENSION, VECTORS_DOCUMENTS_EXTENSION,
			VECTORS_FIELDS_EXTENSION, GEN_EXTENSION, NORMS_EXTENSION,
			COMPOUND_FILE_STORE_EXTENSION, };

	public static final String[] INDEX_EXTENSIONS_IN_COMPOUND_FILE = new String[] {
			FIELD_INFOS_EXTENSION, FIELDS_INDEX_EXTENSION, FIELDS_EXTENSION,
			TERMS_INDEX_EXTENSION, TERMS_EXTENSION, FREQ_EXTENSION,
			PROX_EXTENSION, VECTORS_INDEX_EXTENSION,
			VECTORS_DOCUMENTS_EXTENSION, VECTORS_FIELDS_EXTENSION,
			NORMS_EXTENSION };

	public static final String[] STORE_INDEX_EXTENSIONS = new String[] {
			VECTORS_INDEX_EXTENSION, VECTORS_FIELDS_EXTENSION,
			VECTORS_DOCUMENTS_EXTENSION, FIELDS_INDEX_EXTENSION,
			FIELDS_EXTENSION };

	public static final String[] NON_STORE_INDEX_EXTENSIONS = new String[] {
			FIELD_INFOS_EXTENSION, FREQ_EXTENSION, PROX_EXTENSION,
			TERMS_EXTENSION, TERMS_INDEX_EXTENSION, NORMS_EXTENSION };

	public static final String COMPOUND_EXTENSIONS[] = new String[] {
			FIELD_INFOS_EXTENSION, FREQ_EXTENSION, PROX_EXTENSION,
			FIELDS_INDEX_EXTENSION, FIELDS_EXTENSION, TERMS_INDEX_EXTENSION,
			TERMS_EXTENSION };

	public static final String VECTOR_EXTENSIONS[] = new String[] {
			VECTORS_INDEX_EXTENSION, VECTORS_DOCUMENTS_EXTENSION,
			VECTORS_FIELDS_EXTENSION };

	public static final String fileNameFromGeneration(String base, String ext,
			long gen) {
		if (gen == SegmentInfo.NO) {
			return null;
		} else if (gen == SegmentInfo.WITHOUT_GEN) {
			return segmentFileName(base, ext);
		} else {

			StringBuilder res = new StringBuilder(base.length() + 6
					+ ext.length()).append(base).append('_')
					.append(Long.toString(gen, Character.MAX_RADIX));
			if (ext.length() > 0) {
				res.append('.').append(ext);
			}
			return res.toString();
		}
	}

	public static final boolean isDocStoreFile(String fileName) {
		if (fileName.endsWith(COMPOUND_FILE_STORE_EXTENSION))
			return true;
		for (String ext : STORE_INDEX_EXTENSIONS) {
			if (fileName.endsWith(ext))
				return true;
		}
		return false;
	}

	public static final String segmentFileName(String segmentName, String ext) {
		if (ext.length() > 0) {
			return new StringBuilder(segmentName.length() + 1 + ext.length())
					.append(segmentName).append('.').append(ext).toString();
		} else {
			return segmentName;
		}
	}

	public static final boolean matchesExtension(String filename, String ext) {

		return filename.endsWith("." + ext);
	}

	public static final String stripSegmentName(String filename) {

		int idx = filename.indexOf('_', 1);
		if (idx == -1) {

			idx = filename.indexOf('.');
		}
		if (idx != -1) {
			filename = filename.substring(idx);
		}
		return filename;
	}

	public static boolean isSeparateNormsFile(String filename) {
		int idx = filename.lastIndexOf('.');
		if (idx == -1)
			return false;
		String ext = filename.substring(idx + 1);
		return Pattern.matches(SEPARATE_NORMS_EXTENSION + "[0-9]+", ext);
	}

}
