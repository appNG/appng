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
package org.appng.api.support.environment;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;

import lombok.extern.slf4j.Slf4j;

/**
 * This class implements {@link Environment}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class DefaultEnvironment implements Environment {

	private static final String SEP = System.getProperty("line.separator");
	private PlatformEnvironment platform;
	private SiteEnvironment site;
	private volatile SessionEnvironment session;
	private RequestEnvironment request;
	private boolean initialized;
	private Locale locale = Locale.getDefault();
	private TimeZone timeZone = TimeZone.getDefault();
	private Map<Scope, Boolean> scopeEnabled = new ConcurrentHashMap<>(4);

	protected DefaultEnvironment(ServletContext servletContext, HttpSession httpSession, ServletRequest servletRequest,
			ServletResponse servletResponse) {
		init(servletContext, servletRequest, servletResponse, null);
	}

	protected DefaultEnvironment() {

	}

	@Override
	@Deprecated
	public synchronized void init(ServletContext servletContext, HttpSession httpSession, ServletRequest servletRequest,
			ServletResponse servletResponse, String host) {
		init(servletContext, servletRequest, servletResponse, host);
	}

	public synchronized void init(ServletContext servletContext, ServletRequest servletRequest,
			ServletResponse servletResponse, String host) {
		if (!initialized) {
			if (null != servletContext) {
				platform = new PlatformEnvironment(servletContext);
				enable(Scope.PLATFORM);
				if (null != host) {
					site = new SiteEnvironment(servletContext, host);
					enable(Scope.SITE);
				} else {
					disable(Scope.SITE);
				}
			} else {
				disable(Scope.PLATFORM);
			}

			if (null != servletRequest) {
				request = new RequestEnvironment(servletRequest, servletResponse);
				enable(Scope.REQUEST);
				enable(Scope.SESSION);
				if (null == site) {
					site = new SiteEnvironment(servletContext, servletRequest.getServerName());
					enable(Scope.SITE);
				} else {
					disable(Scope.SITE);
				}
			} else {
				disable(Scope.REQUEST);
				disable(Scope.SESSION);
			}

			initialized = true;
			initLocation();
		} else {
			throw new IllegalStateException("environment has already been initialized!");
		}
	}

	private void initLocation() {
		if (null == getSubject()) {
			String timeZone = getPropertyFromSiteOrPlatform(Platform.Property.TIME_ZONE);
			if (null == timeZone) {
				setTimeZone(TimeZone.getDefault());
			} else {
				setTimeZone(TimeZone.getTimeZone(timeZone));
			}
			String locale = getPropertyFromSiteOrPlatform(Platform.Property.LOCALE);
			if (null == locale) {
				setLocale(Locale.getDefault());
			} else {
				setLocale(Locale.forLanguageTag(locale));
			}
		}
	}

	protected DefaultEnvironment(ServletContext servletContext, HttpSession httpSession,
			ServletRequest servletRequest) {
		this(servletContext, httpSession, servletRequest, null);
	}

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param context
	 *                a {@link ServletContext}
	 * @param host
	 *                the host for the site-{@link Scope}
	 */
	public DefaultEnvironment(ServletContext context, String host) {
		init(context, null, null, host);
	}

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param pageContext
	 *                    a {@link PageContext}
	 */
	public static DefaultEnvironment get(PageContext pageContext) {
		return new DefaultEnvironment(pageContext.getServletContext(), pageContext.getSession(),
				pageContext.getRequest(), pageContext.getResponse());
	}

	/**
	 * Returns a new {@link DefaultEnvironment}. Only {@link Scope#PLATFORM} and {@link Scope#SESSION} will be available
	 * for the returned instance.
	 * 
	 * @param session
	 *                a {@link HttpSession}
	 * 
	 * @return a new {@link DefaultEnvironment}
	 */
	public static DefaultEnvironment get(HttpSession session) {
		return new DefaultEnvironment(session.getServletContext(), session, null);
	}

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param context
	 *                a {@link ServletContext}
	 * @param request
	 *                a {@link ServletRequest}
	 * 
	 * @return a new {@link DefaultEnvironment}
	 */
	public static DefaultEnvironment get(ServletContext context, ServletRequest request) {
		return new DefaultEnvironment(context, ((HttpServletRequest) request).getSession(), request);
	}

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param request
	 *                 a {@link ServletRequest}
	 * @param response
	 *                 a {@link ServletResponse}
	 * 
	 * @return a new {@link DefaultEnvironment}
	 */
	public static DefaultEnvironment get(ServletRequest request, ServletResponse response) {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		return new DefaultEnvironment(httpServletRequest.getServletContext(), httpServletRequest.getSession(), request,
				response);
	}

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param context
	 *                 a {@link ServletContext}
	 * @param request
	 *                 a {@link ServletRequest}
	 * @param response
	 *                 a {@link ServletResponse}
	 * 
	 * @return a new {@link DefaultEnvironment}
	 */
	public static DefaultEnvironment get(ServletContext context, ServletRequest request, ServletResponse response) {
		return new DefaultEnvironment(context, ((HttpServletRequest) request).getSession(), request, response);
	}

	/**
	 * Returns a new {@link DefaultEnvironment}. Only {@link Scope#PLATFORM} will be available for the returned
	 * instance.
	 * 
	 * @param context
	 *                a {@link ServletContext}
	 * 
	 * @return a new {@link DefaultEnvironment}
	 */
	public static DefaultEnvironment get(ServletContext context) {
		return new DefaultEnvironment(context, null, null, null);
	}

	public void setAttribute(Scope scope, String name, Object value) {
		ScopedEnvironment env = getEnvironment(scope);
		if (null != env) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("[{}] setting {}={}", scope, name, value);
			}
			env.setAttribute(name, value);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(Scope scope, String name) {
		ScopedEnvironment env = getEnvironment(scope);
		if (null != env) {
			Object attribute = env.getAttribute(name);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("[{}] getting {}={}", scope, name, attribute);
			}
			return (T) attribute;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> T removeAttribute(Scope scope, String name) {
		ScopedEnvironment env = getEnvironment(scope);
		if (null != env) {
			Object removed = env.removeAttribute(name);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("[{}] removing {}={}", scope, name, removed);
			}
			return (T) removed;
		}
		return null;
	}

	public String getAttributeAsString(Scope scope, String name) {
		ScopedEnvironment env = getEnvironment(scope);
		if (null != env) {
			return env.getAttributeAsString(name);
		}
		return null;
	}

	private ScopedEnvironment getEnvironment(Scope scope) {
		ScopedEnvironment env = null;
		switch (scope) {
		case PLATFORM:
			env = platform;
			break;
		case SITE:
			env = site;
			break;
		case SESSION:
			env = getOrInitSession();
			break;
		case REQUEST:
			env = request;
			break;
		default:
			LOGGER.warn("no environment found for scope {}", scope);
		}
		if (null != env && scopeEnabled.get(scope)) {
			return env;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("scope {} is not available", scope);
		}
		return null;
	}

	private ScopedEnvironment getOrInitSession() {
		if (null == session && scopeEnabled.get(Scope.SESSION)) {
			synchronized (ScopedEnvironment.class) {
				if (null == session) {
					HttpServletRequest httpServletRequest = (HttpServletRequest) request.getServletRequest();
					if (null != httpServletRequest.getSession()) {
						Site currentSite = RequestUtil.getSite(this, httpServletRequest);
						session = new SessionEnvironment(httpServletRequest.getSession(),
								null == currentSite ? null : currentSite.getName());
					} else {
						disable(Scope.SESSION);
					}
				}
			}
		}
		return session;
	}

	public Set<String> keySet(Scope scope) {
		ScopedEnvironment env = getEnvironment(scope);
		if (null != env) {
			return env.keySet();
		}
		return null;
	}

	/**
	 * Returns the current {@link ServletContext}.
	 * 
	 * @return the {@link ServletContext}
	 */
	public ServletContext getServletContext() {
		if (null != platform) {
			return platform.getServletContext();
		}
		return null;
	}

	/**
	 * Returns the current {@link HttpServletRequest}.
	 * 
	 * @return the {@link HttpServletRequest}
	 */
	public HttpServletRequest getServletRequest() {
		if (null != request) {
			return (HttpServletRequest) request.getServletRequest();
		}
		return null;
	}

	/**
	 * Returns the current {@link HttpServletResponse}.
	 * 
	 * @return the {@link HttpServletResponse}
	 */
	public HttpServletResponse getServletResponse() {
		if (null != request) {
			return (HttpServletResponse) request.getServletResponse();
		}
		return null;
	}

	public boolean isSubjectAuthenticated() {
		Subject subject = getSubject();
		return subject != null && subject.isAuthenticated();
	}

	/**
	 * Sets the {@link Subject} fur the current {@link HttpSession}.
	 * 
	 * @param subject
	 *                the {@link Subject} to set
	 */
	public void setSubject(Subject subject) {
		if (null != subject) {
			Site site = RequestUtil.getSite(this, request.getServletRequest());
			boolean createNewSession = site == null
					|| site.getProperties().getBoolean(SiteProperties.RENEW_SESSION_AFTER_LOGIN, true);
			if (createNewSession && subject.isAuthenticated()) {
				Map<String, Object> oldContainer = getOrInitSession().getContainer();
				removeAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID);
				removeAttribute(Scope.SESSION, org.appng.api.Session.Environment.TIMEOUT);
				removeAttribute(Scope.SESSION, org.appng.api.Session.Environment.STARTTIME);
				session.logout();
				HttpSession httpSession = ((HttpServletRequest) request.getServletRequest()).getSession(true);
				session = new SessionEnvironment(httpSession, session.getSiteName());
				oldContainer.keySet().forEach(key -> session.getContainer().put(key, oldContainer.get(key)));
			}
			setLocationFromSubject(subject);
			setAttribute(Scope.SESSION, Session.Environment.SUBJECT, subject);
		}
	}

	public Locale getLocale() {
		if (null == getOrInitSession()) {
			return locale;
		}
		return getAttribute(Scope.SESSION, Session.Environment.LOCALE);
	}

	public void setLocale(Locale locale) {
		if (null == getOrInitSession()) {
			this.locale = locale;
		} else {
			setAttribute(Scope.SESSION, Session.Environment.LOCALE, locale);
		}
	}

	public TimeZone getTimeZone() {
		if (null == getOrInitSession()) {
			return timeZone;
		}
		return getAttribute(Scope.SESSION, Session.Environment.TIMEZONE);
	}

	public void setTimeZone(TimeZone timeZone) {
		if (null == getOrInitSession()) {
			this.timeZone = timeZone;
		} else {
			setAttribute(Scope.SESSION, Session.Environment.TIMEZONE, timeZone);
		}
	}

	private void setLocationFromSubject(Subject subject) {
		if (null != subject) {
			String timeZoneVal = subject.getTimeZone();
			if (null != timeZoneVal) {
				setTimeZone(TimeZone.getTimeZone(timeZoneVal));
			}
			String langVal = subject.getLanguage();
			if (null != langVal) {
				setLocale(Locale.forLanguageTag(langVal));
			}
		}
	}

	public Subject getSubject() {
		return getAttribute(Scope.SESSION, Session.Environment.SUBJECT);
	}

	/**
	 * Removes the current {@link Subject} form the {@link HttpSession} and invalidates the latter.
	 */
	public void logoutSubject() {
		if (null != getOrInitSession()) {
			session.removeAttribute(Session.Environment.SUBJECT);
			session.logout();
			session = null;
		}
	}

	private String getPropertyFromSiteOrPlatform(String propertyName) {
		String property = null;
		Site currentSite = RequestUtil.getSite(this, getServletRequest());
		if (null != currentSite) {
			property = currentSite.getProperties().getString(propertyName);
		} else {
			Properties props = getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
			if (null != props) {
				property = props.getString(propertyName);
			}
		}
		return property;
	}

	/**
	 * Disables the given {@link Scope} for this environment
	 * 
	 * @param scope
	 *              the {@link Scope} to disable
	 * 
	 * @see #enable(Scope)
	 */
	public void disable(Scope scope) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("disabling scope {}", scope);
		}
		scopeEnabled.put(scope, Boolean.FALSE);
	}

	/**
	 * Enables the given {@link Scope} for this environment
	 * 
	 * @param scope
	 *              the {@link Scope} to enable
	 * 
	 * @see #disable(Scope)
	 */
	public void enable(Scope scope) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("enabling scope {}", scope);
		}
		scopeEnabled.put(scope, Boolean.TRUE);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "#" + hashCode());
		String subject = isSubjectAuthenticated() ? getSubject().getAuthName() : "-unknown-";
		sb.append(String.format(" (Locale: %s, Timezone: %s, User: %s)", getLocale().toLanguageTag(),
				getTimeZone().getID(), subject));
		sb.append(SEP);
		if (null != platform) {
			sb.append(platform.toString() + SEP);
		}
		if (null != site) {
			sb.append(site.toString() + SEP);
		}
		if (null != session) {
			sb.append(session.toString() + SEP);
		}
		if (null != request) {
			sb.append(request.toString() + SEP);
		}
		return sb.toString();
	}

	/**
	 * Checks whether this {@link Environment} has been initialized.
	 * 
	 * @return {@code true} if his {@link Environment} has been initialized, {@code false} otherwise.
	 * 
	 * @see #init(ServletContext, HttpSession, ServletRequest, ServletResponse, String)
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Clears the site-scoped attributes for the given {@link Site}.
	 * 
	 * @param site
	 *             The {@link Site} to clear the site-scope for.
	 */
	public void clearSiteScope(Site site) {
		String identifier = Scope.SITE.forSite(site.getHost());
		platform.getServletContext().removeAttribute(identifier);
		LOGGER.info("Clearing site scope with identifier '{}'", identifier);
	}

}
