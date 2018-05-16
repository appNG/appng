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
package org.appng.core.controller.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.bind.JAXBException;

import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.rest.model.Datasource;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.RequestSupportImpl;
import org.appng.core.controller.rest.RestPostProcessor.RestDataSource;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.appng.xml.MarshallService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

public class RestDataSourceTest extends RestOperationTest {

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

		InputStream is = getClass().getClassLoader().getResourceAsStream("rest/" + dataSourceId + ".xml");
		org.appng.xml.platform.Datasource originalDataSource = MarshallService.getMarshallService().unmarshall(is,
				org.appng.xml.platform.Datasource.class);
		Mockito.when(appconfig.getDatasource(dataSourceId)).thenReturn(originalDataSource);

		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setEnvironment(environment);
		Request request = new ApplicationRequest(formsRequest, new DummyPermissionProcessor(subject, site, application),
				requestSupport);

		Mockito.when(application.processDataSource(Mockito.eq(servletResponse), Mockito.eq(false), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(originalDataSource);

		ResponseEntity<Datasource> dataSource = new RestDataSource(site, application, request, true)
				.getDataSource(dataSourceId, new HashMap<>(), environment, servletRequest, servletResponse);
		WritingJsonValidator.validate(dataSource.getBody(), "rest/" + dataSourceId + ".json");
	}

}
