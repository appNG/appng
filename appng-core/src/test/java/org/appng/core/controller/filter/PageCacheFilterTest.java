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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.model.Site;
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

import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;

public class PageCacheFilterTest {

	@Test
	public void test() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest(new MockServletContext());
		req.setServletPath("/foo/bar");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		BlockingCache cache = Mockito.mock(BlockingCache.class);
		Mockito.when(cache.getName()).thenReturn("testcache");
		FilterChain chain = Mockito.mock(FilterChain.class);
		String modifiedDate = "Wed, 28 Mar 2018 09:04:12 GMT";
		String content = "foobar";
		PageCacheFilter pageCacheFilter = new PageCacheFilter() {
			protected net.sf.ehcache.constructs.web.PageInfo buildPage(HttpServletRequest request,
					HttpServletResponse response, FilterChain chain, BlockingCache blockingCache)
					throws net.sf.ehcache.constructs.web.AlreadyGzippedException, Exception {
				List<Header<? extends Serializable>> headers = new ArrayList<>();
				headers.add(new Header<String>(HttpHeaders.LAST_MODIFIED, modifiedDate));
				return new PageInfo(200, "text/plain", new ArrayList<>(), content.getBytes(), false, 1800, headers);
			};
		};
		AtomicReference<PageInfo> actual = new AtomicReference<>();
		Mockito.doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Element element = invocation.getArgumentAt(0, Element.class);
				actual.set((PageInfo) element.getObjectValue());
				return null;
			}
		}).when(cache).put(Mockito.any(Element.class));
		PageInfo pageInfo = pageCacheFilter.buildPageInfo(req, resp, chain, cache);

		Assert.assertEquals(pageInfo, actual.get());
		Assert.assertEquals(modifiedDate, actual.get().getHeaders().stream()
				.filter(h -> h.getName().equals(HttpHeaders.LAST_MODIFIED)).findFirst().get().getValue());
		pageCacheFilter.handleCaching(req, resp, Mockito.mock(Site.class), chain, cache);
		Assert.assertEquals(HttpStatus.OK.value(), resp.getStatus());
		Assert.assertEquals(content.length(), resp.getContentLength());

		MockHttpServletResponse response = new MockHttpServletResponse();
		req.addHeader(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate);
		pageCacheFilter.handleCaching(req, response, Mockito.mock(Site.class), chain, cache);
		Assert.assertEquals(HttpStatus.NOT_MODIFIED.value(), response.getStatus());
		Assert.assertEquals(0, response.getContentLength());
	}

}
