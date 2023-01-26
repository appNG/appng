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

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.service.CoreService;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * A (ServletContext/HttpSession/ServletRequest) listener that keeps track of creation/destruction and usage of
 * {@link HttpSession}s by putting a {@link Session} object, into the {@link HttpSession}
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
@WebListener
public class SessionListener implements HttpSessionListener {

	public static final String SESSION_MANAGER = "sessionManager";
	public static final String META_DATA = "metaData";
	public static final String MDC_SESSION_ID = "sessionID";
	public static final FastDateFormat DATE_PATTERN = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

	public void sessionCreated(HttpSessionEvent event) {
		HttpSession httpSession = event.getSession();
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
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession httpSession = event.getSession();
		if (DefaultEnvironment.get(httpSession).isSubjectAuthenticated()) {
			ApplicationContext ctx = DefaultEnvironment.getGlobal().getAttribute(Scope.PLATFORM,
					Platform.Environment.CORE_PLATFORM_CONTEXT);
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

	public static Session getSession(HttpSession httpSession) {
		return (Session) httpSession.getAttribute(META_DATA);
	}

	protected static void setSession(HttpSession httpSession, Session session) {
		httpSession.setAttribute(META_DATA, session);
	}

}