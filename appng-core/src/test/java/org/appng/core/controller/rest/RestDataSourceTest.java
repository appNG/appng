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
import java.util.Optional;

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
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.junit.Assert;
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

		ResponseEntity<Datasource> dataSource = new RestDataSource(site, application, request, messageSource, true)
				.getDataSource(dataSourceId, new HashMap<>(), environment, servletRequest, servletResponse);
		WritingJsonValidator.validate(dataSource.getBody(), "rest/" + dataSourceId + ".json");
	}

	@Test
	public void testGetChildField() throws JAXBException {
		RestOperation restOperation = new RestDataSource(site, application, request, messageSource, true);

		Datafield listObject = new Datafield();
		listObject.setName("items");
		listObject.setType(FieldType.LIST_OBJECT);

		Datafield object0 = new Datafield();
		object0.setName("items[0]");
		object0.setType(FieldType.OBJECT);
		listObject.getFields().add(object0);

		Datafield text0 = new Datafield();
		text0.setName("name");
		text0.setType(FieldType.TEXT);
		text0.setValue("foo");
		object0.getFields().add(text0);

		Datafield object1 = new Datafield();
		object1.setName("items[1]");
		object1.setType(FieldType.OBJECT);
		listObject.getFields().add(object1);

		Datafield text1 = new Datafield();
		text1.setName("name");
		text1.setType(FieldType.TEXT);
		text1.setValue("foo");
		object1.getFields().add(text1);

		FieldDef listObjectField = new FieldDef();
		listObjectField.setType(FieldType.OBJECT);
		listObjectField.setName("items");
		listObjectField.setBinding("items");

		FieldDef objectField = new FieldDef();
		objectField.setType(FieldType.OBJECT);
		objectField.setName("items[]");
		objectField.setBinding("items[]");
		listObjectField.getFields().add(objectField);

		FieldDef childField = new FieldDef();
		childField.setType(FieldType.TEXT);
		childField.setName("name");
		childField.setBinding("items[].name");
		objectField.getFields().add(childField);

		Optional<FieldDef> foundChild = restOperation.getChildField(listObjectField, listObject, 0, object0);
		Assert.assertEquals(objectField, foundChild.get());

		foundChild = restOperation.getChildField(objectField, object0, 0, text0);
		Assert.assertEquals(childField, foundChild.get());

		foundChild = restOperation.getChildField(listObjectField, listObject, 1, object1);
		Assert.assertEquals(objectField, foundChild.get());

		foundChild = restOperation.getChildField(objectField, object1, 1, text1);
		Assert.assertEquals(childField, foundChild.get());
	}

}
