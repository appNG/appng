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
package org.appng.core.controller.rest;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.TimeZone;

import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.support.ApplicationRequest;
import org.appng.core.model.ApplicationProvider;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RestOperationTest {

	static {
		WritingJsonValidator.writeJson = false;
	}

	@Mock
	Environment environment;
	@Mock
	Site site;
	@Mock
	ApplicationProvider application;
	@Mock
	Subject subject;
	@Mock
	ApplicationRequest request;
	@Mock
	MessageSource messageSource;
	@Mock
	ApplicationConfigProvider appconfig;
	@Mock
	Properties siteProps;
	@Mock
	org.appng.forms.Request formsRequest;
	MockHttpServletResponse servletResponse = new MockHttpServletResponse();
	MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(environment.getSubject()).thenReturn(subject);
		Mockito.when(environment.getLocale()).thenReturn(Locale.GERMANY);
		Mockito.when(environment.getTimeZone()).thenReturn(TimeZone.getDefault());
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(site.getName()).thenReturn("site");
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0], getClass().getClassLoader()));
		Mockito.when(siteProps.getString(SiteProperties.MANAGER_PATH)).thenReturn("/manager");
		Mockito.when(siteProps.getString(SiteProperties.SERVICE_PATH)).thenReturn("/service");
		Mockito.when(application.getName()).thenReturn("application");
		Mockito.when(application.getApplicationConfig()).thenReturn(appconfig);
		Mockito.when(request.getEnvironment()).thenReturn(environment);
		Mockito.when(request.getLocale()).thenReturn(Locale.GERMANY);
	}

	@Test
	public void testHandleException() throws Exception {
		HttpStatus iAmATeapot = HttpStatus.I_AM_A_TEAPOT;
		servletResponse.setStatus(iAmATeapot.value());
		RestOperation.RestErrorHandler restErrorHandler = new RestOperation.RestErrorHandler();

		Mockito.when(application.getProperties()).thenReturn(siteProps);
		Mockito.when(siteProps.getBoolean(Mockito.any(), Mockito.any())).thenReturn(true);
		ResponseEntity<ErrorModel> handleError = restErrorHandler.handleError(new IOException("BOOOM!"), site,
				application, environment, servletRequest, servletResponse);

		Assert.assertEquals(iAmATeapot, handleError.getStatusCode());
		Assert.assertEquals(iAmATeapot, HttpStatus.valueOf(handleError.getBody().getCode()));
		String[] stackTrace = handleError.getBody().getMessage().split(System.lineSeparator());
		Assert.assertEquals("java.io.IOException: BOOOM!", stackTrace[0]);
		Assert.assertEquals(
				"	at org.appng.core.controller.rest.RestOperationTest.testHandleException(RestOperationTest.java:98)",
				stackTrace[1]);
	}

}
