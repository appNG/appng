/*
 * Copyright 2011-2019 the original author or authors.
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionFilter implements Filter {

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String servletPath = httpServletRequest.getServletPath();
		if (!Home.ROOT.equals(servletPath)) {
			HttpSession session = httpServletRequest.getSession();
			Boolean authorized = (Boolean) session.getAttribute(Home.AUTHORIZED);
			if (!Boolean.TRUE.equals(authorized)) {
				LOGGER.info("session {} is not authorized, sending 403.", session.getId());
				((HttpServletResponse) response).sendError(HttpStatus.FORBIDDEN.value(), "Please authenticate first!");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	public void destroy() {

	}

}
