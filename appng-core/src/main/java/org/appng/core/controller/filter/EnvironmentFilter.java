/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.controller.Session;
import org.appng.core.controller.SessionListener;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Filter} that creates a {@link DefaultEnvironment} for the current {@link HttpServletRequest}/
 * {@link HttpServletResponse} and adds it as an attribute to the current request. <br/>
 * Additionally, it sets the attributes for the message diagnostic context ({@link MDC}).
 */
@Slf4j
public class EnvironmentFilter extends OncePerRequestFilter {

	private static final Class<org.apache.catalina.connector.Request> CATALINA_REQUEST = org.apache.catalina.connector.Request.class;
	private static final String HTTPS = "https";
	private static final int ONE_MINUTE = 60;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		DefaultEnvironment env = new DefaultEnvironment(request, response);
		request.setAttribute(getAttributeName(), env);
		requestInitialized(request, env);
		try {
			filterChain.doFilter(request, response);
		} finally {
			requestDestroyed(request, env);
		}
	}

	public void requestInitialized(HttpServletRequest httpServletRequest, Environment env) {
		Site site = env.getSite();
		setSecureFlag(httpServletRequest, site);
		setDiagnosticContext(env, httpServletRequest, site);
		if (null != site && site.getProperties().getBoolean(SiteProperties.SESSION_TRACKING_ENABLED, false)) {
			HttpSession httpSession = httpServletRequest.getSession();
			Session session = SessionListener.getSession(httpSession);
			session.update(httpSession.getCreationTime(), httpSession.getLastAccessedTime(),
					httpSession.getMaxInactiveInterval());
			session.setSite(null == site ? null : site.getName());
			session.setDomain(site == null ? null : site.getDomain());
			session.setUser(env.getSubject() == null ? null : env.getSubject().getAuthName());
			session.setIp(httpServletRequest.getRemoteAddr());
			session.setUserAgent(httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
			session.addRequest();

			if (LOGGER.isTraceEnabled()) {
				String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
				LOGGER.trace(
						"Session updated: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {}, path: {}, referer: {})",
						session.getId(), SessionListener.DATE_PATTERN.format(session.getCreationTime()),
						SessionListener.DATE_PATTERN.format(session.getLastAccessedTime()), session.getRequests(),
						session.getDomain(), session.getUserAgent(), httpServletRequest.getServletPath(), referer);
			}
		}

	}

	protected void setDiagnosticContext(Environment env, HttpServletRequest httpServletRequest, Site site) {
		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		if (platformConfig.getBoolean(Platform.Property.MDC_ENABLED)) {
			MDC.put("path", httpServletRequest.getServletPath());
			String queryString = httpServletRequest.getQueryString();
			if (null != queryString) {
				MDC.put("query", queryString);
			}
			String requestedSessionId = httpServletRequest.getRequestedSessionId();
			if (StringUtils.isNotBlank(requestedSessionId)) {
				MDC.put(SessionListener.MDC_SESSION_ID, requestedSessionId);
			}
			if (null != site) {
				MDC.put("site", site.getName());
			}
			MDC.put("locale", env.getLocale().toString());
			MDC.put("method", httpServletRequest.getMethod());
			MDC.put("timezone", env.getTimeZone().getID());
			MDC.put("ip", httpServletRequest.getRemoteAddr());
			if (null != env.getSubject() && null != env.getSubject().getAuthName()) {
				MDC.put("user", env.getSubject().getAuthName());
			} else {
				MDC.put("user", "-unknown-");
			}
			Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				MDC.put("h." + name.toLowerCase(), httpServletRequest.getHeader(name));
			}
		}
	}

	protected void setSecureFlag(HttpServletRequest httpServletRequest, Site site) {
		if (!httpServletRequest.isSecure()
				&& ((null != site && site.getDomain().startsWith(HTTPS))
						|| HttpHeaders.isRequestSecure(httpServletRequest))
				&& CATALINA_REQUEST.isAssignableFrom(httpServletRequest.getClass())) {
			CATALINA_REQUEST.cast(httpServletRequest).setSecure(true);
		}
	}

	public void requestDestroyed(HttpServletRequest httpServletRequest, Environment env) {
		HttpSession httpSession = httpServletRequest.getSession(false);
		if (null != httpSession && httpSession.isNew()) {
			String userAgent = StringUtils.trimToEmpty(httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
			Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
			List<String> userAgentPatterns = Arrays
					.asList(platformConfig.getClob(Platform.Property.SESSION_FILTER).split(StringUtils.LF));
			if (userAgentPatterns.stream().anyMatch(userAgent::matches)) {
				Site site = environment().getSite();
				if (null != site && RequestUtil
						.getPathInfo(DefaultEnvironment.getGlobal(), site, httpServletRequest.getServletPath())
						.isGui()) {
					httpSession.setMaxInactiveInterval(ONE_MINUTE);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Setting session lifetime for {} to {}s (user-agent: {})", httpSession.getId(),
								ONE_MINUTE, userAgent);
					}
				} else {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Session automatically discarded: {} (user-agent: {})", httpSession.getId(),
								userAgent);
					}
					httpSession.invalidate();
				}

			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("None of the given patterns {} matched user-agent {} for session {}",
						StringUtils.join(userAgentPatterns, StringUtils.SPACE), userAgent, httpSession.getId());
			}
		}
		MDC.clear();
	}

	/**
	 * Retrieves the current {@link Environment} from the {@link RequestContextHolder}.
	 * 
	 * @return the environment
	 */
	public static DefaultEnvironment environment() {
		return (DefaultEnvironment) RequestContextHolder.currentRequestAttributes().getAttribute(getAttributeName(),
				RequestAttributes.SCOPE_REQUEST);
	}

	private static String getAttributeName() {
		return Environment.class.getName();
	}
}
