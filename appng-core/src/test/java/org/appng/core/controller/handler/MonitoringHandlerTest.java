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

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class MonitoringHandlerTest {

	private MockServletContext ctx = new MockServletContext();
	private MonitoringHandler monitoringHandler = new MonitoringHandler();

	@Test
	public void testSystem() throws Exception {
		SiteImpl site = getSite();
		PathInfo path = getPath(site, "/health/system");
		DefaultEnvironment env = getEnv();

		MockHttpServletResponse resp = new MockHttpServletResponse();
		monitoringHandler.handle(getRequest(ctx), resp, env, site, path);
		String content = resp.getContentAsString();

		Assert.assertTrue(content.contains("java.specification.name"));
		Assert.assertTrue(content.contains("java.specification.vendor"));
		Assert.assertTrue(content.contains("java.specification.version"));
	}

	@Test
	public void testEnvironment() throws Exception {
		SiteImpl site = getSite();
		PathInfo path = getPath(site, "/health/environment");
		DefaultEnvironment env = getEnv();

		MockHttpServletResponse resp = new MockHttpServletResponse();
		monitoringHandler.handle(getRequest(ctx), resp, env, site, path);
		String content = resp.getContentAsString();

		Assert.assertTrue(content.contains("JAVA_HOME"));
	}

	@Test
	public void test() throws Exception {
		SiteImpl site = getSite();
		PathInfo path = getPath(site, "/health");
		DefaultEnvironment env = getEnv();

		MockHttpServletResponse resp = new MockHttpServletResponse();
		MockHttpServletRequest req = getRequest(ctx);
		monitoringHandler.handle(req, resp, env, site, path);
		String responseBody = cleanResponse(resp);
		WritingJsonValidator.validate(responseBody, "rest/health.json");

		req = getRequest(ctx);
		req.addParameter("details", "true");
		resp = new MockHttpServletResponse();
		monitoringHandler.handle(req, resp, env, site, path);
		responseBody = cleanResponse(resp);
		WritingJsonValidator.validate(responseBody, "rest/health-detailed.json");
	}

	protected String cleanResponse(MockHttpServletResponse resp) throws UnsupportedEncodingException {
		return resp.getContentAsString().replaceAll("\\d{10}", "1204768206").replaceAll("node=\\[.*\\]:\\d+",
				"node=[127.0.0.1]:5702");
	}

	private PathInfo getPath(SiteImpl site, String servletPath) {
		PathInfo path = new PathInfo(site.getHost(), site.getDomain(), site.getName(), servletPath, "/manager",
				"/service", new ArrayList<>(), new ArrayList<>(), "/repository", ".jsp");
		return path;
	}

	private SiteImpl getSite() throws ParseException {
		SiteImpl site = new SiteImpl();
		site.setName("appng");
		site.setHost("localhost");
		site.setDomain("http://localhost:8080");
		site.setState(SiteState.STARTED);
		site.setStartupTime(FastDateFormat.getInstance("yyMMddHHmm").parse("8204300815"));
		site.setProperties(new PropertyHolder("",
				Arrays.asList(new SimpleProperty("foo", "bar"), new SimpleProperty("answer", "42"),
						new SimpleProperty("awesome", "true"), new SimpleProperty("decimal", "8.15"),
						new SimpleProperty("passWORD", "secret"))));
		Set<SiteApplication> applications = new HashSet<>();
		ApplicationImpl app = new ApplicationImpl();
		app.setName("acme");
		app.setApplicationVersion("42.0.1-RELEASE");
		app.setPrivileged(true);
		app.setContext(Mockito.mock(ConfigurableApplicationContext.class));
		Set<Resource> jars = new HashSet<>();
		Resource jar = Mockito.mock(Resource.class);
		jars.add(jar);
		Mockito.when(jar.getName()).thenReturn("acme.jar");
		Mockito.when(jar.getSize()).thenReturn(42);
		Resources resources = Mockito.mock(Resources.class);
		Mockito.when(resources.getResources(ResourceType.JAR)).thenReturn(jars);
		app.setResources(resources);
		applications.add(new SiteApplication(site, app));
		site.setSiteApplications(applications);
		return site;
	}

	private MockHttpServletRequest getRequest(MockServletContext ctx) {
		MockHttpServletRequest req = new MockHttpServletRequest(ctx);
		req.addHeader(HttpHeaders.AUTHORIZATION, "Basic bW9uaXRvcmluZzpudWxs");
		return req;
	}

	private DefaultEnvironment getEnv() {
		DefaultEnvironment env = DefaultEnvironment.get(ctx);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, new PropertyHolder());
		return env;
	}

}
