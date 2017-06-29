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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.appng.api.Option;

/**
 * 
 * @author Matthias Herlitzius
 */
public class OptionImpl implements Option {

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

	public OptionImpl addAttribute(String key, String value) {
		attributeMap.put(key, value);
		return this;
	}

	public boolean containsAttribute(String key) {
		return attributeMap.containsKey(key);
	}

	public String getAttribute(String key) {
		return attributeMap.get(key);
	}

	public int getAttributeAsInteger(String key) {
		return Integer.valueOf(attributeMap.get(key));
	}

	public Set<String> getAttributeNames() {
		return attributeMap.keySet();
	}

	public Map<String, String> getAttributeMap() {
		return attributeMap;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(getName() + " [ ");
		getAttributeNames().forEach(key -> result.append(key + "=\"" + getAttribute(key) + "\" "));
		result.append("]");
		return result.toString();
	}
}
