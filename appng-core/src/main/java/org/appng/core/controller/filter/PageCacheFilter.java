/*
 * Copyright 2011-2023 the original author or authors.
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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Path;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.api.support.HttpHeaderUtils;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.CachedResponse;
import org.appng.core.service.CacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.tuckey.web.filters.urlrewrite.gzip.GenericResponseWrapper;
import org.tuckey.web.filters.urlrewrite.gzip.ResponseUtil;

import com.hazelcast.cache.ICache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Filter} which caches responses in form of an {@link CachedResponse}
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
public class PageCacheFilter implements javax.servlet.Filter {

	private static final String GZIP = "gzip";
	protected static final String CACHE_HIT = PageCacheFilter.class.getSimpleName() + ".cacheHit";
	private static final Set<String> CACHEABLE_HTTP_METHODS = new HashSet<>(
			Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name()));
	public static final Pattern EXPIRY_PATTERN = Pattern.compile("^(\\d+)(,(no-cache)?)?$");
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

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
		boolean isCacheableRequest = isCacheableRequest(httpServletRequest);
		boolean cacheEnabled = false;
		boolean isException = false;
		ExpiryPolicy expiryPolicy = null;
		DefaultEnvironment env = EnvironmentFilter.environment();
		Site site = env.getSite();
		boolean processed = false;

		if (isCacheableRequest) {
			if (null != site) {
				org.appng.api.model.Properties siteProps = site.getProperties();
				cacheEnabled = siteProps.getBoolean(SiteProperties.CACHE_ENABLED);
				if (cacheEnabled) {
					String exceptions = siteProps.getClob(SiteProperties.CACHE_EXCEPTIONS);
					isException = isException(exceptions, servletPath);

					if (!isException) {
						long start = System.currentTimeMillis();
						Properties cacheTimeouts = siteProps.getProperties(SiteProperties.CACHE_TIMEOUTS);
						boolean antStylePathMatching = siteProps.getBoolean(SiteProperties.CACHE_TIMEOUTS_ANT_STYLE);
						Integer defaultTtl = siteProps.getInteger(SiteProperties.CACHE_TIME_TO_LIVE);
						Expiry expiry = getExpiry(cacheTimeouts, antStylePathMatching, servletPath, defaultTtl);
						Duration expiryDuration = new Duration(TimeUnit.SECONDS, expiry.ttl);
						if (siteProps.getBoolean(SiteProperties.CACHE_EXPIRE_ELEMENTS_BY_CREATION, false)) {
							expiryPolicy = new CreatedExpiryPolicy(expiryDuration);
						} else {
							expiryPolicy = new AccessedExpiryPolicy(expiryDuration);
						}
						if (expiry.noCache) {
							org.appng.core.controller.HttpHeaders.setNoCache(httpServletResponse);
						}
						CachedResponse cachedResponse = handleCaching(httpServletRequest, httpServletResponse, site,
								chain, CacheService.getCache(site), expiryPolicy);
						processed = true;
						if (null != cachedResponse && LOGGER.isDebugEnabled()) {
							LOGGER.debug("Cache handling took {}ms (hit: {}, status: {}) for {}",
									System.currentTimeMillis() - start, httpServletRequest.getAttribute(CACHE_HIT),
									HttpStatus.valueOf(httpServletResponse.getStatus()), cachedResponse.getId());
						}
					}
				}
			} else {
				LOGGER.info("no site found for path {} and host {}", servletPath, request.getServerName());
			}
		}
		if (!processed) {
			chain.doFilter(request, response);
		}
	}

	protected CachedResponse handleCaching(final HttpServletRequest request, final HttpServletResponse response,
			Site site, final FilterChain chain, Cache<String, CachedResponse> cache, ExpiryPolicy expiryPolicy)
			throws ServletException, IOException {
		try {
			CachedResponse cachedResponse = getCachedResponse(request, response, chain, site, cache, expiryPolicy);
			if (null != cachedResponse) {
				if (cachedResponse.isOk() && response.isCommitted()) {
					throw new ServletException("Response already committed after doing buildPage"
							+ " but before writing response from PageInfo.");
				}
				if (cachedResponse.getStatus().is4xxClientError()) {
					response.setStatus(cachedResponse.getStatus().value());
				} else {
					long lastModified = cachedResponse.getHeaders().getLastModified();
					boolean hasModifiedSince = StringUtils.isNotBlank(request.getHeader(HttpHeaders.IF_MODIFIED_SINCE));
					if (hasModifiedSince && lastModified > 0) {
						handleLastModified(request, response, cachedResponse, lastModified);
					} else {
						writeResponse(request, response, cachedResponse);
					}
				}
				return cachedResponse;
			}
		} catch (CacheException e) {
			LOGGER.warn(String.format("error while adding/retrieving from/to cache: %s", calculateKey(request)), e);
		} catch (ClientAbortException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("client aborted request: %s", calculateKey(request)), e);
			}
		}
		return null;
	}

	protected void writeResponse(HttpServletRequest request, HttpServletResponse response, CachedResponse pageInfo)
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
				response.setHeader(HttpHeaders.CONTENT_ENCODING, GZIP);
			}
		} else {
			body = pageInfo.getData();
		}
		response.setStatus(pageInfo.getStatus().value());
		response.setContentLength(body.length);
		response.setContentType(pageInfo.getContentType());
		if (pageInfo.getHitCount() > 0) {
			writeCachedHeaders(response, pageInfo);
		}
		OutputStream out = new BufferedOutputStream(response.getOutputStream());
		out.write(body);
		out.flush();
	}

	private void writeCachedHeaders(HttpServletResponse response, CachedResponse pageInfo) {
		pageInfo.getHeaders().entrySet().stream().filter(e -> !e.getKey().equals(HttpHeaderUtils.X_APPNG_REQUIRED_ROLE))
				.forEach(e -> e.getValue().forEach(v -> response.setHeader(e.getKey(), v)));
	}

	private void handleLastModified(final HttpServletRequest request, final HttpServletResponse response,
			CachedResponse pageInfo, long lastModified) throws IOException {
		HttpHeaderUtils.handleModifiedHeaders(request, response, new HttpHeaderUtils.HttpResource() {

			public long update() throws IOException {
				return lastModified;
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
		writeCachedHeaders(response, pageInfo);
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

	@SuppressWarnings("unchecked")
	protected CachedResponse getCachedResponse(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, Site site, Cache<String, CachedResponse> cache, ExpiryPolicy expiryPolicy)
			throws ServletException, IOException {
		final String key = calculateKey(request);
		boolean cacheHit = false;
		CachedResponse cachedResponse = cache.get(key);
		if (cachedResponse == null) {
			cachedResponse = performRequest(request, response, chain, site, expiryPolicy);
			int size = cachedResponse.getContentLength();
			if (cachedResponse.isOk()) {
				cache.unwrap(ICache.class).put(key, cachedResponse, expiryPolicy);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Adding to cache {}: {} (type: {}, size: {}, ttl: {}s)", cache.getName(), key,
							cachedResponse.getContentType(), size, cachedResponse.getTimeToLive());
				}
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Response has status: {}, size: {} for key {}", cachedResponse.getStatus(), size, key);
			}
		} else {
			HttpStatus status = cachedResponse.getStatus();
			List<String> requiredRoles = cachedResponse.getHeaders().get(HttpHeaderUtils.X_APPNG_REQUIRED_ROLE);
			if (null != requiredRoles) {
				List<String> roles = EnvironmentFilter.environment().getAttribute(Scope.SESSION,
						Session.Environment.APPNG_ROLES);
				if (null != roles) {
					Collection<String> matchedRoles = CollectionUtils.intersection(requiredRoles, roles);
					if (matchedRoles.isEmpty()) {
						LOGGER.debug(
								"Resource required one of the role(s) [{}], none of the current role(s) [{}] did match!",
								StringUtils.join(requiredRoles, ", "), StringUtils.join(roles, ", "));
						status = HttpStatus.FORBIDDEN;
					} else {
						LOGGER.debug("Resource required one of the role(s) [{}], current role(s) [{}], matched [{}].",
								StringUtils.join(requiredRoles, ", "), StringUtils.join(roles, ", "),
								StringUtils.join(matchedRoles, ", "));
					}
				} else {
					LOGGER.debug("Resource required one of the role(s) [{}], but no roles where found.",
							StringUtils.join(requiredRoles, ", "));
					status = HttpStatus.UNAUTHORIZED;
				}
				if (status.is4xxClientError()) {
					return new CachedResponse(cachedResponse.getId(), site, request, status.value(), null, new byte[0],
							null, 0);
				}
			}

			cacheHit = true;
			long hits = cachedResponse.incrementHit();
			if (site.getProperties().getBoolean("cacheHitStats", false)) {
				cache.unwrap(ICache.class).replaceAsync(key, cachedResponse, expiryPolicy);
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Hit in cache {}: {} (type: {}, size: {}, hits: {})", cache.getName(), key,
						cachedResponse.getContentType(), cachedResponse.getContentLength(), hits);
			}
		}
		request.setAttribute(CACHE_HIT, cacheHit);
		return cachedResponse;
	}

	protected CachedResponse performRequest(final HttpServletRequest request, final HttpServletResponse response,
			final FilterChain chain, Site site, ExpiryPolicy expiryPolicy) throws IOException, ServletException {
		final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		final GenericResponseWrapper wrapper = new GenericResponseWrapper(response, outstr);
		chain.doFilter(request, wrapper);
		wrapper.flush();

		HttpHeaders headers = new HttpHeaders();
		wrapper.getHeaderNames().stream().filter(h -> !h.startsWith(HttpHeaders.SET_COOKIE))
				.forEach(n -> wrapper.getHeaders(n).forEach(v -> headers.add(n, v)));

		int ttl = addCacheControl(headers, expiryPolicy);
		return new CachedResponse(calculateKey(request), site, request, wrapper.getStatus(), wrapper.getContentType(),
				outstr.toByteArray(), headers, ttl);
	}

	protected int addCacheControl(HttpHeaders headers, ExpiryPolicy expiryPolicy) {
		Duration expiry = expiryPolicy.getExpiryForCreation();
		int ttl = (int) expiry.getTimeUnit().toSeconds(expiry.getDurationAmount());
		headers.add(HttpHeaders.CACHE_CONTROL, String.format("max-age=%s", ttl));
		return ttl;
	}

	private boolean isCacheableRequest(HttpServletRequest httpServletRequest) {
		return CACHEABLE_HTTP_METHODS.contains(httpServletRequest.getMethod().toUpperCase());
	}

	static boolean isException(String exceptionsProp, String servletPath) {
		if (null != exceptionsProp) {
			Optional<String> matchingRule = Arrays.asList(exceptionsProp.split(StringUtils.LF)).stream()
					.filter(e -> PATH_MATCHER.isPattern(e.trim()) ? PATH_MATCHER.match(e.trim(), servletPath)
							: servletPath.startsWith(e.trim()))
					.findFirst();
			if (matchingRule.isPresent()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("'{}' matched exception '{}'", servletPath, matchingRule.get());
				}
				return true;
			}
		}
		return false;
	}

	static Expiry getExpiry(Properties cachingTimes, boolean antStylePathMatching, String servletPath,
			Integer defaultValue) {
		Integer ttl = defaultValue;
		boolean noCache = false;
		Matcher matcher = null;
		outer: if (null != cachingTimes && !cachingTimes.isEmpty()) {
			if (antStylePathMatching) {
				for (Object path : cachingTimes.keySet()) {
					if (PATH_MATCHER.match(path.toString(), servletPath)) {
						String entry = (String) cachingTimes.get(path);
						matcher = EXPIRY_PATTERN.matcher(entry);
						break outer;
					}
				}
			} else {
				String[] pathSegements = servletPath.split(Path.SEPARATOR);
				int len = pathSegements.length;
				while (len > 0) {
					String segment = StringUtils.join(Arrays.copyOfRange(pathSegements, 0, len--), Path.SEPARATOR);
					String entry = (String) cachingTimes.get(segment);
					if (null != entry) {
						matcher = EXPIRY_PATTERN.matcher(entry);
						break outer;
					}
				}
			}

		}
		if (matcher != null && matcher.matches()) {
			ttl = Integer.valueOf(matcher.group(1));
			noCache = StringUtils.isNotBlank(matcher.group(3));
		}
		return new Expiry(ttl, noCache);
	}

	@Data
	@AllArgsConstructor
	static class Expiry {
		final Integer ttl;
		final boolean noCache;

	};

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
		return StringUtils.containsIgnoreCase(request.getHeader(HttpHeaders.ACCEPT_ENCODING), GZIP);
	}

}
