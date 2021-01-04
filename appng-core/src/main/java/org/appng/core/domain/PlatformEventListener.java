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
package org.appng.core.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Subject;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.PlatformStartup;
import org.appng.core.domain.PlatformEvent.Type;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * An entity listener that creates a new {@link PlatformEvent} on {@link PrePersist}, {@link PreUpdate} and
 * {@link PreRemove}. Also, a {@link PlatformEvent} can be created manually by calling
 * {@link #createEvent(Type, String)}. <br/>
 * Note that this listener is able to work in two scenarios. The first is as a regular Spring bean that can be invoked
 * from other beans. As a JPA entity listener, we use a static reference to the current {@link ApplicationContext} to
 * retrieve an instance of the {@link EntityManager} in use.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class PlatformEventListener implements ApplicationContextAware {

	private static ApplicationContext context;
	private static String auditUser = "<unknown>";
	private static String auditApplication = "appNG";
	private static boolean persist = true;
	@Autowired
	private EntityManager entityManager;

	private EventProvider eventProvider = new EventProvider();

	public PlatformEventListener() {

	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (null == context || PlatformStartup.APPNG_CONTEXT.equals(applicationContext.getDisplayName())) {
			PlatformEventListener.context = applicationContext;
			LOGGER.info("Using application context {}", applicationContext);
		}
	}

	@PrePersist
	public void beforeCreate(Auditable<?> o) {
		createEvent(PlatformEvent.Type.CREATE, o);
	}

	@PostUpdate
	public void onUpdate(Auditable<?> o) {
		createEvent(PlatformEvent.Type.UPDATE, o);
	}

	@PostRemove
	public void onDelete(Auditable<?> o) {
		createEvent(PlatformEvent.Type.DELETE, o);
	}

	private void createEvent(Type type, Auditable<?> auditable) {
		createEvent(type, String.format(auditable.getAuditName()));
	}

	public void createEvent(Type type, String message) {
		HttpServletRequest servletRequest = getServletRequest();
		HttpSession session = null == servletRequest ? null : servletRequest.getSession();
		createEvent(type, message, session, servletRequest);
	}

	public void createEvent(Type type, String message, HttpSession session) {
		createEvent(type, message, session, null);
	}

	private void createEvent(Type type, String message, HttpSession session, HttpServletRequest request) {
		PlatformEvent event = getEventProvider().provide(type, message, session, request);
		if (persist) {
			if (null == entityManager) {
				context.getAutowireCapableBeanFactory().autowireBean(this);
			}
			PlatformTransactionManager ptam = context.getBean(PlatformTransactionManager.class);
			TransactionTemplate transactionTemplate = new TransactionTemplate(ptam);
			transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			transactionTemplate.execute(status -> {
				entityManager.persist(event);
				return null;
			});
		}
		LOGGER.info("Created entry {}", event);
	}

	public synchronized void setAuditUser(String auditUser) {
		PlatformEventListener.auditUser = auditUser;
	}

	public synchronized void setAuditApplication(String auditApplication) {
		PlatformEventListener.auditApplication = auditApplication;
	}

	public synchronized void setPersist(boolean persist) {
		PlatformEventListener.persist = persist;
	}

	public EventProvider getEventProvider() {
		return context.getBeansOfType(EventProvider.class).isEmpty() ? eventProvider
				: context.getBean(EventProvider.class);
	}

	public void setEventProvider(EventProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

	public static class EventProvider {
		public static final String EVENT_UUID = "eventUUID";

		String getExecutionId(HttpServletRequest servletRequest) {
			if (null != servletRequest) {
				String eventUUID = (String) servletRequest.getAttribute(EVENT_UUID);
				if (null == eventUUID) {
					servletRequest.setAttribute(EVENT_UUID, UUID.randomUUID().toString());
				}
				return (String) servletRequest.getAttribute(EVENT_UUID);
			}
			return UUID.randomUUID().toString();
		}

		PlatformEvent provide(Type type, String message, HttpSession session, HttpServletRequest request) {
			PlatformEvent event = new PlatformEvent();
			event.setType(type);
			event.setEvent(message);
			event.setUser(getUser(session));
			event.setRequestId(getExecutionId(request));
			event.setSessionId(getSessionId(session));
			event.setApplication(getApplication());
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

		private String getSessionId(HttpSession session) {
			if (null != session) {
				return StringUtils.substring(session.getId(), 0, 8);
			}
			return null;
		}

		protected String getUser(HttpSession session) {
			Subject s = null;
			if (null != session) {
				s = DefaultEnvironment.get(session).getAttribute(Scope.SESSION, Session.Environment.SUBJECT);
			}
			return s == null ? auditUser : s.getRealname();
		}

		protected String getApplication() {
			return auditApplication;
		}

		protected String getContext(HttpServletRequest request) {
			return null == request ? null : request.getServletPath();
		}

	}

	private HttpServletRequest getServletRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		return null == requestAttributes ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
	}
}
