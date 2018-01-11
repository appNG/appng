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

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.appng.api.Environment;
import org.appng.api.Scope;

/**
 * 
 * Interface for an environment of a certain {@link Scope}.
 * 
 * @author Matthias Müller
 * @see Environment
 */
interface ScopedEnvironment {

	/**
	 * @see Environment#setAttribute(Scope, String, Object)
	 */
	void setAttribute(String name, Object value);

	/**
	 * @see Environment#getAttribute(Scope, String)
	 */
	<T> T getAttribute(String name);

	/**
	 * @see Environment#getAttributeAsString(Scope, String)
	 */
	String getAttributeAsString(String name);

	/**
	 * @see Environment#setAttribute(Scope, String, Object)
	 */
	<T> T removeAttribute(String name);

	/**
	 * Returns the underlying {@link ConcurrentMap} which is used to store the attributes.
	 * 
	 * @return the underlying {@link ConcurrentMap}
	 */
	ConcurrentMap<String, Object> getContainer();

	/**
	 * @see Environment#keySet(Scope)
	 */
	Set<String> keySet();

}
