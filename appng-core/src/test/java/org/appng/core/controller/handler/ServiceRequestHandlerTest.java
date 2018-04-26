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
package org.appng.core.controller.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.SoapService;
import org.appng.api.VHostMode;
import org.appng.api.Webservice;
import org.appng.api.model.Application;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.RequestSupportImpl;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.AccessibleApplication;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.model.PlatformTransformer;
import org.appng.core.service.PropertySupport;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.Messages;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

public class ServiceRequestHandlerTest extends ServiceRequestHandler {

	private static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

	private SiteImpl site;

	@Mock
	private AccessibleApplication application;

	@Mock
	private DefaultEnvironment environment;
	private MockServletContext servletContext = new MockServletContext();
	private MockHttpServletRequest servletRequest = new MockHttpServletRequest(servletContext);
	private MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	public ServiceRequestHandlerTest() throws JAXBException {
		super(MarshallService.getMarshallService(), new PlatformTransformer());
	}

	@Test
	public void testSoap() throws Exception {
		String servletPath = "/services/site1/appng-demoapplication/soap/personService/personService.wsdl";
		setup(servletPath);
		List<String> emptyList = new ArrayList<String>();
		PathInfo pathInfo = new PathInfo("localhost", "localhost", "localhost", servletPath, "/ws", "/services",
				emptyList, emptyList, "", "");
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO)).thenReturn(pathInfo);
		handle(servletRequest, servletResponse, environment, site, pathInfo);
		Assert.assertEquals("[SOAP-Call] site1 appng-demoapplication", servletResponse.getContentAsString());
	}

	@Test
	public void testRest() throws Exception {
		handleRestCall(3, 4, "{\"operation\":\"add\",\"result\":7}", MediaType.APPLICATION_JSON_UTF8_VALUE,
				HttpStatus.OK);
	}

	@Test
	public void testRestWrongURL() throws Exception {
		handleRestCall("", null, HttpStatus.NOT_FOUND, "/services/site1/appng-demoapplication/rest/notfound");
	}

	@Test
	public void testRestHandleBusinessException() throws Exception {
		handleRestCall(11, 47, "{\"message\":\"BOOOM!\"}", MediaType.APPLICATION_JSON_UTF8_VALUE,
				HttpStatus.METHOD_NOT_ALLOWED);
	}

	@Test
	public void testRestHandleNullPointerException() throws Exception {
		handleRestCall(47, 12, "{\"message\":\"NPE\"}", MediaType.APPLICATION_JSON_UTF8_VALUE,
				HttpStatus.I_AM_A_TEAPOT);
	}

	@Test
	public void testRestHandleIllegalArgumentException() throws Exception {
		handleRestCall(-1, 12, "", null, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private void handleRestCall(int a, int b, String content, String contentType, HttpStatus status)
			throws JAXBException, IOException {
		String servletPath = String.format("/services/site1/appng-demoapplication/rest/add/%s/%s", a, b);
		handleRestCall(content, contentType, status, servletPath);
	}

	private void handleRestCall(String content, String contentType, HttpStatus status, String servletPath)
			throws JAXBException, IOException, UnsupportedEncodingException {
		BeanFactoryPostProcessor beanFactoryPostProcessor = new BeanFactoryPostProcessor() {
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				beanFactory.registerSingleton("foobarRest", new FoobarRest());
				beanFactory.registerSingleton("jsonConverter", new MappingJackson2HttpMessageConverter());
			}
		};
		ConfigurableApplicationContext ac = new GenericApplicationContext();
		ac.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
		ac.refresh();
		setup(servletPath, ac);
		servletRequest.setMethod(HttpMethod.GET.name());
		List<String> emptyList = new ArrayList<String>();
		PathInfo pathInfo = new PathInfo("localhost", "localhost", "localhost", servletPath, "/ws", "/services",
				emptyList, emptyList, "", "");
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO)).thenReturn(pathInfo);
		handle(servletRequest, servletResponse, environment, site, pathInfo);

		Assert.assertEquals(content, servletResponse.getContentAsString());
		Assert.assertEquals(contentType, servletResponse.getContentType());
		Assert.assertEquals(status.value(), servletResponse.getStatus());
	}

	@RestController
	@ControllerAdvice
	static class FoobarRest extends ResponseEntityExceptionHandler {

		@RequestMapping(value = "/add/{a}/{b}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
		public ResponseEntity<Result> add(@PathVariable("a") Integer a, @PathVariable("b") Integer b)
				throws BusinessException {
			if (a < 0) {
				throw new IllegalArgumentException("IAE");
			}
			if (a == 47) {
				throw new NullPointerException("NPE");
			}
			if (b == 47) {
				throw new BusinessException("BOOOM!");
			}
			return new ResponseEntity<Result>(new Result("add", a + b), HttpStatus.OK);
		}

		@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
		@ExceptionHandler(BusinessException.class)
		public Error handleBusinessException(Environment environment, Site site, Application application,
				HttpServletRequest request, HttpServletResponse response, Exception e) {
			return new Error(e.getMessage());
		}

		@ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
		@ExceptionHandler(NullPointerException.class)
		public Error handleNullPointerException(Exception e) {
			return new Error(e.getMessage());
		}

		static class Error {
			final String message;

			public Error(String message) {
				this.message = message;
			}

			public String getMessage() {
				return message;
			}
		}

		class Result {
			String operation;
			Integer result;

			public Result(String operation, Integer result) {
				this.operation = operation;
				this.result = result;
			}

			public String getOperation() {
				return operation;
			}

			public void setOperation(String operation) {
				this.operation = operation;
			}

			public Integer getResult() {
				return result;
			}

			public void setResult(Integer result) {
				this.result = result;
			}

		}
	}

	@Test
	public void testDataSource() throws Exception {
		String trimmed = getDatasource(FORMAT_XML);
		Assert.assertEquals(XML_PREFIX + "<datasource xmlns=\"" + MarshallService.NS_PLATFORM + "\" id=\"sites\"/>",
				trimmed);
		Assert.assertEquals(HttpHeaders.CONTENT_TYPE_TEXT_XML, servletResponse.getContentType());
	}

	@Test
	public void testDataSourceJson() throws Exception {
		String trimmed = getDatasource(FORMAT_JSON);
		Assert.assertEquals("{  \"datasource\" : {    \"id\" : \"sites\"  }}", trimmed);
		Assert.assertEquals(HttpHeaders.CONTENT_TYPE_APPLICATION_JSON, servletResponse.getContentType());
	}

	@Test
	public void testDataSourceUnsupportedMediaType() throws Exception {
		String trimmed = getDatasource("undefined");
		Assert.assertEquals("", trimmed);
		Assert.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), servletResponse.getStatus());
	}

	protected String getDatasource(String format) throws JAXBException, IOException, UnsupportedEncodingException {
		String servletPath = "/services/localhost/appng-demoapplication/datasource/" + format + "/sites";
		PathInfo pathInfo = setupPath(servletPath);
		handle(servletRequest, servletResponse, environment, site, pathInfo);
		return servletResponse.getContentAsString().replaceAll("\r", "").replaceAll("\n", "");
	}

	@Test
	public void testActionUnsupportedMediaType() throws Exception {
		String servletPath = "/services/localhost/appng-demoapplication/action/foobar/siteEvent/create";
		PathInfo setupPath = setupPath(servletPath);
		handle(servletRequest, servletResponse, environment, site, setupPath);
		Assert.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), servletResponse.getStatus());
	}

	@Test
	public void testAction() throws Exception {
		String trimmed = getAction(FORMAT_XML);
		Assert.assertEquals(XML_PREFIX + "<action xmlns=\"" + MarshallService.NS_PLATFORM
				+ "\" id=\"create\" eventId=\"siteEvent\"><messages ref=\"create\">"
				+ "<message ref=\"create\">Action-Call</message></messages></action>", trimmed);
		Assert.assertEquals(HttpHeaders.CONTENT_TYPE_TEXT_XML, servletResponse.getContentType());
	}

	@Test
	public void testActionJson() throws Exception {
		String trimmed = getAction(FORMAT_JSON);
		Assert.assertEquals(
				"{\"action\" : "
						+ "{\"messages\" : {\"messageList\" : [ {\"content\" : \"Action-Call\",\"ref\" : \"create\"} ],"
						+ "\"ref\" : \"create\"},\"id\" : \"create\",\"eventId\" : \"siteEvent\",\"async\" : \"false\"}}",
				trimmed);
		Assert.assertEquals(HttpHeaders.CONTENT_TYPE_APPLICATION_JSON, servletResponse.getContentType());
	}

	protected String getAction(String format) throws JAXBException, IOException, UnsupportedEncodingException {
		String servletPath = "/services/localhost/appng-demoapplication/action/" + format + "/siteEvent/create";
		PathInfo pathInfo = setupPath(servletPath);
		handle(servletRequest, servletResponse, environment, site, pathInfo);
		return servletResponse.getContentAsString().replaceAll("\r", "").replaceAll("\n", "").replaceAll("  ", "");
	}

	protected PathInfo setupPath(String servletPath) throws JAXBException {
		setup(servletPath);
		List<String> emptyList = new ArrayList<>();
		return new PathInfo("localhost", "localhost", "site1", servletPath, "/ws", "/services", emptyList, emptyList,
				"", "");
	}

	@Test
	public void testWebservice() throws Exception {
		String servletPath = "/services/localhost/appng-demoapplication/webservice/personService";
		PathInfo pathInfo = setupPath(servletPath);
		handle(servletRequest, servletResponse, environment, site, pathInfo);
		Assert.assertEquals("Webservice-Call", servletResponse.getContentAsString());
		Assert.assertEquals(HttpHeaders.CONTENT_TYPE_TEXT_PLAIN, servletResponse.getContentType());
		Assert.assertEquals(HttpStatus.ACCEPTED.value(), servletResponse.getStatus());
	}

	@Test
	public void testError() throws Exception {
		String servletPath = "/services/foo/bar?jin=fizz";
		PathInfo pathInfo = setupPath(servletPath);
		handle(servletRequest, servletResponse, environment, site, pathInfo);
		Assert.assertEquals("an error occured", servletResponse.getContentAsString());
		Assert.assertEquals(HttpHeaders.CONTENT_TYPE_TEXT_PLAIN, servletResponse.getContentType());
		Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), servletResponse.getStatus());
	}

	private void setup(String servletPath) throws JAXBException {
		setup(servletPath, null);
	}

	private void setup(String servletPath, ConfigurableApplicationContext ac) throws JAXBException {
		MockitoAnnotations.initMocks(this);

		servletRequest.setServletPath(servletPath);
		servletRequest.setRequestURI(servletPath);
		servletResponse.setWriterAccessAllowed(true);
		List<Property> platformProps = new ArrayList<Property>();
		platformProps.add(new PropertyImpl(PropertySupport.PREFIX_PLATFORM + Platform.Property.VHOST_MODE,
				VHostMode.NAME_BASED.name()));
		platformProps
				.add(new PropertyImpl(PropertySupport.PREFIX_PLATFORM + Platform.Property.MAX_UPLOAD_SIZE, "10000"));
		PropertyHolder properties = new PropertyHolder(PropertySupport.PREFIX_PLATFORM, platformProps);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG))
				.thenReturn(properties);
		Mockito.when(environment.getSubject()).thenReturn(new SubjectImpl());
		Mockito.when(environment.getLocale()).thenReturn(Locale.getDefault());
		Mockito.when(application.getName()).thenReturn("appng-demoapplication");
		Mockito.when(application.getBean("environment")).thenReturn(environment);
		Mockito.when(application.getBean(MarshallService.class)).thenReturn(MarshallService.getMarshallService());

		Mockito.when(application.getBean("personService", org.appng.api.SoapService.class))
				.thenReturn(getSoapService());

		Mockito.when(application.getBean("personService", org.appng.api.Webservice.class)).thenReturn(new Webservice() {

			public byte[] processRequest(Site site, Application application, Environment environment, Request request)
					throws BusinessException {
				return "Webservice-Call".getBytes();
			}

			public String getContentType() {
				return HttpHeaders.CONTENT_TYPE_TEXT_PLAIN;
			}

			@Override
			public int getStatus() {
				return HttpStatus.ACCEPTED.value();
			}
		});

		ApplicationRequest applicationRequest = new ApplicationRequest();
		applicationRequest.setRequestSupport(new RequestSupportImpl(null, environment, new StaticMessageSource()));
		Mockito.when(application.getBean("request", ApplicationRequest.class)).thenReturn(applicationRequest);
		PropertyHolder applicationProps = new PropertyHolder("",
				Arrays.asList(new SimpleProperty("permissionsEnabled", Boolean.TRUE.toString())));
		Mockito.when(application.getProperties()).thenReturn(applicationProps);
		site = new SiteImpl();
		site.setHost("localhost");
		site.setName("localhost");
		site.setDomain("localhost");

		ApplicationProvider applicationProvider = new ApplicationProvider(site, application) {

			@Override
			public ConfigurableApplicationContext getContext() {
				return ac;
			}

			@Override
			public Datasource processDataSource(HttpServletResponse servletResponse, boolean applyPermissionsOnRef,
					ApplicationRequest applicationRequest, String dataSourceId, MarshallService marshallService)
					throws InvalidConfigurationException, ProcessingException {
				Datasource datasource = new Datasource();
				datasource.setId(dataSourceId);
				return datasource;
			}

			@Override
			public Action processAction(HttpServletResponse servletResponse, boolean applyPermissionsOnRef,
					ApplicationRequest applicationRequest, String actionId, String eventId,
					MarshallService marshallService) throws InvalidConfigurationException, ProcessingException {
				Messages messages = new Messages();
				messages.setRef(actionId);
				Message m = new Message();
				m.setRef(actionId);
				m.setContent("Action-Call");
				messages.getMessageList().add(m);
				Action action = new Action();
				action.setId(actionId);
				action.setEventId(eventId);
				action.setMessages(messages);
				return action;
			}

		};
		site.getSiteApplications().add(applicationProvider);
		site.setProperties(new PropertyHolder());
		Map<String, Site> siteMap = new HashMap<String, Site>();
		siteMap.put(site.getName(), site);
		SiteImpl site1 = new SiteImpl();
		site1.setHost("site1");
		site1.setName("site1");
		site1.setDomain("site1");
		site1.getSiteApplications().add(applicationProvider);
		site1.setProperties(new PropertyHolder());
		siteMap.put(site1.getName(), site1);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES)).thenReturn(siteMap);
	}

	private SoapService getSoapService() {
		return new SoapService() {

			public void setSite(Site site) {

			}

			public void setApplication(Application application) {

			}

			public void setEnvironment(Environment environment) {

			}

			public String getSchemaLocation() {
				return "test.xsd";
			}

			public String getContextPath() {
				return "";
			}
		};
	}

	@Override
	protected void handleSoap(Site site, AccessibleApplication application, Environment environment,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		servletResponse.getWriter().write("[SOAP-Call] " + site.getName() + " " + application.getName());
	}

}
