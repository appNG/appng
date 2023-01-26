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
package org.appng.core.controller.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.VHostMode;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.environment.DefaultEnvironment;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

public class GuiHandlerTest {

	private MockServletContext servletContext = new MockServletContext();
	private MockHttpSession session = new MockHttpSession(servletContext);
	private MockHttpServletRequest servletRequest = new MockHttpServletRequest(servletContext);
	private MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	@Mock
	private Site site;

	@Mock
	private Application application;

	@Mock
	private Application applicationB;

	@Mock
	private Properties platformProperties;

	@Mock
	private Properties siteProperties;

	@Mock
	private Subject subject;

	@Test
	public void testRedirectToFirstVisibleApplication() throws ServletException, IOException {
		servletRequest.setSession(session);
		MockitoAnnotations.initMocks(this);
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE))
				.thenReturn(VHostMode.NAME_BASED.name());
		Mockito.when(platformProperties.getString(Platform.Property.TEMPLATE_FOLDER)).thenReturn("template");
		Environment initialEnv = DefaultEnvironment.get(servletContext);
		initialEnv.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, platformProperties);
		initialEnv.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, new HashMap<>());

		DefaultEnvironment env = DefaultEnvironment.get(servletRequest, servletResponse);
		Mockito.when(siteProperties.getString(SiteProperties.TEMPLATE, null)).thenReturn("appng");
		Mockito.when(siteProperties.getString(SiteProperties.DEFAULT_APPLICATION)).thenReturn("manager");
		Mockito.when(siteProperties.getString(SiteProperties.MANAGER_PATH)).thenReturn("/manager");

		Mockito.when(site.getName()).thenReturn("localhost");
		Mockito.when(site.getProperties()).thenReturn(siteProperties);
		Set<Application> applications = new HashSet<>();
		applications.add(applicationB);
		applications.add(application);
		Mockito.when(site.getApplications()).thenReturn(applications);
		Mockito.when(application.getName()).thenReturn("someapp");
		Mockito.when(applicationB.isHidden()).thenReturn(true);
		Mockito.when(subject.hasApplication(application)).thenReturn(true);
		Mockito.when(subject.isAuthenticated()).thenReturn(true);
		env.setSubject(subject);
		Map<String, Site> siteMap = new HashMap<>();
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);
		PathInfo pathInfo = new PathInfo("localhost", "http://localhost", "manager", "/gui", "/gui", "/service",
				Arrays.asList("/assets"), Arrays.asList("/de"), "/repository", "jsp");
		new GuiHandler(null).handle(servletRequest, servletResponse, env, site, pathInfo);
		Mockito.verify(site).sendRedirect(env, "/manager/localhost/someapp");
	}
}
