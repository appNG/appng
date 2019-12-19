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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.AttachmentWebservice;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.Webservice;
import org.appng.api.model.Application;
import org.appng.api.model.Authorizable;
import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.model.Subject;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.SiteClassLoader;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.model.RequestProcessor;
import org.appng.core.repository.config.HikariCPConfigurer;
import org.appng.core.service.InitializerServiceTest;
import org.appng.core.service.LdapService;
import org.appng.core.service.PropertySupport;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.xml.BaseObject;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationReference;
import org.appng.xml.platform.PlatformConfig;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

public class TestSupport {

	protected String host = "host.example.com";

	protected String manager = "manager";

	@Mock
	protected HttpServletRequest request;

	@Mock
	protected HttpServletResponse response;

	@Mock
	protected ServletConfig servletConfig;

	@Mock
	protected ServletContext ctx;

	@Mock
	protected ApplicationContext platformCtx;

	@Mock
	protected RequestDispatcher dispatcher;

	protected final Map<String, List<String>> requestParameters = new HashMap<>();

	protected final ByteArrayOutputStream out = new ByteArrayOutputStream();

	@Mock
	protected HttpSession httpSession;

	@Mock
	protected RequestProcessor requestProcessor;

	@Mock
	protected Environment environment;

	protected Properties platformProperties;

	protected Map<String, Site> siteMap;

	protected ConcurrentMap<String, Object> platformMap;

	protected TestApplicationProvider provider;

	private String sitePropPrefix = "platform.site.manager.";

	private List<Property> siteProperties = new ArrayList<>();

	protected String siteRoot;

	class TestApplicationProvider extends ApplicationProvider {

		private Map<String, Object> beans = new HashMap<>();

		public TestApplicationProvider(Site site, Application application) {
			super(site, application);
		}

		public void registerBean(String name, Object instance) {
			beans.put(name, instance);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getBean(String name, Class<T> clazz) {
			if (beans.containsKey(name)) {
				return (T) beans.get(name);
			}
			return super.getBean(name, clazz);
		}

		@Override
		public Object getBean(String name) {
			if (beans.containsKey(name)) {
				return beans.get(name);
			}
			return super.getBean(name);
		}

		@Override
		public boolean isFileBased() {
			return true;
		}

	}

	@Before
	public void setup() throws Exception {
		WritingXmlValidator.writeXml = false;
		Locale.setDefault(Locale.ENGLISH);
		MockitoAnnotations.initMocks(this);

		Mockito.when(servletConfig.getServletContext()).thenReturn(ctx);

		String rootPath = new File("").getAbsoluteFile().getAbsolutePath() + File.separator
				+ InitializerServiceTest.TARGET_TEST_CLASSES;
		Mockito.when(ctx.getRealPath("/")).thenReturn(rootPath);

		PrintWriter writer = new PrintWriter(out);
		Mockito.when(response.getWriter()).thenReturn(writer);

		ServletOutputStream sos = new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}

			public boolean isReady() {
				return true;
			}

			public void setWriteListener(WriteListener listener) {
			}
		};
		Mockito.when(response.getOutputStream()).thenReturn(sos);

		Mockito.when(request.getServerName()).thenReturn(host);
		Mockito.when(request.getMethod()).thenReturn("get");
		Mockito.when(request.getServletContext()).thenReturn(ctx);
		Mockito.when(request.getRequestDispatcher(Mockito.anyString())).thenReturn(dispatcher);

		String path = "../ait-wf-templates/templates/default";
		String absolutePath = new File(new File("").getAbsoluteFile(), path).getAbsolutePath();
		Mockito.when(ctx.getRealPath("/templates/default")).thenReturn(absolutePath);

		ConcurrentHashMap<String, Object> sessionMap = new ConcurrentHashMap<>();
		Mockito.when(request.getAttribute(Scope.REQUEST.name())).thenReturn(sessionMap);

		Mockito.when(request.getSession()).thenReturn(httpSession);
		Mockito.when(httpSession.getAttribute(Scope.SESSION.name())).thenReturn(sessionMap);
		Mockito.when(httpSession.getId()).thenReturn("ACOOLSESSIONID");

		Subject subject = getSubject();

		sessionMap.put(Scope.SESSION.name() + ".currentSubject", subject);

