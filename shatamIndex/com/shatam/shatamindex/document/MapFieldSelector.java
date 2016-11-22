/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFieldSelector implements FieldSelector {

	Map<String, FieldSelectorResult> fieldSelections;

	public MapFieldSelector(Map<String, FieldSelectorResult> fieldSelections) {
		this.fieldSelections = fieldSelections;
	}

	public MapFieldSelector(List<String> fields) {
		fieldSelections = new HashMap<String, FieldSelectorResult>(
				fields.size() * 5 / 3);
		for (final String field : fields)
			fieldSelections.put(field, FieldSelectorResult.LOAD);
	}

	public MapFieldSelector(String... fields) {
		this(Arrays.asList(fields));
	}

	public FieldSelectorResult accept(String field) {
		FieldSelectorResult selection = fieldSelections.get(field);
		return selection != null ? selection : FieldSelectorResult.NO_LOAD;
	}

}
