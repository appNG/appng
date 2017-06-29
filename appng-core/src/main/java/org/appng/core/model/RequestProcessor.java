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
package org.appng.core.model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.InvalidConfigurationException;
import org.appng.api.PathInfo;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.application.Template;
import org.appng.xml.platform.Platform;

/**
 * Processes a request to the appNG GUI.<br/>
 * This includes:
 * <ul>
 * <li>checking whether or not the user is logged in (and redirect to the login if this is not the case)
 * <li>calling the right {@link Application} of the requested {@link Site}
 * <li>building the {@link Platform}-object
 * <li>applying the {@link Template} of the {@link Site} to that object
 * </ul>
 * 
 * @author Matthias Müller
 */
public interface RequestProcessor {

	/**
	 * Processes the request, but without applying the template. Note that
	 * {@link #init(HttpServletRequest, HttpServletResponse, PathInfo, String)} must have been called before.
	 * 
	 * @param site
	 *            the {@link Site} for which the process the request
	 * @return the {@link Platform} representing the result of calling the {@link Site}'s {@link Application}
	 * @throws InvalidConfigurationException
	 *             if something goes wrong while processing the request
	 * @see #processWithTemplate(Site)
	 */
	Platform processPlatform(Site site) throws InvalidConfigurationException;

	/**
	 * Processes the request, including the processing of the template. Note that
	 * {@link #init(HttpServletRequest, HttpServletResponse, PathInfo, String)} must have been called before.
	 * 
	 * @param site
	 *            the {@link Site} for which the process the request
	 * @return the result of calling the {@link Application} and applying the {@link Template}. This should then be
	 *         written to the {@link HttpServletResponse}
	 * @throws InvalidConfigurationException
	 *             if something goes wrong while processing the request
	 * @see #processPlatform(Site)
	 */
	String processWithTemplate(Site site) throws InvalidConfigurationException;

	/**
	 * Returns the content-type of the response
	 */
	String getContentType();

	/**
	 * Returns the content-length of the response
	 */
	Integer getContentLength();

	/**
	 * Initializes the {@code RequestProcessor}.
	 * 
	 * @param servletRequest
	 *            the current {@link HttpServletRequest}
	 * @param servletResponse
	 *            the current {@link HttpServletResponse}
	 * @param pathInfo
	 *            the current {@link PathInfo}
	 * @param templateDir
	 *            the absolute path to the directory where the active template of the {@link Site} resides
	 */
	void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PathInfo pathInfo,
			String templateDir);

	/**
	 * Returns {@code true} if a redirect has been send while processing the request, {@code false} otherwise.
	 */
	boolean isRedirect();

}
