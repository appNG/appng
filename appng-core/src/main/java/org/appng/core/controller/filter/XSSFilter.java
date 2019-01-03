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
package org.appng.core.controller.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.XSSHelper;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.forms.XSSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet filter to prevent XSS attacks.<br/>
 * Inspired by
 * <ul>
 * <li>https://dzone.com/articles/stronger-anti-cross-site
 * <li>https://jsoup.org/cookbook/cleaning-html/whitelist-sanitizer
 * </ul>
 * 
 * @author Matthias Müller
 *
 */
public class XSSFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(XSSFilter.class);
	private XSSUtil xssUtil;

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Site site = RequestUtil.getSite(DefaultEnvironment.get(request.getServletContext()), request);
		HttpServletRequest servletRequest = (HttpServletRequest) request;
		boolean processXss = null != site && null != xssUtil;
		if (processXss) {
			String[] exceptions = site.getProperties().getClob(SiteProperties.XSS_EXCEPTIONS).split(StringUtils.LF);
			if (xssUtil.doProcess(servletRequest, exceptions)) {

				servletRequest = new HttpServletRequestWrapper((HttpServletRequest) request) {
					@Override
					public String getParameter(String name) {
						return xssUtil.stripXss(super.getParameter(name));
					}

					@Override
					public String[] getParameterValues(String name) {
						return xssUtil.stripXss(super.getParameterValues(name));
					}

				};
				if (log.isDebugEnabled()) {
					log.debug("XSS protection enabled for {} {}", servletRequest.getMethod(),
							servletRequest.getServletPath());
				}
			}
		}
		chain.doFilter(servletRequest, response);
		if (processXss) {
			xssUtil.setProcessed(servletRequest, processXss);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		Environment env = DefaultEnvironment.get(filterConfig.getServletContext());
		Properties platformProps = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		if (platformProps.getBoolean(Platform.Property.XSS_PROTECT)) {
			xssUtil = XSSHelper.getXssUtil(platformProps);
		}
	}

	public void destroy() {

	}

}