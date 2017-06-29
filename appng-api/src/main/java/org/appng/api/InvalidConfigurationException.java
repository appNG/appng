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

import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.Site;

/**
 * 
 * An {@code InvalidConfigurationException} is a checked exception thrown by the platform whenever something goes
 * wrong during {@link Application}-execution.<br/>
 * Some examples:
 * <ul>
 * <li>a {@link Application} could not be found
 * <li>a {@link Resource} could not be found
 * <li>there was an error while reading a {@link Resource}, e.g. an invalid XML-file was found
 * <li>etc.
 * </ul>
 * 
 * @author Matthias Müller
 * 
 */
public class InvalidConfigurationException extends Exception {

	private final Site site;
	private final String applicationName;

	/**
	 * Create a new {@code InvalidConfigurationException}.
	 * 
	 * @param site
	 *            the {@link Site} where the error occurred
	 * @param applicationName
	 *            the name of the {@link Application} where the error occurred
	 * @param message
	 *            the error message
	 */
	public InvalidConfigurationException(Site site, String applicationName, String message) {
		super(message);
		this.site = site;
		this.applicationName = applicationName;
	}

	/**
	 * Create a new {@code InvalidConfigurationException}.
	 * 
	 * @param applicationName
	 *            the name of the {@link Application} where the error occurred
	 * @param message
	 *            the error message
	 */
	public InvalidConfigurationException(String applicationName, String message) {
		super(message);
		this.site = null;
		this.applicationName = applicationName;
	}

	/**
	 * Create a new {@code InvalidConfigurationException}.
	 * 
	 * @param applicationName
	 *            the name of the {@link Application} where the error occurred
	 * @param message
	 *            the error message
	 * @param cause
	 *            the cause of the {@code InvalidConfigurationException}
	 */
	public InvalidConfigurationException(String applicationName, String message, Throwable cause) {
		super(message, cause);
		this.site = null;
		this.applicationName = applicationName;
	}

	/**
	 * Returns the {@link Site} where the error occurred, if present.
	 * 
	 * @return the {@link Site}
	 */
	public Site getSite() {
		return site;
	}

	/**
	 * Returns the name of the {@link Application} where the error occurred, if present.
	 * 
	 * @return the name of the {@link Application}
	 */
	public String getApplicationName() {
		return applicationName;
	}

}
