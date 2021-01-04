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
package org.appng.api;

import java.util.Set;

import org.appng.xml.platform.Bean;
import org.appng.xml.platform.BeanOption;

/**
 * A single option which was created from a {@link BeanOption} of a {@link Bean} .
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
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
	 * @deprecated use {@link #getString(String)} instead
	 */
	@Deprecated
	String getAttribute(String name);

	/**
	 * Returns the names of all attributes of this option
	 * 
	 * @return the names of all attributes
	 */
	Set<String> getAttributeNames();

	/**
	 * Returns the attribute with the given name, if present.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return the attribute, if present, {@code null} otherwise
	 */
	String getString(String name);

	/**
	 * Returns an {@link Integer} parsed from the attribute with the given name.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return the integer value, or {@code null} if no such attribute exists (or the value can not be parsed to an
	 *         integer)
	 */
	Integer getInteger(String name);

	/**
	 * Returns a {@link Boolean} parsed from the attribute with the given name.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return {@link Boolean#TRUE} if the value of the attribute equals to (ignoring case) {@code true},
	 *         {@link Boolean#FALSE} otherwise
	 */
	Boolean getBoolean(String name);

	/**
	 * Returns the {@link Enum} constant represented by the attribute with the given name.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @param type
	 *            the type of the {@link Enum}
	 * @return the enum constant, if the (upper-case) attribute value represents a valid enum of the given type
	 *         ,{@code null} otherwise
	 */
	<E extends Enum<E>> E getEnum(String name, Class<E> type);

}
