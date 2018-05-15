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
package org.appng.api.rest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.rest.model.Datasource;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.RequestSupportImpl;
import org.appng.core.model.ApplicationProvider;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.appng.xml.MarshallService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RestDataSourceTest {

	static {
		WritingJsonValidator.writeJson = false;
	}

	@Test
	public void testResultSet() throws Exception {
		runTest("datasource-resultset");
	}

	@Test
	public void testResult() throws Exception {
		runTest("datasource-result");
	}

	protected void runTest(String dataSourceId)
			throws InvalidConfigurationException, ProcessingException, JAXBException, IOException {
		Environment environment = Mockito.mock(Environment.class);
		org.appng.forms.Request formsRequest = Mockito.mock(org.appng.forms.Request.class);
		Site site = Mockito.mock(Site.class);
		Properties siteProps = Mockito.mock(Properties.class);
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(site.getName()).thenReturn("site");
		Mockito.when(siteProps.getString(SiteProperties.MANAGER_PATH)).thenReturn("/manager");
		Mockito.when(siteProps.getString(SiteProperties.SERVICE_PATH)).thenReturn("/service");
		ApplicationProvider application = Mockito.mock(ApplicationProvider.class);
		Mockito.when(application.getName()).thenReturn("application");
		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setEnvironment(environment);
		Subject subject = Mockito.mock(Subject.class);
		Request request = new ApplicationRequest(formsRequest, new DummyPermissionProcessor(subject, site, application),
				requestSupport);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();

		Mockito.when(environment.getSubject()).thenReturn(subject);
		Mockito.when(environment.getLocale()).thenReturn(Locale.getDefault());
		Mockito.when(environment.getTimeZone()).thenReturn(TimeZone.getDefault());
		InputStream is = getClass().getClassLoader().getResourceAsStream(dataSourceId + ".xml");
		Mockito.when(application.processDataSource(Mockito.eq(servletResponse), Mockito.eq(false), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(
						MarshallService.getMarshallService().unmarshall(is, org.appng.xml.platform.Datasource.class));

		ResponseEntity<Datasource> dataSource = new MyRestDataSource(site, application, request)
				.getDataSource(dataSourceId, environment, servletRequest, servletResponse);
		WritingJsonValidator.validate(dataSource.getBody(), dataSourceId + ".json");
	}

	class MyRestDataSource extends RestDataSource {

		public MyRestDataSource(Site site, Application application, Request request) {
			super(site, application, request);
		}

	}
}
