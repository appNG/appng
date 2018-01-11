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
package org.appng.api.support.environment;

import java.util.Collections;
import java.util.Set;

import org.appng.api.Scope;

/**
 * Abstract base-class for all {@link ScopedEnvironment}s.
 * 
 * @author Matthias Müller
 * 
 */
abstract class AbstractEnvironment implements ScopedEnvironment {

	private String identifier;

	protected AbstractEnvironment(Scope scope) {
		this(scope.name());
	}

	protected AbstractEnvironment(String identifier) {
		this.identifier = identifier;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name) {
		return (T) getContainer().get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T removeAttribute(String name) {
		Object attrib = getContainer().remove(name);
		return (T) attrib;
	}

	public final String getAttributeAsString(String name) {
		Object attrib = getContainer().get(name);
		if (attrib != null) {
			return attrib.toString();
		}
		return null;
	}

	public void setAttribute(String name, Object value) {
		getContainer().put(name, value);
	}

	public Set<String> keySet() {
		return Collections.unmodifiableSet(getContainer().keySet());
	}

	protected String getIdentifier() {
		return identifier;
	}

	public String toString() {
		return "[" + getIdentifier() + "] " + getContainer().toString();
	}

}
