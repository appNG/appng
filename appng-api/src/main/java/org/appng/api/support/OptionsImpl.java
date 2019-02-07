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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.appng.api.Option;
import org.appng.api.Options;

/**
 * The default implementation for {@link Options}
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
public class OptionsImpl implements Options {

	protected Map<String, Option> optionsMap = new HashMap<>();

	public void addOption(Option option) {
		optionsMap.put(option.getName(), option);
	}

	public Option getOption(String name) {
		if (optionsMap.containsKey(name)) {
			return optionsMap.get(name);
		} else {
			return new OptionImpl(name);
		}
	}

	public boolean hasOption(String name) {
		return optionsMap.containsKey(name);
	}

	public Set<String> getOptionNames() {
		return optionsMap.keySet();
	}

	public String getOptionValue(String optionName, String attributeName) {
		Option option = getOption(optionName);
		return (null == option) ? null : option.getString(attributeName);
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(optionsMap.values());
		return result.toString();
	}

	public String getString(String name, String attribute) {
		return hasOption(name) ? getOption(name).getString(attribute) : null;
	}

	public Integer getInteger(String name, String attribute) {
		return hasOption(name) ? getOption(name).getInteger(attribute) : null;
	}
	
	public Boolean getBoolean(String name, String attribute) {
		return hasOption(name) ? getOption(name).getBoolean(attribute) : null;
	}
	
	public <E extends Enum<E>> E getEnum(String name, String attribute, Class<E> type) {
		return hasOption(name) ? getOption(name).getEnum(attribute, type) : null;
	}

}
