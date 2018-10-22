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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Option;

/**
 * The default implementation for {@link Option}
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
public class OptionImpl implements Option {

	private static final Pattern INT_PATTERN = Pattern.compile("[+-]?[\\d]+");
	private final String name;
	private Map<String, String> attributeMap = new HashMap<String, String>();

	public OptionImpl(String name) {
		this.name = name;
	}

	public OptionImpl(String name, Map<String, String> map) {
		this.name = name;
		addMap(map);
	}

	public String getName() {
		return name;
	}

	public OptionImpl addMap(Map<String, String> map) {
		attributeMap.putAll(map);
		return this;
	}

	public OptionImpl addAttribute(String name, String value) {
		attributeMap.put(name, value);
		return this;
	}

	public boolean containsAttribute(String name) {
		return attributeMap.containsKey(name);
	}

	public String getAttribute(String name) {
		return getString(name);
	}

	public int getAttributeAsInteger(String name) {
		return Integer.valueOf(attributeMap.get(name));
	}

	public Set<String> getAttributeNames() {
		return attributeMap.keySet();
	}

	public Map<String, String> getAttributeMap() {
		return attributeMap;
	}

	public String getString(String name) {
		return attributeMap.get(name);
	}

	public Integer getInteger(String name) {
		String value = getString(name);
		return null == value ? null : INT_PATTERN.matcher(value).matches() ? Integer.valueOf(value) : null;
	}

	public Boolean getBoolean(String name) {
		return Boolean.valueOf(getString(name));
	}

	public <E extends Enum<E>> E getEnum(String name, Class<E> type) {
		String value = StringUtils.upperCase(getString(name));
		return EnumUtils.isValidEnum(type, value) ? Enum.valueOf(type, value) : null;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(getName() + " [ ");
		getAttributeNames().forEach(key -> result.append(key + "=\"" + getString(key) + "\" "));
		result.append("]");
		return result.toString();
	}
}
