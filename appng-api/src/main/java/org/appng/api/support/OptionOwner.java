/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.List;

import org.appng.xml.platform.Option;

public interface OptionOwner {

	List<org.appng.xml.platform.Option> getOptions();

	void addOption(org.appng.xml.platform.Option option);

	org.appng.xml.platform.Option addOption(String name, String value, boolean selected);

	/**
	 * A selector decides whether or not a given {@link Option} should be selected. Also implements {@link HitCounter},
	 * using the options's value and returning {@code null} by default.
	 * 
	 * @see Option#isSelected()
	 */
	public interface Selector extends HitCounter<String> {
		/**
		 * Selects or de-selects an option by calling {@link Option#setSelected(Boolean)}.
		 * 
		 * @param o
		 *            the {@link Option} which might be selected
		 */
		void select(Option o);

		default Integer count(String optionValue) {
			return null;
		}
	}

	/**
	 * Counts the hits for {@link Option}s.
	 * 
	 * @param <T>
	 *            the type of the elements to count
	 * @see Option#getHits()
	 */
	public interface HitCounter<T> {
		/**
		 * Counts the hits for the option created from the given element.
		 * 
		 * @param element
		 *            the element to count the hits for
		 * @return the number of hits for this option
		 */
		Integer count(T element);
	}

	class OptionOwnerBase implements OptionOwner {

		private List<Option> options;

		public OptionOwnerBase(List<Option> options) {
			this.options = options;
		}

		public List<Option> getOptions() {
			return options;
		}

		public void addOption(Option option) {
			options.add(option);
		}

		public org.appng.xml.platform.Option addOption(String name, String value, boolean selected) {
			org.appng.xml.platform.Option option = new org.appng.xml.platform.Option();
			option.setName(name);
			option.setValue(value);
			option.setSelected(selected);
			addOption(option);
			return option;
		}

	}
}
