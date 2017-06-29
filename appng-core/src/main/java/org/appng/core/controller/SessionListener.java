/*
 * Copyright 2011-2017 the original author or authors.
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

import static org.apache.commons.lang3.time.DateFormatUtils.format;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

import org.apache.commons.collections.list.UnmodifiableList;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * A (ServletContext/HttpSession/ServletRequest) listener that keeps track of creation/destruction and usage of
 * {@link HttpSession}s.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@WebListener
public class SessionListener implements ServletContextListener, HttpSessionListener, ServletRequestListener {

	private static Logger LOGGER = LoggerFactory.getLogger(SessionListener.class);
	private static final Class<org.apache.catalina.connector.Request> CATALINA_REQUEST = org.apache.catalina.connector.Request.class;
	private static final String HTTPS = "https";
	public static final String SESSIONS = "sessions";
	public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

	private static final ConcurrentMap<String, Session> SESSION_MAP = new ConcurrentHashMap<String, Session>();

	public void contextInitialized(ServletContextEvent sce) {
		DefaultEnvironment env = DefaultEnvironment.get(sce.getServletContext());
		saveSessions(env);
	}

	public void contextDestroyed(ServletContextEvent sce) {
		DefaultEnvironment env = DefaultEnvironment.get(sce.getServletContext());
		SESSION_MAP.clear();
		env.removeAttribute(Scope.PLATFORM, SESSIONS);
	}

	public void sessionCreated(HttpSessionEvent event) {
		createSession(event.getSession());
	}

	private Session createSession(HttpSession httpSession) {
		DefaultEnvironment env = DefaultEnvironment.get(httpSession);
		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Integer sessionTimeout = platformConfig.getInteger(Platform.Property.SESSION_TIMEOUT);
		httpSession.setMaxInactiveInterval(sessionTimeout);
		Session session = new Session(httpSession.getId());
		session.update(httpSession.getCreationTime(), httpSession.getLastAccessedTime(),
				httpSession.getMaxInactiveInterval());
		SESSION_MAP.put(session.getId(), session);
		saveSessions(env);
		env.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID, httpSession.getId());
		env.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.TIMEOUT,
				httpSession.getMaxInactiveInterval());
		env.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.STARTTIME,
				httpSession.getCreationTime() / 1000L);
		LOGGER.info("Session created: {} (created: {})", session.getId(),
				format(session.getCreationTime(), DATE_PATTERN));
		return session;
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession httpSession = event.getSession();
		Environment env = DefaultEnvironment.get(httpSession);
		if (!destroySession(httpSession.getId(), env)) {
			LOGGER.info("Session destroyed: {} (created: {}, accessed: {})", httpSession.getId(),
					format(httpSession.getCreationTime(), DATE_PATTERN),
					format(httpSession.getLastAccessedTime(), DATE_PATTERN));
		}
	}

	protected static boolean destroySession(String sessionId, Environment env) {
		Session session = SESSION_MAP.remove(sessionId);
		if (null != session) {
			saveSessions(env);
			LOGGER.info("Session destroyed: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {})",
					session.getId(), format(session.getCreationTime(), DATE_PATTERN),
					format(session.getLastAccessedTime(), DATE_PATTERN), session.getRequests(), session.getDomain(),
					session.getUserAgent());
			return true;
		}
		return false;
	}

	public void requestInitialized(ServletRequestEvent sre) {
		ServletRequest request = sre.getServletRequest();
		DefaultEnvironment env = DefaultEnvironment.get(sre.getServletContext(), request);
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpSession httpSession = httpServletRequest.getSession();
		Site site = RequestUtil.getSite(env, request);
		setSecureFlag(httpServletRequest, site);
		setDiagnosticContext(env, httpServletRequest, site);

		Session session = SESSION_MAP.get(httpSession.getId());
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

		if (LOGGER.isTraceEnabled()) {
			String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
			LOGGER.trace(
					"Session updated: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {}, path: {}, referer: {})",
					session.getId(), format(session.getCreationTime(), DATE_PATTERN),
					format(session.getLastAccessedTime(), DATE_PATTERN), session.getRequests(), session.getDomain(),
					session.getUserAgent(), httpServletRequest.getServletPath(), referer);
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
			MDC.put("sessionID", httpServletRequest.getSession().getId());
			if (null != site) {
				MDC.put("site", site.getName());
			}
			MDC.put("locale", env.getLocale().toString());
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

	private static void saveSessions(Environment env) {
		env.setAttribute(Scope.PLATFORM, SESSIONS,
				UnmodifiableList.decorate(new ArrayList<Session>(SESSION_MAP.values())));
	}

}
