/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.api.support.environment;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.Request;
import org.appng.api.Scope;

/**
 * 
 * Utility class providing constants for commonly used {@link Environment} attributes.
 * 
 * @author Matthias Müller
 * 
 * @see Environment
 * 
 */
public class EnvironmentKeys {

	/** The first segement of the servlet path (scope: {@link Scope#REQUEST}) */
	public static final String BASE_URL = "baseUrl";
	/**
	 * The original servlet path as returned by {@link HttpServletRequest#getServletPath()} (scope:
	 * {@link Scope#REQUEST})
	 */
	public static final String SERVLETPATH = "originalServletPath";
	/** FIXME wrong place for this! */
	public static final String SESSION_PARAMS = "sessionParams";
	/** The actual execute path (scope: {@link Scope#REQUEST}) */
	public static final String EXECUTE_PATH = "executePath";
	/** The default path which points to the login page (scope: {@link Scope#REQUEST}) */
	public static final String DEFAULT_PATH = "defaultPath";
	/** FIXME wrong place for this! */
	public static final String JAR_INFO_MAP = "jarInfoMap";
	/** Whether or not the output format (and optionally the type) has been explicitly set in the URL */
	public static final String EXPLICIT_FORMAT = "explicitFormat";
	/** The {@link PathInfo} for the current {@link Request} (scope: {@link Scope#REQUEST}) */
	public static final String PATH_INFO = "pathInfo";
	/** */
	public static final String JSP_URL_PARAMETERS = "jspUrlParameters";
	/** The servlet path of the previous {@link HttpServletRequest} (scope: {@link Scope#SESSION}) */
	public static final String PREVIOUS_PATH = "previousPath";
	/** The query String as returned by {@link HttpServletRequest#getQueryString()} (scope: {@link Scope#REQUEST}) */
	public static final String QUERY_STRING = "queryString";
	/** A {@link Boolean} defining if the template should render some HTML (scope: {@link Scope#REQUEST}) */
	public static final String RENDER = "render";

	private EnvironmentKeys() {
	}

}
