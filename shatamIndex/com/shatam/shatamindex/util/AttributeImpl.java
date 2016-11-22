/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttributeImpl;

public abstract class AttributeImpl implements Cloneable, Serializable,
		Attribute {

	public abstract void clear();

	@Override
	public String toString() {
		return reflectAsString(false);
	}

	public final String reflectAsString(final boolean prependAttClass) {
		final StringBuilder buffer = new StringBuilder();
		reflectWith(new AttributeReflector() {
			public void reflect(Class<? extends Attribute> attClass,
					String key, Object value) {
				if (buffer.length() > 0) {
					buffer.append(',');
				}
				if (prependAttClass) {
					buffer.append(attClass.getName()).append('#');
				}
				buffer.append(key).append('=')
						.append((value == null) ? "null" : value);
			}
		});
		return buffer.toString();
	}

	@Deprecated
	private static final VirtualMethod<AttributeImpl> toStringMethod = new VirtualMethod<AttributeImpl>(
			AttributeImpl.class, "toString");

	@Deprecated
	protected boolean enableBackwards = true;

	@Deprecated
	private boolean assertExternalClass(Class<? extends AttributeImpl> clazz) {
		final String name = clazz.getName();
		return (!name.startsWith("com.shatam.shatamindex.") && !name
				.startsWith("org.apache.solr."))
				|| name.equals("com.shatam.shatamindex.util.TestAttributeSource$TestAttributeImpl");
	}

	public void reflectWith(AttributeReflector reflector) {
		final Class<? extends AttributeImpl> clazz = this.getClass();
		final LinkedList<WeakReference<Class<? extends Attribute>>> interfaces = AttributeSource
				.getAttributeInterfaces(clazz);
		if (interfaces.size() != 1) {
			throw new UnsupportedOperationException(
					clazz.getName()
							+ " implements more than one Attribute interface, the default reflectWith() implementation cannot handle this.");
		}
		final Class<? extends Attribute> interf = interfaces.getFirst().get();

		if (enableBackwards && toStringMethod.isOverriddenAsOf(clazz)) {
			assert assertExternalClass(clazz) : "no SHATAM/Solr classes should fallback to toString() parsing";

			for (String part : toString().split(",")) {
				final int pos = part.indexOf('=');
				if (pos < 0) {
					throw new UnsupportedOperationException(
							"The backwards compatibility layer to support reflectWith() "
									+ "on old AtributeImpls expects the toString() implementation to return a correct format as specified for method reflectAsString(false)");
				}
				reflector.reflect(interf, part.substring(0, pos).trim(),
						part.substring(pos + 1));
			}
			return;
		}

		final Field[] fields = clazz.getDeclaredFields();
		try {
			for (int i = 0; i < fields.length; i++) {
				final Field f = fields[i];
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				f.setAccessible(true);
				reflector.reflect(interf, f.getName(), f.get(this));
			}
		} catch (IllegalAccessException e) {

			throw new RuntimeException(e);
		}
	}

	public abstract void copyTo(AttributeImpl target);

	@Override
	public Object clone() {
		Object clone = null;
		try {
			clone = super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return clone;
	}
}
