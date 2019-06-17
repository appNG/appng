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
import org.appng.core.controller.AppngCache;
import org.appng.core.service.CacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.tuckey.web.filters.urlrewrite.gzip.GenericResponseWrapper;
import org.tuckey.web.filters.urlrewrite.gzip.ResponseUtil;

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
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
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
			final FilterChain chain, Cache<String, AppngCache> cache) throws ServletException, IOException {
		try {
			AppngCache pageInfo = getCacheElement(request, response, chain, site, cache);
			if (null != pageInfo) {
				if (pageInfo.isOk() && response.isCommitted()) {
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

	protected void writeResponse(HttpServletRequest request, HttpServletResponse response, AppngCache pageInfo)
			throws IOException {
		byte[] body = new byte[0];

		HttpStatus status = pageInfo.getStatus();
		boolean shouldBodyBeZero = ResponseUtil.shouldBodyBeZero(request, status.value());
		boolean acceptGzip = acceptsGzipEncoding(request);
		if (shouldBodyBeZero) {
			body = new byte[0];
		} else if (acceptGzip) {
			if (ResponseUtil.shouldGzippedBodyBeZero(body, request)) {
				body = new byte[0];
			} else {
				body = pageInfo.getGzippedBody();
			}
		} else {
			body = pageInfo.getData();
		}
		response.setStatus(pageInfo.getStatus().value());
		response.setContentLength(body.length);
		response.setContentType(pageInfo.getContentType());
		if (pageInfo.getHitCount() > 0) {
			writeCachedHeaders(response, acceptGzip, pageInfo);
		}
		OutputStream out = new BufferedOutputStream(response.getOutputStream());
		out.write(body);
		out.flush();
	}

	private void writeCachedHeaders(HttpServletResponse response, boolean acceptsGzip, AppngCache pageInfo) {
		pageInfo.getHeaders().forEach((n, vs) -> vs.forEach(v -> response.setHeader(n, v)));
		if (acceptsGzip) {
			response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
		}
	}

	private void handleLastModified(final HttpServletRequest request, final HttpServletResponse response,
			AppngCache pageInfo, long lastModified) throws IOException {
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
		writeCachedHeaders(response, acceptsGzipEncoding(request), pageInfo);
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

	protected AppngCache getCacheElement(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, Site site, Cache<String, AppngCache> cache) throws ServletException, IOException {
		final String key = calculateKey(request);
		AppngCache pageInfo = cache.get(key);
		if (pageInfo == null) {
			pageInfo = performRequest(request, response, chain, site, cache);
			int size = pageInfo.getContentLength();
			if (pageInfo.isOk()) {
				cache.put(key, pageInfo);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Adding to cache {}: {} (type: {}, size: {})", cache.getName(), key,
							pageInfo.getContentType(), size);
				}
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("PageInfo was not ok (status: {}, size: {}) for key {}", pageInfo.getStatus(), size,
							key);
				}
			}
		} else {
			// update hit statistic
			long hits = pageInfo.incrementHit();
			cache.replace(key, pageInfo);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Hit in cache {}: {} (type: {}, size: {}, hits: {})", cache.getName(), key,
						pageInfo.getContentType(), pageInfo.getContentLength(), hits);
			}
		}
		return pageInfo;
	}

	protected AppngCache performRequest(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, Site site, Cache<String, AppngCache> cache) throws IOException, ServletException {
		final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
		chain.doFilter(request, wrapper);
		wrapper.flush();

		HttpHeaders headers = new HttpHeaders();
		wrapper.getHeaderNames().stream().filter(h -> !h.startsWith(HttpHeaders.SET_COOKIE))
				.forEach(n -> wrapper.getHeaders(n).forEach(v -> headers.add(n, v)));
		Integer ttl = site.getProperties().getInteger("cacheTimeToLife", 1800);

		return new AppngCache(calculateKey(request), site, request, wrapper.getStatus(), wrapper.getContentType(),
				outstr.toByteArray(), headers, ttl);
	}

	private boolean isCacheableRequest(HttpServletRequest httpServletRequest) {
		return cacheableHttpMethods.contains(httpServletRequest.getMethod().toUpperCase());
	}

	private boolean isException(String servletPath, Site site) {
		String clob = site.getProperties().getClob(SiteProperties.EHCACHE_EXCEPTIONS);
		if (null != clob) {
			Set<String> exceptions = new HashSet<>(Arrays.asList(clob.split(StringUtils.LF)));
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
