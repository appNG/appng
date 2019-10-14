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

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.Cache;
import javax.cache.expiry.ExpiryPolicy;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.appng.api.model.Site;
import org.appng.core.controller.AppngCache;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import com.hazelcast.cache.ICache;

public class PageCacheFilterTest {

	@Test
	public void test() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest(new MockServletContext());
		req.setServletPath("/foo/bar");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		@SuppressWarnings("unchecked")
		ICache<String, AppngCache> cache = Mockito.mock(ICache.class);
		Mockito.when(cache.getName()).thenReturn("testcache");
		Mockito.when(cache.unwrap(ICache.class)).thenReturn(cache);
		FilterChain chain = Mockito.mock(FilterChain.class);
		String modifiedDate = "Wed, 28 Mar 2018 09:04:12 GMT";
		String content = "foobar";
		PageCacheFilter pageCacheFilter = new PageCacheFilter() {
			@Override
			protected AppngCache performRequest(final HttpServletRequest request, final HttpServletResponse response,
					final FilterChain chain, Site site, Cache<String, AppngCache> cache, ExpiryPolicy expiryPolicy)
					throws IOException, ServletException {
				chain.doFilter(request, response);
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.LAST_MODIFIED, modifiedDate);
				return new AppngCache("GET/" + req.getServletPath(), site, request, 200, "text/plain",
						content.getBytes(), headers, 1800);
			};

			@Override
			protected void writeResponse(HttpServletRequest request, HttpServletResponse response, AppngCache pageInfo)
					throws IOException {
				if ("/aborted".equals(request.getServletPath())) {
					throw new ClientAbortException("aborted!");
				}
				super.writeResponse(request, response, pageInfo);
			}
		};
		AtomicReference<AppngCache> actual = new AtomicReference<>();
		Mockito.doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				AppngCache element = invocation.getArgumentAt(1, AppngCache.class);
				actual.set(element);
				return null;
			}
		}).when(cache).put(Mockito.any(String.class), Mockito.any(AppngCache.class), Mockito.any(ExpiryPolicy.class));
		
		Site site = Mockito.mock(Site.class);

		AppngCache pageInfo = pageCacheFilter.getCacheElement(req, resp, chain, site, cache, null);
		Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.eq(resp));
		Assert.assertEquals(pageInfo, actual.get());
		long dateAsMillis = 1522227852000L;
		Assert.assertEquals(dateAsMillis, actual.get().getHeaders().getLastModified());

		// test gzip
		req.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
		pageCacheFilter.handleCaching(req, resp, site, chain, cache, null);
		Assert.assertEquals(HttpStatus.OK.value(), resp.getStatus());
		Assert.assertEquals(26, resp.getContentLength());
		Mockito.verify(chain, Mockito.times(2)).doFilter(Mockito.any(), Mockito.eq(resp));

		// test if-modified-since
		MockHttpServletResponse response = new MockHttpServletResponse();
		req.addHeader(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate);
		pageCacheFilter.handleCaching(req, response, site, chain, cache, null);
		Assert.assertEquals(HttpStatus.NOT_MODIFIED.value(), response.getStatus());
		Assert.assertEquals(0, response.getContentLength());
		Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.eq(response));

		// test aborted
		MockHttpServletRequest aborted = new MockHttpServletRequest(new MockServletContext());
		aborted.setServletPath("/aborted");
		MockHttpServletResponse abortedResponse = new MockHttpServletResponse();
		pageCacheFilter.handleCaching(aborted, abortedResponse, Mockito.mock(Site.class), chain, cache, null);
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
	public void testGetExpireAfterSeconds() {
		Integer defaultCacheTime = Integer.valueOf(1800);
		Integer oneMin = Integer.valueOf(60);
		Integer tenMins = Integer.valueOf(600);
		Integer oneHour = Integer.valueOf(3600);
		Properties cachingTimes = new Properties();
		String servletPath = "/foo/bar/lore/ipsum";
		
		Integer expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, servletPath, defaultCacheTime);
		Assert.assertEquals(defaultCacheTime, expireAfterSeconds);
		
		cachingTimes.put("", oneMin);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, servletPath, defaultCacheTime);
		Assert.assertEquals(oneMin, expireAfterSeconds);

		cachingTimes.put(servletPath, oneMin);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, servletPath, defaultCacheTime);
		Assert.assertEquals(oneMin, expireAfterSeconds);

		cachingTimes.clear();
		cachingTimes.put("/foo/bar/lore", tenMins);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, servletPath, defaultCacheTime);
		Assert.assertEquals(tenMins, expireAfterSeconds);

		cachingTimes.clear();
		cachingTimes.put("/foo", oneHour);
		expireAfterSeconds = PageCacheFilter.getExpireAfterSeconds(cachingTimes, servletPath, defaultCacheTime);
		Assert.assertEquals(oneHour, expireAfterSeconds);
	}

}
