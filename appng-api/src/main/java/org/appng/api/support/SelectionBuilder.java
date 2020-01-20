/*
 * Copyright 2011-2020 the original author or authors.
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

import java.util.Collection;

import org.appng.api.model.NameProvider;
import org.appng.api.support.OptionOwner.HitCounter;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.OptionGroup;
import org.appng.xml.platform.SelectionType;

/**
 * A builder for {@link org.appng.xml.platform.Selection}s, providing a fluent API.<br/>
 * Example:
 * 
 * <pre>Selection persons = new SelectionBuilder<Person>("persons")
 * 	.title("persons")
 * 	.options(Arrays.asList(a, b, c))
 * 	.select(b)
 * 	.disable(c)
 * 	.type(SelectionType.SELECT_MULTIPLE)
 * .build();</pre>
 * 
 * @param <T>
 *            the type to create {@link Option}s from
 */
public class SelectionBuilder<T> extends OptionsBuilder<T, SelectionBuilder<T>.Selection> {

	private Selection selection;

	class Selection extends org.appng.xml.platform.Selection implements OptionOwner {
		public void addOption(Option option) {
			getOptions().add(option);
		}

		public Option addOption(String name, String value, boolean selected) {
			Option o = new Option();
			getOptions().add(o);
			o.setName(name);
			o.setValue(value);
			o.setSelected(selected);
			return o;
		}
	};

	public SelectionBuilder(String id) {
		super();
		this.selection = new Selection();
		selection.setId(id);
		setOwner(selection);
	}

	/**
	 * Sets the title for the selection
	 * 
	 * @param title
	 *            the title
	 * @return this builder
	 */
	public SelectionBuilder<T> title(String title) {
		Label label = new Label();
		label.setValue(title);
		selection.setTitle(label);
		return this;
	}

	/**
	 * Sets the {@link SelectionType} for the selection
	 * 
	 * @param type
	 *            the type
	 * @return this builder
	 */
	public SelectionBuilder<T> type(SelectionType type) {
		selection.setType(type);
		return this;
	}

	/**
	 * Adds a {@link OptionGroup} to the selection
	 * 
	 * @param group
	 *            the group
	 * @return this builder
	 */
	public SelectionBuilder<T> addGroup(OptionGroup group) {
		this.selection.getOptionGroups().add(group);
		return this;
	}

	@Override
	public SelectionBuilder<T> options(Iterable<T> values) {
		super.options(values);
		return this;
	}

	@Override
	public SelectionBuilder<T> name(NameProvider<T> nameProvider) {
		super.name(nameProvider);
		return this;
	}

	@Override
	public SelectionBuilder<T> selector(Selector selector) {
		super.selector(selector);
		return this;
	}

	@Override
	public SelectionBuilder<T> select(Collection<T> selected) {
		super.select(selected);
		return this;
	}

	@Override
	public SelectionBuilder<T> select(T selected) {
		super.select(selected);
		return this;
	}

	@Override
	public SelectionBuilder<T> disable(Collection<T> disabled) {
		super.disable(disabled);
		return this;
	}

	@Override
	public SelectionBuilder<T> disable(T disabled) {
		super.disable(disabled);
		return this;
	}

	@Override
	public SelectionBuilder<T> defaultOption(String name, String value) {
		super.defaultOption(name, value);
		return this;
	}

	@Override
	public SelectionBuilder<T> hitCounter(HitCounter<T> counter) {
		super.hitCounter(counter);
		return this;
	}

	@Override
	public Selection build() {
		super.build();
		return selection;
	}

}
