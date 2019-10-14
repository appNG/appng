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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.model.Site;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import lombok.Data;

/**
 * A simple value object representing an element of the site cache. Instances of this class will be put into the cache.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Data
public class AppngCache implements Serializable {

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

	public long incrementHit() {
		calculateExpire(this.lastAccessedTime = new Date());
		return hits.incrementAndGet();
	}

	public AppngCache(String id, Site site, HttpServletRequest request, int status, String contentType, byte[] data,
			HttpHeaders headers, int timeToLive) {
		this.id = id;
		this.status = HttpStatus.valueOf(status);
		this.contentType = contentType;
		this.data = data;
		this.headers = headers;
		this.contentLength = data.length;
		this.timeToLive = timeToLive;
		this.site = site.getName();
		this.domain = site.getDomain();
		calculateExpire(this.creationTime = new Date());
		this.servletPath = request.getServletPath();
		this.queryString = request.getQueryString();
	}

	private void calculateExpire(Date baseline) {
		this.expirationTime = DateUtils.addSeconds(baseline, timeToLive);
	}

	public long getHitCount() {
		return hits.get();
	}

	public Date getCreatedOrUpdated() {
		return creationTime;
	}

	public boolean isOk() {
		return status.equals(HttpStatus.OK) && contentLength > 0;
	}

	public byte[] getGzippedBody() throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		final GZIPOutputStream gzipped = new GZIPOutputStream(bytes);
		gzipped.write(data);
		gzipped.close();
		return bytes.toByteArray();
	}

	@Override
	public String toString() {
		return getId();
	}

}
