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

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import lombok.Data;

/**
 * A simple value object representing an element of the site cache. Instances of
 * this class will be put into the cache.
 * 
 * @author Matthias Herlitzius
 */
@Data
public class AppngCacheElement implements Serializable {

	private String id;
	private String site;
	private String domain;
	private AtomicLong hits = new AtomicLong(0);
	private Date creationTime;
	private Date lastAccessedTime;
	private Date expirationTime;
	private HttpStatus status;
	private int timeToLive;
	protected String servletPath;
	protected String queryString;
	protected String contentType;
	protected int contentLength;
	protected HttpHeaders headers;
	protected byte[] data;

	public void incrementHit() {
		hits.incrementAndGet();
	}

	public AppngCacheElement(int status, String contentType, byte[] data, HttpHeaders headers) {
		this.status = HttpStatus.valueOf(status);
		this.contentType = contentType;
		this.data = data;
		this.headers = headers;
		this.contentLength = data.length;
	}

	@Override
	public String toString() {
		return getId();
	}

}
