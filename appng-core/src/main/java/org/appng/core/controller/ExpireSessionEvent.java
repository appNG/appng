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

import javax.servlet.ServletContext;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardManager;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.messaging.Event;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;

/**
 * Event needed to expire a session in a sticky session scenario when Tomcat's {@link StandardManager} is being
 * used.<br/>
 * Since the {@link Session}s are kept in a distributed cache, the session to expire might not be present on the current
 * node. Therefore, send an event to the other nodes so one of them can expire the session.
 */
class ExpireSessionEvent extends Event {
	private static final long serialVersionUID = 1L;
	private final String sessionId;

	public ExpireSessionEvent(String siteName, String sessionId) {
		super(siteName);
		this.sessionId = sessionId;
	}

	public void perform(Environment env, Site site) throws InvalidConfigurationException, BusinessException {
		ServletContext servletContext = ((DefaultEnvironment) env).getServletContext();
		Manager manager = (Manager) servletContext.getAttribute(Controller.SESSION_MANAGER);
		SessionListener.expire(manager, env, sessionId, site, true);
	}

}