/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.api.support;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.appng.api.model.Identifiable;
import org.appng.api.model.NameProvider;
import org.appng.api.model.Nameable;
import org.appng.api.support.OptionOwner;
import org.appng.api.support.OptionOwner.HitCounter;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.Option;

/**
 * A builder for {@link Option}s, providing a fluent API.
 * 
 * @author Matthias Müller
 *
 * @param <T>
 *            the type to create {@link Option}s from
 * @param <R>
 *            the owner of the options
 */
public class OptionsBuilder<T, R extends OptionOwner> {

	private R owner;
	private Iterable<T> elements;
	private NameProvider<T> nameProvider;
	private Selector selector;
	private Collection<T> selected;
	private Collection<T> disabled;
	private Option defaultOption;
	private HitCounter<T> counter;

	/**
	 * Create a new builder, using the given owner.
	 * 
	 * @param owner
	 *            the owner of the options
	 */
	public OptionsBuilder(R owner) {
		this.owner = owner;
	}

	protected OptionsBuilder() {
	}

	/**
	 * Sets the owner for this builder
	 * 
	 * @param owner
	 *            the owner
	 */
	protected void setOwner(R owner) {
		this.owner = owner;
	}

	/**
	 * Sets the elements to build options from
	 * 
	 * @param elements
	 *            the elements
	 * @return this builder
	 */
	public OptionsBuilder<T, R> options(Iterable<T> elements) {
		this.elements = elements;
		return this;
	}

	/**
	 * Sets the {@link NameProvider} to use when setting an option's name
	 * 
	 * @param nameProvider
	 *            the provider
	 * @return this builder
	 */
	public OptionsBuilder<T, R> name(NameProvider<T> nameProvider) {
		this.nameProvider = nameProvider;
		return this;
	}

	/**
	 * Sets the {@link Selector} to use when selecting options.
	 * 
	 * @param selector
	 *            the selector
	 * @return this builder
	 * 
	 * @see #select(Object)
	 * @see #select(Collection)
	 */
	public OptionsBuilder<T, R> selector(Selector selector) {
		this.selector = selector;
		return this;
	}

	/**
	 * Selects some elements, i.e. the created options will be selected.
	 * 
	 * @param selected
	 *            the selected elements
	 * @return this builder
	 * 
	 * @see #selector
	 * @see #select(Object)
	 */
	public OptionsBuilder<T, R> select(Collection<T> selected) {
		this.selected = selected;
		return this;
	}

	/**
	 * Selects a single elements, i.e. the created option will be selected.
	 * 
	 * @param selected
	 *            the selected element
	 * @return this builder
	 * 
	 * @see #selector
	 * @see #select(Collection)
	 */
	public OptionsBuilder<T, R> select(T selected) {
		this.selected = Arrays.asList(selected);
		return this;
	}

	/**
	 * Disables a single element, i.e. the created option will be disabled
	 * 
	 * @param disabled
	 *            the disabled element
	 * @return this builder
	 * 
	 * @see #disable(Object)
	 */
	public OptionsBuilder<T, R> disable(Collection<T> disabled) {
		this.disabled = disabled;
		return this;
	}

	/**
	 * Disables some elements, i.e. the created options will be disabled
	 * 
	 * @param disabled
	 *            the disabled elements
	 * @return this builder
	 * 
	 * @see #disable(Collection)
	 */
	public OptionsBuilder<T, R> disable(T disabled) {
		this.disabled = Arrays.asList(disabled);
		return this;
	}

	/**
	 * Adds an option at the very first position
	 * 
	 * @param name
	 *            the name for the option to be added
	 * @param value
	 *            the value for the option to be added
	 * @return this builder
	 */
	public OptionsBuilder<T, R> defaultOption(String name, String value) {
		this.defaultOption = new Option();
		defaultOption.setName(name);
		defaultOption.setValue(value);
		return this;
	}

	/**
	 * Sets a {@link HitCounter}
	 * 
	 * @param counter
	 *            the counter
	 * @return this builder
	 */
	public OptionsBuilder<T, R> hitCounter(HitCounter<T> counter) {
		this.counter = counter;
		return this;
	}

	/**
	 * Creates the options and adds the to the owner
	 * 
	 * @return the owner the options
	 */
	public R build() {
		if (null != defaultOption) {
			owner.getOptions().add(defaultOption);
		}
		if (null != elements) {
			for (T t : elements) {
				String name = t.toString();
				if (null != nameProvider) {
					name = nameProvider.getName(t);
				} else if (Nameable.class.isAssignableFrom(t.getClass())) {
					name = Nameable.class.cast(t).getName();
				}

				String value = name;
				if (Identifiable.class.isAssignableFrom(t.getClass())) {
					Serializable id = Identifiable.class.cast(t).getId();
					if (null != id) {
						value = id.toString();
					}
				} else if (t.getClass().isEnum()) {
					value = Enum.class.cast(t).name();
				}

				boolean isSelected = null != selected && selected.contains(t);
				Option option = new Option();
				option.setName(name);
				option.setValue(value);
				option.setSelected(isSelected);
				if (null != selector) {
					selector.select(option);
					if (null == counter) {
						option.setHits(selector.count(option.getValue()));
					}
				}
				if (null != disabled && disabled.contains(t)) {
					option.setDisabled(true);
				}
				if (null != counter) {
					option.setHits(counter.count(t));
				}

				owner.getOptions().add(option);
			}
		}
		return owner;
	}

}
