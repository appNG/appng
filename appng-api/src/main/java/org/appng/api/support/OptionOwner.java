/*
 * Copyright 2011-2018 the original author or authors.
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
	 * A selector decides whether or not a given {@link Option} should be selected
	 */
	public interface Selector {
		/**
		 * Selects or de-selects an option by calling {@link Option#setSelected(Boolean)}.
		 * 
		 * @param o
		 *            the {@link Option} which might be selected
		 */
		void select(Option o);
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
