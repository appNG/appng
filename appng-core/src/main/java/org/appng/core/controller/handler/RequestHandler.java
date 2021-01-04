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
package org.appng.core.controller.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.model.Site;

/**
 * Handles a {@link HttpServletRequest}.
 * 
 * @author Matthias Müller
 * 
 */
public interface RequestHandler {

	/**
	 * Constant used as a request-attribute to indicate a {@link HttpServletRequest} has been forwarded by appNG.
	 */
	String FORWARDED = "forwarded";

	/**
	 * Handles the given {@link HttpServletRequest},
	 * 
	 * @param servletRequest
	 *            the current {@link HttpServletRequest}
	 * @param servletResponse
	 *            the current {@link HttpServletResponse}
	 * @param environment
	 *            the current {@link Environment}
	 * @param site
	 *            the current {@link Site}
	 * @param pathInfo
	 *            the current {@link PathInfo}
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if a resource could not be found
	 */
	void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment environment,
			Site site, PathInfo pathInfo) throws ServletException, IOException;

}
