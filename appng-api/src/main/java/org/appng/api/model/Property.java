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
package org.appng.api.model;

/**
 * A {@code Property} is a single configuration value, used to configure the appNG platform, a {@link Site} or a
 * {@link Application}.
 * 
 * @author Matthias Müller
 * 
 * @see Properties
 */
public interface Property {

	/**
	 * Returns the {@code String}-value of this {@code Property}
	 * 
	 * @return the {@code String}-value
	 * @see Properties#getString(String)
	 * @see Properties#getString(String, String)
	 */
	String getString();

	/**
	 * Returns the {@code Boolean}-value of this {@code Property}:<li>
	 * <ul>
	 * <li>{@link Boolean#TRUE} if {@link #getString()} returns {@code true} or {@code 1}
	 * <li>{@link Boolean#FALSE} if the {@link #getString()} returns a non-null {@link String}
	 * <li>{@code null} otherwise
	 * </ul>
	 * 
	 * @return the {@code Boolean}-value (may be {@code null})
	 * 
	 * @see Properties#getBoolean(String)
	 * @see Properties#getBoolean(String, Boolean)
	 */
	Boolean getBoolean();

	/**
	 * Returns the {@code Integer}-value of this {@code Property} using {@link Integer#parseInt(String)}, if
	 * {@link #getString()} returns a non-null {@link String}, {@code null} otherwise.
	 * 
	 * @return the {@code Integer}-value (may be {@code null})
	 * 
	 * @see Properties#getInteger(String)
	 * @see Properties#getInteger(String, Integer)
	 */
	Integer getInteger();

	/**
	 * Returns the {@code Float}-value of this {@code Property} using {@link Float#parseFloat(String)}, if
	 * {@link #getString()} returns a non-null {@link String}, {@code null} otherwise.
	 * 
	 * @return the {@code Float}-value (may be {@code null})
	 * 
	 * @see Properties#getFloat(String)
	 * @see Properties#getFloat(String, Float)
	 */
	Float getFloat();

	/**
	 * Returns the {@code Double}-value of this {@code Property} using {@link Double#parseDouble(String)}, if
	 * {@link #getString()} returns a non-null {@link String}, {@code null} otherwise.
	 * 
	 * @return the {@code Double}-value (may be {@code null})
	 * @see Properties#getDouble(String)
	 * @see Properties#getDouble(String, Double)
	 */
	Double getDouble();

	/**
	 * If this {@code Property} contains BLOB data, this data is being returned.
	 * 
	 * @return the BLOB data, (may be {@code null})
	 * 
	 * @see Properties#getBlob(String)
	 */
	byte[] getBlob();

	/**
	 * If this {@code Property} contains CLOB data, this data is being returned.
	 * 
	 * @return the CLOB data, (may be {@code null})
	 * 
	 * @see Properties#getClob(String)
	 * @see Properties#getClob(String, String)
	 */
	String getClob();

	/**
	 * Returns the name of this {@code Property}, as used in {@code Properties.getXXX(String)}.
	 * 
	 * @return the name
	 */
	String getName();

	@Deprecated
	boolean isMandatory();

	/**
	 * Returns the default {@link String}-value of this {@code Property}.
	 * 
	 * @return the default {@link String}-value
	 */
	String getDefaultString();

	/**
	 * Returns the description of this {@code Property}.
	 * 
	 * @return the description
	 */
	String getDescription();

}
