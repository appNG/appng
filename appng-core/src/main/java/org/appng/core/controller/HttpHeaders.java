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
package org.appng.core.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

/**
 * Useful constants and methods for setting HTTP-Headers as specified by
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</a>.
 * 
 * @author Matthias Müller
 * 
 */
public class HttpHeaders extends org.springframework.http.HttpHeaders {

	/**
	 * The <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">Content-Type</a>
	 * {@value org.springframework.http.MediaType#APPLICATION_JSON_VALUE}
	 */
	public static final String CONTENT_TYPE_APPLICATION_JSON = MediaType.APPLICATION_JSON_VALUE;

	/**
	 * The <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">Content-Type</a>
	 * {@value org.springframework.http.MediaType#TEXT_PLAIN_VALUE}
	 */
	public static final String CONTENT_TYPE_TEXT_PLAIN = MediaType.TEXT_PLAIN_VALUE;

	/**
	 * The <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">Content-Type</a>
	 * {@value org.springframework.http.MediaType#TEXT_HTML_VALUE}
	 */
	public static final String CONTENT_TYPE_TEXT_HTML = MediaType.TEXT_HTML_VALUE;

	/**
	 * The <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17">Content-Type</a>
	 * {@value org.springframework.http.MediaType#TEXT_XML_VALUE}
	 */
	public static final String CONTENT_TYPE_TEXT_XML = MediaType.TEXT_XML_VALUE;

	/** The HTTP {@code X-Forwarded-For} header field name */
	public static final String X_FORWARDED_FOR = "X-Forwarded-For";

	/** The HTTP {@code X-Forwarded-Proto} header field name. */
	public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

	/**
	 * Constant for charset UTF-8
	 */
	public static final String CHARSET_UTF8 = "UTF-8";

	/** the https protocol */
	public static final String PROTO_HTTPS = "https";

	/** the http protocol */
	public static final String PROTO_HTTP = "http";

	private HttpHeaders() {
	}

	/**
	 * Checks if the given {@link HttpServletRequest} is secure. This is the case if either
	 * {@link HttpServletRequest#isSecure()} returns {@code true} or if the request header {@value #X_FORWARDED_PROTO}
	 * has the value {@value #PROTO_HTTPS}.
	 * 
	 * @param httpServletRequest
	 *            the {@link HttpServletRequest}
	 * @return {@code true} if the given request is secure, {@code false} otherwise
	 */
	public static boolean isRequestSecure(HttpServletRequest httpServletRequest) {
		return httpServletRequest.isSecure() || PROTO_HTTPS.equals(httpServletRequest.getHeader(X_FORWARDED_PROTO));
	}

	/**
	 * Avoids caching of the page using {@value org.springframework.http.HttpHeaders#CACHE_CONTROL},
	 * {@value org.springframework.http.HttpHeaders#PRAGMA} and {@value org.springframework.http.HttpHeaders#EXPIRES}
	 * headers.
	 * 
	 * @param httpServletResponse
	 *            the HttpServletResponse
	 */
	public static void setNoCache(HttpServletResponse httpServletResponse) {
		httpServletResponse.setHeader(CACHE_CONTROL, "no-cache,no-store,max-age=0");
		httpServletResponse.setHeader(PRAGMA, "No-cache");
		httpServletResponse.setHeader(EXPIRES, "Thu, 01 Jan 1970 00:00:00 GMT");
	}

	public static String getContentType(String contentType, String charSet) {
		return contentType + "; charset=" + charSet;
	}
}
