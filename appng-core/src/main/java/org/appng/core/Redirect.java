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
package org.appng.core;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.appng.core.controller.HttpHeaders;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for sending HTTP redirects to a {@link HttpServletResponse}. See
 * <a href="https://tools.ietf.org/html/rfc2616#section-10.3">RFC 2616</a> for further details.
 * 
 * @author Matthias Müller
 * @author Matthias Herlitzius
 * 
 * @see HttpServletResponse#SC_MOVED_PERMANENTLY
 * @see HttpServletResponse#SC_FOUND
 * @see HttpServletResponse#SC_SEE_OTHER
 * @see HttpServletResponse#SC_TEMPORARY_REDIRECT
 */
@Slf4j
public class Redirect {

	/**
	 * Sends a redirect with the given {@code statusCode} and {@code target} to a {@link HttpServletResponse}.
	 * 
	 * @param response
	 *                   the response
	 * @param statusCode
	 *                   the HTTP status-code
	 * @param target
	 *                   the redirect target
	 */
	public static void to(HttpServletResponse response, Integer statusCode, String target) {
		to(response, statusCode, "", target);
	}

	/**
	 * Sends a redirect with the given {@code statusCode} and {@code target} to a {@link HttpServletResponse}.
	 * 
	 * @param response
	 *                   the response
	 * @param statusCode
	 *                   the HTTP status-code
	 * @param origin
	 *                   the (optional) origin of the request, only used for logging
	 * @param target
	 *                   the redirect target
	 */
	public static void to(HttpServletResponse response, Integer statusCode, String origin, String target) {
		if (StringUtils.isNotBlank(origin)) {
			LOGGER.info("Redirecting request {} to {} ({})", origin, target, statusCode);
		} else {
			LOGGER.info("Redirecting request to {} ({})", target, statusCode);
		}
		response.setStatus(statusCode);
		response.setHeader(HttpHeaders.LOCATION, target);
		if (HttpServletResponse.SC_MOVED_PERMANENTLY == statusCode.intValue() && !target.startsWith("/")) {
			response.setHeader(HttpHeaders.CONNECTION, "close");
		}
	}

	private Redirect() {
	}

}
