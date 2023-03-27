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
package org.appng.core.controller.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.VHostMode;
import org.appng.api.model.Site;
import org.appng.api.support.HttpHeaderUtils;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.CachedResponse;
import org.appng.core.service.CacheService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hazelcast.config.CacheConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class PageCacheFilterTest {

	@Test
	public void test() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest req = new MockHttpServletRequest(servletContext);
		req.setServletPath("/foo/bar");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		// setup environment
		org.appng.api.model.Properties props = Mockito.mock(org.appng.api.model.Properties.class);
		Mockito.when(props.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		Map<Object, Object> platform = new ConcurrentHashMap<>();
		platform.put(Platform.Environment.PLATFORM_CONFIG, props);
		servletContext.setAttribute(Scope.PLATFORM.name(), platform);
		ServletRequestAttributes attributes = new ServletRequestAttributes(req, resp);
		RequestContextHolder.setRequestAttributes(attributes);
		DefaultEnvironment env = new DefaultEnvironment(req, resp);
		attributes.setAttribute(Environment.class.getName(), env, RequestAttributes.SCOPE_REQUEST);

		HazelcastInstance hz = Hazelcast.newHazelcastInstance();
		CacheConfig<String, CachedResponse> configuration = new CacheConfig<>("testcache");
		CacheManager cacheManager = CacheService.createCacheManager(hz, false);
		Cache<String, CachedResponse> cache = cacheManager.createCache("testcache", configuration);

		FilterChain chain = Mockito.mock(FilterChain.class);
		String modifiedDate = "Wed, 28 Mar 2018 09:04:12 GMT";
		String content = "foobar";
		long lastModifiedSeconds = 1522227852000L;
		PageCacheFilter pageCacheFilter = new PageCacheFilter() {
			@Override
			protected CachedResponse performRequest(final HttpServletRequest request,
					final HttpServletResponse response, final FilterChain chain, Site site, ExpiryPolicy expiryPolicy)
					throws IOException, ServletException {
				chain.doFilter(request, response);
				response.addDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedSeconds);
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaderUtils.X_APPNG_REQUIRED_ROLE, "user");
				headers.add(HttpHeaderUtils.X_APPNG_REQUIRED_ROLE, "viewer");
				addCacheControl(headers, expiryPolicy);
				headers.setLastModified(lastModifiedSeconds);
				return new CachedResponse("GET/" + req.getServletPath(), site, request, 200, "text/plain",
						content.getBytes(), headers, 1800);
			};

			@Override
			protected void writeResponse(HttpServletRequest request, HttpServletResponse response,
					CachedResponse pageInfo) throws IOException {
				if ("/aborted".equals(request.getServletPath())) {
					throw new ClientAbortException("aborted!");
				}
				super.writeResponse(request, response, pageInfo);
			}
		};

		Site site = Mockito.mock(Site.class);
		org.appng.api.model.Properties siteProps = Mockito.mock(org.appng.api.model.Properties.class);
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(siteProps.getBoolean("cacheHitStats", false)).thenReturn(true);

		AccessedExpiryPolicy expiryPolicy = new AccessedExpiryPolicy(new Duration(TimeUnit.SECONDS, 30));
		CachedResponse pageInfo = pageCacheFilter.getCachedResponse(req, resp, chain, site, cache, expiryPolicy);
		Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.eq(resp));
		Assert.assertEquals(0, pageInfo.getHitCount());
		Assert.assertEquals(modifiedDate, resp.getHeader(HttpHeaders.LAST_MODIFIED));
		Assert.assertEquals(lastModifiedSeconds, resp.getDateHeader(HttpHeaders.LAST_MODIFIED));
		Assert.assertEquals(false, req.getAttribute(PageCacheFilter.CACHE_HIT));

		// test not authorized
		CachedResponse unauthorizde = pageCacheFilter.getCachedResponse(req, resp, chain, site, cache, expiryPolicy);
		Assert.assertEquals(HttpStatus.UNAUTHORIZED, unauthorizde.getStatus());

		// test insufficient roles
		env.setAttribute(Scope.SESSION, Session.Environment.APPNG_ROLES, Arrays.asList("notallowed"));
		CachedResponse insufficient = pageCacheFilter.getCachedResponse(req, resp, chain, site, cache, expiryPolicy);
		Assert.assertEquals(HttpStatus.FORBIDDEN, insufficient.getStatus());

		// test hit
		env.setAttribute(Scope.SESSION, Session.Environment.APPNG_ROLES, Arrays.asList("user"));
		CachedResponse cacheHit = pageCacheFilter.getCachedResponse(req, resp, chain, site, cache, expiryPolicy);
		Assert.assertEquals(pageInfo.getId(), cacheHit.getId());
		Assert.assertEquals(pageInfo.getHeaders(), cacheHit.getHeaders());
		Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.eq(resp));

		Assert.assertEquals(1, cacheHit.getHitCount());
		Assert.assertEquals("max-age=30", cacheHit.getHeaders().getCacheControl());
		Assert.assertEquals(true, req.getAttribute(PageCacheFilter.CACHE_HIT));

		// test gzip
		req.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
		pageCacheFilter.handleCaching(req, resp, site, chain, cache, expiryPolicy);
		Assert.assertEquals(HttpStatus.OK.value(), resp.getStatus());
		Assert.assertEquals(26, resp.getContentLength());
		Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.eq(resp));

		// test if-modified-since
		MockHttpServletResponse response = new MockHttpServletResponse();
		req.addHeader(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate);
		pageCacheFilter.handleCaching(req, response, site, chain, cache, expiryPolicy);
		Assert.assertEquals(HttpStatus.NOT_MODIFIED.value(), response.getStatus());
		Assert.assertEquals(0, response.getContentLength());
		Mockito.verify(chain, Mockito.times(0)).doFilter(Mockito.any(), Mockito.eq(response));

		// test aborted
		MockHttpServletRequest aborted = new MockHttpServletRequest(servletContext);
		aborted.setServletPath("/aborted");
		MockHttpServletResponse abortedResponse = new MockHttpServletResponse();
		pageCacheFilter.handleCaching(aborted, abortedResponse, Mockito.mock(Site.class), chain, cache, expiryPolicy);
		Assert.assertEquals(HttpStatus.OK.value(), abortedResponse.getStatus());
		Assert.assertEquals(0, abortedResponse.getContentLength());
		Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.eq(abortedResponse));
	}

	@Test
	public void testIsException() {
		String servletPath = "/foo/bar/lore/ipsum";
		Assert.assertFalse(PageCacheFilter.isException("/foo/me", servletPath));
		Assert.assertTrue(PageCacheFilter.isException(servletPath, servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/bar/lore/", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/bar", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/", servletPath));
	}

	@Test
	public void testIsExceptionPathMatch() {
		String servletPath = "/foo/bar/lore/ipsum";
		Assert.assertTrue(PageCacheFilter.isException(servletPath, servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/bar/lore/ip?um", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/bar/*/*", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/bar/**", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/**", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/*/lore/*", servletPath));
		Assert.assertTrue(PageCacheFilter.isException("/foo/**/ipsum", servletPath));
	}

	@Test
	public void testGetExpireAfterSeconds() {
		Integer defaultCacheTime = Integer.valueOf(1800);
		Integer oneMin = Integer.valueOf(60);
		Integer tenMins = Integer.valueOf(600);
		Integer oneHour = Integer.valueOf(3600);
		Properties cachingTimes = new Properties();
		String servletPath = "/foo/bar/lore/ipsum";

		Integer expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, false, servletPath,
				defaultCacheTime);
		Assert.assertEquals(defaultCacheTime, expireAfterSeconds);

		cachingTimes.put("", oneMin);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, false, servletPath, defaultCacheTime);
		Assert.assertEquals(oneMin, expireAfterSeconds);

		cachingTimes.put(servletPath, oneMin);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, false, servletPath, defaultCacheTime);
		Assert.assertEquals(oneMin, expireAfterSeconds);

		cachingTimes.clear();
		cachingTimes.put("/foo/bar/lore", tenMins);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, false, servletPath, defaultCacheTime);
		Assert.assertEquals(tenMins, expireAfterSeconds);

		cachingTimes.clear();
		cachingTimes.put("/foo", oneHour);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, false, servletPath, defaultCacheTime);
		Assert.assertEquals(oneHour, expireAfterSeconds);
	}

	@Test
	public void testGetExpireAfterSecondsAntStyle() {
		Integer defaultCacheTime = Integer.valueOf(1800);
		Integer oneMin = Integer.valueOf(60);
		Integer tenMins = Integer.valueOf(600);
		Integer oneHour = Integer.valueOf(3600);
		Properties cachingTimes = new Properties();
		String servletPath = "/foo/bar/lore/ipsum";

		Integer expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, true, servletPath,
				defaultCacheTime);
		Assert.assertEquals(defaultCacheTime, expireAfterSeconds);

		cachingTimes.put("/**", oneMin);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, true, servletPath, defaultCacheTime);
		Assert.assertEquals(oneMin, expireAfterSeconds);

		cachingTimes.put(servletPath, oneMin);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, true, servletPath, defaultCacheTime);
		Assert.assertEquals(oneMin, expireAfterSeconds);

		cachingTimes.clear();
		cachingTimes.put("/foo/bar/lore/**", tenMins);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, true, servletPath, defaultCacheTime);
		Assert.assertEquals(tenMins, expireAfterSeconds);
		cachingTimes.clear();
		cachingTimes.put("/foo/**", oneHour);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, true, servletPath, defaultCacheTime);
		Assert.assertEquals(oneHour, expireAfterSeconds);
	}

}