		Mockito.when(request.getParameterValues(Mockito.anyString())).thenAnswer(new Answer<String[]>() {
			public String[] answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				List<String> list = requestParameters.get(name);
				return list.toArray(new String[list.size()]);
			}
		});

		addGetParameter("xsl", "false");
		enableParameters(requestParameters.keySet().iterator());

		siteMap = new HashMap<>();
		SiteImpl site = new SiteImpl();
		site.setId(1);
		site.setHost(host);
		site.setName(manager);
		site.setSiteClassLoader(new SiteClassLoader(new URL[0], getClass().getClassLoader(), site.getName()));
		site.setState(SiteState.STARTED);
		site.setStartupTime(FastDateFormat.getInstance("yyyy-MM-dd").parse("2019-04-30"));

		ApplicationImpl application1 = new ApplicationImpl() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> T getBean(String name, Class<T> clazz) {
				if ("foo".equals(name) && Webservice.class.equals(clazz)) {
					return (T) new Webservice() {

						public String getContentType() {
							return HttpHeaders.CONTENT_TYPE_TEXT_PLAIN;
						}

						public byte[] processRequest(Site site, Application application, Environment environment,
								org.appng.api.Request request) throws BusinessException {
							return "webservice call".getBytes();
						}
					};
				} else if ("bar".equals(name) && Webservice.class.equals(clazz)) {
					return (T) new AttachmentWebservice() {

						public String getContentType() {
							return HttpHeaders.CONTENT_TYPE_TEXT_PLAIN;
						}

						public byte[] processRequest(Site site, Application application, Environment environment,
								org.appng.api.Request request) throws BusinessException {
							String servletPath = TestSupport.this.request.getServletPath();
							String foo = " " + servletPath.substring(servletPath.indexOf("/bar/") + 5);
							return ("attachment webservice call" + foo).getBytes();
						}

						public String getFileName() {
							String servletPath = TestSupport.this.request.getServletPath();
							String foo = servletPath.substring(servletPath.indexOf("/bar/") + 5);
							if (StringUtils.isBlank(foo)) {
								return null;
							}
							return "test.txt";
						}

						public boolean isAttachment() {
							return true;
						}
					};
				}
				return null;
			}
		};
		application1.setId(1);
		application1.setName("application1");
		application1.setDisplayName("application1");
		application1.setFileBased(false);
		application1.setProperties(new PropertyHolder());

		siteRoot = new File(site.getSiteClassLoader().getResource("repository/" + manager).getPath()).getAbsolutePath();

		addSiteProperty(SiteProperties.MANAGER_PATH, "/manager");
		addSiteProperty(SiteProperties.WWW_DIR, "/www");
		addSiteProperty(SiteProperties.ASSETS_DIR, "/assets");
		addSiteProperty(SiteProperties.DOCUMENT_DIR, "/de;/en");
		addSiteProperty(SiteProperties.ERROR_PAGE, "fehler");
		addSiteProperty(SiteProperties.ERROR_PAGES, "/de=fehler|/en=error");
		addSiteProperty(SiteProperties.DEFAULT_APPLICATION, "application1");
		addSiteProperty(SiteProperties.DEFAULT_PAGE, "index");
		addSiteProperty(SiteProperties.SITE_ROOT_DIR, siteRoot);
		addSiteProperty(SiteProperties.INDEX_TIMEOUT, "2000");
		addSiteProperty(SiteProperties.INDEX_DIR, "index");
		addSiteProperty(SiteProperties.TEMPLATE, "appng");
		addSiteProperty(SiteProperties.AUTH_APPLICATION, "appng-authentication");
		addSiteProperty(SiteProperties.AUTH_LOGIN_PAGE, "webform");
		addSiteProperty(SiteProperties.AUTH_LOGIN_REF, "webform");
		addSiteProperty(SiteProperties.DATASOURCE_CONFIGURER, HikariCPConfigurer.class.getName());
		addSiteProperty(LdapService.LDAP_PASSWORD, "secret");

		site.setProperties(new PropertyHolder(sitePropPrefix, siteProperties));
		siteMap.put(site.getName(), site);

		this.platformProperties = new PropertyHolder(PropertySupport.PREFIX_PLATFORM, new ArrayList<>());
		new PropertySupport((PropertyHolder) platformProperties).initPlatformConfig("target/root", true);
		platformMap = new ConcurrentHashMap<>();
		platformMap.put(Platform.Environment.SITES, siteMap);
		platformMap.put(Platform.Environment.PLATFORM_CONFIG, platformProperties);
		platformMap.put(Platform.Environment.CORE_PLATFORM_CONTEXT, platformCtx);

		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG))
				.thenReturn(platformProperties);

		provider = new TestApplicationProvider(site, application1) {
			@Override
			public ApplicationReference process(ApplicationRequest applicationRequest, MarshallService marshallService,
					Path pathInfo, PlatformConfig masterConfig) {
				ApplicationReference ApplicationReference = new ApplicationReference();
				ApplicationReference.setConfig(new ApplicationConfig());
				return ApplicationReference;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T> T getBean(Class<T> clazz) {
				if (clazz.equals(MessageSource.class)) {
					return (T) new ResourceBundleMessageSource();
				}
				return null;
			}

		};
		provider.setActive(true);
		provider.setFileBased(true);
		provider.setContext(Mockito.mock(ConfigurableApplicationContext.class));
		site.getSiteApplications().add(provider);

		Mockito.when(ctx.getAttribute(Scope.PLATFORM.name())).thenReturn(platformMap);
	}

	protected Subject getSubject() {
		SubjectImpl subject = new SubjectImpl() {

			@Override
			public boolean isAuthenticated() {
				return true;
			}

			@Override
			public boolean hasApplication(Application application) {
				return true;
			}

			@Override
			public boolean isAuthorized(Authorizable<?> authorizable) {
				return true;
			}
		};
		subject.setName("admin");
		subject.setRealname("godfather");
		subject.setLanguage("de");
		return subject;
	}

	public void addSiteProperty(String key, String value) {
		siteProperties.add(new PropertyImpl(sitePropPrefix + key, value));
	}

	private void addGetParameter(String name, String value) {
		List<String> list = new ArrayList<>();
		list.add(value);
		requestParameters.put(name, list);
	}

	private void enableParameters(final Iterator<String> it) {
		Mockito.when(request.getParameterNames()).thenReturn(new Enumeration<String>() {
			public String nextElement() {
				return it.next();
			}

			public boolean hasMoreElements() {
				return it.hasNext();
			}
		});
	}

	public void validateXml() throws Exception {
		String result = new String(out.toByteArray());
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String method = stackTrace[3].getMethodName();
		String substring = result.substring(result.indexOf("\n") + 1);
		WritingXmlValidator.validateXml(substring, "xml/" + method + ".xml");
	}

	public void validateXml(String result) throws Exception {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String method = stackTrace[2].getMethodName();
		WritingXmlValidator.validateXml(result, "xml/" + getClass().getSimpleName() + "-" + method + ".xml");
	}

	public void validateXml(BaseObject result, String namePrefix) throws Exception {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String method = stackTrace[2].getMethodName();
		WritingXmlValidator.validateXml(result, "xml/" + namePrefix + method + ".xml");
	}

}
