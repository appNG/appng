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
package org.appng.core.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

public class SessionListenerTest {

	private static ServletContext servletContext = new MockServletContext();
	private MockHttpSession session1 = new MockHttpSession(servletContext, "ZUS383883OTOTOLSKKL");
	private MockHttpSession session2 = new MockHttpSession(servletContext, "ERTERTZGFHFGHGFH234");
	private static Map<String, Object> platformMap;

	private static SessionListener sessionListener;

	@BeforeClass
	public static void setup() {
		sessionListener = new SessionListener();

		platformMap = new ConcurrentHashMap<>();

		Properties props = Mockito.mock(Properties.class);
		Mockito.when(props.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		Mockito.when(props.getClob(Platform.Property.SESSION_FILTER)).thenReturn(".*test.*");
		platformMap.put(Platform.Environment.PLATFORM_CONFIG, props);
		Map<String, Site> sitemap = new HashMap<>();
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getDomain()).thenReturn("http://localhost:8080");
		Mockito.when(site.getHost()).thenReturn("localhost");
		Properties siteProps = Mockito.mock(Properties.class);
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(siteProps.getBoolean(SiteProperties.SESSION_TRACKING_ENABLED, false)).thenReturn(true);
		sitemap.put(site.getHost(), site);
		platformMap.put(Platform.Environment.SITES, sitemap);
		servletContext.setAttribute(Scope.PLATFORM.name(), platformMap);
	}

	@Test
	public void testSessionCreated() {
		sessionListener.sessionCreated(new HttpSessionEvent(session1));
		addRequest(session1);
		addRequest(session1);
		Assert.assertEquals(2, ((Session) session1.getAttribute(SessionListener.META_DATA)).getRequests());
	}

	@Test
	public void testSessionDestroyed() {
		sessionListener.sessionCreated(new HttpSessionEvent(session1));
		addRequest(session1);
		sessionListener.sessionCreated(new HttpSessionEvent(session2));
		addRequest(session2);
		sessionListener.sessionDestroyed(new HttpSessionEvent(session1));

		Assert.assertNull(session1.getAttribute(SessionListener.META_DATA));
		Assert.assertNotNull(session2.getAttribute(SessionListener.META_DATA));
	}

	@Test
	public void testSessionFilter() {
		MockHttpSession session = new MockHttpSession(servletContext);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setSession(session);
		request.addHeader(HttpHeaders.USER_AGENT, "this is test");
		session.setNew(true); // This has to be called after request.setSession
		ServletRequestEvent requestEvent = new ServletRequestEvent(servletContext, request);
		sessionListener.requestDestroyed(requestEvent);
		Assert.assertTrue(session.isInvalid());
	}

	private void addRequest(HttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		ServletRequestEvent requestEvent = new ServletRequestEvent(servletContext, request);
		request.setSession(session);
		sessionListener.requestInitialized(requestEvent);
	}

}
