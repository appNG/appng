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
package org.appng.core.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;

public class SessionListenerTest {

	private static ServletContext servletContext = new MockServletContext();
	private MockHttpSession session1 = new MockHttpSession(servletContext, "ZUS383883OTOTOLSKKL");
	private MockHttpSession session2 = new MockHttpSession(servletContext, "ERTERTZGFHFGHGFH234");
	private static Map<String, Object> platformMap;

	private static SessionListener sessionListener;

	@BeforeClass
	public static void setup() {
		CacheManager.create(SessionListenerTest.class.getClassLoader().getResourceAsStream("WEB-INF/conf/ehcache.xml"));
		sessionListener = new SessionListener();
		sessionListener.contextInitialized(new ServletContextEvent(servletContext));
		Assert.assertEquals(Status.STATUS_ALIVE, SessionListener.getSessionCache().getStatus());

		platformMap = new ConcurrentHashMap<>();
		Properties props = Mockito.mock(Properties.class);
		Mockito.when(props.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		platformMap.put(Platform.Environment.PLATFORM_CONFIG, props);
		Map<String, Site> sitemap = new HashMap<>();
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getDomain()).thenReturn("http://localhost:8080");
		Mockito.when(site.getHost()).thenReturn("localhost");
		Mockito.when(site.getProperties()).thenReturn(Mockito.mock(Properties.class));
		sitemap.put(site.getHost(), site);
		platformMap.put(Platform.Environment.SITES, sitemap);
		servletContext.setAttribute(Scope.PLATFORM.name(), platformMap);
	}

	@AfterClass
	public static void tearDown() {
		Assert.assertNotNull(SessionListener.getSessions());
		sessionListener.contextDestroyed(new ServletContextEvent(servletContext));
		Assert.assertEquals(Status.STATUS_SHUTDOWN, SessionListener.getSessionCache().getStatus());
	}

	@Test
	public void testSessionCreated() {
		sessionListener.sessionCreated(new HttpSessionEvent(session1));
		addRequest(session1);
		Assert.assertTrue(getSessions().contains(new Session(session1.getId())));
	}

	@Test
	public void testSessionDestroyed() {
		sessionListener.sessionCreated(new HttpSessionEvent(session1));
		addRequest(session1);
		sessionListener.sessionCreated(new HttpSessionEvent(session2));
		addRequest(session2);
		sessionListener.sessionDestroyed(new HttpSessionEvent(session1));
		List<Session> sessionList = getSessions();
		Assert.assertFalse(sessionList.contains(new Session(session1.getId())));
		Assert.assertTrue(sessionList.contains(new Session(session2.getId())));
	}

	private void addRequest(HttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		ServletRequestEvent requestEvent = new ServletRequestEvent(servletContext, request);
		request.setSession(session);
		sessionListener.requestInitialized(requestEvent);
	}

	private List<Session> getSessions() {
		return SessionListener.getSessions();
	}
}
