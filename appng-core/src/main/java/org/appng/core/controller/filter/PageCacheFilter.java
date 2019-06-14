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
package org.appng.core.controller.filter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.RequestUtil;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.api.support.HttpHeaderUtils;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.AppngCacheElement;
import org.appng.core.service.CacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.tuckey.web.filters.urlrewrite.gzip.GenericResponseWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Filter} which caches responses in form of an AppngCacheElement
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 *
 */
@Slf4j
public class PageCacheFilter implements javax.servlet.Filter {

	private FilterConfig filterConfig;
	private Set<String> cacheableHttpMethods = new HashSet<>(
			Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name()));

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) request;
		String servletPath = httpServletRequest.getServletPath();
		if (response.isCommitted()) {
			throw new IOException("The response has already been committed for servletPath: " + servletPath);
		}

		Environment env = DefaultEnvironment.get(filterConfig.getServletContext());
		String hostIdentifier = RequestUtil.getHostIdentifier(request, env);
		Site site = RequestUtil.getSiteByHost(env, hostIdentifier);
		boolean ehcacheEnabled = false;
		boolean isException = false;
		if (null != site) {
			ehcacheEnabled = site.getProperties().getBoolean(SiteProperties.EHCACHE_ENABLED);
			isException = isException(servletPath, site);
		} else {
			LOGGER.info("no site found for path {} and host {}", servletPath, hostIdentifier);
		}
		boolean isCacheableRequest = isCacheableRequest(httpServletRequest);
		if (ehcacheEnabled && isCacheableRequest && !isException) {
			handleCaching(httpServletRequest, httpServletResponse, site, chain, CacheService.getCache(site));
		} else {
			chain.doFilter(request, response);
		}
	}

	protected void handleCaching(final HttpServletRequest request, final HttpServletResponse response, Site site,
			final FilterChain chain, Cache<String, AppngCacheElement> cache) throws ServletException, IOException {
		try {
			AppngCacheElement pageInfo = buildPageInfo(request, response, chain, cache);
			if (null != pageInfo) {
				if (pageInfo.getStatus().equals(HttpStatus.OK) && response.isCommitted()) {
					throw new ServletException("Response already committed after doing buildPage"
							+ " but before writing response from PageInfo.");
				}
				long lastModified = pageInfo.getHeaders().getLastModified();

				boolean hasModifiedSince = StringUtils.isNotBlank(request.getHeader(HttpHeaders.IF_MODIFIED_SINCE));

				if (hasModifiedSince && lastModified > 0) {
					handleLastModified(request, response, pageInfo, lastModified);
				} else {
					writeResponse(request, response, pageInfo);
				}

			}
		} catch (CacheException e) {
			LOGGER.warn(String.format("error while adding/retrieving from/to cache: %s", calculateKey(request)), e);
		} catch (ClientAbortException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("client aborted request: %s", calculateKey(request)), e);
			}
		}
	}

	protected void writeResponse(HttpServletRequest request, HttpServletResponse response, AppngCacheElement pageInfo)
			throws IOException {
		byte[] body = new byte[0];

		HttpStatus status = pageInfo.getStatus();
		boolean shouldBodyBeZero = HttpStatus.NO_CONTENT.equals(status) || HttpStatus.NOT_MODIFIED.equals(status);
		if (shouldBodyBeZero) {
			body = new byte[0];
		} else if (acceptsGzipEncoding(request)) {
			// TODO handle gzip
//			body = pageInfo.getGzippedBody();
//			if (ResponseUtil.shouldGzippedBodyBeZero(body, request)) {
//				body = new byte[0];
//			} else {
//				ResponseUtil.addGzipHeader(response);
//			}

		} else {
			body = pageInfo.getData();
		}
		response.setStatus(pageInfo.getStatus().value());
		response.setContentLength(body.length);
		response.setContentType(pageInfo.getContentType());
		setHeaders(response, false, pageInfo);
		OutputStream out = new BufferedOutputStream(response.getOutputStream());
		out.write(body);
		out.flush();
	}

	private void setHeaders(HttpServletResponse response, boolean acceptsGzip, AppngCacheElement pageInfo) {
		pageInfo.getHeaders().forEach((n, vs) -> vs.forEach(v -> response.addHeader(n, v)));
	}

	private void handleLastModified(final HttpServletRequest request, final HttpServletResponse response,
			AppngCacheElement pageInfo, long lastModified) throws IOException {
		HttpHeaderUtils.handleModifiedHeaders(request, response, new HttpHeaderUtils.HttpResource() {

			public long update() throws IOException {
				return new Date(lastModified).getTime();
			}

			public boolean needsUpdate() {
				return false;
			}

			public byte[] getData() throws IOException {
				return pageInfo.getData();
			}

			public String getContentType() {
				return pageInfo.getContentType();
			}
		}, true);
		setHeaders(response, acceptsGzipEncoding(request), pageInfo);
	}

	static class CacheHeaderUtils extends HttpHeaderUtils {
		static Date getDate(String lastModified) {
			try {
				return StringUtils.isEmpty(lastModified) ? null : HTTP_DATE.parse(lastModified);
			} catch (ParseException e) {
			}
			return null;
		}
	}

	protected AppngCacheElement buildPageInfo(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, Cache<String, AppngCacheElement> cache) throws ServletException, IOException {
		final String key = calculateKey(request);

		AppngCacheElement pageInfo = cache.get(key);
		if (pageInfo == null) {
			pageInfo = buildPage(request, response, chain, cache);
			int size = pageInfo.getContentLength();
			if (pageInfo.getStatus().equals(HttpStatus.OK) && size > 0) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("PageInfo ok. Adding to cache {} with key {}", cache.getName(), key);
				}
				cache.put(key, pageInfo);
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("PageInfo was not ok (status: {}, size: {}) for key {}", pageInfo.getStatus(), key);
				}
			}

		}
		return pageInfo;
	}

	protected AppngCacheElement buildPage(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, Cache<String, AppngCacheElement> cache) throws IOException, ServletException {
		final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
		chain.doFilter(request, wrapper);
		wrapper.flush();

		HttpHeaders headers = new HttpHeaders();
		response.getHeaderNames().forEach(n -> response.getHeaders(n).forEach(v -> headers.add(n, v)));

		return new AppngCacheElement(wrapper.getStatus(), wrapper.getContentType(), outstr.toByteArray(), headers);
	}

	private boolean isCacheableRequest(HttpServletRequest httpServletRequest) {
		return cacheableHttpMethods.contains(httpServletRequest.getMethod().toUpperCase());
	}

	private boolean isException(String servletPath, Site site) {
		String clob = site.getProperties().getClob(SiteProperties.EHCACHE_EXCEPTIONS);
		if (null != clob) {
			List<String> exceptionList = Arrays.asList(clob.split("\n"));
			Set<String> exceptions = new HashSet<>(exceptionList);
			for (String e : exceptions) {
				if (servletPath.startsWith(e.trim())) {
					return true;
				}
			}
		}
		return false;
	}

	protected String calculateKey(final HttpServletRequest request) {
		StringBuilder keyBuilder = new StringBuilder(request.getMethod());
		keyBuilder = keyBuilder.append(request.getServletPath());
		String queryString = request.getQueryString();
		if (StringUtils.isNotBlank(queryString)) {
			keyBuilder = keyBuilder.append("?").append(queryString);
		}
		return keyBuilder.toString();
	}

	protected boolean acceptsGzipEncoding(HttpServletRequest request) {
		return StringUtils.containsIgnoreCase(request.getHeader(HttpHeaders.ACCEPT_ENCODING), "gzip");
	}

}
