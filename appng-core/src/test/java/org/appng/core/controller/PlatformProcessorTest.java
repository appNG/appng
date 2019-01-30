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

import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.appng.api.InvalidConfigurationException;
import org.appng.api.PathInfo;
import org.appng.api.PermissionProcessor;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.PathInfoTest;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.PlatformProcessor;
import org.appng.core.model.PlatformTransformer;
import org.appng.xml.MarshallService;
import org.appng.xml.transformation.StyleSheetProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class PlatformProcessorTest extends TestSupport {

	private static final int CONTENT_LENGTH = 2141;

	private PathInfo pathInfo = new PathInfo(PathInfoTest.HOST, PathInfoTest.DOMAIN, PathInfoTest.CURRENT_SITE,
			"/manager/manager/application1/page1/action/2", PathInfoTest.MANAGER_PATH, PathInfoTest.SERVICE_PATH,
			PathInfoTest.ASSETS_DIRS, PathInfoTest.DOCUMENT_DIRS, PathInfoTest.REPOSITORY, PathInfoTest.JSP);

	private PlatformProcessor mp = new PlatformProcessor();

	private ConcurrentMap<String, Object> sessionMap = new ConcurrentHashMap<String, Object>();

	@Mock
	private ApplicationRequest applicationRequest;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		Mockito.when(httpSession.getAttribute(Scope.SESSION.name())).thenReturn(sessionMap);
		Mockito.when(ctx.getAttribute(Scope.PLATFORM.name())).thenReturn(platformMap);
		URL resource = PlatformProcessorTest.class.getClassLoader().getResource("template/appng");
		String templatePath = resource.toURI().getPath();
		initRequest();
		DefaultEnvironment env = DefaultEnvironment.get(ctx, request, response);
		provider.registerBean("environment", env);
		Mockito.when(applicationRequest.getEnvironment()).thenReturn(env);
		provider.registerBean("request", applicationRequest);
		pathInfo.setApplicationName("application1");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		MarshallService marshallService = MarshallService.getMarshallService();
		TransformerFactory tf = TransformerFactory.newInstance();
		marshallService.setDocumentBuilderFactory(dbf);
		marshallService.setTransformerFactory(tf);
		marshallService.setCdataElements(new ArrayList<>());
		StyleSheetProvider styleSheetProvider = new StyleSheetProvider();
		styleSheetProvider.setDocumentBuilderFactory(dbf);
		styleSheetProvider.setTransformerFactory(tf);
		styleSheetProvider.init();
		PlatformTransformer platformTransformer = new PlatformTransformer();
		platformTransformer.setStyleSheetProvider(styleSheetProvider);
		mp.setPlatformTransformer(platformTransformer);
		mp.setMarshallService(marshallService);
		mp.init(request, response, pathInfo, templatePath);
	}

	@Test(expected = InvalidConfigurationException.class)
	public void testNotLoggedInNoDefaultApplication() throws Exception {
		try {
			mp.processWithTemplate(siteMap.get(manager));
		} catch (InvalidConfigurationException e) {
			Assert.assertEquals("application 'appng-authentication' not found for site 'manager'", e.getMessage());
			throw e;
		}
		Assert.fail();
	}

	@Test
	public void testTransform() throws Exception {
		SubjectImpl subject = new SubjectImpl();
		subject.setAuthenticated(true);
		Site site = siteMap.get(manager);
		Application application = site.getApplication("application1");
		PermissionProcessor dummyPermissionProcessor = new DummyPermissionProcessor(subject, site, application);
		Mockito.when(applicationRequest.getPermissionProcessor()).thenReturn(dummyPermissionProcessor);

		sessionMap.put(Session.Environment.SUBJECT, subject);
		platformMap.put(Platform.Environment.APPNG_VERSION, "42-Final");
		String result = mp.processWithTemplate(site);
		Assert.assertEquals(Integer.valueOf(CONTENT_LENGTH), mp.getContentLength());
		validateXml(result);
	}

	@Test
	public void testTransformError() throws Exception {
		SubjectImpl subject = new SubjectImpl();
		subject.setAuthenticated(true);
		Site site = siteMap.get(manager);
		Application application = site.getApplication("application1");
		PermissionProcessor dummyPermissionProcessor = new DummyPermissionProcessor(subject, site, application);
		Mockito.when(applicationRequest.getPermissionProcessor()).thenReturn(dummyPermissionProcessor);

		sessionMap.put(Session.Environment.SUBJECT, subject);
		platformMap.put(Platform.Environment.APPNG_VERSION, "42-Final");
		mp.setPlatformTransformer(null);

		String result = mp.processWithTemplate(site);
		Assert.assertNotNull(mp.getContentLength());
		Assert.assertEquals("text/html", mp.getContentType());
		Assert.assertTrue(result.contains("<h2>500 - Internal Server Error</h2>"));
		Assert.assertTrue(result.contains("Site: manager<br/>"));
		Assert.assertTrue(result.contains("Application: application1<br/>"));
		Assert.assertTrue(result.contains("Template: appng<br/>Thread: main<br/>"));
		Assert.assertTrue(result.contains("<h3>Stacktrace</h3>"));
		Assert.assertTrue(result.contains("<pre>java.lang.NullPointerException"));
		Assert.assertTrue(result.contains("<h3>XML</h3>"));
		Mockito.verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	@Test
	public void testLoggedIn() throws Exception {
		SubjectImpl subject = new SubjectImpl();
		subject.setAuthenticated(true);
		Site site = siteMap.get(manager);
		Application application = site.getApplication("application1");
		PermissionProcessor dummyPermissionProcessor = new DummyPermissionProcessor(subject, site, application);
		Mockito.when(applicationRequest.getPermissionProcessor()).thenReturn(dummyPermissionProcessor);

		sessionMap.put(Session.Environment.SUBJECT, subject);
		platformMap.put(Platform.Environment.APPNG_VERSION, "42-Final");
		String result = mp.processWithTemplate(site);
		Assert.assertEquals(Integer.valueOf(CONTENT_LENGTH), mp.getContentLength());
		validateXml(result);
	}

	private void initRequest() {
		ConcurrentMap<String, Object> reqMap = new ConcurrentHashMap<String, Object>();
		reqMap.put("doXsl", true);
		reqMap.put("showXsl", false);
		Mockito.when(request.getAttribute(Scope.REQUEST.name())).thenReturn(reqMap);
		Mockito.when(request.getMethod()).thenReturn("GET");
		Mockito.when(request.getServerName()).thenReturn(host);
		Mockito.when(request.getSession()).thenReturn(httpSession);
	}

}
