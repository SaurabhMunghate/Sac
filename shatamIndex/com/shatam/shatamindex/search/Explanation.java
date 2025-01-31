/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search;

import java.io.Serializable;
import java.util.ArrayList;

public class Explanation implements java.io.Serializable {
	private float value;
	private String description;
	private ArrayList<Explanation> details;

	public Explanation() {
	}

	public Explanation(float value, String description) {
		this.value = value;
		this.description = description;
	}

	public boolean isMatch() {
		return (0.0f < getValue());
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	protected String getSummary() {
		return getValue() + " = " + getDescription();
	}

	public Explanation[] getDetails() {
		if (details == null)
			return null;
		return details.toArray(new Explanation[0]);
	}

	public void addDetail(Explanation detail) {
		if (details == null)
			details = new ArrayList<Explanation>();
		details.add(detail);
	}

	@Override
	public String toString() {
		return toString(0);
	}

	protected String toString(int depth) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			buffer.append("  ");
		}
		buffer.append(getSummary());
		buffer.append("\n");

		Explanation[] details = getDetails();
		if (details != null) {
			for (int i = 0; i < details.length; i++) {
				buffer.append(details[i].toString(depth + 1));
			}
		}

		return buffer.toString();
	}

	public String toHtml() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<ul>\n");

		buffer.append("<li>");
		buffer.append(getSummary());
		buffer.append("<br />\n");

		Explanation[] details = getDetails();
		if (details != null) {
			for (int i = 0; i < details.length; i++) {
				buffer.append(details[i].toHtml());
			}
		}

		buffer.append("</li>\n");
		buffer.append("</ul>\n");

		return buffer.toString();
	}

	public static abstract class IDFExplanation implements Serializable {

		public abstract float getIdf();

		public abstract String explain();
	}
}
