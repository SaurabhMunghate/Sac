/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.util;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.shatam.shatamindex.analysis.TokenStream;
import com.shatam.shatamindex.analysis.tokenattributes.CharTermAttributeImpl;
import com.shatam.shatamindex.analysis.tokenattributes.TermAttribute;

public class AttributeSource {

	public static abstract class AttributeFactory {

		public abstract AttributeImpl createAttributeInstance(
				Class<? extends Attribute> attClass);

		public static final AttributeFactory DEFAULT_ATTRIBUTE_FACTORY = new DefaultAttributeFactory();

		private static final class DefaultAttributeFactory extends
				AttributeFactory {
			private static final WeakHashMap<Class<? extends Attribute>, WeakReference<Class<? extends AttributeImpl>>> attClassImplMap = new WeakHashMap<Class<? extends Attribute>, WeakReference<Class<? extends AttributeImpl>>>();

			private DefaultAttributeFactory() {
			}

			@Override
			public AttributeImpl createAttributeInstance(
					Class<? extends Attribute> attClass) {
				try {
					return getClassForInterface(attClass).newInstance();
				} catch (InstantiationException e) {
					throw new IllegalArgumentException(
							"Could not instantiate implementing class for "
									+ attClass.getName());
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException(
							"Could not instantiate implementing class for "
									+ attClass.getName());
				}
			}

			private static Class<? extends AttributeImpl> getClassForInterface(
					Class<? extends Attribute> attClass) {
				synchronized (attClassImplMap) {
					final WeakReference<Class<? extends AttributeImpl>> ref = attClassImplMap
							.get(attClass);
					Class<? extends AttributeImpl> clazz = (ref == null) ? null
							: ref.get();
					if (clazz == null) {
						try {

							if (TermAttribute.class.equals(attClass)) {
								clazz = CharTermAttributeImpl.class;
							} else {
								clazz = Class.forName(
										attClass.getName() + "Impl", true,
										attClass.getClassLoader()).asSubclass(
										AttributeImpl.class);
							}
							attClassImplMap
									.put(attClass,
											new WeakReference<Class<? extends AttributeImpl>>(
													clazz));
						} catch (ClassNotFoundException e) {
							throw new IllegalArgumentException(
									"Could not find implementing class for "
											+ attClass.getName());
						}
					}
					return clazz;
				}
			}
		}
	}

	public static final class State implements Cloneable {
		AttributeImpl attribute;
		State next;

		@Override
		public Object clone() {
			State clone = new State();
			clone.attribute = (AttributeImpl) attribute.clone();

			if (next != null) {
				clone.next = (State) next.clone();
			}

			return clone;
		}
	}

	private final Map<Class<? extends Attribute>, AttributeImpl> attributes;
	private final Map<Class<? extends AttributeImpl>, AttributeImpl> attributeImpls;
	private final State[] currentState;

	private AttributeFactory factory;

