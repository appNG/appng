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

import java.io.Serializable;
import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * A simple value object representing a users's http-session.
 * 
 * @author Matthias Müller
 * 
 * @see SessionListener
 */
@Getter
@EqualsAndHashCode(of = "id")
public class Session implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	private @Setter String domain;
	private @Setter String site;
	private @Setter String user;
	private @Setter String userAgent;
	private @Setter String ip;
	private int requests = 0;
	private Date creationTime;
	private Date lastAccessedTime;
	private int maxInactiveInterval;
	private boolean expired;
	private boolean allowExpire = true;

	public Session(String id) {
		this.id = id;
	}

	Session(String id, long creationTime, long lastAccessedTime, int maxInactiveInterval) {
		this.id = id;
		update(creationTime, lastAccessedTime, maxInactiveInterval);
	}

	public void update(long creationtime, long lastAccessedTime, int maxInactiveInterval) {
		this.creationTime = new Date(creationtime);
		this.lastAccessedTime = new Date(lastAccessedTime);
		this.maxInactiveInterval = maxInactiveInterval;
	}

	void expire() {
		this.expired = true;
	}

	public void addRequest() {
		requests++;
	}
	
	public Date getExpiryDate() {
		return new Date(getLastAccessedTime().getTime() + getMaxInactiveInterval() * 1000);
	}

	public void setAllowExpire(boolean allowExpire) {
		this.allowExpire = allowExpire;
	}

	public String getShortId() {
		return getId().substring(0, 8);
	}

	@Override
	public String toString() {
		return getShortId();
	}

	@Override
	public Session clone() {
		Session session = new Session(id);
		session.allowExpire = allowExpire;
		session.creationTime = creationTime;
		session.lastAccessedTime = lastAccessedTime;
		session.maxInactiveInterval = maxInactiveInterval;
		session.requests = requests;
		session.expired = expired;
		session.domain = domain;
		session.ip = ip;
		session.site = site;
		session.user = user;
		session.userAgent = userAgent;
		return session;
	}
}
