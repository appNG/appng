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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.io.output.NullOutputStream;
import org.appng.api.AttachmentWebservice;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.Webservice;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.handler.JspHandler;
import org.appng.core.controller.handler.MonitoringHandler;
import org.appng.core.controller.handler.RequestHandler;
import org.appng.core.model.RequestProcessor;
import org.appng.core.service.TemplateService;
import org.appng.tools.os.OperatingSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ControllerTest extends Controller {

	TestSupport base;
	DefaultEnvironment env;

	@Before
	public void initTest() throws Exception {
		base = new TestSupport();
		base.addSiteProperty(SiteProperties.SERVICE_PATH, "/services");
		base.setup();
		Mockito.when(base.request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("dummy");
		Mockito.when(base.platformCtx.getBean("requestProcessor", RequestProcessor.class))
				.thenReturn(base.requestProcessor);
		Mockito.when(base.platformCtx.getBean(TemplateService.class)).thenReturn(new TemplateService());
		ApplicationRequest applicationRequest = Mockito.mock(ApplicationRequest.class);
		Mockito.when(applicationRequest.getEnvironment()).thenReturn(Mockito.mock(DefaultEnvironment.class));
		base.provider.registerBean("request", applicationRequest);
		env = Mockito.spy(DefaultEnvironment.get(base.ctx));
		base.provider.registerBean("environment", (Environment) env);
		Mockito.when(base.ctx.getAttribute(PlatformStartup.APPNG_STARTED)).thenReturn(true);
	}

	@Test
	public void testAttachmentWebService() {
		when(base.request.getServletPath()).thenReturn("/services/manager/application1/webservice/foobar");
		try {
			AttachmentWebservice attachmentWebservice = getAttachmentWebservice("attachment webservice call",
					"test.txt");
			base.provider.registerBean("foobar", attachmentWebservice);
			doGet(base.request, base.response);
			Mockito.verify(base.response).setContentType(HttpHeaders.CONTENT_TYPE_TEXT_PLAIN);
			Mockito.verify(base.response).setHeader(Mockito.eq(HttpHeaders.CONTENT_DISPOSITION),
					Mockito.eq("attachment; filename=\"test.txt\""));
			Assert.assertEquals("attachment webservice call", new String(base.out.toByteArray()));
		} catch (Exception e) {
			fail(e);
		}
	}

	protected void fail(Exception e) {
		LOGGER.error("error during test", e);
		Assert.fail(e.getMessage());
	}

	@Test
	public void testAttachmentWebServiceNoFile() {
		when(base.request.getServletPath()).thenReturn("/services/manager/application1/webservice/foobar");
		try {
			AttachmentWebservice attachmentWebservice = getAttachmentWebservice("attachment webservice call",
					"test.txt");
			base.provider.registerBean("foobar", attachmentWebservice);
			doGet(base.request, base.response);
			Mockito.verify(base.response).setContentType(HttpHeaders.CONTENT_TYPE_TEXT_PLAIN);
			Assert.assertEquals("attachment webservice call", new String(base.out.toByteArray()));
		} catch (Exception e) {
			fail(e);
		}
	}

	private AttachmentWebservice getAttachmentWebservice(final String content, final String fileName) {
		return new AttachmentWebservice() {

			public byte[] processRequest(Site site, Application application, Environment environment, Request request)
					throws BusinessException {
				return content.getBytes();
			}

			public String getContentType() {
				return HttpHeaders.CONTENT_TYPE_TEXT_PLAIN;
			}

			public boolean isAttachment() {
				return true;
			}

			public String getFileName() {
				return fileName;
			}
		};
	}

	@Test
	public void testWebservice() {
		prepareWebserice(HttpMethod.GET);
		try {
			doGet(base.request, base.response);
			Assert.assertEquals("GET webservice call", new String(base.out.toByteArray()));
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testWebservicePut() {
		prepareWebserice(HttpMethod.PUT);
		try {
			doPut(base.request, base.response);
			Assert.assertEquals("PUT webservice call", new String(base.out.toByteArray()));
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testWebserviceDelete() {
		prepareWebserice(HttpMethod.DELETE);
		try {
			doDelete(base.request, base.response);
			Assert.assertEquals("DELETE webservice call", new String(base.out.toByteArray()));
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testMonitoring() {
		prepareMonitoring("/health");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Mockito.verify(base.response).setContentType(HttpHeaders.CONTENT_TYPE_APPLICATION_JSON);
			Assert.assertTrue(actual.contains("\"name\" : \"manager\""));
			Assert.assertTrue(actual.contains("\"state\" : \"STARTED\""));
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testMonitoringSystem() {
		prepareMonitoring("/health/system");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertTrue(actual.contains("java.runtime.name"));
			Assert.assertTrue(actual.contains("java.runtime.version"));
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testMonitoringEnv() {
		prepareMonitoring("/health/environment");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			if (OperatingSystem.isLinux()) {
				Assert.assertTrue(actual.contains("LANG"));
				Assert.assertTrue(actual.contains("USER"));
			} else if (OperatingSystem.isWindows()) {
				Assert.assertTrue(actual.contains("USERNAME"));
				Assert.assertTrue(actual.contains("Path"));
			}
		} catch (Exception e) {
			fail(e);
		}
	}

	private void prepareMonitoring(String path) {
		monitoringHandler = new MonitoringHandler();
		when(base.request.getServletPath()).thenReturn(path);
		Properties platformCfg = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String sharedSecret = platformCfg.getString(Platform.Property.SHARED_SECRET);
		String baseAuth = "Basic " + Base64.getEncoder().encodeToString(("monitoring:" + sharedSecret).getBytes());
		when(base.request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(baseAuth);
	}

	private void prepareWebserice(HttpMethod method) {
		when(base.request.getServletPath()).thenReturn("/services/manager/application1/webservice/foobar");
		when(base.request.getMethod()).thenReturn(method.name());
		base.provider.registerBean("foobar", new Webservice() {

			public byte[] processRequest(Site site, Application application, Environment environment, Request request)
					throws BusinessException {
				return (method.name() + " webservice call").getBytes();
			}

			public String getContentType() {
				return null;
			}
		});
	}

	@Test
	public void testStaticFromRepository() {
		when(base.request.getServletPath()).thenReturn("/assets/test.txt");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertEquals("/repository/manager/www/assets/test.txt", actual);
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testStatic() {
		when(base.request.getServletPath()).thenReturn("/test.txt");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertEquals("/test.txt", actual);
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testStaticPut() {
		when(base.request.getServletPath()).thenReturn("/test.txt");
		try {
			doPut(base.request, base.response);
			Assert.assertEquals(0, base.out.toByteArray().length);
			Mockito.verify(base.response).sendError(HttpStatus.FORBIDDEN.value());
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testStaticDelete() {
		when(base.request.getServletPath()).thenReturn("/test.txt");
		try {
			doDelete(base.request, base.response);
			Assert.assertEquals(0, base.out.toByteArray().length);
			Mockito.verify(base.response).sendError(HttpStatus.FORBIDDEN.value());
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testTemplate() {
		when(base.request.getServletPath()).thenReturn("/template/assets/test.txt");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertEquals("/repository/manager/www/template/assets/test.txt", actual);
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testTemplateWithTemplateInPath() {
		when(base.request.getServletPath()).thenReturn("/template/resources/template/test.txt");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertEquals("/repository/manager/www/template/resources/template/test.txt", actual);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testTemplateResourceFromApplication() {
		when(base.request.getServletPath()).thenReturn("/template_dummy-application/test.txt");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertEquals("/WEB-INF/cache/platform/manager/dummy-application/resources/test.txt", actual);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testDoc() {
		String rootPath = base.platformProperties.getString("platform.platformRootPath");
		String wwwRootPath = rootPath + File.separator + "repository" + File.separator + "manager" + File.separator
				+ "www";
		String realPath = new File(new File("").getAbsoluteFile(), wwwRootPath).getAbsolutePath();
		when(base.request.getServletPath()).thenReturn("/de/test");
		when(base.ctx.getRealPath("/repository/manager/www")).thenReturn(realPath);
		final String jspCalled = "JSP called";
		try {
			jspHandler = Mockito.mock(JspHandler.class);
			Answer<Void> jspAnswer = new Answer<Void>() {
				public Void answer(InvocationOnMock invocation) throws Throwable {
					HttpServletResponse httpServletResponse = (HttpServletResponse) invocation.getArguments()[1];
					httpServletResponse.getOutputStream().write(jspCalled.getBytes());
					return null;
				}
			};
			Mockito.doAnswer(jspAnswer).when(jspHandler).handle(isA(HttpServletRequest.class),
					isA(HttpServletResponse.class), isA(Environment.class), isA(Site.class), isA(PathInfo.class));

			doGet(base.request, base.response);
			Assert.assertEquals(jspCalled, new String(base.out.toByteArray()));
		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			Assert.fail(writer.toString());
		}
	}

	@Test
	public void testErrorPage() {
		when(base.request.getServletPath()).thenReturn("/errorpage");
		try {
			doGet(base.request, base.response);
			verify(base.request).getRequestDispatcher("/de/fehler.jsp");
			verify(base.request).setAttribute(RequestHandler.FORWARDED, Boolean.TRUE);
			verify(base.dispatcher).forward(base.request, base.response);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testErrorDoc() {
		when(base.request.getServletPath()).thenReturn("/errorpage");
		try {
			when(base.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).thenReturn("/de/foobar");
			doGet(base.request, base.response);
			verify(base.request).getRequestDispatcher("/de/fehler.jsp");
			verify(base.request).setAttribute(RequestHandler.FORWARDED, Boolean.TRUE);
			verify(base.dispatcher).forward(base.request, base.response);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRoot() {
		when(base.request.getServletPath()).thenReturn("");
		try {
			doGet(base.request, base.response);
			verify(base.response).setStatus(301);
			verify(base.response).setHeader(HttpHeaders.LOCATION, "/de/index");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRootWithPath() {
		when(base.request.getServletPath()).thenReturn("/de");
		try {
			doGet(base.request, base.response);
			verify(base.response).setStatus(301);
			verify(base.response).setHeader(HttpHeaders.LOCATION, "/de/index");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testDocNoSite() {
		when(base.request.getServerName()).thenReturn("localhost");
		when(base.request.getServletPath()).thenReturn("/de");
		try {
			doGet(base.request, base.response);
			verify(base.response).getStatus();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testJsp() throws ServletException {
		jspHandler = Mockito.mock(JspHandler.class);
		when(base.request.getServletPath()).thenReturn("/foo/bar.jsp");
		try {
			when(base.request.getAttribute(RequestHandler.FORWARDED)).thenReturn(Boolean.TRUE);
			doGet(base.request, base.response);
			verify(jspHandler).handle(Mockito.eq(base.request), Mockito.eq(base.response), Mockito.eq(env),
					Mockito.eq(base.siteMap.get("manager")), Mockito.any(PathInfo.class));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testStaticForSite() {
		when(base.request.getServletPath()).thenReturn("/de/test.pdf");
		try {
			doGet(base.request, base.response);
			Assert.assertEquals("/repository/manager/www/de/test.pdf", new String(base.out.toByteArray()));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testStaticNoSite() {
		when(base.request.getServerName()).thenReturn("localhost");
		when(base.request.getServletPath()).thenReturn("/repository/manager/www/de/test.txt");
		try {
			doGet(base.request, base.response);
			Assert.assertEquals("/repository/manager/www/de/test.txt", new String(base.out.toByteArray()));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHead() throws IOException, ServletException {
		when(base.request.getServletPath()).thenReturn("/test.txt");
		doHead(base.request, base.response);
		Assert.assertEquals(0, base.out.size());
	}

	@Test
	public void testApplication() throws InvalidConfigurationException {
		String queryString = "foo=bar";
		Mockito.when(base.request.getQueryString()).thenReturn(queryString);
		testApplication("/manager/manager/application1/page1");
		Mockito.verify(env).setAttribute(Scope.REQUEST, EnvironmentKeys.QUERY_STRING, queryString);
	}

	@Test
	public void testInvalidApplication() throws InvalidConfigurationException {
		testApplication("/manager/manager/pluin1/page1");
	}

	public void testApplication(String path) throws InvalidConfigurationException {
		when(base.requestProcessor.processWithTemplate(isA(Site.class), isA(File.class))).thenReturn("ok");
		when(base.request.getServletPath()).thenReturn(path);
		try {
			doGet(base.request, base.response);
			Mockito.verify(env).setAttribute(Scope.REQUEST, EnvironmentKeys.SERVLETPATH, path);
			Mockito.verify(env).setAttribute(Scope.REQUEST, EnvironmentKeys.BASE_URL, "/manager");
			String result = new String(base.out.toByteArray());
			Assert.assertEquals("ok" + System.getProperty("line.separator"), result);
			verify(base.requestProcessor).processWithTemplate(isA(Site.class), isA(File.class));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testNameBasedHost() {
		String siteName = RequestUtil.getSiteName(base.environment, base.request);
		Assert.assertEquals(base.host, siteName);
	}

	@Test
	public void testServerLocalName() {
		Mockito.when(base.request.getAttribute("SERVER_LOCAL_NAME")).thenReturn(base.host);
		String siteName = RequestUtil.getSiteName(base.environment, base.request);
		Assert.assertEquals(base.host, siteName);
		Assert.assertEquals(base.host, base.request.getServerName());
	}

	@Test
	public void testLoadingPage() {
		Mockito.when(base.ctx.getAttribute(PlatformStartup.APPNG_STARTED)).thenReturn(false);
		when(base.request.getServletPath()).thenReturn("/dummy");
		try {
			doGet(base.request, base.response);
			String actual = new String(base.out.toByteArray());
			Assert.assertEquals(new String(loadingScreen), actual);
		} catch (Exception e) {
			fail(e);
		} finally {
			Mockito.when(base.ctx.getAttribute(PlatformStartup.APPNG_STARTED)).thenReturn(true);
		}
	}

	@Override
	public void init() throws ServletException {
	}

	public HttpServletResponse wrapResponseForHeadRequest(HttpServletResponse response) {
		return new HttpServletResponseWrapper(response) {
			public ServletOutputStream getOutputStream() throws IOException {
				return new ServletOutputStream() {
					public void write(int arg0) throws IOException {
					}

					public boolean isReady() {
						return true;
					}

					public void setWriteListener(WriteListener listener) {
					}
				};
			}

			@Override
			public PrintWriter getWriter() throws IOException {
				return new PrintWriter(NullOutputStream.NULL_OUTPUT_STREAM);
			}
		};
	}

	@Override
	public void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content,
			String encoding) throws IOException, ServletException {
		response.getOutputStream().write(request.getServletPath().getBytes());
	}

	@Override
	public ServletContext getServletContext() {
		return base.ctx;
	}

	@Override
	protected Environment getEnvironment(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		return env;
	}

}
