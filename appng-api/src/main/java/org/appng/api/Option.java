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
 * A single option which was created from a {@link BeanOption} of a {@link Bean} .
 * 
 * @author Matthias Herlitzius
 * 
 * @see Options
 */
public interface Option {

	/**
	 * Returns the name of this option.
	 * 
	 * @return the name of this option.
	 */
	String getName();

	/**
	 * Checks whether this option has an attribute with the given name.
	 * 
	 * @param name
	 *            the name of the attribute to check
	 * @return {@code true} if this option contains an attribute with the given name, {@code false} otherwise
	 */
	boolean containsAttribute(String name);

	/**
	 * Returns the attribute with the given name, if present.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return the attribute, if present, {@code null} otherwise
	 */
	String getAttribute(String name);

	/**
	 * Returns the names of all attributes of this option
	 * 
	 * @return the names of all attributes
	 */
	Set<String> getAttributeNames();

}
