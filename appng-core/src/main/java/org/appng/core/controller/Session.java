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

/**
 * A simple value object representing a users's http-session.
 * 
 * @author Matthias Müller
 * 
 * @see SessionListener
 */
public class Session implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	private String domain;
	private String site;
	private String user;
	private String userAgent;
	private String ip;
	private int requests = 0;
	private Date creationTime;
	private Date lastAccessedTime;
	private int maxInactiveInterval;
	private boolean expire;
	private boolean allowExpire = true;

	public Session(String id) {
		this.id = id;
	}

	Session(String id, long creationTime, long lastAccessedTime, int maxInactiveInterval) {
		this.id = id;
		update(creationTime, lastAccessedTime, maxInactiveInterval);
	}

	void update(long creationtime, long lastAccessedTime, int maxInactiveInterval) {
		this.creationTime = new Date(creationtime);
		this.lastAccessedTime = new Date(lastAccessedTime);
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public Date getLastAccessedTime() {
		return lastAccessedTime;
	}

	public Date getExpiryDate() {
		return new Date(getLastAccessedTime().getTime() + getMaxInactiveInterval() * 1000);
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	void expire() {
		this.expire = true;
	}

	public boolean isExpired() {
		return expire;
	}

	public String getDomain() {
		return domain;
	}

	void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSite() {
		return site;
	}

	void setSite(String site) {
		this.site = site;
	}

	public String getUser() {
		return user;
	}

	void setUser(String user) {
		this.user = user;
	}

	public String getUserAgent() {
		return userAgent;
	}

	void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getIp() {
		return ip;
	}

	void setIp(String ip) {
		this.ip = ip;
	}

	public String getId() {
		return id;
	}

	public int getRequests() {
		return requests;
	}

	public void addRequest() {
		requests++;
	}

	public String getShortId() {
		return getId().substring(0, 8);
	}

	public boolean isAllowExpire() {
		return allowExpire;
	}

	public void setAllowExpire(boolean allowExpire) {
		this.allowExpire = allowExpire;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other == null ? false : (other instanceof Session) ? other.hashCode() == hashCode() : false;
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
		session.expire = expire;
		session.domain = domain;
		session.ip = ip;
		session.site = site;
		session.user = user;
		session.userAgent = userAgent;
		return session;
	}
}
