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
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.Cache;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.appng.api.model.Site;
import org.appng.core.controller.AppngCacheElement;
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

public class PageCacheFilterTest {

	@Test
	public void test() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest(new MockServletContext());
		req.setServletPath("/foo/bar");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		@SuppressWarnings("unchecked")
		Cache<String, AppngCacheElement> cache = Mockito.mock(Cache.class);
		Mockito.when(cache.getName()).thenReturn("testcache");
		FilterChain chain = Mockito.mock(FilterChain.class);
		String modifiedDate = "Wed, 28 Mar 2018 09:04:12 GMT";
		String content = "foobar";
		PageCacheFilter pageCacheFilter = new PageCacheFilter() {
			@Override
			protected AppngCacheElement buildPage(HttpServletRequest request,
					HttpServletResponse response, FilterChain chain, Cache<String, AppngCacheElement> blockingCache)
					throws ServletException, IOException {
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.LAST_MODIFIED, modifiedDate);
				return new AppngCacheElement(200, "text/plain", content.getBytes(), headers);
			};

			@Override
			protected void writeResponse(HttpServletRequest request, HttpServletResponse response, AppngCacheElement pageInfo)
					throws IOException {
				if ("/aborted".equals(request.getServletPath())) {
					throw new ClientAbortException("aborted!");
				}
				super.writeResponse(request, response, pageInfo);
			}
		};
		AtomicReference<AppngCacheElement> actual = new AtomicReference<>();
		Mockito.doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				AppngCacheElement element = invocation.getArgumentAt(1, AppngCacheElement.class);
				actual.set(element);
				return null;
			}
		}).when(cache).put(Mockito.any(),Mockito.any());
		AppngCacheElement pageInfo = pageCacheFilter.buildPageInfo(req, resp, chain, cache);

		Assert.assertEquals(pageInfo, actual.get());
		long dateAsMillis=1522227852000L;
		Assert.assertEquals(dateAsMillis, actual.get().getHeaders().getLastModified());
		pageCacheFilter.handleCaching(req, resp, Mockito.mock(Site.class), chain, cache);
		Assert.assertEquals(HttpStatus.OK.value(), resp.getStatus());
		Assert.assertEquals(content.length(), resp.getContentLength());

		MockHttpServletResponse response = new MockHttpServletResponse();
		req.addHeader(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate);
		pageCacheFilter.handleCaching(req, response, Mockito.mock(Site.class), chain, cache);
		Assert.assertEquals(HttpStatus.NOT_MODIFIED.value(), response.getStatus());
		Assert.assertEquals(0, response.getContentLength());

		MockHttpServletRequest aborted = new MockHttpServletRequest(new MockServletContext());
		aborted.setServletPath("/aborted");
		MockHttpServletResponse abortedResponse = new MockHttpServletResponse();
		pageCacheFilter.handleCaching(aborted, abortedResponse, Mockito.mock(Site.class), chain, cache);
		Assert.assertEquals(HttpStatus.OK.value(), abortedResponse.getStatus());
		Assert.assertEquals(0, abortedResponse.getContentLength());
	}

}
