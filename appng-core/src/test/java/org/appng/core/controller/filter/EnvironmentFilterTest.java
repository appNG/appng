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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.controller.Session;
import org.appng.core.controller.SessionListener;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class EnvironmentFilterTest {

	private static ServletContext servletContext = new MockServletContext();
	private static Map<String, Object> platformMap;

	private static EnvironmentFilter environmentFilter;

	@BeforeClass
	public static void setup() {
		environmentFilter = new EnvironmentFilter();

		platformMap = new ConcurrentHashMap<>();

		Properties props = Mockito.mock(Properties.class);
		Mockito.when(props.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		Mockito.when(props.getClob(Platform.Property.SESSION_FILTER)).thenReturn("^.*test.*$\r\n\n^nomatch$");
		platformMap.put(Platform.Environment.PLATFORM_CONFIG, props);
		Map<String, Site> sitemap = new HashMap<>();
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getDomain()).thenReturn("http://localhost:8080");
		Mockito.when(site.getHost()).thenReturn("localhost");
		Mockito.when(site.getName()).thenReturn("localhost");
		Properties siteProps = Mockito.mock(Properties.class);
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(siteProps.getBoolean(SiteProperties.SESSION_TRACKING_ENABLED, false)).thenReturn(true);
		sitemap.put(site.getHost(), site);
		platformMap.put(Platform.Environment.SITES, sitemap);
		servletContext.setAttribute(Scope.PLATFORM.name(), platformMap);
	}

	@Test
	public void testSessionFilter() throws ServletException, IOException {
		MockHttpSession session = new MockHttpSession(servletContext);
		Session sessionMeta = new Session("4711");
		session.setAttribute(SessionListener.META_DATA, sessionMeta);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setSession(session);
		request.addHeader(HttpHeaders.USER_AGENT, "this is test");
		session.setNew(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		environmentFilter.doFilterInternal(request, response, new MockFilterChain());
		Assert.assertNotNull(EnvironmentFilter.environment());
		Assert.assertEquals(1, sessionMeta.getRequests());
		Assert.assertEquals("localhost", sessionMeta.getSite());
		Assert.assertEquals("http://localhost:8080", sessionMeta.getDomain());
		Assert.assertEquals("127.0.0.1", sessionMeta.getIp());
		Assert.assertEquals("this is test", sessionMeta.getUserAgent());
	}

	

}
