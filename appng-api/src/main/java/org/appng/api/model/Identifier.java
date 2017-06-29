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
package org.appng.api.model;

import java.util.Date;

import org.appng.xml.application.PackageInfo;

/**
 * Provides general information about a {@link PackageInfo}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public interface Identifier extends Named<Integer>, Versionable<Date> {

	/**
	 * Returns the display name.
	 * 
	 * @return the display name
	 */
	String getDisplayName();

	/**
	 * Returns the version.
	 * 
	 * @return the version
	 */
	String getPackageVersion();

	/**
	 * Returns the timestamp the {@link Application} was build.
	 * 
	 * @return the timestamp the {@link Application} was build
	 */
	String getTimestamp();

	/**
	 * Returns the description.
	 * 
	 * @return the description
	 */
	String getLongDescription();

	/**
	 * Returns the version of appNG the {@link Application} is compatible with.
	 * 
	 * @return the version of appNG the {@link Application} is compatible with
	 */
	String getAppNGVersion();

	/**
	 * Checks whether the {@link Application} is installed.
	 * 
	 * @return {@code true} if the {@link Application} is installed, {@code false} otherwise
	 */
	boolean isInstalled();

	/**
	 * Checks whether the {@link Application} has a SNAPSHOT-version
	 * 
	 * @return {@code true} if the {@link Application} has a SNAPSHOT-version, {@code false} otherwise
	 */
	boolean isSnapshot();

}
