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
package org.appng.core.controller;

import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.service.CoreService;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * A (ServletContext/HttpSession/ServletRequest) listener that keeps track of creation/destruction and usage of
 * {@link HttpSession}s by putting a {@link Session} object, which is updated on each request, into the
 * {@link HttpSession}
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
@WebListener
public class SessionListener implements ServletContextListener, HttpSessionListener, ServletRequestListener {

	public static final String SESSION_MANAGER = "sessionManager";
	public static final String META_DATA = "metaData";
	private static final String MDC_SESSION_ID = "sessionID";
	private static final Class<org.apache.catalina.connector.Request> CATALINA_REQUEST = org.apache.catalina.connector.Request.class;
	private static final String HTTPS = "https";
	private static final FastDateFormat DATE_PATTERN = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

	public void contextInitialized(ServletContextEvent sce) {
	}

	public void contextDestroyed(ServletContextEvent sce) {
	}

	public void sessionCreated(HttpSessionEvent event) {
		createSession(event.getSession());
	}

	private Session createSession(HttpSession httpSession) {
		Session session = getSession(httpSession);
		if (null == session) {
			DefaultEnvironment env = DefaultEnvironment.get(httpSession);
			Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
			Integer sessionTimeout = platformConfig.getInteger(Platform.Property.SESSION_TIMEOUT);
			httpSession.setMaxInactiveInterval(sessionTimeout);
			session = new Session(httpSession.getId());
			session.update(httpSession.getCreationTime(), httpSession.getLastAccessedTime(),
					httpSession.getMaxInactiveInterval());
			env.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID, httpSession.getId());
			env.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.TIMEOUT,
					httpSession.getMaxInactiveInterval());
			env.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.STARTTIME,
					httpSession.getCreationTime() / 1000L);
			MDC.put(MDC_SESSION_ID, session.getId());
			LOGGER.info("Session created: {} (created: {})", session.getId(),
					DATE_PATTERN.format(session.getCreationTime()));
			setSession(httpSession, session);
		}
		return session;
	}

	private void setSession(HttpSession httpSession, Session session) {
		httpSession.setAttribute(META_DATA, session);
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession httpSession = event.getSession();
		Environment env = DefaultEnvironment.get(httpSession);
		if (env.isSubjectAuthenticated()) {
			ApplicationContext ctx = env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
			ctx.getBean(CoreService.class).createEvent(Type.INFO, "session expired", httpSession);
		}
		Session session = getSession(httpSession);
		if (null != session) {
			LOGGER.info("Session destroyed: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {})",
					session.getId(), DATE_PATTERN.format(session.getCreationTime()),
					DATE_PATTERN.format(session.getLastAccessedTime()), session.getRequests(), session.getDomain(),
					session.getUserAgent());
			httpSession.removeAttribute(META_DATA);
		} else {
			LOGGER.info("Session destroyed: {} (created: {}, accessed: {})", httpSession.getId(),
					DATE_PATTERN.format(httpSession.getCreationTime()),
					DATE_PATTERN.format(httpSession.getLastAccessedTime()));
		}
	}

	private static Session getSession(HttpSession httpSession) {
		return (Session) httpSession.getAttribute(META_DATA);
	}

	public void requestInitialized(ServletRequestEvent sre) {
		ServletRequest request = sre.getServletRequest();
		DefaultEnvironment env = DefaultEnvironment.get(sre.getServletContext());
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		Site site = RequestUtil.getSite(env, request);
		setSecureFlag(httpServletRequest, site);
		setDiagnosticContext(env, httpServletRequest, site);
		if (site.getProperties().getBoolean(SiteProperties.SESSION_TRACKING_ENABLED, false)) {
			HttpSession httpSession = httpServletRequest.getSession();
			Session session = (Session) httpSession.getAttribute(META_DATA);
			if (null == session) {
				session = createSession(httpSession);
			}
			session.update(httpSession.getCreationTime(), httpSession.getLastAccessedTime(),
					httpSession.getMaxInactiveInterval());
			session.setSite(null == site ? null : site.getName());
			session.setDomain(site == null ? null : site.getDomain());
			session.setUser(env.getSubject() == null ? null : env.getSubject().getAuthName());
			session.setIp(request.getRemoteAddr());
			session.setUserAgent(httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
			session.addRequest();
			setSession(httpSession, session);

			if (LOGGER.isTraceEnabled()) {
				String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
				LOGGER.trace(
						"Session updated: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {}, path: {}, referer: {})",
						session.getId(), DATE_PATTERN.format(session.getCreationTime()),
						DATE_PATTERN.format(session.getLastAccessedTime()), session.getRequests(), session.getDomain(),
						session.getUserAgent(), httpServletRequest.getServletPath(), referer);
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
				MDC.put(MDC_SESSION_ID, requestedSessionId);
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

	public void requestDestroyed(ServletRequestEvent sre) {
		MDC.clear();
	}

}
