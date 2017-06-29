/*
 * Copyright 2011-2017 the original author or authors.
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

import org.appng.xml.platform.Label;
import org.appng.xml.platform.Option;

/**
 * Provides factory-methods for creating {@link org.appng.xml.platform.OptionGroup}s.
 * 
 * @author Matthias Müller
 */
public class OptionGroupFactory extends OptionFactory<OptionGroupFactory.OptionGroup> {

	protected OptionGroup getOwner(String id, String title) {
		return new OptionGroup(id, title);
	}

	public class OptionGroup extends org.appng.xml.platform.OptionGroup implements OptionOwner {

		private OptionOwner optionOwner;

		public OptionGroup(String id, String labelId) {
			setId(id);
			setLabel(new Label());
			getLabel().setId(labelId);
			optionOwner = new OptionOwnerBase(getOptions());
		}

		public void addOption(Option option) {
			optionOwner.addOption(option);
		}

		public Option addOption(String name, String value, boolean selected) {
			return optionOwner.addOption(name, value, selected);
		}

	}
}
