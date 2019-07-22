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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.DataFormatException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.RequestUtil;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.api.support.HttpHeaderUtils;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.service.CacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.web.AlreadyCommittedException;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.filter.CachingFilter;
import net.sf.ehcache.constructs.web.filter.FilterNonReentrantException;

/**
 * A {@link Filter} which caches responses based on the request. Largely based on {@link CachingFilter}
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 *
 */
@Slf4j
public class PageCacheFilter extends CachingFilter {

	private Set<String> cacheableHttpMethods = new HashSet<>(
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
			handleCaching(request, response, site, chain, CacheService.getBlockingCache(site));
		} else {
			chain.doFilter(request, response);
		}
	}

	protected void handleCaching(final HttpServletRequest request, final HttpServletResponse response, Site site,
			final FilterChain chain, BlockingCache blockingCache) throws Exception, IOException, DataFormatException {
		try {
			logRequestHeaders(request);
			PageInfo pageInfo = buildPageInfo(request, response, chain, blockingCache);
			if (null != pageInfo) {
				if (pageInfo.isOk() && response.isCommitted()) {
					throw new AlreadyCommittedException("Response already committed after doing buildPage"
							+ " but before writing response from PageInfo.");
				}
				Optional<Header<? extends Serializable>> lastModified = pageInfo.getHeaders().stream()
						.filter(h -> h.getName().equalsIgnoreCase(HttpHeaders.LAST_MODIFIED)).findFirst();
				boolean hasModifiedSince = StringUtils.isNotBlank(request.getHeader(HttpHeaders.IF_MODIFIED_SINCE));

				if (hasModifiedSince && lastModified.isPresent()) {
					handleLastModified(request, response, pageInfo, lastModified);
				} else {
					writeResponse(request, response, pageInfo);
				}

			}
		} catch (CacheException e) {
			LOGGER.warn(String.format("error while adding/retrieving from/to cache: %s", calculateKey(request)), e);
		} catch (ClientAbortException e) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("client aborted request: %s", calculateKey(request)), e);
			}
		}
	}

	private void handleLastModified(final HttpServletRequest request, final HttpServletResponse response,
			PageInfo pageInfo, Optional<Header<? extends Serializable>> lastModified) throws IOException {
		HttpHeaderUtils.handleModifiedHeaders(request, response, new HttpHeaderUtils.HttpResource() {

			public long update() throws IOException {
				return CacheHeaderUtils.getDate((String) lastModified.get().getValue()).getTime();
			}

			public boolean needsUpdate() {
				return false;
			}

			public byte[] getData() throws IOException {
				return pageInfo.getUngzippedBody();
			}

			public String getContentType() {
				return pageInfo.getContentType();
			}
		}, true);
		setCookies(pageInfo, response);
		setHeaders(pageInfo, acceptsGzipEncoding(request), response);
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
					boolean filterNotDisabled = filterNotDisabled(request);
					if (pageInfo.isOk() && size > 0 && filterNotDisabled) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("PageInfo ok. Adding to cache {} with key {}", blockingCache.getName(), key);
						}
						blockingCache.put(new Element(key, pageInfo));
					} else {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(
									"PageInfo was not ok (status: {}, size: {}, caching disabled: {}). Putting null into cache {} with key {}",
									pageInfo.getStatusCode(), size, !filterNotDisabled, blockingCache.getName(), key);
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
