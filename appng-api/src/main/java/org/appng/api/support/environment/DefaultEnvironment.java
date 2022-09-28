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
import javax.servlet.ServletException;
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
	private SessionEnvironment session;
	private RequestEnvironment request;
	private boolean initialized;
	private Locale locale = Locale.getDefault();
	private TimeZone timeZone = TimeZone.getDefault();
	private Map<Scope, Boolean> scopeEnabled = new ConcurrentHashMap<>(4);
	private static DefaultEnvironment global;

	public static DefaultEnvironment initGlobal(ServletContext ctx) {
		return global = DefaultEnvironment.get(ctx);
	}

	public static DefaultEnvironment getGlobal() {
		return global;
	}

	protected DefaultEnvironment() {

	}

	@Deprecated
	protected DefaultEnvironment(ServletContext servletContext, HttpSession httpSession,
			ServletRequest servletRequest) {
		this(servletRequest, null);
	}

	/**
	 * @deprecated use {@link #DefaultEnvironment(ServletRequest, ServletResponse)} instead.
	 */
	@Deprecated
	protected DefaultEnvironment(ServletContext servletContext, HttpSession httpSession, ServletRequest servletRequest,
			ServletResponse servletResponse) {
		this(servletRequest, servletResponse);
	}

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param context
	 *                a {@link ServletContext}
	 * @param host
	 *                the host for the site-{@link Scope}
	 * 
	 * @deprecated use {@link DefaultEnvironment#get(ServletContext)} instead
	 */
	@Deprecated
	public DefaultEnvironment(ServletContext context, String host) {
		init(context, null, null);
	}

	public DefaultEnvironment(ServletRequest servletRequest, ServletResponse servletResponse) {
		init(servletRequest.getServletContext(), servletRequest, servletResponse);
	}

	/**
	 * Initializes the environment
	 * 
	 * @deprecated use {@link #init(ServletContext, ServletRequest, ServletResponse)} instead.
	 */
	@Override
	@Deprecated
	public void init(ServletContext context, HttpSession session, ServletRequest request, ServletResponse response,
			String host) {
		init(context, request, response);
	}

	public synchronized void init(ServletContext servletContext, ServletRequest servletRequest,
			ServletResponse servletResponse) {
		if (!initialized) {
			if (null != servletContext) {
				platform = new PlatformEnvironment(servletContext);
				enable(Scope.PLATFORM);
			} else {
				disable(Scope.PLATFORM);
			}

			if (null != servletRequest) {
				Site currentSite = RequestUtil.getSite(this, servletRequest);
				request = new RequestEnvironment(servletRequest, servletResponse);
				enable(Scope.REQUEST);

				String siteName = null == currentSite ? null : currentSite.getName();
				session = new SessionEnvironment((HttpServletRequest) servletRequest, siteName);
				enable(Scope.SESSION);

				if (null != currentSite) {
					site = new SiteEnvironment(servletContext, currentSite);
					enable(Scope.SITE);
				} else if (null == scopeEnabled.get(Scope.SITE)) {
					disable(Scope.SITE);
				}

			} else {
				disable(Scope.REQUEST);
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

	/**
	 * Returns a fully initialized DefaultEnvironment.
	 * 
	 * @param pageContext
	 *                    a {@link PageContext}
	 */
	public static DefaultEnvironment get(PageContext pageContext) {
		return new DefaultEnvironment(pageContext.getRequest(), pageContext.getResponse());
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
		DefaultEnvironment env = new DefaultEnvironment();
		env.session = new SessionEnvironment(session, null);
		env.platform = new PlatformEnvironment(session.getServletContext());
		env.enable(Scope.SESSION);
		env.enable(Scope.PLATFORM);
		env.initLocation();
		env.initialized = true;
		return env;
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
	 * 
	 * @deprecated use {@link #get(ServletRequest, ServletResponse)} instead!
	 */
	@Deprecated
	public static DefaultEnvironment get(ServletContext context, ServletRequest request) {
		return get(request, null);
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
		return new DefaultEnvironment(request, response);
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
	 * 
	 * @deprecated use {@link #get(ServletRequest, ServletResponse)} instead!
	 */
	@Deprecated
	public static DefaultEnvironment get(ServletContext context, ServletRequest request, ServletResponse response) {
		return get(request, response);
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
		DefaultEnvironment environment = new DefaultEnvironment();
		environment.init(context, null, null);
		return environment;
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

	public ScopedEnvironment getEnvironment(Scope scope) {
		if (scopeEnabled.get(scope)) {
			ScopedEnvironment env = null;
			switch (scope) {
			case PLATFORM:
				env = platform;
				break;
			case SITE:
				env = site;
				break;
			case SESSION:
				env = session;
				break;
			case REQUEST:
				env = request;
				break;
			case URL:
				break;
			}
			if (null == env) {
				LOGGER.warn("Scope {} is not available!", scope);
			}
			return env;
		} else {
			LOGGER.warn("Scope {} is not enabled!", scope);
		}
		return null;
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

	public Map<String, Object> getSession() {
		return session.getAttributes();
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
				Map<String, Object> oldContainer = session.getContainer();
				removeAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID);
				removeAttribute(Scope.SESSION, org.appng.api.Session.Environment.TIMEOUT);
				removeAttribute(Scope.SESSION, org.appng.api.Session.Environment.STARTTIME);
				session.logout();
				HttpServletRequest httpServletRequest = (HttpServletRequest) request.getServletRequest();
				session = new SessionEnvironment(httpServletRequest, session.getSiteName());
				oldContainer.keySet().forEach(key -> session.getContainer().put(key, oldContainer.get(key)));
			}
			setLocationFromSubject(subject);
			setAttribute(Scope.SESSION, Session.Environment.SUBJECT, subject);
		}
	}

	public Locale getLocale() {
		if (null == session) {
			return locale;
		}
		return getAttribute(Scope.SESSION, Session.Environment.LOCALE);
	}

	public void setLocale(Locale locale) {
		if (null == session) {
			this.locale = locale;
		} else {
			setAttribute(Scope.SESSION, Session.Environment.LOCALE, locale);
		}
	}

	public TimeZone getTimeZone() {
		if (null == session) {
			return timeZone;
		}
		return getAttribute(Scope.SESSION, Session.Environment.TIMEZONE);
	}

	public void setTimeZone(TimeZone timeZone) {
		if (null == session) {
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

	@Override
	public Site getSite() {
		return getAttribute(Scope.SITE, SiteEnvironment.SITE);
	}

	public Subject getSubject() {
		return getAttribute(Scope.SESSION, Session.Environment.SUBJECT);
	}

	/**
	 * Removes the current {@link Subject} form the {@link HttpSession} and invalidates the latter.
	 */
	public void logoutSubject() {
		if (null != session) {
			session.removeAttribute(Session.Environment.SUBJECT);
			session.logout();
			session = null;
		}
		if (null != request) {
			try {
				((HttpServletRequest) request.getServletRequest()).logout();
			} catch (ServletException e) {
				LOGGER.error("error during logout", e);
			}
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

	public void initSiteScope(Site site) {
		clearSiteScope(site);
		ServletContext servletContext = ((PlatformEnvironment) global.getEnvironment(Scope.PLATFORM))
				.getServletContext();
		this.site = new SiteEnvironment(servletContext,  site);
		enable(Scope.SITE);
	}

	/**
	 * Clears the site-scoped attributes for the given {@link Site}.
	 * 
	 * @param site
	 *             The {@link Site} to clear the site-scope for.
	 */
	public void clearSiteScope(Site site) {
		String identifier = SiteEnvironment.remove(platform.getServletContext(), site);
		LOGGER.info("Clearing site scope with identifier '{}'", identifier);
	}

}
