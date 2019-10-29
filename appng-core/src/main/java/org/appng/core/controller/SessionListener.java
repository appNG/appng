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
package org.appng.core.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
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

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.service.CacheService;
import org.appng.core.service.CoreService;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * A (ServletContext/HttpSession/ServletRequest) listener that keeps track of creation/destruction and usage of
 * {@link HttpSession}s by creating a {@link Session} that is a lightweight copy.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
@WebListener
public class SessionListener implements ServletContextListener, HttpSessionListener, ServletRequestListener {

	/**
	 * Value to be set for the session-scoped parameter {@link Environment} attribute #EXPIRE_SESSIONS} to indicate that
	 * all sessions should be expired.
	 */
	public static final String ALL = "ALL";
	/**
	 * A string flag to be set in the {@link Environment} with {@link Scope#SESSION}, indicating that
	 * {@link #expire(Manager, Environment, Site)} should be called. Must either contain {@value #ALL} or a single
	 * session ID.
	 */
	public static final String EXPIRE_SESSIONS = "expireSessions";

	/** name for the cache containing the {@link Session}s */
	static final String SESSIONS = "appNG_sessions";
	private static final String MDC_SESSION_ID = "sessionID";
	private static final Class<org.apache.catalina.connector.Request> CATALINA_REQUEST = org.apache.catalina.connector.Request.class;
	private static final String HTTPS = "https";
	private static final FastDateFormat DATE_PATTERN = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

	public void contextInitialized(ServletContextEvent sce) {
		MutableConfiguration<String, Session> mutableConfiguration = new MutableConfiguration<>();
		mutableConfiguration.setStatisticsEnabled(true);
		mutableConfiguration.setTypes(String.class, Session.class);
		mutableConfiguration.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
		CacheService.getCacheManager().createCache(SESSIONS, mutableConfiguration);
		LOGGER.info("Created eternal cache '{}'.", SESSIONS);
	}

	public void contextDestroyed(ServletContextEvent sce) {
		// getSessionCache().removeAll();
	}

	public void sessionCreated(HttpSessionEvent event) {
		createSession(event.getSession());
	}

	private Session createSession(HttpSession httpSession) {
		Session session;
		if (getSessionCache().containsKey(httpSession.getId())) {
			session = getSessionCache().get(httpSession.getId());
		} else {
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
			getSessionCache().put(httpSession.getId(), session);
		}
		return session;
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession httpSession = event.getSession();
		Environment env = DefaultEnvironment.get(httpSession);
		if (env.isSubjectAuthenticated()) {
			ApplicationContext ctx = env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
			ctx.getBean(CoreService.class).createEvent(Type.INFO, "session expired", httpSession);
		}
		if (!destroySession(httpSession.getId(), env)) {
			LOGGER.info("Session destroyed: {} (created: {}, accessed: {})", httpSession.getId(),
					DATE_PATTERN.format(httpSession.getCreationTime()),
					DATE_PATTERN.format(httpSession.getLastAccessedTime()));
		}
	}

	protected static boolean destroySession(String sessionId, Environment env) {
		Session session = getSession(sessionId);
		if (null != session) {
			getSessionCache().remove(sessionId);
			LOGGER.info("Session destroyed: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {})",
					session.getId(), DATE_PATTERN.format(session.getCreationTime()),
					DATE_PATTERN.format(session.getLastAccessedTime()), session.getRequests(), session.getDomain(),
					session.getUserAgent());
			return true;
		}
		return false;
	}

	private static Session getSession(String sessionId) {
		return getSessionCache().get(sessionId);
	}

	public void requestInitialized(ServletRequestEvent sre) {
		ServletRequest request = sre.getServletRequest();
		DefaultEnvironment env = DefaultEnvironment.get(sre.getServletContext(), request);
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpSession httpSession = httpServletRequest.getSession();
		Site site = RequestUtil.getSite(env, request);
		setSecureFlag(httpServletRequest, site);
		setDiagnosticContext(env, httpServletRequest, site);

		Session session = getSession(httpSession.getId());
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
		getSessionCache().replace(httpSession.getId(), session);

		if (LOGGER.isTraceEnabled()) {
			String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
			LOGGER.trace(
					"Session updated: {} (created: {}, accessed: {}, requests: {}, domain: {}, user-agent: {}, path: {}, referer: {})",
					session.getId(), DATE_PATTERN.format(session.getCreationTime()),
					DATE_PATTERN.format(session.getLastAccessedTime()), session.getRequests(), session.getDomain(),
					session.getUserAgent(), httpServletRequest.getServletPath(), referer);
		}

	}

	static Cache<String, Session> getSessionCache() {
		return CacheService.getCacheManager().getCache(SESSIONS);
	}

	protected void setDiagnosticContext(Environment env, HttpServletRequest httpServletRequest, Site site) {
		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		if (platformConfig.getBoolean(Platform.Property.MDC_ENABLED)) {
			MDC.put("path", httpServletRequest.getServletPath());
			String queryString = httpServletRequest.getQueryString();
			if (null != queryString) {
				MDC.put("query", queryString);
			}
			MDC.put(MDC_SESSION_ID, httpServletRequest.getSession().getId());
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

	/**
	 * Returns a list of all cache {@link Session}s
	 * 
	 * @return the list
	 */
	public static List<Session> getSessions() {
		Cache<String, Session> sessionCache = getSessionCache();
		List<Session> sessions = new ArrayList<>();
		sessionCache.forEach(s -> sessions.add(s.getValue()));
		Collections.sort(sessions, (s1, s2) -> ObjectUtils.compare(s1.getCreationTime(), s2.getCreationTime()));
		return sessions;
	}

	static void expire(Manager manager, Environment env, Site site) {
		expire(manager, env, env.removeAttribute(Scope.SESSION, EXPIRE_SESSIONS), site, false);
	}

	static void expire(Manager manager, Environment env, String sessionId, Site site, boolean triggeredByEvent) {
		if (ALL.equals(sessionId)) {
			getSessions().parallelStream()
					.forEach(session -> expireSession(manager, env, session, site, triggeredByEvent));
		} else if (null != sessionId) {
			expireSession(manager, env, getSession(sessionId), site, triggeredByEvent);
		}
	}

	private static void expireSession(Manager manager, Environment env, Session session, Site site,
			boolean triggeredByEvent) {
		if (null != session) {
			org.apache.catalina.Session containerSession = getContainerSession(manager, session.getId());
			if (null != containerSession) {
				if (session.isExpired()) {
					LOGGER.info("expiring session {}", session.getId());
					containerSession.expire();
				}
			} else if (!triggeredByEvent && manager instanceof StandardManager) {
				site.sendEvent(new ExpireSessionEvent(site.getName(), session.getId()));
			} else {
				LOGGER.debug("session to expire not found in {}: {} (created: {}, last access: {})",
						manager.getClass().getSimpleName(), session.getId(), session.getCreationTime(),
						session.getLastAccessedTime());
				destroySession(session.getId(), env);
			}
		}
	}

	static org.apache.catalina.Session getContainerSession(Manager manager, String sessionId) {
		try {
			return manager.findSession(sessionId);
		} catch (IOException e) {
			LOGGER.warn(String.format("error while retrieving session %s", sessionId), e);
		}
		return null;
	}

	public static void markSessionExpired(String id) {
		Session session = getSession(id);
		if (null != session) {
			session.expire();
			getSessionCache().replace(id, session);
		}
	}

}
