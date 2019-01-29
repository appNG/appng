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
import java.util.Date;

import org.appng.api.model.Site;

import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.web.PageInfo;

/**
 * A simple value object representing an element of the site cache. Instances of this class will be used to display
 * cache elements in the appNG Manager.
 * 
 * @author Matthias Herlitzius
 */
public class AppngCache extends CacheElementBase {

	private String id;
	private String site;
	private String domain;
	private long hitCount;
	private Date creationTime;
	private Date lastAccessedTime;
	private Date lastUpdateTime;
	private Date createdOrUpdated;
	private Date expirationTime;
	private int timeToLive;
	private int timeToIdle;

	public AppngCache(Object key, Site site, PageInfo ehcachePageInfo, Element element) throws IOException {
		this.id = String.valueOf(key);
		this.site = site.getName();
		this.domain = site.getDomain();
		this.hitCount = element.getHitCount();
		this.creationTime = new Date(element.getCreationTime());
		this.lastAccessedTime = new Date(element.getLastAccessTime());
		this.lastUpdateTime = new Date(element.getLastUpdateTime());
		this.createdOrUpdated = new Date(element.getLatestOfCreationAndUpdateTime());
		this.expirationTime = new Date(element.getExpirationTime());
		this.timeToLive = element.getTimeToLive();
		this.timeToIdle = element.getTimeToIdle();
		this.contentType = ehcachePageInfo.getContentType();
		this.contentLength = ehcachePageInfo.getUngzippedBody().length;
	}

	public String getId() {
		return id;
	}

	public String getSite() {
		return site;
	}

	public long getHitCount() {
		return hitCount;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public Date getLastAccessedTime() {
		return lastAccessedTime;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public Date getCreatedOrUpdated() {
		return createdOrUpdated;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public int getTimeToLive() {
		return timeToLive;
	}

	public int getTimeToIdle() {
		return timeToIdle;
	}

	public String getDomain() {
		return domain;
	}

	@Override
	public String toString() {
		return getId();
	}

}
