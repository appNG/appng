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
package org.appng.core.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.servlet.http.HttpServletRequest;

import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Subject;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.PlatformEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * An entity listener that creates a new {@link PlatformEvent} on {@link PostPersist}, {@link PostUpdate} and
 * {@link PreRemove}. Also, a {@link PlatformEvent} can be created manually by calling
 * {@link #createEvent(Type, String)}
 * 
 * @author Matthias Müller
 *
 */
public class PlatformEventListener implements ApplicationContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(PlatformEventListener.class);
	private static ApplicationContext CONTEXT;
	private static String auditUser = "<unknown>";
	private static boolean persist = true;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		PlatformEventListener.CONTEXT = applicationContext;
	}

	private EntityManager getEntityManager() {
		return PlatformEventListener.CONTEXT.getBean("entityManager", EntityManager.class);
	}

	@PostPersist
	public void afterCreate(Object o) {
		createEvent(o, PlatformEvent.Type.CREATE);
	}

	@PostUpdate
	public void afterUpdate(Object o) {
		createEvent(o, PlatformEvent.Type.UPDATE);
	}

	@PreRemove
	public void afterDelete(Object o) {
		createEvent(o, PlatformEvent.Type.DELETE);
	}

	protected String getSubjectName() {
		HttpServletRequest request = getServletRequest();
		if (null != request) {
			DefaultEnvironment env = DefaultEnvironment.get(request.getSession());
			Subject s = env.getAttribute(Scope.SESSION, Session.Environment.SUBJECT);
			if (s != null) {
				return s.getRealname();
			}
		}
		return auditUser;
	}

	private void createEvent(Object o, Type type) {
		Auditable<?> auditable = (Auditable<?>) o;
		String message = String.format("%s %s (ID: %s)", auditable.getAuditName(), auditable.getName(),
				auditable.getId());
		createEvent(auditable, type, message);
	}

	private void createEvent(Auditable<?> auditable, Type type, String message) {
		createEvent(type, message, auditable.getVersion());
	}

	public void createEvent(Type type, String message) {
		createEvent(type, message, new Date());
	}

	private void createEvent(Type type, String message, Date created) {
		PlatformEvent event = new PlatformEvent();
		event.setType(type);
		event.setUser(getSubjectName());
		event.setCreated(created);
		event.setEvent(message);
		try {
			event.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {

		}
		HttpServletRequest request = getServletRequest();
		if (null != request) {
			event.setContextPath(request.getContextPath());
			event.setHost(request.getLocalName());
			event.setServletPath(request.getServletPath());
		}
		if (persist) {
			PlatformTransactionManager txMngr = CONTEXT.getBean(PlatformTransactionManager.class);
			TransactionStatus tx = txMngr.getTransaction(null);
			getEntityManager().persist(event);
			if (tx.isNewTransaction()) {
				txMngr.commit(tx);
			}
		}
		LOG.info("Created entry {}", event);
	}

	private HttpServletRequest getServletRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		return null == requestAttributes ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
	}

	public void setAuditUser(String auditUser) {
		PlatformEventListener.auditUser = auditUser;
	}

	public void setPersist(boolean persist) {
		PlatformEventListener.persist = persist;
	}

}
