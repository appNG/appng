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
package org.appng.core.controller.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.RequestUtil;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.web.AlreadyCommittedException;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.filter.CachingFilter;
import net.sf.ehcache.constructs.web.filter.FilterNonReentrantException;

/**
 * A {@link Filter} which caches responses based on the request. Largely based on {@link CachingFilter}
 * 
 * @author Matthias Herlitzius
 *
 */
public class PageCacheFilter extends CachingFilter {

	private static Logger LOG = LoggerFactory.getLogger(PageCacheFilter.class);
	private Set<String> cacheableHttpMethods = new HashSet<String>(
			Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name()));

	@Override
	protected void doFilter(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain) throws AlreadyGzippedException, AlreadyCommittedException,
			FilterNonReentrantException, LockTimeoutException, Exception {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String servletPath = httpServletRequest.getServletPath();
		if (response.isCommitted()) {
			throw new IOException("The response has already been committed for servletPath: " + servletPath);
		}

		ServletContext servletContext = filterConfig.getServletContext();
		Environment env = DefaultEnvironment.get(servletContext, request, response);
		String hostIdentifier = RequestUtil.getHostIdentifier(request, env);
		Site site = RequestUtil.getSiteByHost(env, hostIdentifier);
		boolean ehcacheEnabled = false;
		boolean isException = false;
		if (null != site) {
			ehcacheEnabled = site.getProperties().getBoolean(SiteProperties.EHCACHE_ENABLED);
			isException = isException(servletPath, site);
		} else {
			LOG.info("no site found for path {} and host {}", servletPath, hostIdentifier);
		}
		boolean isCacheableRequest = isCacheableRequest(httpServletRequest);
		if (ehcacheEnabled && isCacheableRequest && !isException) {
			try {
				logRequestHeaders(request);
				BlockingCache blockingCache = CacheService.getBlockingCache(site);
				PageInfo pageInfo = buildPageInfo(request, response, chain, blockingCache);
				if (null != pageInfo && pageInfo.isOk()) {
					if (response.isCommitted()) {
						throw new AlreadyCommittedException("Response already committed after doing buildPage"
								+ " but before writing response from PageInfo.");
					}
					writeResponse(request, response, pageInfo);
				}
			} catch (CacheException e) {
				LOG.warn("error while adding/retrieving from/to cache: " + calculateKey(request), e);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	protected PageInfo buildPageInfo(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, BlockingCache blockingCache) throws Exception {
		// Look up the cached page
		final String key = calculateKey(request);
		PageInfo pageInfo = null;
		try {
			Element element = blockingCache.get(key);
			if (element == null || element.getObjectValue() == null) {
				try {
					// Page is not cached - build the response, cache it, and
					// send to client
					pageInfo = buildPage(request, response, chain, blockingCache);
					int size = ArrayUtils.getLength(pageInfo.getUngzippedBody());
					if (pageInfo.isOk() && size > 0) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("PageInfo ok. Adding to cache {} with key {}", blockingCache.getName(), key);
						}
						blockingCache.put(new Element(key, pageInfo));
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("PageInfo was not ok ({}, size: {}). Putting null into cache {} with key {}",
									pageInfo.getStatusCode(), size, blockingCache.getName(), key);
						}
						blockingCache.put(new Element(key, null));
					}
				} catch (final Throwable throwable) {
					// Must unlock the cache if the above fails. Will be logged
					// at Filter
					blockingCache.put(new Element(key, null));
					throw new Exception(throwable);
				}
			} else {
				pageInfo = (PageInfo) element.getObjectValue();
			}
		} catch (CacheException e) {
			// do not release the lock, because you never acquired it
			throw e;
		}
		return pageInfo;
	}

	protected PageInfo buildPage(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, BlockingCache blockingCache) throws AlreadyGzippedException, Exception {

		// Invoke the next entity in the chain
		final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
		chain.doFilter(request, wrapper);
		wrapper.flush();

		long timeToLiveSeconds = blockingCache.getCacheConfiguration().getTimeToLiveSeconds();

		// Return the page info
		return new PageInfo(wrapper.getStatus(), wrapper.getContentType(), wrapper.getCookies(), outstr.toByteArray(),
				true, timeToLiveSeconds, wrapper.getAllHeaders());
	}

	private boolean isCacheableRequest(HttpServletRequest httpServletRequest) {
		return cacheableHttpMethods.contains(httpServletRequest.getMethod().toUpperCase());
	}

	private boolean isException(String servletPath, Site site) {
		String clob = site.getProperties().getClob(SiteProperties.EHCACHE_EXCEPTIONS);
		if (null != clob) {
			List<String> exceptionList = Arrays.asList(clob.split("\n"));
			Set<String> exceptions = new HashSet<String>(exceptionList);
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

	protected PageInfo buildPageInfo(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain) throws Exception {
		throw new UnsupportedOperationException("This method is not supported in " + this.getClass().getName());
	}

	protected PageInfo buildPage(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain) throws AlreadyGzippedException, Exception {
		throw new UnsupportedOperationException("This method is not supported in " + this.getClass().getName());
	}

	public void doInit(FilterConfig filterConfig) throws CacheException {

	}

	protected CacheManager getCacheManager() {
		return CacheService.getCacheManager();
	}

}
