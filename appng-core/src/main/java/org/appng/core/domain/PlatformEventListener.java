/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.core.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Subject;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.PlatformEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * An entity listener that creates a new {@link PlatformEvent} on {@link PrePersist}, {@link PreUpdate} and
 * {@link PreRemove}. Also, a {@link PlatformEvent} can be created manually by calling
 * {@link #createEvent(Type, String)}. <br/>
 * Note that this listener is able to work in two scenarios. The first is as a regular Spring bean that can be invoked
 * from other beans. As a JPA entity listener, whe use a static reference to the current {@link ApplicationContext} to
 * retrieve an instance of the {@link EntityManager} in use.
 * 
 * @author Matthias Müller
 *
 */
public class PlatformEventListener implements ApplicationContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(PlatformEventListener.class);
	private static ApplicationContext CONTEXT;
	private static String auditUser = "<unknown>";
	private static boolean persist = true;
	@Autowired
	private EntityManager entityManager;

	private EventProvider eventProvider = new EventProvider();

	public PlatformEventListener() {

	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		PlatformEventListener.CONTEXT = applicationContext;
		LOG.info("Using application context {}", applicationContext);
	}

	@PrePersist
	public void beforeCreate(Auditable<?> o) {
		createEvent(o, PlatformEvent.Type.CREATE);
	}

	@PostUpdate
	public void onUpdate(Auditable<?> o) {
		createEvent(o, PlatformEvent.Type.UPDATE);
	}

	@PostRemove
	public void onDelete(Auditable<?> o) {
		createEvent(o, PlatformEvent.Type.DELETE);
	}

	private void createEvent(Auditable<?> auditable, Type type) {
		String message = String.format(auditable.getAuditName());
		createEvent(auditable, type, message);
	}

	private void createEvent(Auditable<?> auditable, Type type, String message) {
		createEvent(type, message, auditable.getVersion());
	}

	public void createEvent(Type type, String message) {
		createEvent(type, message, new Date());
	}

	private void createEvent(Type type, String message, Date created) {
		PlatformEvent event = getEventProvider().provide(type, message);
		if (persist) {
			if (null == entityManager) {
				CONTEXT.getAutowireCapableBeanFactory().autowireBean(this);
			}			
			entityManager.persist(event);
		}
		LOG.info("Created entry {}", event);
	}

	public void setAuditUser(String auditUser) {
		PlatformEventListener.auditUser = auditUser;
	}

	public void setPersist(boolean persist) {
		PlatformEventListener.persist = persist;
	}

	public EventProvider getEventProvider() {
		return CONTEXT.getBeansOfType(EventProvider.class).isEmpty() ? eventProvider
				: CONTEXT.getBean(EventProvider.class);
	}

	public void setEventProvider(EventProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

	public static class EventProvider {
		public static final String EVENT_UUID = "eventUUID";

		public String getExecutionId(HttpServletRequest servletRequest) {
			if (null != servletRequest) {
				String eventUUID = (String) servletRequest.getAttribute(EVENT_UUID);
				if (null == eventUUID) {
					servletRequest.setAttribute(EVENT_UUID, UUID.randomUUID().toString());
				}
				return (String) servletRequest.getAttribute(EVENT_UUID);
			}
			return UUID.randomUUID().toString();
		}

		public PlatformEvent provide(Type type, String message) {
			HttpServletRequest request = getServletRequest();
			PlatformEvent event = new PlatformEvent();
			event.setType(type);
			event.setEvent(message);
			event.setUser(getUser(request));
			event.setRequestId(getExecutionId(request));
			event.setSessionId(getSessionId(request));
			event.setApplication(getApplication(request));
			event.setContext(getContext(request));
			try {
				event.setHostName(InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
			}
			if (null != request) {
				event.setOrigin(request.getRemoteHost());
			} else {
				event.setOrigin(event.getHostName());
			}
			return event;
		}

		public String getSessionId(HttpServletRequest servletRequest) {
			if (null != servletRequest) {
				return StringUtils.substring(servletRequest.getSession().getId(), 0, 8);
			}
			return null;
		}

		public String getUser(HttpServletRequest servletRequest) {
			if (null != servletRequest) {
				DefaultEnvironment env = DefaultEnvironment.get(servletRequest.getSession());
				Subject s = env.getAttribute(Scope.SESSION, Session.Environment.SUBJECT);
				if (s != null) {
					return s.getRealname();
				}
			}
			return auditUser;
		}

		public String getApplication(HttpServletRequest servletRequest) {
			if (null != servletRequest) {
				String contextPath = servletRequest.getContextPath();
				if (StringUtils.isBlank(contextPath)) {
					return "appNG";
				}
				return contextPath;
			}
			return null;
		}

		public String getContext(HttpServletRequest servletRequest) {
			if (null != servletRequest) {
				return servletRequest.getServletPath();
			}
			return null;
		}

		public HttpServletRequest getServletRequest() {
			RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
			return null == requestAttributes ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
		}
	}
}
