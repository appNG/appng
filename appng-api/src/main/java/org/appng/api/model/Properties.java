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

import java.util.List;
import java.util.Set;

/**
 * Everywhere in appNG where something needs to be configurable, {@link Properties} come into operation. Based upon
 * simple key-value-pairs, they offer the ability to provide default-values and type-conversion.
 * 
 * @author Matthias Müller
 * 
 * @see Property
 * @see Site#getProperties()
 * @see Application#getProperties()
 */
public interface Properties {

	/**
	 * Returns a {@link List} of {@link String}s which is parsed from the value of the {@link Property} with the given
	 * name (if present) or from the default-value.
	 * 
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-string to parse the list from
	 * @param delimiter
	 *                     the delimiter to split the (default-)value by
	 * 
	 * @return a (possibly empty) {@link List}, never {@code null}
	 */
	List<String> getList(String name, String defaultValue, String delimiter);

	/**
	 * Returns a {@link List} of {@link String}s which is parsed from the value of the {@link Property} with the given
	 * name (if present).
	 * 
	 * @param name
	 *                  the name of the {@link Property}
	 * @param delimiter
	 *                  the delimiter to split the value by
	 * 
	 * @return {@link List} of {@link String}s which is parsed from the value of the {@link Property} with the given
	 *         name (if present)
	 */
	List<String> getList(String name, String delimiter);

	/**
	 * Returns the string-value of the given {@link Property}.
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return the string-value of the given {@link Property}, or {@code null} if no such property exists.
	 */
	String getString(String name);

	/**
	 * Returns the string-value of the given {@link Property} (if existing), or the default-value.
	 * 
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-value
	 * 
	 * @return the string-value of the given {@link Property} (if existing), or the default-value
	 */
	String getString(String name, String defaultValue);

	/**
	 * Returns a {@link Boolean} parsed from the given {@link Property} (if existing).
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return a {@link Boolean}, or {@code null} if no such {@link Property} exists.
	 */
	Boolean getBoolean(String name);

	/**
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-value
	 * 
	 * @return a {@link Boolean}, or the default-value if no such {@link Property} exists.
	 */
	Boolean getBoolean(String name, Boolean defaultValue);

	/**
	 * Returns an {@link Integer} parsed from the given {@link Property} (if existing).
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return an {@link Integer}, or {@code null} if no such {@link Property} exists.
	 */
	Integer getInteger(String name);

	/**
	 * Returns an {@link Integer} parsed from the given {@link Property} (if existing), or the default-value.
	 * 
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-value
	 * 
	 * @return an {@link Integer}, or the default-value if no such {@link Property} exists.
	 */
	Integer getInteger(String name, Integer defaultValue);

	/**
	 * Returns an {@link Float} parsed from the given {@link Property} (if existing).
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return an {@link Float}, or {@code null} if no such {@link Property} exists.
	 */
	Float getFloat(String name);

	/**
	 * Returns a {@link Float} parsed from the given {@link Property} (if existing), or the default-value.
	 * 
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-value
	 * 
	 * @return an {@link Float}, or the default-value if no such {@link Property} exists.
	 */
	Float getFloat(String name, Float defaultValue);

	/**
	 * Returns an {@link Double} parsed from the given {@link Property} (if existing).
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return an {@link Double}, or {@code null} if no such {@link Property} exists.
	 */
	Double getDouble(String name);

	/**
	 * Returns a {@link Double} parsed from the given {@link Property} (if existing), or the default-value.
	 * 
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-value
	 * 
	 * @return a {@link Double}, or the default-value if no such {@link Property} exists.
	 */
	Double getDouble(String name, Double defaultValue);

	/**
	 * Returns the string-value of the given {@link Property}.
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return the string-value of the given {@link Property}, or {@code null} if no such {@link Property} exists.
	 */
	String getClob(String name);

	/**
	 * Returns the string-value of the given {@link Property} (if existing), or the default-value.
	 * 
	 * @param name
	 *                     the name of the {@link Property}
	 * @param defaultValue
	 *                     the default-value
	 * 
	 * @return the string-value of the given {@link Property} (if existing), or the default-value
	 */
	String getClob(String name, String defaultValue);

	/**
	 * Returns the byte-value of the given {@link Property}.
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return the byte-value of the given {@link Property}, or {@code null} if no such {@link Property} exists.
	 */
	byte[] getBlob(String name);

	/**
	 * Returns all {@link Property} names.
	 * 
	 * @return all {@link Property} names
	 */
	Set<String> getPropertyNames();

	/**
	 * Checks whether the {@link Property} with the given name exists.
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return {@code true} if the {@link Property} exists, {@code false} otherwise
	 */
	boolean propertyExists(String name);

	/**
	 * Takes this {@link Properties} and transforms it into some (plain, old, uncool) {@link java.util.Properties}.
	 * 
	 * @return some {@link java.util.Properties}
	 */
	java.util.Properties getPlainProperties();

	/**
	 * Returns some {@link java.util.Properties} parsed from the given {@link Property} (if existing).
	 * 
	 * @param name
	 *             the name of the {@link Property}
	 * 
	 * @return some {@link java.util.Properties}, or {@code null} if no such {@link Property} exists.
	 */
	java.util.Properties getProperties(String name);

	/**
	 * Returns the description for the {@link Property} with the given name, if any
	 * 
	 * @param name
	 *             the name of the property
	 * 
	 * @return the description, if any
	 */
	String getDescriptionFor(String name);

	/**
	 * Returns the object representation of the {@link Property} according to it's {@link Property.Type}:
	 * <ul>
	 * <li>an {@link Integer} (for type {@link Property.Type#INT})
	 * <li>a {@link Double} (for type {@link Property.Type#DECIMAL})
	 * <li>a {@link Boolean} (for type {@link Property.Type#BOOLEAN})
	 * <li>a {@link String} (for types {@link Property.Type#TEXT}, {@link Property.Type#PASSWORD} and {@link Property.Type#MULTILINE})
	 * <ul>
	 * 
	 * @param  the
	 *             name of the property
	 * 
	 * @return     the object representation
	 */
	default Object getObject(String name) {
		return null;
	}

}
