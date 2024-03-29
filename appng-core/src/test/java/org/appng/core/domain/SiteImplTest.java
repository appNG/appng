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
package org.appng.core.domain;

import static org.appng.api.Scope.REQUEST;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Properties;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.controller.messaging.NodeEvent;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class SiteImplTest {

	@Mock
	private DefaultEnvironment environment;

	@Mock
	private Properties properties;

	@Mock
	private Path path;

	private SiteClassLoader siteClassLoader;
	private boolean isClosed = false;
	private SiteImpl site;

	private MockHttpServletResponse response = new MockHttpServletResponse();
	private MockHttpServletRequest request = new MockHttpServletRequest();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(environment.getServletResponse()).thenReturn(response);
		Mockito.when(environment.getServletRequest()).thenReturn(request);
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO)).thenReturn(path);
		Mockito.when(environment.getAttribute(REQUEST, HttpHeaders.USER_AGENT)).thenReturn("Mozilla");
		Mockito.when(path.getGuiPath()).thenReturn("/ws");
		Mockito.when(properties.getBoolean(SiteProperties.APPEND_TAB_ID, false)).thenReturn(true);
		site = new SiteImpl() {
			@Override
			public Properties getProperties() {
				return properties;
			}
		};
		site.setName("foo");
		siteClassLoader = new SiteClassLoader(new URL[0], getClass().getClassLoader(), site.getName()) {
			public void close() throws IOException {
				isClosed = true;
			}
		};
		site.setSiteClassLoader(siteClassLoader);
	}

	@Test
	public void testSiteInternalRedirect() {
		site.sendRedirect(environment, "page/foo/bar#anchor", HttpServletResponse.SC_MOVED_TEMPORARILY);
		Assert.assertEquals("/ws/foo/page/foo/bar#anchor", response.getHeader(HttpHeaders.LOCATION));
		Assert.assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
	}

	@Test
	public void testSiteInternalRedirectMSIE() {
		Mockito.when(environment.getAttribute(REQUEST, HttpHeaders.USER_AGENT)).thenReturn("MSIE");
		site.sendRedirect(environment, "page/foo/bar#anchor", HttpServletResponse.SC_MOVED_TEMPORARILY);
		Assert.assertEquals("/ws/foo/page/foo/bar?tab=anchor#anchor", response.getHeader(HttpHeaders.LOCATION));
		Assert.assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
	}

	@Test
	public void testExternalRedirect() {
		site.sendRedirect(environment, "/some/uri", HttpServletResponse.SC_MOVED_TEMPORARILY);
		Assert.assertEquals("/some/uri", response.getHeader(HttpHeaders.LOCATION));
		Assert.assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
	}

	@Test
	public void testSetSiteState() {
		Mockito.when(environment.getAttribute(Scope.PLATFORM, NodeEvent.NODE_STATE))
				.thenReturn(new HashMap<String, NodeState>());
		Properties platformCfg = Mockito.mock(Properties.class);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG)).thenReturn(platformCfg);
		Mockito.when(platformCfg.getInteger("setSiteStateStartedDelay", 10)).thenReturn(10);
		String nodeId = Messaging.getNodeId();
		NodeState nodeState = NodeEvent.clusterState(environment, nodeId).get(nodeId);
		Map<String, SiteState> stateMap = nodeState.getSiteStates();
		site.setState(SiteState.STARTED, environment);
		Assert.assertEquals(SiteState.STARTED, stateMap.get(site.getName()));
		site.setState(SiteState.DELETED, environment);
		Assert.assertNull(stateMap.get(site.getName()));
	}

	@Test
	public void testCloseClassloader() throws IOException {
		site.setSiteApplications(new HashSet<>());
		site.closeSiteContext();
		try {
			URLClassLoader.class.getMethod("close");
			Assert.assertTrue("siteclassloader should be closed", isClosed);
		} catch (NoSuchMethodException e) {
			// no java 7
			Assert.assertFalse("siteclassloader should not be closed", isClosed);
		}
	}

}
