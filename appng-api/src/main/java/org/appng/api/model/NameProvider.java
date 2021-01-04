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
package org.appng.api.model;

/**
 * Provides a way to retrieve a human readable name for a certain type.
 * 
 * @author Matthias Müller
 * 
 * @param <T>
 *            the type to provide a name for
 */
public interface NameProvider<T> {

	/**
	 * Returns the name for the given instance
	 * 
	 * @param instance
	 *            the instance
	 * @return the name
	 */
	String getName(T instance);

}
