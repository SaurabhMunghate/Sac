/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import java.util.Set;

public class SetBasedFieldSelector implements FieldSelector {

	private Set<String> fieldsToLoad;
	private Set<String> lazyFieldsToLoad;

	public SetBasedFieldSelector(Set<String> fieldsToLoad,
			Set<String> lazyFieldsToLoad) {
		this.fieldsToLoad = fieldsToLoad;
		this.lazyFieldsToLoad = lazyFieldsToLoad;
	}

	public FieldSelectorResult accept(String fieldName) {
		FieldSelectorResult result = FieldSelectorResult.NO_LOAD;
		if (fieldsToLoad.contains(fieldName) == true) {
			result = FieldSelectorResult.LOAD;
		}
		if (lazyFieldsToLoad.contains(fieldName) == true) {
			result = FieldSelectorResult.LAZY_LOAD;
		}
		return result;
	}
}