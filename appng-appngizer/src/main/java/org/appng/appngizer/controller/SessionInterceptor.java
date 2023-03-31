/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.appngizer.controller;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionInterceptor implements WebRequestInterceptor {

	@Override
	public void preHandle(WebRequest request) throws Exception {
		ServletWebRequest servletWebRequest = ServletWebRequest.class.cast(request);
		HttpServletRequest httpServletRequest = servletWebRequest.getRequest();
		String pathInfo = httpServletRequest.getPathInfo();
		if (!Home.ROOT.equals(pathInfo)) {
			HttpSession session = httpServletRequest.getSession();

			Properties platformConfig = DefaultEnvironment.getGlobal()
					.getAttribute(Scope.PLATFORM, org.appng.api.Platform.Environment.PLATFORM_CONFIG);
			String sharedSecret = platformConfig.getString(org.appng.api.Platform.Property.SHARED_SECRET);

			Enumeration<String> headers = httpServletRequest.getHeaders(HttpHeaders.AUTHORIZATION);
			Boolean authorized = false;
			while (headers.hasMoreElements()) {
				if (String.format("Bearer %s", sharedSecret).equals(headers.nextElement())) {
					authorized = true;
					break;
				}
			}

			authorized |= Boolean.TRUE.equals(session.getAttribute(Home.AUTHORIZED));
			if (!authorized) {
				LOGGER.info("session {} is not authorized, sending 403.", session.getId());
				servletWebRequest.getResponse().sendError(HttpStatus.FORBIDDEN.value(), "Please authenticate first!");
				return;
			}
		}
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) throws Exception {
		// empty
	}

	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws Exception {
		// empty
	}

}
