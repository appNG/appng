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
package org.appng.api;

import java.util.Set;

import org.appng.xml.platform.Bean;
import org.appng.xml.platform.BeanOption;

/**
 * {@link Options} are created from the {@link BeanOption}s of a {@link Bean} and contain several {@link Option}s. They
 * are then passed to the bean, which is either an {@link ActionProvider}, a {@link DataProvider} or a
 * {@link FormValidator}.
 * 
 * @author Matthias Herlitzius
 * 
 * @see ActionProvider
 * @see DataProvider
 * @see FormValidator
 */
public interface Options {

	/**
	 * Returns the names of all available {@link Option}s
	 * 
	 * @return the names of all available {@link Option}s
	 */
	Set<String> getOptionNames();

	/**
	 * Checks whether there is an {@link Option} with the given name.
	 * 
	 * @param name
	 *            the name of the {@link Option} to check
	 * @return {@code true} there is an {@link Option} with the given name, {@code false} otherwise
	 */
	boolean hasOption(String name);

	/**
	 * Returns the {@link Option} with the given name, if present.
	 * 
	 * @param name
	 *            the name of the {@link Option} to get
	 * @return the {@link Option} with the given name, if present, {@code null} otherwise
	 */
	Option getOption(String name);

	/**
	 * Returns the attribute with the given name for the {@link Option} with the given name.
	 * 
	 * @param name
	 *            the name of the {@link Option} to get the attribute from
	 * @param attribute
	 *            the name of the attribute of the {@link Option}
	 * @return the attribute with the given name, or {@code null} if either the {@link Option} or the attribute does not
	 *         exist,
	 */
	String getOptionValue(String name, String attribute);

}
