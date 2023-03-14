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
import java.net.URLClassLoader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.servlet.JspServlet;
import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.model.Site;
import org.appng.core.controller.filter.MetricsFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link RequestHandler} responsible for serving JSPs. Internally,the {@link HttpServletRequest} is forwarded to the
 * <a href="http://tomcat.apache.org/tomcat-8.0-doc/jasper-howto.html">Jasper JSP Engine</a>, namely a
 * {@code org.apache.jasper.servlet.JspServlet}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class JspHandler implements RequestHandler {

	protected JspServlet jspServlet = new JspServlet();

	public JspHandler(ServletConfig servletConfig) throws ServletException {
		this.jspServlet.init(servletConfig);
	}

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment environment,
			Site site, PathInfo pathInfo) throws ServletException, IOException {
		servletRequest.setAttribute(MetricsFilter.SERVICE_TYPE, "jsp");
		String servletPath = servletRequest.getServletPath();
		LOGGER.debug("serving jsp {}", servletPath);
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			URLClassLoader siteClassLoader = site.getSiteClassLoader();
			Thread.currentThread().setContextClassLoader(siteClassLoader);
			jspServlet.service(servletRequest, servletResponse);
		} catch (ServletException e) {
			LOGGER.error(String.format("error while serving jsp at %s", servletPath), e);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
		int status = servletResponse.getStatus();
		LOGGER.debug("returned {} for request {}", status, servletPath);
	}
}