	public AttributeSource() {
		this(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
	}

	public AttributeSource(AttributeSource input) {
		if (input == null) {
			throw new IllegalArgumentException(
					"input AttributeSource must not be null");
		}
		this.attributes = input.attributes;
		this.attributeImpls = input.attributeImpls;
		this.currentState = input.currentState;
		this.factory = input.factory;
	}

	public AttributeSource(AttributeFactory factory) {
		this.attributes = new LinkedHashMap<Class<? extends Attribute>, AttributeImpl>();
		this.attributeImpls = new LinkedHashMap<Class<? extends AttributeImpl>, AttributeImpl>();
		this.currentState = new State[1];
		this.factory = factory;
	}

	public AttributeFactory getAttributeFactory() {
		return this.factory;
	}

	public Iterator<Class<? extends Attribute>> getAttributeClassesIterator() {
		return Collections.unmodifiableSet(attributes.keySet()).iterator();
	}

	public Iterator<AttributeImpl> getAttributeImplsIterator() {
		final State initState = getCurrentState();
		if (initState != null) {
			return new Iterator<AttributeImpl>() {
				private State state = initState;

				public void remove() {
					throw new UnsupportedOperationException();
				}

				public AttributeImpl next() {
					if (state == null)
						throw new NoSuchElementException();
					final AttributeImpl att = state.attribute;
					state = state.next;
					return att;
				}

				public boolean hasNext() {
					return state != null;
				}
			};
		} else {
			return Collections.<AttributeImpl> emptySet().iterator();
		}
	}

	private static final WeakHashMap<Class<? extends AttributeImpl>, LinkedList<WeakReference<Class<? extends Attribute>>>> knownImplClasses = new WeakHashMap<Class<? extends AttributeImpl>, LinkedList<WeakReference<Class<? extends Attribute>>>>();

	static LinkedList<WeakReference<Class<? extends Attribute>>> getAttributeInterfaces(
			final Class<? extends AttributeImpl> clazz) {
		synchronized (knownImplClasses) {
			LinkedList<WeakReference<Class<? extends Attribute>>> foundInterfaces = knownImplClasses
					.get(clazz);
			if (foundInterfaces == null) {

				knownImplClasses
						.put(clazz,
								foundInterfaces = new LinkedList<WeakReference<Class<? extends Attribute>>>());

				Class<?> actClazz = clazz;
				do {
					for (Class<?> curInterface : actClazz.getInterfaces()) {
						if (curInterface != Attribute.class
								&& Attribute.class
										.isAssignableFrom(curInterface)) {
							foundInterfaces
									.add(new WeakReference<Class<? extends Attribute>>(
											curInterface
													.asSubclass(Attribute.class)));
						}
					}
					actClazz = actClazz.getSuperclass();
				} while (actClazz != null);
			}
			return foundInterfaces;
		}
	}

	public void addAttributeImpl(final AttributeImpl att) {
		final Class<? extends AttributeImpl> clazz = att.getClass();
		if (attributeImpls.containsKey(clazz))
			return;
		final LinkedList<WeakReference<Class<? extends Attribute>>> foundInterfaces = getAttributeInterfaces(clazz);

		for (WeakReference<Class<? extends Attribute>> curInterfaceRef : foundInterfaces) {
			final Class<? extends Attribute> curInterface = curInterfaceRef
					.get();
			assert (curInterface != null) : "We have a strong reference on the class holding the interfaces, so they should never get evicted";

			if (!attributes.containsKey(curInterface)) {

				this.currentState[0] = null;
				attributes.put(curInterface, att);
				attributeImpls.put(clazz, att);
			}
		}
	}

	public <A extends Attribute> A addAttribute(Class<A> attClass) {
		AttributeImpl attImpl = attributes.get(attClass);
		if (attImpl == null) {
			if (!(attClass.isInterface() && Attribute.class
					.isAssignableFrom(attClass))) {
				throw new IllegalArgumentException(
						"addAttribute() only accepts an interface that extends Attribute, but "
								+ attClass.getName()
								+ " does not fulfil this contract.");
			}
			addAttributeImpl(attImpl = this.factory
					.createAttributeInstance(attClass));
		}
		return attClass.cast(attImpl);
	}

	public boolean hasAttributes() {
		return !this.attributes.isEmpty();
	}

	public boolean hasAttribute(Class<? extends Attribute> attClass) {
		return this.attributes.containsKey(attClass);
	}

	public <A extends Attribute> A getAttribute(Class<A> attClass) {
		AttributeImpl attImpl = attributes.get(attClass);
		if (attImpl == null) {
			throw new IllegalArgumentException(
					"This AttributeSource does not have the attribute '"
							+ attClass.getName() + "'.");
		}
		return attClass.cast(attImpl);
	}

	private State getCurrentState() {
		State s = currentState[0];
		if (s != null || !hasAttributes()) {
			return s;
		}
		State c = s = currentState[0] = new State();
		final Iterator<AttributeImpl> it = attributeImpls.values().iterator();
		c.attribute = it.next();
		while (it.hasNext()) {
			c.next = new State();
			c = c.next;
			c.attribute = it.next();
		}
		return s;
	}

	public void clearAttributes() {
		for (State state = getCurrentState(); state != null; state = state.next) {
			state.attribute.clear();
		}
	}

	public State captureState() {
		final State state = this.getCurrentState();
		return (state == null) ? null : (State) state.clone();
	}

	public void restoreState(State state) {
		if (state == null)
			return;

		do {
			AttributeImpl targetImpl = attributeImpls.get(state.attribute
					.getClass());
			if (targetImpl == null) {
				throw new IllegalArgumentException(
						"State contains AttributeImpl of type "
								+ state.attribute.getClass().getName()
								+ " that is not in in this AttributeSource");
			}
			state.attribute.copyTo(targetImpl);
			state = state.next;
		} while (state != null);
	}

	@Override
	public int hashCode() {
		int code = 0;
		for (State state = getCurrentState(); state != null; state = state.next) {
			code = code * 31 + state.attribute.hashCode();
		}
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof AttributeSource) {
			AttributeSource other = (AttributeSource) obj;

			if (hasAttributes()) {
				if (!other.hasAttributes()) {
					return false;
				}

				if (this.attributeImpls.size() != other.attributeImpls.size()) {
					return false;
				}

				State thisState = this.getCurrentState();
				State otherState = other.getCurrentState();
				while (thisState != null && otherState != null) {
					if (otherState.attribute.getClass() != thisState.attribute
							.getClass()
							|| !otherState.attribute
									.equals(thisState.attribute)) {
						return false;
					}
					thisState = thisState.next;
					otherState = otherState.next;
				}
				return true;
			} else {
				return !other.hasAttributes();
			}
		} else
			return false;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append('(');
		if (hasAttributes()) {
			for (State state = getCurrentState(); state != null; state = state.next) {
				if (sb.length() > 1)
					sb.append(',');
				sb.append(state.attribute.toString());
			}
		}
		return sb.append(')').toString();
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

	public final void reflectWith(AttributeReflector reflector) {
		for (State state = getCurrentState(); state != null; state = state.next) {
			state.attribute.reflectWith(reflector);
		}
	}

	public AttributeSource cloneAttributes() {
		final AttributeSource clone = new AttributeSource(this.factory);

		if (hasAttributes()) {

			for (State state = getCurrentState(); state != null; state = state.next) {
				clone.attributeImpls.put(state.attribute.getClass(),
						(AttributeImpl) state.attribute.clone());
			}

			for (Entry<Class<? extends Attribute>, AttributeImpl> entry : this.attributes
					.entrySet()) {
				clone.attributes.put(entry.getKey(),
						clone.attributeImpls.get(entry.getValue().getClass()));
			}
		}

		return clone;
	}

	public final void copyTo(AttributeSource target) {
		for (State state = getCurrentState(); state != null; state = state.next) {
			final AttributeImpl targetImpl = target.attributeImpls
					.get(state.attribute.getClass());
			if (targetImpl == null) {
				throw new IllegalArgumentException(
						"This AttributeSource contains AttributeImpl of type "
								+ state.attribute.getClass().getName()
								+ " that is not in the target");
			}
			state.attribute.copyTo(targetImpl);
		}
	}

}
