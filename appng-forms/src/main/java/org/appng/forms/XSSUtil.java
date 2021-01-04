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
package org.appng.forms;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.owasp.esapi.Encoder;

import lombok.extern.slf4j.Slf4j;

/**
 * A utility class helping with XSS prevention.<br/>
 * Uses <a href="https://www.javadoc.io/doc/org.owasp.esapi/esapi/2.1.0.1">ESAPI</a> and
 * <a href="https://jsoup.org/cookbook/cleaning-html/whitelist-sanitizer">JSOUP</a>.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class XSSUtil {

	/**
	 * request-attribute indicating XSS has been stripped from the {@link HttpServletRequest} ({@link Boolean#TRUE} in
	 * that case)
	 */
	private static final String XSS_STRIPPED = XSSUtil.class.getName() + ".xssStripped";

	private Encoder encoder;
	private Whitelist whitelist;
	private String[] exceptions;

	public XSSUtil(Encoder encoder) {
		this(encoder, Whitelist.basic());
	}

	public XSSUtil(Encoder encoder, Whitelist whitelist, String... exceptions) {
		this.encoder = encoder;
		this.whitelist = whitelist;
		this.exceptions = exceptions;
	}

	public String stripXss(String parameter) {
		if (null == parameter) {
			return parameter;
		}
		return Jsoup.clean(encoder.canonicalize(parameter), whitelist);
	}

	public String[] stripXss(String[] values) {
		if (values == null) {
			return null;
		}
		String[] encodedValues = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			encodedValues[i] = stripXss(values[i]);
		}
		return encodedValues;
	}

	public boolean doProcess(HttpServletRequest request) {
		return doProcess(request, exceptions);
	}

	public boolean doProcess(HttpServletRequest request, String... exceptions) {
		if (null != request.getAttribute(XSS_STRIPPED)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("request attribute '{}' is {} for request {}, no need to process", XSS_STRIPPED,
						request.getAttribute(XSS_STRIPPED), request.getServletPath());
			}
			return false;
		}
		if (null != exceptions) {
			for (String exception : exceptions) {
				if (!(StringUtils.isBlank(exception) || exception.startsWith("#"))
						&& request.getServletPath().startsWith(exception.trim())) {
					return false;
				}
			}
		}
		return true;
	}

	public void setProcessed(HttpServletRequest request, boolean processed) {
		request.setAttribute(XSS_STRIPPED, processed);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setting request attribute '{}' to TRUE for request {}", XSS_STRIPPED, request.getServletPath());
		}
	}

}
